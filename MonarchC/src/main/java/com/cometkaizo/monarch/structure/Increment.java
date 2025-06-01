package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.bytecode.Chunk;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Increment {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any refParsers = new Any();
        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            // ref
            var ref = refParsers.parse(ctx);
            if (!ref.hasValue()) return failExpecting("expression");
            raw.ref = ref.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal("++")) return failExpecting("'++'");
            ctx.whitespace();

            if (!ctx.literal(";")) return failExpecting("';'");
            ctx.whitespace();

            ctx.popStructure();
            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var refParserName = ctx.word();
                if (refParserName == null) return false;
                refParsers.add(refParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();
            return false;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public Structure.Raw<?> ref;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis {
        public final Locatable ref;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);

            Locatable ref = null;
            if (raw.ref.analyze(ctx) instanceof Locatable expr) {
                ref = expr;
            } else ctx.report(new WrongTypeErr("assignment target", "locatable value"), this);
            this.ref = ref;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            Chunk c = ctx.data();

            // p:location
            ref.assembleLocation(ctx); // A

            { // add
                // 1:b
                c.opPush(1);
                ctx.stackSize().add(Size.ONE_BYTE); // B : required because of ref.assembleLocation(ctx)

                // ?:a
                ref.assembleLocation(ctx); // C

                c.opPush(ref.footprintAtLocation().ptrAmt());
                c.opPush(ref.footprintAtLocation().byteAmt());
                c.opMGet(); // (D)
                ctx.stackSize().subtract(Size.ONE_BYTE); // -B
                ctx.stackSize().subtract(Size.ONE_PTR); // -C

                // ?:value
                c.opAdd(ref.footprintAtLocation(), Size.ONE_BYTE);
            }

            c.opPush(ref.footprintAtLocation().ptrAmt());
            c.opPush(ref.footprintAtLocation().byteAmt());
            c.opMSet(); // (-D)
            ctx.stackSize().subtract(Size.ONE_PTR); // -A
        }
    }
}
