package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Time {
    public static class Parser extends Structure.Parser<Raw> {
        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = new Raw();

            if (!ctx.literal("time")) return failExpecting("'time'");
            ctx.whitespace();

            return success(raw);
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr {
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
        }
        @Override
        public void assemble(AssembleContext ctx) {
            ctx.data().opTime();
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return ByteLit.Analysis.TYPE;
        }
    }
}
