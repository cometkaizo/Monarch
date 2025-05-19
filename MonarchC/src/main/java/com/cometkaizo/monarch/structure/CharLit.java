package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharLit {
    public static class Parser extends Structure.Parser<Raw> {
        private static final Map<String, Character> ESCAPES = Map.ofEntries(
                Map.entry("\\\\", '\\'),
                Map.entry("\\n", '\n'),
                Map.entry("\\0", '\0'),
                Map.entry("\\'", '\''),
                Map.entry("\\\"", '"')
        );
        private static final Pattern FMT = Pattern.compile("^(" + Matcher.quoteReplacement(String.join("|", ESCAPES.keySet())) + "|[^'])");
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            if (!ctx.literal("'")) return failExpecting("single quote");

            var value = ctx.chars.checkAndAdvance(FMT);
            if (value == null) return failExpecting("character or escape sequence");
            raw.value = value.length() == 1 ? value.charAt(0) : ESCAPES.get(value);

            if (!ctx.literal("'")) return failExpecting("single quote");

            return success(raw);
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public char value;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr {
        public final byte value;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.value = (byte)raw.value;
        }

        @Override
        public void assemble(AssembleContext ctx) {
            ctx.data().opPush(value);
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return ByteLit.Analysis.TYPE;
        }
    }
}
