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

        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("while")) return fail();
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

            ctx.popStructure();
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
    public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
        public Structure.Raw<?> condition;
        public List<Structure.Raw<?>> statements = new ArrayList<>();
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements ExprConsumer, Breakable, Block {
        public final Expr condition;
        public final List<Structure.Analysis> statements;
        @NoPrint private Chunk.Info.Label endLabel;
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

            ctx.popStructure();
        }

        @Override
        public void assemble(AssembleContext ctx) {
            endLabel = ctx.data().createLabel();
            var startLabel = ctx.data().createLabel();

            condition.assemble(ctx);
            ctx.data().opJumpIf(startLabel);
            ctx.stackSize().subtract(condition.footprint());
            ctx.data().opJumpToIndex(endLabel);

            var startStackSize = ctx.stackSize().capture();

            ctx.data().writeLabel(startLabel);
            statements.forEach(s -> s.assemble(ctx));

            if (!ctx.stackSize().capture().equals(startStackSize))
                throw new LingeringStackElementsException(this, ctx.stackSize().capture().minus(startStackSize));

            condition.assemble(ctx);
            ctx.data().opJumpIf(startLabel);
            ctx.stackSize().subtract(condition.footprint());

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
