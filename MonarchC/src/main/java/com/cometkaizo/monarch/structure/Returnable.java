package com.cometkaizo.monarch.structure;

import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.analysis.Expr;

public interface Returnable {
    void assembleReturn(Expr value, AssembleContext ctx);
}
