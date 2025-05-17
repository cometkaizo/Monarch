package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.IncompatibleTypesErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.util.Diagnostic;

public class Equals {
    public static class Parser extends BiOperator.Parser<Raw> {
        @Override
        protected Raw newRaw() {
            return new Raw();
        }
        @Override
        protected String operationSymbol() {
            return "==";
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
        protected Diagnostic validateLeft(Expr expr) {
            return expr.isVoid() ? new WrongTypeErr("left operand", "expression") : null;
        }
        @Override
        protected Diagnostic validateRight(Expr expr) {
            if (expr.isVoid()) return new WrongTypeErr("right operand", "expression");
            if (left != null && !expr.type().equals(left.type())) return new IncompatibleTypesErr(expr.type(), left.type());
            return null;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            left.assemble(ctx);
            right.assemble(ctx);

            ctx.data().opEquals(left.footprint(), right.footprint());
            ctx.stackSize().subtract(left.footprint());
            ctx.stackSize().subtract(right.footprint());
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return BooleanLit.Analysis.TYPE;
        }
    }
}
