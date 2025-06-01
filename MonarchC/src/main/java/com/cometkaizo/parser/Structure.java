package com.cometkaizo.parser;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.bytecode.Chunk;
import com.cometkaizo.monarch.structure.diagnostic.DuplicateParserErr;
import com.cometkaizo.monarch.structure.diagnostic.UnknownParserErr;
import com.cometkaizo.util.CharPosition;
import com.cometkaizo.util.NoPrint;
import com.cometkaizo.util.StringUtils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.cometkaizo.util.CollectionUtils.prepend;
import static com.cometkaizo.util.StringUtils.displayFields;

public final class Structure {

    // TODO: 2024-04-26 refactor subclasses of this into each structure (like Raw, Analysis, and Runtime)
    public abstract static class Parser<R extends Raw<?>> {

        public Result<? extends R> parse(ParseContext ctx) {
            return parse(ctx, this::parseImpl);
        }
        public <V extends Raw<?>> V createRaw(ParseContext ctx, Supplier<V> constructor) {
            return parse(ctx, _ctx -> success(constructor.get())).valueNonNull();
        }
        protected <V extends Raw<?>> Result<V> parse(ParseContext ctx, Function<ParseContext, ? extends Result<V>> parser) {
            ctx.enterFrame();

            var startIndex = ctx.chars.position();
            var result = parser.apply(ctx);
            var endIndex = ctx.chars.position();

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

        protected abstract Result<? extends R> parseImpl(ParseContext ctx);

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

        protected <V extends Raw<?>> Result<V> success(V value) {
            return new Result<>(true, value, null);
        }
        protected <V extends Raw<?>> Result<V> success() {
            return new Result<>(true, null, null);
        }
        protected <V extends Raw<?>> Result<V> fail() {
            return fail(defaultFailMessage());
        }
        protected <V extends Raw<?>> Result<V> failExpecting(String expected) {
            return fail(defaultFailMessage() + ": " + expected + " expected here");
        }
        protected <V extends Raw<?>> Result<V> fail(String message) {
            return new Result<>(false, null, message);
        }
        private String defaultFailMessage() {
            return StringUtils.nameNoPkg(getClass());
        }
        public static class Result<V extends Raw<?>> {
            private final boolean success;
            private final V value;
            private final String failMessage;
            private Result(boolean success, V value, String failMessage) {
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
            public Optional<V> value() {
                return Optional.ofNullable(valueOrNull());
            }
            public V valueOrNull() {
                if (!success) throw new IllegalStateException("Failed result has no value");
                return value;
            }
            public V valueNonNull() {
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
            @SuppressWarnings("RedundantCast")
            protected Result<?> parseImpl(ParseContext ctx) {
                for (var parser : parsers) {
                    var member = parser.parse(ctx);
                    if (member.success()) {
                        return (Result<?>) member; // this cast is actually necessary, but IntelliJ does not see that
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
    }

    public abstract static class Raw<T extends Analysis> {
        public Raw<?> parent;
        private volatile T analysis;
        public CharPosition startIndex, endIndex;
        public T analyze(AnalysisContext ctx) {
            if (parent != null && parent.analysis == null) parent.analyze(ctx);
            return analyzeInIsolation(ctx);
        }
        public T analyzeInIsolation(AnalysisContext ctx) {
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
        @NoPrint public final CharPosition startIndex;
        @NoPrint public final CharPosition endIndex;

        protected <T extends Analysis> Analysis(Raw<T> raw, AnalysisContext ctx) {
            raw.analysis = (T)this;
            this.parent = findParent(raw, ctx);//ctx.topStructure();
            this.startIndex = raw.startIndex;
            this.endIndex = raw.endIndex;
            if (this.parent != null) {
                this.ancestors = new AncestorList(prepend(this.parent.ancestors, this.parent));
            } else {
                this.ancestors = new AncestorList(List.of());
            }
        }

        private <T extends Analysis> Analysis findParent(Raw<T> raw, AnalysisContext ctx) {
            if (raw.parent == null) return null;
            var parent = raw.parent.analyze(ctx);
            if (parent == null) return findParent(raw.parent, ctx);
            return parent;
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

            public String toHierarchyString() {
                StringBuilder b = new StringBuilder("{");
                for (int i = size() - 1; i >= 0; i --) {
                    b.append(StringUtils.nameNoPkg(get(i).getClass()));
                    if (i > 0) b.append(" > ");
                }
                return b.append('}').toString();
            }
        }
    }

    @Deprecated
    public interface Interpreter {
        void assemble(Chunk c);
        String name();
    }
}
