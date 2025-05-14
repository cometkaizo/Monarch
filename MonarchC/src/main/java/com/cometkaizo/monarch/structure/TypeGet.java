package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.AnalysisContext;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.Structure;

public class TypeGet {
    public static abstract class Parser<R extends Raw<?>> extends Structure.Parser<R> {
    }
    public static abstract class Raw<A extends Structure.Analysis> extends Structure.Raw<A> {
    }
    public static abstract class Analysis extends Structure.Analysis {
        protected Analysis(Structure.Raw<?> raw, AnalysisContext ctx) {
            super(raw, ctx);
        }

        public abstract String name();
        public abstract Type type();

        @Override
        public void assemble(AssembleContext ctx) {

        }
    }
}
