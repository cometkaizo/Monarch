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

public class Deref {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any refParsers = new Any();
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("*")) return failExpecting("'*'");
            ctx.whitespace();

            // ref
            var ref = refParsers.parse(ctx);
            if (!ref.hasValue()) return failExpecting("expression");
            raw.ref = ref.valueNonNull();

            ctx.popStructure();
            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var refParserName = ctx.word();
                if (refParserName == null) return false;
                refParsers.add(refParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();
            return false;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
        public Structure.Raw<?> ref;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr, ExprConsumer, Locatable {
        public final Expr ref;
        public final Type type;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);

            Expr ref = null;
            Type type = null;
            if (raw.ref.analyze(ctx) instanceof Expr expr) {
                if (!expr.isVoid()) {
                    if (expr.footprint().equals(Size.ONE_PTR)) {
                        ref = expr;
                        type = ref.type() instanceof Type.Ref refType ?
                                refType.targetType() :
                                ByteLit.Analysis.TYPE;
                    } else ctx.report(new WrongTypeErr("dereference parameter (" + expr.typeName() + ")", "one-pointer expression"), this);
                } else ctx.report(new WrongTypeErr("dereference parameter", "expression"), this);
            } else ctx.report(new WrongTypeErr("dereference parameter", "expression"), this);
            this.ref = ref;
            this.type = type;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            Chunk c = ctx.data();

            ref.assemble(ctx); // will push 1 ptr onto stack

            Size targetFootprint = type.footprint();

            c.opPush(targetFootprint.ptrAmt());
            c.opPush(targetFootprint.byteAmt());
            c.opMGet();

            ctx.stackSize().subtract(ref.footprint());
            ctx.stackSize().add(targetFootprint);
        }

        @Override
        public Type type() {
            return ref == null ? null :
                    ref.type() instanceof Type.Ref refType ?
                            refType.targetType() :
                            ByteLit.Analysis.TYPE;
        }

        @Override
        public void assembleLocation(AssembleContext ctx) {
            ref.assemble(ctx);
        }

        @Override
        public Type typeAtLocation() {
            return type;
        }
    }
}
