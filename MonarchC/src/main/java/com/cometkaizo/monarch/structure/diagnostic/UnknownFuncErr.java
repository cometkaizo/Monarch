package com.cometkaizo.monarch.structure.diagnostic;

import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.util.Diagnostic;

import java.util.Arrays;
import java.util.stream.Collectors;

public record UnknownFuncErr(String funcName, String unitName, Type... paramTypes) implements Diagnostic {
    public UnknownFuncErr(String funcName, String unitName) {
        this(funcName, unitName, (Type[]) null);
    }

    @Override
    public String getString() {
        String paramTypesStr = paramTypes == null ? "" :
                "(" + Arrays.stream(paramTypes).map(t -> t == null ? "void" : t.name()).collect(Collectors.joining(", ")) + ")";
        return "Unknown function '" + funcName + paramTypesStr + "' in unit '" + unitName + "'";
    }
}
