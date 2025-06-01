package com.cometkaizo.monarch.structure;

import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.StringUtils;

public class Comment {
    public static class Parser extends Structure.Parser<Structure.Raw<?>> {
        @Override
        protected Result<?> parseImpl(ParseContext ctx) {
            if (!ctx.literal("//")) return failExpecting("'//'");
            ctx.chars.checkAndAdvance(c -> !StringUtils.isNewline(c));
            ctx.whitespace();
            return success();
        }
    }
}
