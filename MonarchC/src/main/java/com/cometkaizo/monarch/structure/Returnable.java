package com.cometkaizo.monarch.structure;

import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.analysis.Expr;
import com.cometkaizo.monarch.structure.resource.Type;

public interface Returnable {
    Type returnType();
    void assembleReturn(Expr value, AssembleContext ctx);
}
