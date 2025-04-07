package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

import java.util.ArrayList;
import java.util.List;

public class CompilationUnit {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any parsers = new Any();

        @Override
        protected Result parseImpl(ParseContext ctx) {
            if (parsers.isEmpty()) parsers.add("compile_with", ctx);

            var raw = ctx.pushStructure(new Raw());
            ctx.whitespace();

            while (ctx.chars.hasNext()) {
                var member = parsers.parse(ctx);
                if (member.success()) member.value().ifPresent(raw.members::add);
                else return fail();
            }

            ctx.popStructure();
            return success(raw);
        }

        public Any parsers() {
            return parsers;
        }
    }

    public static class Raw extends Structure.Raw<Analysis> {
        public String name;
        public List<Structure.Raw<?>> members = new ArrayList<>();

        @Override
        public Analysis analyzeImpl(AnalysisContext ctx) {
            ctx.addCompilationUnit(this);
            return new Analysis(this, ctx);
        }
    }

    public static class Analysis extends Structure.Analysis {
        public final String name;
        public final List<Structure.Analysis> members;
        private Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);
            this.name = raw.name;
            this.members = ctx.analyze(raw.members);
            ctx.popStructure();
        }

        @Override
        public void assemble(AssembleContext ctx) {
            members.forEach(m -> m.assemble(ctx));
        }

    }
}
