package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.monarch.structure.diagnostic.UnknownTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;

public class StaticTypeGet {
    public static class Parser extends TypeGet.Parser<Raw> {
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            raw.name = ctx.word();
            if (raw.name == null) return fail();
            ctx.whitespace();

            return success(raw);
        }
    }
    public static class Raw extends TypeGet.Raw<Analysis> {
        public String name;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends TypeGet.Analysis {
        private final String name;
        public final Type.Static type;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.name = raw.name;

            Type.Static type = null;
            var typeOpt = ctx.getType(name);
            if (typeOpt.isPresent()) {
                if (typeOpt.get() instanceof Type.Static typeStatic) type = typeStatic;
            } else ctx.report(new UnknownTypeErr(name), this);
            this.type = type;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Type.Static type() {
            return type;
        }
    }
}
