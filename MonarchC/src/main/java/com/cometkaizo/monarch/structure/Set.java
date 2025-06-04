package com.cometkaizo.monarch.structure;

import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Set {
    public static class Parser extends Structure.Parser<Locatable.Set.Raw> {
        private final Any refParsers = new Any();
        private final Any valueParsers = new Any();

        @Override
        protected Result<Locatable.Set.Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Locatable.Set.Raw());

            var ref = refParsers.parse(ctx);
            if (!ref.hasValue()) return failExpecting("target");
            raw.ref = ref.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal("=")) return failExpecting("'='");
            ctx.whitespace();

            var value = valueParsers.parse(ctx);
            if (!value.hasValue()) return failExpecting("expression");
            raw.value = value.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal(";")) return failExpecting("';'");
            ctx.whitespace();

            ctx.popStructure();
            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (ctx.literal("targets")) return parseParserList(refParsers, ctx);
            else if (ctx.literal("values")) return parseParserList(valueParsers, ctx);
            else return false;
        }
    }
}
