package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.IncompatibleTypesErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Set {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any refParsers = new Any();
        private final Any valueParsers = new Any();

        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            var ref = refParsers.parse(ctx);
            if (!ref.hasValue()) return failExpecting("target");
            raw.ref = ref.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal("=")) return failExpecting("'='");
            ctx.whitespace();

            var value = valueParsers.parse(ctx);
            if (!value.hasValue()) return failExpecting("expression");
            raw.value = value.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal(";")) return failExpecting("';'");
            ctx.whitespace();

            ctx.popStructure();
            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            Any parser;
            if (ctx.literal("targets")) parser = refParsers;
            else if (ctx.literal("values")) parser = valueParsers;
            else return false;
            ctx.whitespace();

            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var valueParserName = ctx.word();
                if (valueParserName == null) return false;
                parser.add(valueParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();
            return false;
        }

    }
    public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
        public Structure.Raw<?> ref, value;

        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements ExprConsumer {
        public final Locatable ref;
        public final Expr value;
        public final Type type;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);

            Locatable ref = null;
            Expr value = null;
            Type type = null;

            if (raw.ref.analyze(ctx) instanceof Locatable expr) {
                ref = expr;
            } else ctx.report(new WrongTypeErr("assignment target", "locatable value"), this);

            if (raw.value.analyze(ctx) instanceof Expr expr) {
                if (!expr.isVoid()) value = expr;
                else ctx.report(new WrongTypeErr("value", "expression"), this);
            } else ctx.report(new WrongTypeErr("value", "expression"), this);

            if (ref != null && value != null) {
                if (isCompatible(ref, value)) type = ref.typeAtLocation();
                else ctx.report(new IncompatibleTypesErr(value.type(), ref.typeAtLocation()), this);
            }

            this.ref = ref;
            this.value = value;
            this.type = type;

            ctx.popStructure();
        }

        private boolean isCompatible(Locatable ref, Expr value) {
            Type left = ref.typeAtLocation();
            Type right = value.type();
            return left != null && left.equals(right) ||
                    left instanceof Type.Ref(boolean targetTypeKnown, Type targetType) &&
                            (!targetTypeKnown || targetType.equals(right));
        }

        @Override
        public void assemble(AssembleContext ctx) {
            // p:location
            ref.assembleLocation(ctx);
            // ?:value
            value.assemble(ctx);

            // 1:byte_size,1:ptr_size
            ctx.data().opPush(value.footprint().ptrAmt());
            ctx.data().opPush(value.footprint().byteAmt());

            // set the value to the pointer
            ctx.data().opMSet();
            ctx.stackSize().subtract(value.footprint());
            ctx.stackSize().subtract(Size.ONE_PTR);
        }
    }
}
