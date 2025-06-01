package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Block;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.analysis.diagnostic.LingeringStackElementsException;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.bytecode.Chunk;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

import java.util.ArrayList;
import java.util.List;

public class If {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any conditionParsers = new Any(), statementParsers = new Any();
        private final Condition.Parser conditionParser = new Condition.Parser(conditionParsers);
        private final Else.Parser elseParser = new Else.Parser(statementParsers);

        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("if")) return failExpecting("'if'");
            ctx.whitespace();
            if (!ctx.literal("(")) return failExpecting("'('");
            ctx.whitespace();

            var condition = conditionParser.parse(ctx);
            if (!condition.hasValue()) return failExpecting("expression");
            raw.condition = condition.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal(")")) return failExpecting("')'");
            ctx.whitespace();
            if (!ctx.literal("{")) return failExpecting("'{'");
            ctx.whitespace();

            while (!ctx.literal("}")) {
                var statement = statementParsers.parse(ctx);
                if (!statement.success()) return failExpecting("statement");
                statement.value().ifPresent(raw.statements::add);

                ctx.whitespace();
                if (!ctx.chars.hasNext()) return failExpecting("'}'");
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

    public static class Condition {
        public static class Parser extends Structure.Parser<Raw> {
            private final Any conditionParsers;
            public Parser(Any conditionParsers) {
                this.conditionParsers = conditionParsers;
            }
            @Override
            protected Result<Raw> parseImpl(ParseContext ctx) {
                var raw = ctx.pushStructure(new Raw());

                var value = conditionParsers.parse(ctx);
                if (!value.hasValue()) return failExpecting("expression");
                raw.value = value.valueNonNull();

                ctx.popStructure();
                return success(raw);
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
            protected Analysis(Raw raw, AnalysisContext ctx) {
                super(raw, ctx);
                Expr value = null;
                if (raw.value.analyze(ctx) instanceof Expr expr) {
                    if (BooleanLit.Analysis.TYPE.equals(expr.type())) value = expr;
                    else ctx.report(new WrongTypeErr("condition", "boolean expression"), this);
                } else ctx.report(new WrongTypeErr("condition", "expression"), this);
                this.value = value;
            }
            @Override
            public void assemble(AssembleContext ctx) {
                value.assemble(ctx);
            }
        }
    }

    public static class Else {
        public static class Parser extends Structure.Parser<Raw> {
            private final Any statementParsers;
            public Parser(Any statementParsers) {
                this.statementParsers = statementParsers;
            }

            @Override
            protected Result<Raw> parseImpl(ParseContext ctx) {
                var raw = ctx.pushStructure(new Raw());

                if (!ctx.literal("else")) return failExpecting("'else'");
                ctx.whitespace();
                if (!ctx.literal("{")) return failExpecting("'{'");
                ctx.whitespace();

                while (!ctx.literal("}")) {
                    var statement = statementParsers.parse(ctx);
                    if (!statement.success()) return failExpecting("statement");
                    statement.value().ifPresent(raw.statements::add);

                    ctx.whitespace();
                    if (!ctx.chars.hasNext()) return failExpecting("'}'");
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

    public static class Raw extends Structure.Raw<Analysis> {
        public Condition.Raw condition;
        public List<Structure.Raw<?>> statements = new ArrayList<>(), elseStatements = new ArrayList<>();
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Block {
        public final Condition.Analysis condition;
        public final List<Structure.Analysis> statements, elseStatements;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);

            this.condition = raw.condition.analyze(ctx);
            this.statements = ctx.analyze(raw.statements);
            this.elseStatements = ctx.analyze(raw.elseStatements);

            ctx.popStructure();
        }

        @Override
        public void assemble(AssembleContext ctx) {
            Chunk c = ctx.data();
            condition.assemble(ctx);

            var ifStartLabel = c.createLabel();
            var ifEndLabel = c.createLabel();
            var elseStartLabel = c.createLabel();
            var elseEndLabel = c.createLabel();
            boolean hasElse = !elseStatements.isEmpty();

            c.opJumpIf(ifStartLabel);
            ctx.stackSize().subtract(condition.value.footprint());
            if (hasElse) c.opJumpToIndex(elseStartLabel);
            else c.opJumpToIndex(ifEndLabel);

            var ifStartStackSize = ctx.stackSize().capture();

            c.writeLabel(ifStartLabel);
            statements.forEach(s -> s.assemble(ctx));
            c.writeLabel(ifEndLabel);

            if (!ctx.stackSize().capture().equals(ifStartStackSize))
                throw new LingeringStackElementsException(this, ctx.stackSize().capture().minus(ifStartStackSize));

            if (hasElse) {
                var elseStartStackSize = ctx.stackSize().capture();

                c.opJumpToIndex(elseEndLabel);
                c.writeLabel(elseStartLabel);
                elseStatements.forEach(s -> s.assemble(ctx));
                c.writeLabel(elseEndLabel);

                if (!ctx.stackSize().capture().equals(elseStartStackSize))
                    throw new LingeringStackElementsException(this, "else", ctx.stackSize().capture().minus(elseStartStackSize));
            }
        }

        @Override
        public List<Structure.Analysis> statements() {
            return statements;
        }
    }
}
