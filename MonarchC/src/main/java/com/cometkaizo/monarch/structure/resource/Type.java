package com.cometkaizo.monarch.structure.resource;

import com.cometkaizo.analysis.Size;

public interface Type {
    String name();
    Size footprint();
    record Static(String name, Size footprint) implements Type { }
    record Ref(boolean targetTypeKnown, Type targetType) implements Type {
        public Ref(Type targetType) {
            this(true, targetType);
        }
        public Ref() {
            this(false, null);
        }
        @Override
        public String name() {
            return "&" + (targetType == null ? "void" : targetType.name());
        }
        @Override
        public Size footprint() {
            return Size.ONE_PTR;
        }
    }
}
