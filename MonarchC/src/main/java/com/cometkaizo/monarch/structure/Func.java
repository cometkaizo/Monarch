package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.*;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.bytecode.Chunk;
import com.cometkaizo.monarch.structure.diagnostic.DifferentTypesErr;
import com.cometkaizo.monarch.structure.diagnostic.UnknownTypeErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.monarch.structure.resource.Vars;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class Func {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any statementParsers;
        private final Any paramsParsers;

        public Parser() {
            this.statementParsers = new Any();
            this.paramsParsers = new Any();
        }

        @Override
        protected Result parseImpl(ParseContext ctx) {
            if (!ctx.literal("function")) return fail();
            var raw = ctx.pushStructure(new Raw());
            if (!ctx.whitespace()) return fail();

            // name
            raw.name = ctx.word();
            if (raw.name == null) return fail();
            ctx.whitespace();

            // params
            var params = paramsParsers.parse(ctx);
            if (params.success()) raw.params = params.valueNonNull();
            else return fail();
            ctx.whitespace();

            // return type
            if (!ctx.literal(":")) return fail();
            ctx.whitespace();
            raw.returnType = ctx.word();
            if (raw.returnType == null) return fail();
            ctx.whitespace();

            // body
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

        protected boolean parseSettingsImpl(ParseContext ctx) {
            Any parser;
            if (ctx.literal("statements")) parser = statementParsers;
            else if (ctx.literal("parameters")) parser = paramsParsers;
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
    public static class Raw extends Structure.Raw<Analysis> {
        public String name, returnType;
        public List<Structure.Raw<?>> statements = new ArrayList<>();
        public Structure.Raw<?> params;

        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements StackResource.Manager, Returnable, Block {
        public final String name;
        public final StackResource.Manager.Simple resources = new StackResource.Manager.Simple();
        public final List<Structure.Analysis> statements;
        public final ParenParamsDecl.Analysis params;
        public final Type returnType;

        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);

            this.name = raw.name;

            var returnType = ctx.getType(raw.returnType);
            if ("void".equals(raw.returnType)) this.returnType = null;
            else if (returnType.isPresent()) {
                this.returnType = returnType.get();
            } else {
                this.returnType = null;
                ctx.report(new UnknownTypeErr(raw.returnType));
            }

            resources.getOrCreate(Vars.Params.class, Vars.Params::new);

            var params = raw.params.analyze(ctx);
            if (params instanceof ParenParamsDecl.Analysis paramsDecl) this.params = paramsDecl;
            else {
                this.params = null;
                ctx.report(new WrongTypeErr("params list", "params declaration"));
            }

            this.statements = ctx.analyze(raw.statements);
            checkReturnStatementTypes(this, ctx);

            ctx.popStructure();
        }

        private void checkReturnStatementTypes(Block block, AnalysisContext ctx) {
            for (var statement : block.statements()) {
                if (statement instanceof Expr expr) {
                    if (this.returnType != expr.type()) {
                        ctx.report(new DifferentTypesErr(expr.type(), this.returnType));
                    }
                } else if (statement instanceof Block deepBlock) {
                    checkReturnStatementTypes(deepBlock, ctx);
                }
            }
        }

        @Override
        public void assemble(AssembleContext ctx) {
            var interpreter = ctx.interpreters().get(Interpreter.class, Interpreter::new);

            var funcLoc = ctx.data().createLabel();
            ctx.data().writeLabel(funcLoc);
            interpreter.addFunction(name, funcLoc);

            resources.assembleSetup(ctx);
            params.assemble(ctx);
            statements.forEach(s -> s.assemble(ctx));
            assembleReturn(null, ctx); // failsafe return
        }

        @Override
        public void assembleReturn(Expr value, AssembleContext ctx) {
            var junk = resources.offset(ctx);
            if (!junk.isZero()) {
                ctx.data().opPopAll(junk);
                ctx.stackSize().subtract(junk);
            }

            if (value != null) value.assemble(ctx);
            ctx.data().opMove(resources.offset(ctx), resources.footprint().plus(0, 1));

            resources.assembleCleanup(ctx);
            ctx.data().opJumpToPtr();
        }

        @Override
        public Size offsetOf(Predicate<? super StackResource> condition, AssembleContext ctx) {
            return resources.offsetOf(condition, ctx);
        }

        @Override
        public Size offset(AssembleContext ctx) {
            return resources.offset(ctx);
        }

        @Override
        public <T extends StackResource> T getOrCreate(Class<T> type, Function<StackResource.Manager, T> generator) {
            return resources.getOrCreate(type, generator);
        }
        @Override
        public <T extends StackResource> Optional<T> get(Class<T> type) {
            return resources.get(type);
        }

        @Override
        public List<Structure.Analysis> statements() {
            return statements;
        }
    }
    public static class Interpreter implements Structure.Interpreter {
        public static final String NAME = "function";
        private final Chunk.JumpArrSwitchBuilder funcSwitch = new Chunk.JumpArrSwitchBuilder();
        public void addFunction(String name, Chunk.Info.Label location) {
            funcSwitch.addBranch(name, location);
        }

        @Override
        public void assemble(Chunk c) {
            funcSwitch.apply(c);
        }
        @Override
        public String name() {
            return NAME;
        }
    }
}
