package com.cometkaizo.monarch.structure;

import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class ArrayDeref {
    public static class Parser extends Structure.Parser<Deref.Raw> {
        private final Any refParsers = new Any(), offsetParsers = new Any();
        @Override
        protected Result<Deref.Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Deref.Raw());
            var addRaw = ctx.pushStructure(createRaw(Add.Raw::new, ctx));

            var ref = refParsers.parse(ctx);
            if (!ref.hasValue()) return failExpecting("target");
            addRaw.left = ref.valueNonNull();

            if (!ctx.literal("[")) return failExpecting("'['");
            ctx.whitespace();

            var offset = offsetParsers.parse(ctx);
            if (!offset.hasValue()) return failExpecting("expression");
            addRaw.right = offset.valueNonNull();

            if (!ctx.literal("]")) return failExpecting("']'");
            ctx.whitespace();

            raw.ref = addRaw;
            ctx.popStructure();
            ctx.popStructure();
            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (ctx.literal("targets")) return parseParserList(refParsers, ctx);
            else if (ctx.literal("values")) return parseParserList(offsetParsers, ctx);
            else return false;
        }
    }
}
