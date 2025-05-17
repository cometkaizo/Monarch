package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class TypeDecl {

    public static class Parser extends Structure.Parser<Raw> {
        private final Any sizeParsers = new Any();
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            if (!ctx.literal("type")) return failExpecting("'type'");
            if (!ctx.whitespace()) return fail();

            raw.name = ctx.word();
            if (raw.name == null) return failExpecting("name");
            ctx.whitespace();

            if (!ctx.literal("=")) return failExpecting("'='");
            ctx.whitespace();
            if (!ctx.literal("(")) return failExpecting("'('");
            ctx.whitespace();

            var byteSize = sizeParsers.parse(ctx);
            if (!byteSize.hasValue()) return failExpecting("size");
            raw.byteSize = byteSize.valueNonNull();

            if (!ctx.literal(",")) return failExpecting("','");
            ctx.whitespace();

            var ptrSize = sizeParsers.parse(ctx);
            if (!ptrSize.hasValue()) return failExpecting("size");
            raw.ptrSize = ptrSize.valueNonNull();

            if (!ctx.literal(")")) return failExpecting("')'");
            ctx.whitespace();
            if (!ctx.literal(";")) return failExpecting("';'");
            ctx.whitespace();

            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var sizeParserName = ctx.word();
                if (sizeParserName == null) return false;
                sizeParsers.add(sizeParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();

            return false;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public String name;
        public Structure.Raw<?> byteSize, ptrSize;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis {
        public final String name;
        public final Size size;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.name = raw.name;

            Size size = null;
            if (raw.byteSize.analyze(ctx) instanceof StaticExpr<?> byteExpr) {
                var byteSizeNum = byteExpr.staticEvaluate(Number.class);
                if (byteSizeNum.isPresent()) {
                    if (raw.ptrSize.analyze(ctx) instanceof StaticExpr<?> ptrExpr) {
                        var ptrSizeNum = ptrExpr.staticEvaluate(Number.class);
                        if (ptrSizeNum.isPresent()) {
                            size = new Size(byteSizeNum.get().intValue(), ptrSizeNum.get().intValue());
                        } else ctx.report(new WrongTypeErr("pointer size parameter", "integer static expression"), this);
                    } else ctx.report(new WrongTypeErr("pointer size parameter", "static expression"), this);
                } else ctx.report(new WrongTypeErr("byte size parameter", "integer static expression"), this);
            } else ctx.report(new WrongTypeErr("byte size parameter", "static expression"), this);
            this.size = size;
        }

        @Override
        public void assemble(AssembleContext ctx) {

        }

        public Type.Static toType() {
            return new Type.Static(name, size);
        }
    }
}
