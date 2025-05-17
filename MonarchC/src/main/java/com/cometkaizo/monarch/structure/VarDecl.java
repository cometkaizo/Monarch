package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.StackResource;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.DuplicateVarErr;
import com.cometkaizo.monarch.structure.diagnostic.NoResourcesErr;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.monarch.structure.resource.Var;
import com.cometkaizo.monarch.structure.resource.Vars;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class VarDecl {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any typeParsers = new Any();
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            if (!ctx.literal("var")) return fail();
            if (!ctx.whitespace()) return fail();

            // name
            raw.name = ctx.word();
            if (raw.name == null) return fail();
            ctx.whitespace();

            // type
            if (!ctx.literal(":")) return fail();
            ctx.whitespace();
            var type = typeParsers.parse(ctx);
            if (!type.hasValue()) return fail();
            raw.type = type.valueNonNull();
            ctx.whitespace();

            if (!ctx.literal(";")) return fail();
            ctx.whitespace();

            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var typeParserName = ctx.word();
                if (typeParserName == null) return false;
                typeParsers.add(typeParserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();
            return false;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public String name;
        public Structure.Raw<?> type;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis {
        public final String name;
        public final Type type;

        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.name = raw.name;

            Type type = null;
            if (raw.type.analyze(ctx) instanceof TypeGet.Analysis typeGet) {
                if (typeGet.type() != null) type = typeGet.type();
            } else ctx.report(new WrongTypeErr("variable type", "valid type"), this);
            this.type = type;

            var m = ancestors.ofType(StackResource.Manager.class);
            if (m.isPresent()) {
                boolean success = m.get().getOrCreate(Vars.class, Vars::new).addVar(new Var(name, this.type));
                if (!success) ctx.report(new DuplicateVarErr(name), this);
            } else ctx.report(new NoResourcesErr("var_decl"), this);
        }

        @Override
        public void assemble(AssembleContext ctx) {

        }
    }
}
