package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.util.Diagnostic;

public class RShift {
    public static class Parser extends BiOperator.Parser<Raw> {
        @Override
        protected Raw newRaw() {
            return new Raw();
        }
        @Override
        protected String operationSymbol() {
            return ">>";
        }
    }
    public static class SetParser extends BiOperatorSet.Parser<Raw> {
        @Override
        protected Raw newRaw() {
            return new Raw();
        }
        @Override
        protected String operationSymbol() {
            return ">>=";
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
            if (right.type() != ByteLit.Analysis.TYPE) return new WrongTypeErr("right operand", "byte expression");
            return null;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            right.assemble(ctx);
            left.assemble(ctx);

            ctx.data().opRShift(left.footprint());
            ctx.stackSize().subtract(right.footprint());
        }

        @Override
        public Type type() {
            return left == null ? null : left.type();
        }
    }
}
