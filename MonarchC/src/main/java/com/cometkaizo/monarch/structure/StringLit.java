package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringLit {
    public static class Parser extends Structure.Parser<Raw> {
        private static final Map<String, Character> ESCAPES = Map.ofEntries(
                Map.entry("\\\\", '\\'),
                Map.entry("\\n", '\n'),
                Map.entry("\\0", '\0'),
                Map.entry("\\'", '\''),
                Map.entry("\\\"", '"')
        );
        private static final Pattern FMT = Pattern.compile("^(" + Matcher.quoteReplacement(String.join("|", ESCAPES.keySet())) + "|[^\"])");
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            if (!ctx.literal("\"")) return fail();

            while (!ctx.literal("\"")) {
                var value = ctx.chars.checkAndAdvance(FMT);
                if (value == null) return fail();
                raw.value.append(value.length() == 1 ? value.charAt(0) : ESCAPES.get(value));

                if (!ctx.chars.hasNext()) return fail();
            }

            ctx.whitespace();
            return success(raw);
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public StringBuilder value = new StringBuilder();
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr {
        public final String value;
        public final Type.Static type;

        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.value = raw.value.toString();
            this.type = new Type.Static("array", new Size(value.getBytes().length, 0));
        }

        @Override
        public void assemble(AssembleContext ctx) {
//            Chunk c = ctx.data();
//
//            Size targetFootprint = type.targetType().footprint();
//            c.opPush(targetFootprint.ptrAmt());
//            c.opPush(targetFootprint.byteAmt());
//            c.opMalloc();
//
//            c.opCopy(Size.ZERO, Size.ONE_PTR);
//            c.opPushAll(value.getBytes());
//            c.opPush(targetFootprint.ptrAmt());
//            c.opPush(targetFootprint.byteAmt());
//            c.opMSet();
            ctx.data().opPushAll(value.getBytes());
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return type;
        }
    }
}
