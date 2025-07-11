package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.UnknownTypeErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class FloatToInt {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any valueParsers = new Any();
        private final Any typeParsers = new Any();
        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = new Raw();

            // value to convert
            var value = valueParsers.parse(ctx);
            if (!value.hasValue()) return failExpecting("expression");
            raw.value = value.valueNonNull();

            if (!ctx.literal("toInt")) return failExpecting("'toInt'");
            ctx.whitespace();

            // int type to convert to
            var type = typeParsers.parse(ctx);
            if (!type.hasValue()) return failExpecting("target type");
            raw.type = type.valueNonNull();

            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (ctx.literal("values")) return parseParserList(valueParsers, ctx);
            else if (ctx.literal("types")) return parseParserList(typeParsers, ctx);
            else return false;
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
                if (!expr.isVoid()) {
                    if (isFloat(expr.type())) value = expr;
                    else ctx.report(new WrongTypeErr("value", "float expression"), this);
                } else ctx.report(new WrongTypeErr("value", "expression"), this);
            } else ctx.report(new WrongTypeErr("value", "expression"), this);

            if (raw.type.analyze(ctx) instanceof TypeGet.Analysis typeGet) {
                if (typeGet.type() != null) type = typeGet.type();
                else ctx.report(new UnknownTypeErr(typeGet.name()), this);
            } else ctx.report(new WrongTypeErr("target type", "valid type"), this);

            this.value = value;
            this.type = type;
        }
        private boolean isFloat(Type type) {
            return Float32Lit.Analysis.TYPE.equals(type) || Float64Lit.Analysis.TYPE.equals(type);
        }

        @Override
        public void assemble(AssembleContext ctx) {
            value.assemble(ctx);
            ctx.data().opFloatToInt(value.footprint(), type.footprint());
            ctx.stackSize().subtract(value.footprint());
            ctx.stackSize().add(type.footprint());
        }

        @Override
        public Type type() {
            return type;
        }
    }
}
