package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Ref {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any valueParsers = new Any();
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("&")) return fail();
            ctx.whitespace();

            // value
            var value = valueParsers.parse(ctx);
            if (!value.hasValue()) return fail();
            raw.value = value.valueNonNull();

            ctx.popStructure();
            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var valueParserName = ctx.word();
                if (valueParserName == null) return false;
                valueParsers.add(valueParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();
            return false;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
        public Structure.Raw<?> value;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr, ExprConsumer {
        public final Locatable value;
        public final Type.Ref type;

        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            Locatable value = null;
            Type.Ref type = null;

            if (raw.value.analyze(ctx) instanceof Locatable expr) {
                value = expr;
                type = new Type.Ref(value.typeAtLocation());
            } else ctx.report(new WrongTypeErr("reference parameter", "locatable value"), this);

            this.value = value;
            this.type = type;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            value.assembleLocation(ctx);
        }

        @Override
        public Type type() {
            return type;
        }
    }
}
