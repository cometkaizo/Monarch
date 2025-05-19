package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.bytecode.Chunk;
import com.cometkaizo.monarch.structure.diagnostic.UnknownTypeErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Cast {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any valueParsers = new Any();
        private final Any typeParsers = new Any();
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            // value to cast
            var value = valueParsers.parse(ctx);
            if (!value.hasValue()) return failExpecting("value");
            raw.value = value.valueNonNull();

            if (!ctx.literal("as")) return failExpecting("'as'");
            ctx.whitespace();

            // type to cast to
            var type = typeParsers.parse(ctx);
            if (!type.hasValue()) return failExpecting("target type");
            raw.type = type.valueNonNull();

            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            Any parser;
            if (ctx.literal("values")) parser = valueParsers;
            else if (ctx.literal("types")) parser = typeParsers;
            else return false;
            ctx.whitespace();

            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var typeParserName = ctx.word();
                if (typeParserName == null) return false;
                parser.add(typeParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();
            return false;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public Structure.Raw<?> value, type;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr {
        public final Expr value;
        public final Type type;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);

            Expr value = null;
            Type type = null;

            if (raw.value.analyze(ctx) instanceof Expr expr) {
                if (!expr.isVoid()) value = expr;
                else ctx.report(new WrongTypeErr("value", "expression"), this);
            } else ctx.report(new WrongTypeErr("value", "expression"), this);

            if (raw.type.analyze(ctx) instanceof TypeGet.Analysis typeGet) {
                if (typeGet.type() != null) type = typeGet.type();
                else ctx.report(new UnknownTypeErr(typeGet.name()), this);
            } else ctx.report(new WrongTypeErr("target type", "valid type"), this);

            this.value = value;
            this.type = type;
        }

        private boolean changeSize() {
            return !type.footprint().equals(value.footprint());
        }

        @Override
        public void assemble(AssembleContext ctx) {
            Chunk c = ctx.data();
            value.assemble(ctx);
            if (changeSize()) {
                c.opPushZeros(type.footprint());
                c.opOr(type.footprint(), value.footprint());
            }
            ctx.stackSize().subtract(value.footprint());
            ctx.stackSize().add(type.footprint());
        }

        @Override
        public Type type() {
            return type;
        }
    }
}
