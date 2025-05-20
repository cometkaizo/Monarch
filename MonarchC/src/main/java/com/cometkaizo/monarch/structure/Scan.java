package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Scan {
    public static class Parser extends Structure.Parser<Raw> {
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var parent = ctx.topStructure();
            var raw = new Raw();

            if (!ctx.literal("scan")) return failExpecting("'scan'");
            ctx.whitespace();

            // only require ; if this is not used as an expression
            if (!(parent instanceof ExprConsumer)) {
                if (!ctx.literal(";")) return failExpecting("';'");
                ctx.whitespace();
            }

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
            ctx.data().opScan();
            if (returnValueUsed()) ctx.stackSize().add(footprint());
            else ctx.data().opPop();
        }

        @Override
        public Type type() {
            return ByteLit.Analysis.TYPE;
        }

        private boolean returnValueUsed() {
            return parent instanceof ExprConsumer;
        }
    }
}
