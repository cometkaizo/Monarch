package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.*;
import com.cometkaizo.analysis.diagnostic.LingeringStackElementsException;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.bytecode.Chunk;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.monarch.structure.resource.Vars;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.NoPrint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class Func {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any statementParsers = new Any();
        private final Any paramsParsers = new Any();
        private final Any returnTypeParsers = new Any();

        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            if (!ctx.literal("function")) return failExpecting("'function'");
            var raw = ctx.pushStructure(new Raw());
            if (!ctx.whitespace()) return failExpecting("whitespace");

            // name
            raw.name = ctx.word();
            if (raw.name == null) return failExpecting("name");
            ctx.whitespace();

            // params
            var params = paramsParsers.parse(ctx);
            if (!params.hasValue()) return failExpecting("parameters");
            raw.params = params.valueNonNull();
            ctx.whitespace();

            // return type
            if (!ctx.literal(":")) return failExpecting("':'");
            ctx.whitespace();
            if (!ctx.literal("void")) {
                var returnType = returnTypeParsers.parse(ctx);
                if (!returnType.hasValue()) return failExpecting("return type");
                raw.returnType = returnType.valueNonNull();
            }
            ctx.whitespace();

            // body
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

        protected boolean parseSettingsImpl(ParseContext ctx) {
            Any parsers;
            if (ctx.literal("statements")) parsers = statementParsers;
            else if (ctx.literal("parameters")) parsers = paramsParsers;
            else if (ctx.literal("return") && ctx.whitespace() && ctx.literal("types")) parsers = returnTypeParsers;
            else return false;

            ctx.whitespace();
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var parserName = ctx.word();
                if (parserName == null) return false;
                parsers.add(parserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (!ctx.literal(")")) return false;
            ctx.whitespace();

            return true;
        }

    }
    public static class Raw extends Structure.Raw<Analysis> {
        public String name;
        public List<Structure.Raw<?>> statements = new ArrayList<>();
        public Structure.Raw<?> params, returnType; // null returnType = void

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
        public final Type returnType; // null = void
        @NoPrint private Chunk.Info.Label returnLabel;

        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);

            this.name = raw.name;

            Type returnType = null;
            if (raw.returnType != null) {
                if (raw.returnType.analyze(ctx) instanceof TypeGet.Analysis typeGet) {
                    if (typeGet.type() != null) returnType = typeGet.type();
                } else ctx.report(new WrongTypeErr("function return type", "valid return type"), this);
            }
            this.returnType = returnType;

            resources.getOrCreate(Vars.Params.class, Vars.Params::new);

            var params = raw.params.analyze(ctx);
            if (params instanceof ParenParamsDecl.Analysis paramsDecl) this.params = paramsDecl;
            else {
                this.params = null;
                ctx.report(new WrongTypeErr("params list", "params declaration"), this);
            }

            this.statements = ctx.analyze(raw.statements);

            ctx.popStructure();
        }

        @Override
        public void assemble(AssembleContext ctx) {
            var interpreter = ctx.interpreters().get(Interpreter.class, Interpreter::new);

            var funcLoc = ctx.data().createLabel();
            ctx.data().writeLabel(funcLoc);
            interpreter.addFunction(name, funcLoc);

            returnLabel = ctx.data().createLabel();
            resources.assembleSetup(ctx);
            params.assemble(ctx);
            statements.forEach(s -> s.assemble(ctx));
            assembleFinalReturn(ctx);
        }

        @Override
        public Type returnType() {
            return returnType;
        }

        @Override
        public void assembleReturn(Expr value, AssembleContext ctx) {
            var junk = resources.offset(ctx);
            if (!junk.isZero()) {
                ctx.data().opPopAll(junk);
                ctx.stackSize().subtract(junk);
                throw new LingeringStackElementsException(this, name, ctx.stackSize());
            }

            if (value != null) {
                value.assemble(ctx);
                ctx.stackSize().subtract(value.footprint());
                ctx.data().opJumpToIndex(returnLabel);
            }
        }

        public void assembleFinalReturn(AssembleContext ctx) {
            ctx.data().writeLabel(returnLabel);

            var junk = resources.offset(ctx);
            if (!junk.isZero()) {
                ctx.data().opPopAll(junk);
                ctx.stackSize().subtract(junk);
                throw new LingeringStackElementsException(this, name, ctx.stackSize());
            }

            // move resources + return pointer to the top of the stack
            // at this point, the return value should be on top of the stack (but it is not included in stackSize)
            Size resourcesOffset = resources.offset(ctx).plus(returnType == null ? Size.ZERO : returnType.footprint());
            ctx.data().opMove(resourcesOffset, resources.footprint().plus(Size.ONE_PTR));

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
