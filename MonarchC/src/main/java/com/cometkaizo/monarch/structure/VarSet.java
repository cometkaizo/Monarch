package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.analysis.StackResource;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.DifferentTypesErr;
import com.cometkaizo.monarch.structure.diagnostic.NoResourcesErr;
import com.cometkaizo.monarch.structure.diagnostic.UnknownVarErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.monarch.structure.resource.Vars;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.NoPrint;

public class VarSet {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any valueParsers = new Any();

        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            raw.name = ctx.word();
            if (raw.name == null) return fail();
            ctx.whitespace();

            if (!ctx.literal("=")) return fail();
            ctx.whitespace();

            var value = valueParsers.parse(ctx);
            if (!value.hasValue()) return fail();
            raw.value = value.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal(";")) return fail();
            ctx.whitespace();

            ctx.popStructure();
            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var statementParserName = ctx.word();
                if (statementParserName == null) return false;
                valueParsers.add(statementParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();
            return false;
        }

    }
    public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
        public String name;
        public Structure.Raw<?> value;

        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements ExprConsumer {
        public final String name;
        public final Expr value;
        @NoPrint public final Vars vars;
        public final Type type;
        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);

            this.name = raw.name;
            Vars vars = null;
            Type type = null;
            Expr value = null;

            if (raw.value.analyze(ctx) instanceof Expr expr) {
                if (!expr.isVoid()) value = expr;
                else ctx.report(new WrongTypeErr("value", "expression"));
            } else ctx.report(new WrongTypeErr("value", "expression"));

            var m = ancestors.ofType(StackResource.Manager.class);
            if (m.isPresent()) {
                vars = m.get().getOrCreate(Vars.class, Vars::new);
                if (vars.has(name)) {
                    type = vars.get(name).type();
                    if (value != null && !type.equals(value.type())) {
                        ctx.report(new DifferentTypesErr(value.type(), type));
                    }
                } else ctx.report(new UnknownVarErr(name));
            } else ctx.report(new NoResourcesErr("var_decl"));

            this.value = value;
            this.type = type;
            this.vars = vars;

            ctx.popStructure();
        }

        @Override
        public void assemble(AssembleContext ctx) {
            value.assemble(ctx);
            ctx.data().opSet(type.footprint(), vars.offsetOf(name, ctx));
            ctx.stackSize().subtract(type.footprint());
        }
    }
}
