package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.IncompatibleTypesErr;
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
            Returnable returnable = null;
            var returnableOpt = ancestors.ofType(Returnable.class);
            if (returnableOpt.isPresent()) {
                returnable = returnableOpt.get();
            } else ctx.report(new WrongEnvironmentErr("return statements", "returnable"), this);
            this.returnable = returnable;

            Expr value = null;
            if (raw.value != null) {
                if (raw.value.analyze(ctx) instanceof Expr expr) value = expr;
                else ctx.report(new WrongTypeErr("value", "expression"), this);
            }
            this.value = value;

            if (returnable != null && value != null) {
                if (!returnable.returnType().equals(value.type()))
                    ctx.report(new IncompatibleTypesErr(value.type(), returnable.returnType()));
            }
        }

        @Override
        public void assemble(AssembleContext ctx) {
            this.returnable.assembleReturn(value, ctx);
        }
    }
}
