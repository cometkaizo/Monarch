package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.IncompatibleTypesErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.util.Diagnostic;

public class Modulo {
    public static class Parser extends BiOperator.Parser<Raw> {
        @Override
        protected Raw newRaw() {
            return new Raw();
        }
        @Override
        protected String operationSymbol() {
            return "%";
        }
    }
    public static class SetParser extends BiOperatorSet.Parser<Raw> {
        @Override
        protected Raw newRaw() {
            return new Raw();
        }
        @Override
        protected String operationSymbol() {
            return "%=";
        }
    }
    public static class Raw extends BiOperator.Raw<Analysis> {
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends BiOperator.Analysis implements Expr {
        public final boolean isFloat;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            var left = raw.left.analyze(ctx);
            isFloat = left instanceof Expr expr && isFloat(expr);
        }
        private boolean isFloat(Expr expr) {
            return Float32Lit.isTypeOf(expr) || Float64Lit.isTypeOf(expr);
        }

        @Override
        protected Diagnostic validateLeft(Expr left) {
            return left.isVoid() ? new WrongTypeErr("left operand", "expression") : null;
        }
        @Override
        protected Diagnostic validateRight(Expr right) {
            if (right.isVoid()) return new WrongTypeErr("right operand", "expression");
            if (left == null) return null;
            if (right.type().equals(left.type())) return null;
            if (isFloat && isFloat(right)) return null;
            return new IncompatibleTypesErr(right.type(), left.type());
        }

        @Override
        public void assemble(AssembleContext ctx) {
            right.assemble(ctx);
            left.assemble(ctx);

            if (isFloat) ctx.data().opModuloFloat(left.footprint(), right.footprint());
            else ctx.data().opModulo(left.footprint(), right.footprint());
            ctx.stackSize().subtract(footprint());
        }

        @Override
        public Type type() {
            return left == null ? null : left.type();
        }
    }
}
