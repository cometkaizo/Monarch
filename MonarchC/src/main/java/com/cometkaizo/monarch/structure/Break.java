package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.WrongEnvironmentErr;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.NoPrint;

public class Break {
    public static class Parser extends Structure.Parser<Raw> {
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            if (!ctx.literal("break")) return fail();
            ctx.whitespace();
            if (!ctx.literal(";")) return fail();
            ctx.whitespace();

            return success(raw);
        }
    }
    public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements ExprConsumer {
        @NoPrint public final Breakable returnable;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            var returnable = ancestors.ofType(Breakable.class);
            if (returnable.isPresent()) {
                this.returnable = returnable.get();
            } else {
                this.returnable = null;
                ctx.report(new WrongEnvironmentErr("break statements", "breakable"), this);
            }
        }

        @Override
        public void assemble(AssembleContext ctx) {
            this.returnable.assembleBreak(ctx);
        }
    }
}
