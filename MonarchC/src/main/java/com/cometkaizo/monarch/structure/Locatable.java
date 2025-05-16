package com.cometkaizo.monarch.structure;

import com.cometkaizo.analysis.Size;
import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.monarch.structure.resource.Type;

public interface Locatable {
    /**
     * Assembles instructions that push a single pointer of the location of this expression to the stack.
     */
    void assembleLocation(AssembleContext ctx);
    Type typeAtLocation();
    default Size footprintAtLocation() {
        return typeAtLocation().footprint();
    }
}
