package com.cometkaizo.monarch.structure;

import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class BiOperatorSet {
    public static abstract class Parser<R extends BiOperator.Raw<?>> extends Structure.Parser<Locatable.Set.Raw> {
        private final Any refParsers = new Any(), operandParsers = new Any();

        @Override
        protected Result<Locatable.Set.Raw> parseImpl(ParseContext ctx) {
            var setRaw = ctx.pushStructure(new Locatable.Set.Raw());

            { // ref
                var ref = refParsers.parse(ctx);
                if (!ref.hasValue()) return failExpecting("target");
                setRaw.ref = ref.valueNonNull();
                ctx.whitespace();
            }

            if (!ctx.literal(operationSymbol())) return failExpecting("'" + operationSymbol() + "'");
            ctx.whitespace();

            { // expr
                var exprRaw = ctx.pushStructure(createRaw(this::newRaw, ctx));
                exprRaw.left = createRaw(() -> new Locatable.Get.Raw(setRaw.ref), ctx);

                var value = operandParsers.parse(ctx);
                if (!value.hasValue()) return failExpecting("expression");
                exprRaw.right = value.valueNonNull();
                ctx.whitespace();

                setRaw.value = exprRaw;
                ctx.popStructure();
            }

            if (!ctx.literal(";")) return failExpecting("';'");
            ctx.whitespace();

            ctx.popStructure();
            return success(setRaw);
        }

        protected abstract R newRaw();
        protected abstract String operationSymbol();

        protected boolean parseSettingsImpl(ParseContext ctx) {
            Any parsers;
            if (ctx.literal("targets")) parsers = refParsers;
            else if (ctx.literal("values")) parsers = operandParsers;
            else return false;

            ctx.whitespace();
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var statementParserName = ctx.word();
                if (statementParserName == null) return false;
                parsers.add(statementParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();

            return false;
        }
    }
}
