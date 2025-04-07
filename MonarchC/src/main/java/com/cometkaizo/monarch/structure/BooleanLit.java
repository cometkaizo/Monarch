package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class BooleanLit {
    public static class Parser extends Structure.Parser<Raw> {
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            var valueStr = ctx.word();
            if ("true".equals(valueStr)) raw.value = true;
            else if ("false".equals(valueStr)) raw.value = false;
            else return fail();

            ctx.whitespace();

            return success(raw);
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public boolean value;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr {
        public static final Type TYPE = new Type.Static("boolean", new Size(1, 0));
        public final boolean value;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.value = raw.value;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            ctx.data().opPush(value ? 0x01 : 0x00);
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return TYPE;
        }
    }
}
