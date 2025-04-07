package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.StackResource;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.NoResourcesErr;
import com.cometkaizo.monarch.structure.diagnostic.UnknownTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.monarch.structure.resource.Var;
import com.cometkaizo.monarch.structure.resource.Vars;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class VarDecl {
    public static class Parser extends Structure.Parser<Raw> {
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
            raw.type = ctx.word();
            if (raw.type == null) return fail();
            ctx.whitespace();

            if (!ctx.literal(";")) return fail();
            ctx.whitespace();

            return success(raw);
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public String name, type;
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
            var type = ctx.getType(raw.type);
            if (type.isPresent()) this.type = type.get();
            else {
                ctx.report(new UnknownTypeErr(raw.type));
                this.type = null;
            }

            var m = ancestors.ofType(StackResource.Manager.class);
            if (m.isPresent()) m.get().getOrCreate(Vars.class, Vars::new).addVar(new Var(name, this.type), ctx);
            else ctx.report(new NoResourcesErr("var_decl"));
        }

        @Override
        public void assemble(AssembleContext ctx) {

        }
    }
}
