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
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("print")) return failExpecting("'print'");
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
        public final Expr value;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);

            var value = raw.value.analyze(ctx);
            if (value instanceof Expr expr) {
                if (!expr.isVoid()) this.value = expr;
                else {
                    this.value = null;
                    ctx.report(new WrongTypeErr("value", "expression"), this);
                }
            } else {
                this.value = null;
                ctx.report(new WrongTypeErr("value", "expression"), this);
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
