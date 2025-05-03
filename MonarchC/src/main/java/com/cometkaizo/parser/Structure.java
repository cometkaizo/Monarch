package com.cometkaizo.parser;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.bytecode.Chunk;
import com.cometkaizo.monarch.Compiler;
import com.cometkaizo.monarch.structure.diagnostic.DuplicateParserErr;
import com.cometkaizo.monarch.structure.diagnostic.UnknownParserErr;
import com.cometkaizo.util.NoPrint;
import com.cometkaizo.util.StringUtils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.cometkaizo.util.CollectionUtils.prepend;
import static com.cometkaizo.util.StringUtils.displayFields;

public final class Structure {

    // TODO: 2024-04-26 refactor subclasses of this into each structure (like Raw, Analysis, and Runtime)
    public abstract static class Parser<R extends Raw<?>> {

        public Parser<? extends R>.Result parse(ParseContext ctx) {
            ctx.enterFrame();

            int startIndex = ctx.chars.cursor();
            var result = parseImpl(ctx);
            int endIndex = ctx.chars.cursor();

            if (result.success()) {
                result.value().ifPresent(raw -> {
                    raw.parent = ctx.topStructure();
                    raw.startIndex = startIndex;
                    raw.endIndex = endIndex;
                });
                ctx.exitFrameSuccess();
            } else {
                ctx.exitFrameFail(result.failMessage());
            }
            return result;
        }

        protected abstract Parser<? extends R>.Result parseImpl(ParseContext ctx);

        public boolean parseSettings(ParseContext ctx) {
            ctx.enterFrame();
            boolean success = parseSettingsImpl(ctx);
            if (success) ctx.exitFrameSuccess();
            else ctx.exitFrameFail(defaultFailMessage());
            return success;
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            return false;
        }

        protected Result success(R value) {
            return new Result(true, value, null);
        }
        protected Result success() {
            return new Result(true, null, null);
        }
        protected Result fail() {
            return fail(defaultFailMessage());
        }
        protected Result failExpecting(String expected) {
            return fail(expected + " expected here");
        }
        protected Result fail(String message) {
            return new Result(false, null, message);
        }
        private String defaultFailMessage() {
            return StringUtils.nameNoPkg(getClass());
        }
        public class Result {
            private final boolean success;
            private final R value;
            private final String failMessage;
            private Result(boolean success, R value, String failMessage) {
                this.success = success;
                this.value = value;
                this.failMessage = failMessage;
            }
            public boolean success() {
                return success;
            }
            public boolean hasValue() {
                return success && value != null;
            }
            public Optional<R> value() {
                return Optional.ofNullable(valueOrNull());
            }
            public R valueOrNull() {
                if (!success) throw new IllegalStateException("Failed result has no value");
                return value;
            }
            public R valueNonNull() {
                return value().orElseThrow();
            }
            public String failMessage() {
                if (success) throw new IllegalStateException("Successful result has not fail message");
                return failMessage;
            }
        }

        public static class Any extends Parser<Raw<?>> {
            private final List<Parser<?>> parsers = new ArrayList<>();

            @Override
            protected Parser<? extends Raw<?>>.Result parseImpl(ParseContext ctx) {
                for (var parser : parsers) {
                    var member = parser.parse(ctx);
                    if (member.success()) {
                        return member;
                    }
                }
                return fail();
            }

            public boolean add(String name, ParseContext ctx) {
                var parser = ctx.getParser(name);
                if (parser.isEmpty()) ctx.report(new UnknownParserErr(name));
                else if (parsers.contains(parser.get())) ctx.report(new DuplicateParserErr(name));
                else {
                    parsers.add(parser.get());
                    return true;
                }
                return false;
            }
            public List<Structure.Parser<?>> asList() {
                return parsers;
            }
            public boolean isEmpty() {
                return parsers.isEmpty();
            }
            public boolean isNotEmpty() {
                return !isEmpty();
            }
            public int size() {
                return parsers.size();
            }

            public void clear() {
                parsers.clear();
            }
        }
        public static class One extends Parser<Raw<?>> {
            private final Compiler compiler;
            private Structure.Parser<?> parser;

            public One(Compiler compiler) {
                this.compiler = compiler;
            }

            @Override
            protected Parser<? extends Raw<?>>.Result parseImpl(ParseContext ctx) {
                if (parser == null) return fail();
                return parser.parse(ctx);
            }

            public void set(String name, ParseContext ctx) {
                var parser = compiler.getParser(name);
                if (parser.isEmpty()) ctx.report(new UnknownParserErr(name));
                else this.parser = parser.get();
            }
        }
    }

    public abstract static class Raw<T extends Analysis> {
        public Raw<?> parent;
        private volatile T analysis;
        public int startIndex, endIndex;
        public T analyze(AnalysisContext ctx) {
            if (analysis == null) {
                synchronized (this) {
                    if (analysis == null) {
                        analysis = analyzeImpl(ctx);
                    }
                }
            }
            return analysis;
        }
        protected abstract T analyzeImpl(AnalysisContext ctx);
    }

    public abstract static class Analysis {
        @NoPrint public final Analysis parent;
        @NoPrint public final AncestorList ancestors;
        @NoPrint public final int startIndex;
        @NoPrint public final int endIndex;

        protected Analysis(Raw<?> raw, AnalysisContext ctx) {
            this.parent = ctx.topStructure();
            this.startIndex = raw.startIndex;
            this.endIndex = raw.endIndex;
            if (this.parent != null) {
                this.ancestors = new AncestorList(prepend(this.parent.ancestors, this.parent));
            } else {
                this.ancestors = new AncestorList(List.of());
            }
        }

        public abstract void assemble(AssembleContext ctx);

        @Override
        public String toString() {
            return displayFields(this, StringUtils::nameNoPkg, f -> f.getAnnotation(NoPrint.class) == null);
        }

        public static class AncestorList extends AbstractList<Analysis> {
            private final List<Analysis> backer;
            public AncestorList(List<Analysis> backer) {
                this.backer = backer;
            }

            public <T> Optional<T> ofType(Class<T> type) {
                return allOfType(type).findFirst();
            }
            public <T> Stream<T> allOfType(Class<T> type) {
                return stream().filter(type::isInstance).map(type::cast);
            }

            @Override
            public Analysis get(int index) {
                return backer.get(index);
            }

            @Override
            public int size() {
                return backer.size();
            }
        }
    }

    @Deprecated
    public interface Interpreter {
        void assemble(Chunk c);
        String name();
    }
}
