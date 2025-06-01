package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.bytecode.Chunk;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class MallocSet {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any valueParsers = new Any();
        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("&!")) return failExpecting("'&!'");
            ctx.whitespace();

            // value to set to the new pointer
            var value = valueParsers.parse(ctx);
            if (!value.hasValue()) return failExpecting("initializer expression");
            raw.value = value.valueNonNull();

            ctx.popStructure();
            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var valueParserName = ctx.word();
                if (valueParserName == null) return false;
                valueParsers.add(valueParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();
            return false;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
        public Structure.Raw<?> value;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr, ExprConsumer {
        public final Expr value;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);

            Expr value = null;
            if (raw.value.analyze(ctx) instanceof Expr expr) {
                if (!expr.isVoid()) {
                    value = expr;
                } else ctx.report(new WrongTypeErr("allocation initializer", "expression"), this);
            } else ctx.report(new WrongTypeErr("allocation initializer", "expression"), this);
            this.value = value;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            Chunk c = ctx.data();

            // malloc a new ptr
            c.opPush(value.footprint().ptrAmt());
            c.opPush(value.footprint().byteAmt());
            c.opMalloc();

            // p:location
            c.opCopy(Size.ZERO, Size.ONE_PTR);

            // ?:value
            value.assemble(ctx);

            // 1:byte_size,1:ptr_size
            c.opPush(value.footprint().ptrAmt());
            c.opPush(value.footprint().byteAmt());

            // set the value to the ptr
            c.opMSet();

            ctx.stackSize().subtract(value.footprint());
            ctx.stackSize().add(Size.ONE_PTR);
        }

        @Override
        public Type type() {
            return new Type.Ref(value.type());
        }
    }
}
