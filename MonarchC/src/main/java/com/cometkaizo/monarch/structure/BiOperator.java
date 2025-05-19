package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.Diagnostic;

public class BiOperator {
    public static abstract class Parser<R extends Raw<?>> extends Structure.Parser<R> {
        private final Any operandParsers = new Any();
        
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(newRaw());

            var left = operandParsers.parse(ctx);
            if (!left.hasValue()) return fail();
            raw.left = left.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal(operationSymbol())) return fail();
            ctx.whitespace();

            var right = operandParsers.parse(ctx);
            if (!right.hasValue()) return fail();
            raw.right = right.valueNonNull();
            ctx.whitespace();

            ctx.popStructure();
            return success(raw);
        }

        protected abstract R newRaw();
        protected abstract String operationSymbol();

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var statementParserName = ctx.word();
                if (statementParserName == null) return false;
                operandParsers.add(statementParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();

            return false;
        }
    }
    public static abstract class Raw<A extends Structure.Analysis> extends Structure.Raw<A> implements ExprConsumer {
        public Structure.Raw<?> left, right;
    }
    public static abstract class Analysis extends Structure.Analysis implements Expr, ExprConsumer {
        public final Expr left, right;
        protected Analysis(Raw<?> raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);
            Expr left = null, right = null;

            if (raw.left.analyze(ctx) instanceof Expr expr) {
                var err = validateLeft(expr);
                if (err == null) left = expr;
                else ctx.report(err, this);
            } else ctx.report(new WrongTypeErr("left operand", "expression"), this);
            this.left = left;

            if (raw.right.analyze(ctx) instanceof Expr expr) {
                var err = validateRight(expr);
                if (err == null) right = expr;
                else ctx.report(err, this);
            } else ctx.report(new WrongTypeErr("right operand", "expression"), this);
            this.right = right;

            ctx.popStructure();
        }

        protected abstract Diagnostic validateLeft(Expr left);
        protected abstract Diagnostic validateRight(Expr right);
    }
}
