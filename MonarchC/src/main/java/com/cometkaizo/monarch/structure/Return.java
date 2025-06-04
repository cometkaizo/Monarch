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
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("return")) return failExpecting("'return'");
            if (!ctx.whitespace()) return failExpecting("whitespace");

            var value = valueParsers.parse(ctx);
            if (!value.hasValue()) return failExpecting("expression");
            raw.value = value.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal(";")) return failExpecting("';'");
            ctx.whitespace();

            ctx.popStructure();
            return success(raw);
        }

        @Override
        protected boolean parseSettingsImpl(ParseContext ctx) {
            return parseParserList(valueParsers, ctx);
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
