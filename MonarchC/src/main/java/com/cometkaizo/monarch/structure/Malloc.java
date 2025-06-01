package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

// todo: make a version of this that accepts a type
public class Malloc {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any sizeParsers = new Any();
        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("&!")) return failExpecting("'&!'");
            ctx.whitespace();

            // byte size
            var byteSize = sizeParsers.parse(ctx);
            if (!byteSize.hasValue()) return failExpecting("byte size");
            raw.byteSize = byteSize.valueNonNull();

            // ptr size
            var ptrSize = sizeParsers.parse(ctx);
            if (!ptrSize.hasValue()) return failExpecting("pointer size");
            raw.ptrSize = ptrSize.valueNonNull();

            ctx.popStructure();
            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var sizeParserName = ctx.word();
                if (sizeParserName == null) return false;
                sizeParsers.add(sizeParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();
            return false;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
        public Structure.Raw<?> byteSize, ptrSize;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr, ExprConsumer {
        public final Expr byteSize, ptrSize;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);

            byteSize = getSize(raw.byteSize, ctx, "byte");
            ptrSize = getSize(raw.ptrSize, ctx, "ptr");
        }
        private Expr getSize(Structure.Raw<?> sizeRaw, AnalysisContext ctx, String name) {
            Expr size = null;
            if (sizeRaw.analyze(ctx) instanceof Expr expr) {
                if (!expr.isVoid()) {
                    if (expr.footprint().equals(Size.ONE_BYTE)) size = expr;
                    else ctx.report(new WrongTypeErr("allocation " + name + " size value (" + expr.typeName() + ")", "one-byte expression"), this);
                } else ctx.report(new WrongTypeErr("allocation " + name + " size value", "expression"), this);
            } else ctx.report(new WrongTypeErr("allocation " + name + " size value", "expression"), this);
            return size;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            var c = ctx.data();

            ptrSize.assemble(ctx);
            byteSize.assemble(ctx);
            c.opMalloc();

            ctx.stackSize().subtract(ptrSize.footprint());
            ctx.stackSize().subtract(byteSize.footprint());
            ctx.stackSize().add(Size.ONE_PTR);
        }

        @Override
        public Type type() {
            return new Type.Ref();
        }
    }
}
