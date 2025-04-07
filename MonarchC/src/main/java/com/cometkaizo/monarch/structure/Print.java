package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Print {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any valueParsers = new Any();

        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("print")) return fail();
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
        public final Expr value;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);

            var value = raw.value.analyze(ctx);
            if (value instanceof Expr expr) {
                if (!expr.isVoid()) this.value = expr;
                else {
                    this.value = null;
                    ctx.report(new WrongTypeErr("value", "expression"));
                }
            } else {
                this.value = null;
                ctx.report(new WrongTypeErr("value", "expression"));
            }

            ctx.popStructure();
        }

        @Override
        public void assemble(AssembleContext ctx) {
            value.assemble(ctx);
            ctx.data().opPrint(value.footprint());
            ctx.stackSize().subtract(value.footprint());
        }
    }
}
