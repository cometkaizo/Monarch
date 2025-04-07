package com.cometkaizo.monarch.structure;

import com.cometkaizo.monarch.structure.diagnostic.UnknownParserErr;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class CompileSettings {
    public static class Parser extends Structure.Parser<Structure.Raw<?>> {
        @Override
        protected Structure.Parser<Structure.Raw<?>>.Result parseImpl(ParseContext ctx) {
            if (!ctx.literal("compile")) return fail();
            if (!ctx.whitespace()) return fail();
            var name = ctx.word();
            if (name == null) return fail();
            if (!ctx.whitespace()) return fail();
            if (!ctx.literal("with")) return fail();
            if (!ctx.whitespace()) return fail();

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
