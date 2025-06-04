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
import com.cometkaizo.util.NoPrint;

import java.util.ArrayList;
import java.util.List;

public class While {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any conditionParsers = new Any(), statementParsers = new Any();
        private final Condition.Parser conditionParser = new Condition.Parser(conditionParsers);

        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("while")) return failExpecting("'while'");
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

            ctx.popStructure();
            return success(raw);
        }

        @Override
        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (ctx.literal("statements")) return parseParserList(statementParsers, ctx);
            else if (ctx.literal("conditions")) return parseParserList(conditionParsers, ctx);
            else return false;
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

    public static class Raw extends Structure.Raw<Analysis> {
        public Condition.Raw condition;
        public List<Structure.Raw<?>> statements = new ArrayList<>();
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Breakable, Block {
        public final Condition.Analysis condition;
        public final List<Structure.Analysis> statements;
        @NoPrint private Chunk.Info.Label endLabel;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);

            this.condition = raw.condition.analyze(ctx);
            this.statements = ctx.analyze(raw.statements);

            ctx.popStructure();
        }

        @Override
        public void assemble(AssembleContext ctx) {
            endLabel = ctx.data().createLabel();
            var startLabel = ctx.data().createLabel();

            condition.assemble(ctx);
            ctx.data().opJumpIf(startLabel);
            ctx.stackSize().subtract(condition.value.footprint());
            ctx.data().opJumpToIndex(endLabel);

            var startStackSize = ctx.stackSize().capture();

            ctx.data().writeLabel(startLabel);
            statements.forEach(s -> s.assemble(ctx));

            if (!ctx.stackSize().capture().equals(startStackSize))
                throw new LingeringStackElementsException(this, ctx.stackSize().capture().minus(startStackSize));

            condition.assemble(ctx);
            ctx.data().opJumpIf(startLabel);
            ctx.stackSize().subtract(condition.value.footprint());

            ctx.data().writeLabel(endLabel);
        }

        @Override
        public void assembleBreak(AssembleContext ctx) {
            ctx.data().opJumpToIndex(endLabel);
        }

        @Override
        public List<Structure.Analysis> statements() {
            return statements;
        }
    }
}
