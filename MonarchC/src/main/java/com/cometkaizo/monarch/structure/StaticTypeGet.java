package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.monarch.structure.diagnostic.UnknownUnitErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;

import static com.cometkaizo.monarch.structure.CompilationUnit.Parser.UNIT_NAME_FMT;

public class StaticTypeGet {
    public static class Parser extends TypeGet.Parser<Raw> {
        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = new Raw();

            var unitOrTypeName = ctx.chars.checkAndAdvance(UNIT_NAME_FMT);
            if (unitOrTypeName == null) return failExpecting("type or unit name");
            ctx.whitespace();

            // unit name may or may not be specified
            if (ctx.literal(":")) {
                ctx.whitespace();

                raw.unitName = unitOrTypeName;

                raw.typeName = ctx.word();
                if (raw.typeName == null) return failExpecting("type name");
                ctx.whitespace();
            } else {
                raw.unitName = ctx.getCurrentCompilationUnit().name;
                raw.typeName = unitOrTypeName;
            }

            return success(raw);
        }
    }
    public static class Raw extends TypeGet.Raw<Analysis> {
        public String unitName, typeName;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends TypeGet.Analysis {
        public final String unitName, typeName;
        public final Type.Static type;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.unitName = raw.unitName;
            this.typeName = raw.typeName;

            Type.Static type = null;
            var unitOpt = ctx.getCompilationUnit(unitName);
            if (unitOpt.isPresent()) {
                var typeOpt = unitOpt.get().findMember(TypeDecl.Raw.class, t -> t.name.equals(typeName));
                if (typeOpt.isPresent()) type = typeOpt.get().analyze(ctx).toType();
            } else ctx.report(new UnknownUnitErr(unitName), this);
            this.type = type;
        }

        @Override
        public String name() {
            return typeName;
        }

        @Override
        public Type.Static type() {
            return type;
        }
    }
}
