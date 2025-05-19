package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class RefTypeGet {
    public static class Parser extends TypeGet.Parser<Raw> {
        private final Any targetParsers = new Any();
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            if (!ctx.literal("&")) return failExpecting("'&'");
            ctx.whitespace();

            var target = targetParsers.parse(ctx);
            if (!target.hasValue()) return failExpecting("target type");
            raw.target = target.valueNonNull();
            ctx.whitespace();

            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var targetParserName = ctx.word();
                if (targetParserName == null) return false;
                targetParsers.add(targetParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();
            return false;
        }
    }
    public static class Raw extends TypeGet.Raw<Analysis> {
        public Structure.Raw<?> target;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends TypeGet.Analysis {
        private final String name;
        public final TypeGet.Analysis target;
        public final Type.Ref type;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);

            String name = "void";
            TypeGet.Analysis target = null;
            Type.Ref type = null;

            if (raw.target.analyze(ctx) instanceof TypeGet.Analysis typeGet) {
                target = typeGet;
                name = typeGet.name();
                type = new Type.Ref(typeGet.type());
            } else ctx.report(new WrongTypeErr("reference type target", "valid type"), this);

            this.name = name;
            this.target = target;
            this.type = type;
        }

        @Override
        public String name() {
            return "&" + name;
        }

        @Override
        public Type.Ref type() {
            return type;
        }
    }
}
