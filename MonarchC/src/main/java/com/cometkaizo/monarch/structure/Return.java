package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.WrongEnvironmentErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.NoPrint;

public class Return {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any valueParsers = new Any();
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("return")) return fail();
            if (!ctx.whitespace()) return fail();

            var value = valueParsers.parse(ctx);
            if (!value.hasValue()) return fail();
            raw.value = value.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal(";")) return fail();
            ctx.whitespace();

            ctx.popStructure();
            return success(raw);
        }

        @Override
        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var statementParserName = ctx.word();
                if (statementParserName == null) return false;
                valueParsers.add(statementParserName, ctx);

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
    public static class Analysis extends Structure.Analysis implements ExprConsumer {
        @NoPrint public final Returnable returnable;
        public final Expr value;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            var returnable = ancestors.ofType(Returnable.class);
            if (returnable.isPresent()) {
                this.returnable = returnable.get();
            } else {
                this.returnable = null;
                ctx.report(new WrongEnvironmentErr("return statements", "returnable"), this);
            }

            if (raw.value != null) {
                var value = raw.value.analyze(ctx);
                if (value instanceof Expr expr) this.value = expr;
                else {
                    ctx.report(new WrongTypeErr("value", "expression"), this);
                    this.value = null;
                }
            } else this.value = null;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            this.returnable.assembleReturn(value, ctx);
        }
    }
}
