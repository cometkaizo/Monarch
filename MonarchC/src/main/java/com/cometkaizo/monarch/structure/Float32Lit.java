package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.NumberFormatErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Float32Lit {
    public static boolean isTypeOf(Expr expr) {
        return expr != null && Analysis.TYPE.equals(expr.type());
    }
    public static class Parser extends Structure.Parser<Raw> {
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            Double value = ctx.decimal();
            if (value == null) {
                if (ctx.literal("NaN")) value = Double.NaN;
                else return failExpecting("floating point value");
            }
            if (!(ctx.literal("f32") || ctx.literal("F32"))) return failExpecting("'f32' or 'F32'");
            if (!isFloat(value)) {
                ctx.report(new NumberFormatErr(value, "float32"));
                return fail();
            }
            raw.value = value.floatValue();
            ctx.whitespace();

            return success(raw);
        }

        private boolean isFloat(Double value) {
            return value.isNaN() || -Float.MAX_VALUE <= value && value <= Float.MAX_VALUE;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public float value;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr, StaticExpr<Float> {
        public static final Type TYPE = new Type.Static("float32", new Size(Float.BYTES, 0));
        public final float value;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.value = raw.value;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            int bits = Float.floatToIntBits(value);
            ctx.data().opPushAll(bits >> 8 * 3, bits >> 8 * 2, bits >> 8, bits);
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return TYPE;
        }

        @Override
        public Float staticEvaluate() {
            return value;
        }
    }
}
