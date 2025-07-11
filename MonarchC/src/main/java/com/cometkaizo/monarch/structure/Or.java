package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.IncompatibleTypesErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.util.Diagnostic;

public class Or {
    public static class Parser extends BiOperator.Parser<Raw> {
        @Override
        protected Raw newRaw() {
            return new Raw();
        }
        @Override
        protected String operationSymbol() {
            return "|";
        }
    }
    public static class SetParser extends BiOperatorSet.Parser<Raw> {
        @Override
        protected Raw newRaw() {
            return new Raw();
        }
        @Override
        protected String operationSymbol() {
            return "|=";
        }
    }
    public static class Raw extends BiOperator.Raw<Analysis> {
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends BiOperator.Analysis implements Expr {
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
        }

        @Override
        protected Diagnostic validateLeft(Expr left) {
            return left.isVoid() ? new WrongTypeErr("left operand", "expression") : null;
        }
        @Override
        protected Diagnostic validateRight(Expr right) {
            if (right.isVoid()) return new WrongTypeErr("right operand", "expression");
            if (left != null && !right.type().equals(left.type())) return new IncompatibleTypesErr(right.type(), left.type());
            return null;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            left.assemble(ctx);
            right.assemble(ctx);

            ctx.data().opOr(left.footprint(), right.footprint());
            ctx.stackSize().subtract(footprint());
        }

        @Override
        public Type type() {
            return left.type();
        }
    }
}
