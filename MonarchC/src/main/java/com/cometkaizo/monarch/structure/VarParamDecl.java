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
import com.cometkaizo.parser.ParamDecl;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.NoPrint;

public class VarParamDecl {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any typeParsers = new Any();
        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = new Raw();

            // name
            raw.name = ctx.word();
            if (raw.name == null) return failExpecting("name");
            ctx.whitespace();

            // type
            if (!ctx.literal(":")) return failExpecting("':'");
            ctx.whitespace();
            var type = typeParsers.parse(ctx);
            if (!type.hasValue()) return failExpecting("type");
            raw.type = type.valueNonNull();
            ctx.whitespace();

            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            return parseParserList(typeParsers, ctx);
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
    public static class Analysis extends Structure.Analysis implements ParamDecl {
        public final String name;
        public final Type type;
        @NoPrint public final StackResource.Manager resources;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.name = raw.name;

            Type type = null;
            if (raw.type.analyze(ctx) instanceof TypeGet.Analysis typeGet) {
                if (typeGet.type() != null) type = typeGet.type();
            } else ctx.report(new WrongTypeErr("variable type", "valid type"), this);
            this.type = type;

            var m = ancestors.ofType(StackResource.Manager.class);
            if (m.isPresent()) {
                this.resources = m.get();
                boolean success = this.resources.getOrCreate(Vars.class, Vars::new).addParam(new Var(name, this.type));
                if (!success) ctx.report(new DuplicateVarErr(name), this);
            } else {
                ctx.report(new NoResourcesErr("var_param_decl"), this);
                this.resources = null;
            }
        }

        @Override
        public void assemble(AssembleContext ctx) {

        }

        @Override
        public Type type() {
            return type;
        }
    }
}
