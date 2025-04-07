package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.util.Diagnostic;

public record UnknownFuncErr(String funcName, String unitName) implements Diagnostic {
    @Override
    public String getString() {
        return "Unknown function '" + funcName + "' in unit '" + unitName + "'";
    }
}
