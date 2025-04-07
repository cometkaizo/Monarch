package com.cometkaizo.parser;

import com.cometkaizo.analysis.Size;
import com.cometkaizo.monarch.structure.resource.Type;

public interface ParamDecl {
    Type type();
    default Size footprint() {
        return type().footprint();
    }
}
