package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.UnknownFuncErr;
import com.cometkaizo.monarch.structure.diagnostic.UnknownUnitErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.cometkaizo.monarch.structure.CompilationUnit.Parser.UNIT_NAME_FMT;

public class FuncCall {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any argsParsers = new Any();

        @Override
        protected Result parseImpl(ParseContext ctx) {
            var parent = ctx.topStructure();
            var raw = ctx.pushStructure(new Raw());

            var unitOrFuncName = ctx.chars.checkAndAdvance(UNIT_NAME_FMT);
            if (unitOrFuncName == null) return fail();
            ctx.whitespace();

            // unit name may or may not be specified
            if (ctx.literal(":")) {
                ctx.whitespace();

                raw.unitName = unitOrFuncName;

                raw.funcName = ctx.word();
                if (raw.funcName == null) return fail();
                ctx.whitespace();
            } else {
                raw.unitName = ctx.getCurrentCompilationUnit().name;
                raw.funcName = unitOrFuncName;
            }

            // params
            if (!ctx.literal("(")) return fail();
            while (true) {
                ctx.whitespace();
                var param = argsParsers.parse(ctx);
                if (param.success()) param.value().ifPresent(raw.args::add);
                else break;
                ctx.whitespace();
                if (!ctx.literal(",")) break;
            }
            ctx.whitespace();

            if (!ctx.literal(")")) return fail();
            ctx.whitespace();

            // only require ; if this is not used as an expression
            if (!(parent instanceof ExprConsumer)) {
                if (!ctx.literal(";")) return fail();
                ctx.whitespace();
            }

            ctx.popStructure();
            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var argParserName = ctx.word();
                if (argParserName == null) return false;
                argsParsers.add(argParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();

            return false;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
        public String unitName, funcName;
        public List<Structure.Raw<?>> args = new ArrayList<>();
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr, ExprConsumer {
        public final String unitName, funcName;
        public final List<Expr> args;
        public final Type returnType;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.unitName = raw.unitName;
            this.funcName = raw.funcName;

            var args = new ArrayList<Expr>();
            for (var rawArg : raw.args) {
                if (rawArg.analyze(ctx) instanceof Expr expr) args.add(expr);
                else {
                    ctx.report(new WrongTypeErr("argument", "expression"), this);
                    args.add(null);
                }
            }
            this.args = Collections.unmodifiableList(args);

            // find return type
            var unitOpt = ctx.getCompilationUnit(unitName);
            Type returnType = null;
            if (unitOpt.isPresent()) {
                var funcOpt = unitOpt.get().findMember(Func.Raw.class, f -> funcName.equals(f.name));
                if (funcOpt.isPresent()) {
                    var func = funcOpt.get().analyze(ctx);
                    returnType = func.returnType;
                    validateArgTypes(ctx, func);
                } else ctx.report(new UnknownFuncErr(funcName, unitName), this);
            } else ctx.report(new UnknownUnitErr(unitName), this);
            this.returnType = returnType;
        }

        private void validateArgTypes(AnalysisContext ctx, Func.Analysis func) {
            var params = func.params.params;
            if (params.size() == args.size()) for (int i = 0; i < params.size(); i++) {
                if (!params.get(i).type().equals(args.get(i).type())) {
                    ctx.report(new UnknownFuncErr(funcName, unitName, argTypes()), this);
                    break;
                }
            } else ctx.report(new UnknownFuncErr(funcName, unitName, argTypes()), this);
        }

        private Type[] argTypes() {
            return args.stream().map(e -> e == null ? null : e.type()).toArray(Type[]::new);
        }

        @Override
        public void assemble(AssembleContext ctx) {
            var after = ctx.data().createLabel();
            ctx.data().opPushPtr(after);
            ctx.stackSize().add(0, 1);

            // assemble args
            args.forEach(a -> a.assemble(ctx));

            // call function
            ctx.data().opPushPtrArr(funcName.getBytes());
            ctx.data().opPushPtrArr(Func.Interpreter.NAME.getBytes());
            ctx.data().opPushPtrArr(unitName.getBytes());
            ctx.data().opJumpToUnit();
            ctx.stackSize().subtract(argsFootprint());

            ctx.data().writeLabel(after);
            ctx.stackSize().subtract(0, 1);

            if (returnValueUsed()) ctx.stackSize().add(footprint());
            else ctx.data().opPopAll(footprint());
        }

        @Override
        public Type type() {
            return returnType;
        }

        public Size argsFootprint() {
            var sum = new Size.Mutable();
            for (var arg : args) {
                sum.add(arg.footprint());
            }
            return sum.capture();
        }

        private boolean returnValueUsed() {
            return parent instanceof ExprConsumer;
        }
    }
}
