package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Int32Lit {
    public static boolean isTypeOf(Expr expr) {
        return expr != null && Analysis.TYPE.equals(expr.type());
    }
    public static class Parser extends Structure.Parser<Raw> {
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            Integer value = ctx.integer();
            if (value == null) return failExpecting("integer");
            if (!(ctx.literal("i32") || ctx.literal("I32"))) return failExpecting("'i32' or 'I32'");
            raw.value = value;
            ctx.whitespace();

            return success(raw);
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public int value;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr, StaticExpr<Integer> {
        public static final Type TYPE = new Type.Static("int32", new Size(Integer.BYTES, 0));
        public final int value;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.value = raw.value;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            ctx.data().opPushAll(value >> 8 * 3, value >> 8 * 2, value >> 8, value);
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return TYPE;
        }

        @Override
        public Integer staticEvaluate() {
            return value;
        }
    }
}
