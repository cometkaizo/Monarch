package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.UnknownFuncErr;
import com.cometkaizo.monarch.structure.diagnostic.UnknownTypeErr;
import com.cometkaizo.monarch.structure.diagnostic.UnknownUnitErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.cometkaizo.util.CollectionUtils.only;

public class FuncCall {
    public static class Parser extends Structure.Parser<Raw> {
        private static final Pattern UNIT_NAME_FMT = Pattern.compile("[^: ]+");
        private final Any argsParsers = new Any();

        @Override
        protected Result parseImpl(ParseContext ctx) {
            var parent = ctx.topStructure();
            var raw = ctx.pushStructure(new Raw());

            // unit/namespace
            raw.unitName = ctx.chars.checkAndAdvance(UNIT_NAME_FMT);
            if (raw.unitName == null) return fail();
            ctx.whitespace();

            if (!ctx.literal(":")) return fail();
            ctx.whitespace();

            // name
            raw.funcName = ctx.word();
            if (raw.funcName == null) return fail();
            ctx.whitespace();

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

                var statementParserName = ctx.word();
                if (statementParserName == null) return false;
                argsParsers.add(statementParserName, ctx);

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
                else ctx.report(new WrongTypeErr("argument", "expression"), this);
            }
            this.args = Collections.unmodifiableList(args);

            // find return type
            var unit = ctx.getCompilationUnit(unitName);
            Type returnType = null;
            if (unit.isPresent()) {
                var func = only(unit.get().members, Func.Raw.class).stream().filter(f -> funcName.equals(f.name)).findAny();
                if (func.isPresent()) {
                    String returnTypeName = func.get().returnType;
                    if (!"void".equals(returnTypeName)) {
                        var returnTypeOpt = ctx.getType(returnTypeName);
                        if (returnTypeOpt.isPresent()) {
                            returnType = returnTypeOpt.get();
                        } else ctx.report(new UnknownTypeErr(returnTypeName), this);
                    }
                } else ctx.report(new UnknownFuncErr(funcName, unitName), this);
            } else ctx.report(new UnknownUnitErr(unitName), this);
            this.returnType = returnType;
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
