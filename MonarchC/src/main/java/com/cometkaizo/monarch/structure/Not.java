package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.diagnostic.WrongTypeErr;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.ParseContext;
import com.cometkaizo.parser.Structure;

public class Not {
    public static class Parser extends Structure.Parser<Raw> {
        private final Any operandParsers = new Any();
        
        @Override
        protected Result<Raw> parseImpl(ParseContext ctx) {
            var raw = ctx.pushStructure(new Raw());

            if (!ctx.literal("!")) return failExpecting("'!'");
            ctx.whitespace();

            var value = operandParsers.parse(ctx);
            if (!value.hasValue()) return failExpecting("expression");
            raw.value = value.valueNonNull();
            ctx.whitespace();

            return success(raw);
        }

        protected boolean parseSettingsImpl(ParseContext ctx) {
            return parseParserList(operandParsers, ctx);
        }
    }
    public static class Raw extends Structure.Raw<Analysis> {
        public Structure.Raw<?> value;
        @Override
        protected Analysis analyzeImpl(AnalysisContext ctx) {
            return new Analysis(this, ctx);
        }
    }
    public static class Analysis extends Structure.Analysis implements Expr {
        public final Expr value;
        protected Analysis(Raw raw, AnalysisContext ctx) {
            super(raw, ctx);
            ctx.pushStructure(this);
            Expr value = null;

            if (raw.value.analyze(ctx) instanceof Expr expr) {
                if (expr.type() == BooleanLit.Analysis.TYPE) value = expr;
                else ctx.report(new WrongTypeErr("operand", "boolean expression"), this);
            } else ctx.report(new WrongTypeErr("operand", "expression"), this);
            this.value = value;

            ctx.popStructure();
        }
        @Override
        public void assemble(AssembleContext ctx) {
            value.assemble(ctx);

            ctx.data().opPush(0x01);
            ctx.data().opXor(value.footprint(), Size.ONE_BYTE);
            ctx.stackSize().subtract(value.footprint());
            ctx.stackSize().add(footprint());
        }

        @Override
        public Type type() {
            return BooleanLit.Analysis.TYPE;
        }
    }
}
