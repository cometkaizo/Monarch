package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Int64Lit {
    public static boolean isTypeOf(Expr expr) {
        return expr != null && Analysis.TYPE.equals(expr.type());
    }
    public static class Parser extends Structure.Parser<Raw> {
        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = new Raw();

            Long value = ctx.longInteger();
            if (value == null) return failExpecting("integer");
            if (!(ctx.literal("i64") || ctx.literal("I64"))) return failExpecting("'i64' or 'I64'");
            raw.value = value;
            ctx.whitespace();

            return success(raw);
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public long value;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr, StaticExpr<Long> {
        public static final Type TYPE = new Type.Static("int64", new Size(Integer.BYTES, 0));
        public final long value;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.value = raw.value;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            int firstHalf = (int)(value >> 8 * 4), secondHalf = (int) value;
            ctx.data().opPushAll(firstHalf >> 8 * 3, firstHalf >> 8 * 2, firstHalf >> 8, firstHalf,
                    secondHalf >> 8 * 3, secondHalf >> 8 * 2, secondHalf >> 8, secondHalf);
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return TYPE;
        }

        @Override
        public Long staticEvaluate() {
            return value;
        }
    }
}
