package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.diagnostic.LingeringStackElementsException;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.cometkaizo.util.CollectionUtils.only;

public class CompilationUnit {
    public static class Parser extends Structure.Parser<Raw> {
        public static final Pattern UNIT_NAME_FMT = Pattern.compile("[A-Za-z0-9._-]+");
        private final Any parsers = new Any();
        private final String name;

        public Parser(String name) {
            this.name = name;
        }

        @Override
        protected Result parseImpl(ParseContext ctx) {
            if (parsers.isEmpty()) parsers.add("compile_with", ctx);

            var raw = ctx.pushStructure(new Raw(name));
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

        public Raw(String name) {
            this.name = name;
        }

        @Override
        public Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }

        // todo: change to hierarchy similar to Vars
        public <T> Optional<T> findMember(Class<T> type, Predicate<? super T> filter) {
            return only(members, type).stream().filter(filter).findAny();
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
            if (!ctx.stackSize().capture().isZero())
                throw new LingeringStackElementsException(this, name, ctx.stackSize());
        }

    }
}
