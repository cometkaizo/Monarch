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
            if (!ctx.literal("compile")) return failExpecting("'compile'");
            if (!ctx.whitespace()) return failExpecting("whitespace");
            if (!ctx.literal("with")) return failExpecting("'with'");
            if (!ctx.whitespace()) return failExpecting("whitespace");
            var name = ctx.word();
            if (name == null) return failExpecting("name");
            if (!unitParser.parsers().add(name, ctx)) return fail();
            ctx.whitespace();
            return success();
        }
    }
}
