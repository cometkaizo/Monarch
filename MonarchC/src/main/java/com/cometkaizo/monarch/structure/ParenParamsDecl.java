package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.parser.ParamDecl;
import com.cometkaizo.parser.ParamsDecl;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParenParamsDecl {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any paramParsers;

        public Parser() {
            this.paramParsers = new Any();
        }

        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();
            if (!ctx.literal("(")) return fail();

            while (true) {
                ctx.whitespace();
                var param = paramParsers.parse(ctx);
                if (param.success()) param.value().ifPresent(raw.params::add);
                else break;
                ctx.whitespace();
                if (!ctx.literal(",")) break;
            }
            ctx.whitespace();

            if (!ctx.literal(")")) return fail();
            ctx.whitespace();

            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var statementParserName = ctx.word();
                if (statementParserName == null) return false;
                paramParsers.add(statementParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();

            return false;
        }

    }
    public static class Raw extends Structure.Raw<Analysis> {
        public List<Structure.Raw<?>> params = new ArrayList<>();
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements ParamsDecl {
        public final List<ParamDecl> params;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            var params = new ArrayList<ParamDecl>(raw.params.size());

            for (var rawParam : raw.params) {
                if (rawParam.analyze(ctx) instanceof ParamDecl param) params.add(param);
                else ctx.report(new WrongTypeErr("params element", "param"));
            }

            this.params = Collections.unmodifiableList(params);
        }

        @Override
        public void assemble(AssembleContext ctx) {

        }
    }
}
