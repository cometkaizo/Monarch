package com.cometkaizo.monarch.structure;

import com.cometkaizo.monarch.structure.diagnostic.UnknownParserErr;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class CompileSettings {
    public static class Parser extends Structure.Parser<Structure.Raw<?>> {
        @Override
        protected Result<Structure.Raw<?>> parseImpl(ParseContext ctx) {
            if (!ctx.literal("compile")) return failExpecting("'compile'");
            if (!ctx.whitespace()) return failExpecting("whitespace");
            var name = ctx.word();
            if (name == null) return failExpecting("name");
            if (!ctx.whitespace()) return failExpecting("whitespace");
            if (!ctx.literal("with")) return failExpecting("'with'");
            if (!ctx.whitespace()) return failExpecting("whitespace");

            var parser = ctx.getParser(name);
            if (parser.isEmpty()) {
                ctx.report(new UnknownParserErr(name));
                return fail();
            } else if (!parser.get().parseSettings(ctx)) return fail();
            ctx.whitespace();
            return success();
        }
    }
}
