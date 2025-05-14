package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.IncompatibleTypesErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongSizeErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.util.Diagnostic;

public class Subtract {
    public static class Parser extends BiOperator.Parser<Raw> {
        @Override
        protected Raw newRaw() {
            return new Raw();
        }
        @Override
        protected String operationSymbol() {
            return "-";
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

            if (footprint().ptrAmt() != 0) {
                ctx.report(new WrongSizeErr("operand cannot have pointer size"), this);
            }
        }

        @Override
        protected Diagnostic validateLeft(Expr expr) {
            return expr.isVoid() ? new WrongTypeErr("left operand", "expression") : null;
        }
        @Override
        protected Diagnostic validateRight(Expr expr) {
            if (expr.isVoid()) return new WrongTypeErr("right operand", "expression");
            if (left != null) {
                if (expr.type().equals(left.type())) return null;
                if (expr.type() instanceof Type.Ref || left.type() instanceof Type.Ref) return null;
                return new IncompatibleTypesErr(expr.type(), left.type());
            }
            return null;
        }
        @Override
        public void assemble(AssembleContext ctx) {
            right.assemble(ctx);
            left.assemble(ctx);

            ctx.data().opSubtract(left.footprint(), right.footprint());
            ctx.stackSize().subtract(footprint());

//            Old method, using add operation:
//            // a - b
//
//            right.assemble(ctx); // a - _
//
//            // invert b (1's complement)
//            int[] ones = new int[footprint().byteAmt()];
//            Arrays.fill(ones, 0xFF);
//            ctx.data().opPushAll(ones);
//            ctx.data().opXor(footprint(), footprint());
//
//            left.assemble(ctx); // _ - b
//
//            // add together
//            ctx.data().opAdd(footprint(), footprint());
//            ctx.stackSize().subtract(footprint());
//
//            // add 1
//            ctx.data().opPush(1);
//            ctx.data().opAdd(footprint(), new Size(1, 0));
        }

        @Override
        public Type type() {
            return left.type();
        }
    }
}
