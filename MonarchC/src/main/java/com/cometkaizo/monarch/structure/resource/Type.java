package com.cometkaizo.monarch.structure.resource;

import com.cometkaizo.analysis.Size;

public interface Type {
    String name();
    Size footprint();
    record Static(String name, Size footprint) implements Type { }
}
