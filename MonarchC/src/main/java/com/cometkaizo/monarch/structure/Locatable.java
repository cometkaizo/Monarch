package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.IncompatibleTypesErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.Structure;

public interface Locatable {
    /**
     * Assembles instructions that push a single pointer of the location of this expression to the stack.
     */
    void assembleLocation(AssembleContext ctx);
    Type typeAtLocation();
    default Size footprintAtLocation() {
        return typeAtLocation().footprint();
    }

    class Set {
        public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
            public Structure.Raw<?> ref, value;

            @Override
            protected Analysis analyzeImpl(AnalysisContext ctx) {
                return new Analysis(this, ctx);
            }
        }
        public static class Analysis extends Structure.Analysis implements ExprConsumer {
            public final Locatable ref;
            public final Expr value;
            public final Type type;

            public Analysis(Raw raw, AnalysisContext ctx) {
                super(raw, ctx);

                Locatable ref = null;
                Expr value = null;
                Type type = null;

                if (raw.ref.analyze(ctx) instanceof Locatable locatable) {
                    ref = locatable;
                } else ctx.report(new WrongTypeErr("assignment target", "locatable value"), this);

                if (raw.value.analyze(ctx) instanceof Expr expr) {
                    if (!expr.isVoid()) value = expr;
                    else ctx.report(new WrongTypeErr("value", "expression"), this);
                } else ctx.report(new WrongTypeErr("value", "expression"), this);

                if (ref != null && value != null) {
                    if (isCompatible(ref, value)) type = ref.typeAtLocation();
                    else ctx.report(new IncompatibleTypesErr(value.type(), ref.typeAtLocation()), this);
                }

                this.ref = ref;
                this.value = value;
                this.type = type;
            }

            private boolean isCompatible(Locatable ref, Expr value) {
                Type left = ref.typeAtLocation();
                Type right = value.type();
                return left != null && left.equals(right) ||
                        left instanceof Type.Ref(boolean targetTypeKnown, Type targetType) &&
                                (!targetTypeKnown || targetType.equals(right));
            }

            @Override
            public void assemble(AssembleContext ctx) {
                // p:location
                ref.assembleLocation(ctx);
                // ?:value
                value.assemble(ctx);

                // 1:byte_size,1:ptr_size
                ctx.data().opPush(value.footprint().ptrAmt());
                ctx.data().opPush(value.footprint().byteAmt());

                // set the value to the pointer
                ctx.data().opMSet();
                ctx.stackSize().subtract(value.footprint());
                ctx.stackSize().subtract(Size.ONE_PTR);
            }
        }
    }

    class Get {
        public static class Raw extends Structure.Raw<Analysis> {
            public Structure.Raw<?> ref;
            public Raw(Structure.Raw<?> ref) {
                this.ref = ref;
            }

            @Override
            protected Analysis analyzeImpl(AnalysisContext ctx) {
                return new Analysis(this, ctx);
            }
        }
        public static class Analysis extends Structure.Analysis implements Expr, Locatable {
            public final Locatable ref;
            protected Analysis(Raw raw, AnalysisContext ctx) {
                super(raw, ctx);
                Locatable ref = null;
                if (raw.ref.analyze(ctx) instanceof Locatable locatable) {
                    ref = locatable;
                } else ctx.report(new WrongTypeErr("target", "locatable value"));
                this.ref = ref;
            }
            @Override
            public void assemble(AssembleContext ctx) {
                ref.assembleLocation(ctx);
                ctx.data().opPush(ref.footprintAtLocation().ptrAmt());
                ctx.data().opPush(ref.footprintAtLocation().byteAmt());
                ctx.data().opMGet();
                ctx.stackSize().subtract(Size.ONE_PTR);
                ctx.stackSize().add(ref.footprintAtLocation());
            }
            @Override
            public Type type() {
                return ref == null ? null : ref.typeAtLocation();
            }
            @Override
            public void assembleLocation(AssembleContext ctx) {
                ref.assembleLocation(ctx);
            }
            @Override
            public Type typeAtLocation() {
                return ref == null ? null : ref.typeAtLocation();
            }
        }
    }
}
