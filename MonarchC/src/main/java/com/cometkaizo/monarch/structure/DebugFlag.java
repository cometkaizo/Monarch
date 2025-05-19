package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class DebugFlag {
    public static class Parser extends Structure.Parser<Raw> {
        @Override
        protected Result parseImpl(ParseContext ctx) {
            if (!ctx.literal("debugFlag")) return failExpecting("'debugFlag'");
            ctx.whitespace();
            if (!ctx.literal(";")) return fail("';'");
            ctx.whitespace();
            return success(new Raw());
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        @Override
        public Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis {
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
        }

        @Override
        public void assemble(AssembleContext ctx) {
            ctx.data().opDebugFlag();
        }
    }
}
