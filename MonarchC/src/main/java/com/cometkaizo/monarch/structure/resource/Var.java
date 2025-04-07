package com.cometkaizo.monarch.structure.resource;

import com.cometkaizo.analysis.Size;

public record Var(String name, Type type) {
    public Size footprint() {
        return type.footprint();
    }
}
