package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.util.Diagnostic;

public record NumberFormatErr(Number num, String numType) implements Diagnostic {
    @Override
    public String getString() {
        return num + " is not a " + numType;
    }
}
