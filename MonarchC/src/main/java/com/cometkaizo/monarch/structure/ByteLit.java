package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.NumberFormatErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class ByteLit {
    public static class Parser extends Structure.Parser<Raw> {
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            Integer value = ctx.integer();
            if (value == null) return fail();
            if (!(ctx.literal("b") || ctx.literal("B"))) return fail();
            if (!isByte(value)) {
                ctx.report(new NumberFormatErr(value, "byte"));
                return fail();
            }
            raw.value = value.byteValue();
            ctx.whitespace();

            return success(raw);
        }

        private boolean isByte(Integer value) {
            return value >= 0 && value < 256;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public byte value;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr, StaticExpr<Byte> {
        public static final Type TYPE = new Type.Static("byte", Size.ONE_BYTE);
        public final byte value;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.value = raw.value;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            ctx.data().opPush(value);
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return TYPE;
        }

        @Override
        public Byte staticEvaluate() {
            return value;
        }
    }
}
