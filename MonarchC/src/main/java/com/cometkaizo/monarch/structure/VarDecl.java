package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.ExprConsumer;
import com.cometkaizo.analysis.StackResource;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.CannotInferVarTypeErr;
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
        private final Any typeParsers = new Any(), initializerParsers = new Any();
        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("var")) return failExpecting("'var'");
            if (!ctx.whitespace()) return failExpecting("whitespace");

            // name
            raw.name = ctx.word();
            if (raw.name == null) return failExpecting("name");
            ctx.whitespace();

            // type
            if (ctx.literal(":")) {
                ctx.whitespace();
                var type = typeParsers.parse(ctx);
                if (!type.hasValue()) return failExpecting("type");
                raw.type = type.valueNonNull();
                ctx.whitespace();
            }

            if (ctx.literal("=")) {
                ctx.whitespace();

                var initializer = parse(ctx, _ctx -> {
                    var initializerRaw = new Set.Raw();

                    // expr
                    var initializerExpr = initializerParsers.parse(ctx);
                    if (!initializerExpr.hasValue()) return failExpecting("expression");
                    initializerRaw.value = initializerExpr.valueNonNull();
                    ctx.whitespace();

                    // target
                    var refRaw = createRaw(ctx, VarGet.Raw::new);
                    refRaw.name = raw.name;
                    initializerRaw.ref = refRaw;

                    return success(initializerRaw);
                });

                if (!initializer.hasValue()) return fail();
                raw.initializer = initializer.valueNonNull();
            }
            if (!ctx.literal(";")) return failExpecting("';'");
            ctx.whitespace();

            ctx.popStructure();
            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            Any parsers;
            if (ctx.literal("types")) parsers = typeParsers;
            else if (ctx.literal("initializers")) parsers = initializerParsers;
            else return false;

            ctx.whitespace();
            if (!ctx.literal("(")) return false;

            do {
                ctx.whitespace();

                var parserName = ctx.word();
                if (parserName == null) return false;
                parsers.add(parserName, ctx);

                ctx.whitespace();
            } while (ctx.literal(","));

            if (ctx.literal(")")) return true;
            ctx.whitespace();
            return false;
        }
    }
    public static class Raw extends Structure.Raw<Analysis> implements ExprConsumer {
        public String name;
        public Structure.Raw<?> type;
        public Set.Raw initializer;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements ExprConsumer {
        public final String name;
        public final Type type;
        public final Set.Analysis initializer;

        public Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            this.name = raw.name;

            Type type = null;
            if (raw.type != null) {
                if (raw.type.analyze(ctx) instanceof TypeGet.Analysis typeGet) {
                    if (typeGet.type() != null) type = typeGet.type();
                } else ctx.report(new WrongTypeErr("variable type", "valid type"), this);
            } else if (raw.initializer != null) {
                if (raw.initializer.value.analyzeInIsolation(ctx) instanceof Expr expr) {
                    if (!expr.isVoid()) type = expr.type();
                    else ctx.report(new CannotInferVarTypeErr(), this);
                } else ctx.report(new CannotInferVarTypeErr(), this);
            } else ctx.report(new CannotInferVarTypeErr(), this);
            this.type = type;

            var m = ancestors.ofType(StackResource.Manager.class);
            if (m.isPresent()) {
                boolean success = m.get().getOrCreate(Vars.class, Vars::new).addVar(new Var(name, this.type));
                if (!success) ctx.report(new DuplicateVarErr(name), this);
            } else ctx.report(new NoResourcesErr("var_decl"), this);

            if (raw.initializer == null) initializer = null;
            else initializer = raw.initializer.analyze(ctx);
        }

        @Override
        public void assemble(AssembleContext ctx) {
            if (initializer != null) initializer.assemble(ctx);
        }
    }
}
