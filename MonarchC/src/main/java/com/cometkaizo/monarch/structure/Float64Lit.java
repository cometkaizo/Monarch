package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.NumberFormatErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Float64Lit {
    public static boolean isTypeOf(Expr expr) {
        return expr != null && Analysis.TYPE.equals(expr.type());
    }
    public static class Parser extends Structure.Parser<Raw> {
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            Double value = ctx.decimal();
            if (value == null) return failExpecting("floating point value");
            if (!(ctx.literal("f64") || ctx.literal("F64"))) return failExpecting("'f64' or 'F64'");
            if (!isFloat(value)) {
                ctx.report(new NumberFormatErr(value, "float64"));
                return fail();
            }
            raw.value = value;
            ctx.whitespace();

            return success(raw);
        }

        private boolean isFloat(Double value) {
            return -Float.MAX_VALUE <= value && value <= Float.MAX_VALUE;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public double value;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr, StaticExpr<Double> {
        public static final Type TYPE = new Type.Static("float64", new Size(Double.BYTES, 0));
        public final double value;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.value = raw.value;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            long bits = Double.doubleToLongBits(value);
            int firstHalf = (int)(bits >> 8 * 4), secondHalf = (int)bits;
            ctx.data().opPushAll(firstHalf >> 8 * 3, firstHalf >> 8 * 2, firstHalf >> 8, firstHalf,
                    secondHalf >> 8 * 3, secondHalf >> 8 * 2, secondHalf >> 8, secondHalf);
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return TYPE;
        }

        @Override
        public Double staticEvaluate() {
            return value;
        }
    }
}
