package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.analysis.StackResource;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.UnknownVarErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.monarch.structure.resource.Vars;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.NoPrint;

public class VarGet {
    public static class Parser extends Structure.Parser<Raw> {
        @Override
        protected Result parseImpl(ParseContext ctx) {
            var raw = new Raw();

            raw.name = ctx.word();
            if (raw.name == null) return failExpecting("name");
            ctx.whitespace();

            return success(raw);
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public String name;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr, Locatable {
        @NoPrint public final Vars vars;
        public final String name;
        public final Type type;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);

            this.name = raw.name;
            Vars vars = null;
            Type type = null;

            var m = ancestors.ofType(StackResource.Manager.class);
            if (m.isPresent()) {
                vars = m.get().getOrCreate(Vars.class, Vars::new);
                if (vars.has(name)) type = vars.get(name).type();
                else ctx.report(new UnknownVarErr(name), this);
            } else ctx.report(new UnknownVarErr(name), this);
            this.vars = vars;
            this.type = type;
        }
        @Override
        public void assemble(AssembleContext ctx) {
            ctx.data().opCopy(vars.offsetOf(name, ctx), footprint());
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return type;
        }

        @Override
        public void assembleLocation(AssembleContext ctx) {
            ctx.data().opPushPtrStack(vars.offsetOf(name, ctx).plus(footprint()));
            ctx.stackSize().add(Size.ONE_PTR);
        }

        @Override
        public Type typeAtLocation() {
            return type;
        }
    }
}
