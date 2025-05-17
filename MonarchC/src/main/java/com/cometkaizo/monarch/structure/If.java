package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Block;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

import java.util.ArrayList;
import java.util.List;

public class If {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any conditionParsers = new Any(), statementParsers = new Any();
        private final Else.Parser elseParser = new Else.Parser(statementParsers);

        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("if")) return fail();
            ctx.whitespace();
            if (!ctx.literal("(")) return fail();
            ctx.whitespace();

            var condition = conditionParsers.parse(ctx);
            if (!condition.hasValue()) return fail();
            raw.condition = condition.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal(")")) return fail();
            ctx.whitespace();
            if (!ctx.literal("{")) return fail();
            ctx.whitespace();

            while (!ctx.literal("}")) {
                var statement = statementParsers.parse(ctx);
                if (!statement.success()) return fail();
                statement.value().ifPresent(raw.statements::add);

                ctx.whitespace();
                if (!ctx.chars.hasNext()) return fail();
            }
            ctx.whitespace();

            ctx.popStructure();

            // else (possibly)
            var elseRaw = elseParser.parse(ctx);
            if (elseRaw.hasValue()) {
                raw.elseStatements = elseRaw.valueNonNull().statements;
            }

            return success(raw);
        }

        @Override
        protected boolean parseSettingsImpl(ParseContext ctx) {
            Any parser;
            if (ctx.literal("statements")) parser = statementParsers;
            else if (ctx.literal("conditions")) parser = conditionParsers;
            else return false;

            ctx.whitespace();
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var parserName = ctx.word();
                if (parserName == null) return false;
                parser.add(parserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (!ctx.literal(")")) return false;
            ctx.whitespace();

            return true;
        }

    }

    public static class Else {
        public static class Parser extends Structure.Parser<Raw> {
            private final Any statementParsers;
            public Parser(Any statementParsers) {
                this.statementParsers = statementParsers;
            }

            @Override
            protected Result parseImpl(ParseContext ctx) {
                var raw = ctx.pushStructure(new Raw());

                if (!ctx.literal("else")) return fail();
                ctx.whitespace();
                if (!ctx.literal("{")) return fail();
                ctx.whitespace();

                while (!ctx.literal("}")) {
                    var statement = statementParsers.parse(ctx);
                    if (!statement.success()) return fail();
                    statement.value().ifPresent(raw.statements::add);

                    ctx.whitespace();
                    if (!ctx.chars.hasNext()) return fail();
                }
                ctx.whitespace();

                ctx.popStructure();
                return success(raw);
            }
        }
        public static class Raw extends Structure.Raw<Structure.Analysis> {
            public List<Structure.Raw<?>> statements = new ArrayList<>();
            @Override
            protected Structure.Analysis analyzeImpl(AnalysisContext ctx) {
                return null;
            }
        }
    }

    public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
        public Structure.Raw<?> condition;
        public List<Structure.Raw<?>> statements = new ArrayList<>(), elseStatements = new ArrayList<>();
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements ExprConsumer, Block {
        public final Expr condition;
        public final List<Structure.Analysis> statements, elseStatements;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);

            Expr condition = null;
            if (raw.condition.analyze(ctx) instanceof Expr expr) {
                if (BooleanLit.Analysis.TYPE.equals(expr.type())) condition = expr;
                else ctx.report(new WrongTypeErr("condition", "boolean expression"), this);
            } else ctx.report(new WrongTypeErr("condition", "expression"), this);
            this.condition = condition;

            this.statements = ctx.analyze(raw.statements);
            this.elseStatements = ctx.analyze(raw.elseStatements);

            ctx.popStructure();
        }

        @Override
        public void assemble(AssembleContext ctx) {
            condition.assemble(ctx);

            var ifStartLabel = ctx.data().createLabel();
            var ifEndLabel = ctx.data().createLabel();
            var elseStartLabel = ctx.data().createLabel();
            var elseEndLabel = ctx.data().createLabel();
            boolean hasElse = !elseStatements.isEmpty();

            ctx.data().opJumpIf(ifStartLabel);
            ctx.stackSize().subtract(condition.footprint());
            if (hasElse) ctx.data().opJumpToIndex(elseStartLabel);
            else ctx.data().opJumpToIndex(ifEndLabel);

            ctx.data().writeLabel(ifStartLabel);
            statements.forEach(s -> s.assemble(ctx));
            ctx.data().writeLabel(ifEndLabel);

            if (hasElse) {
                ctx.data().opJumpToIndex(elseEndLabel);
                ctx.data().writeLabel(elseStartLabel);
                elseStatements.forEach(s -> s.assemble(ctx));
                ctx.data().writeLabel(elseEndLabel);
            }
        }

        @Override
        public List<Structure.Analysis> statements() {
            return statements;
        }
    }
}
