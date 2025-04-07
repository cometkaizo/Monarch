package com.cometkaizo.monarch.structure;

import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class CompileWith {
    public static class Parser extends Structure.Parser<Structure.Raw<?>> {
        private final CompilationUnit.Parser unitParser;
        public Parser(CompilationUnit.Parser unitParser) {
            this.unitParser = unitParser;
        }

        @Override
        protected Structure.Parser<Structure.Raw<?>>.Result parseImpl(ParseContext ctx) {
            if (!ctx.literal("compile")) return fail();
            if (!ctx.whitespace()) return fail();
            if (!ctx.literal("with")) return fail();
            if (!ctx.whitespace()) return fail();
            var name = ctx.word();
            if (name == null) return fail();
            if (!unitParser.parsers().add(name, ctx)) return fail();
            ctx.whitespace();
            return success();
        }
    }
}
