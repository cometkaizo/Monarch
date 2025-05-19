package com.cometkaizo.analysis.diagnostic;

import com.cometkaizo.analysis.Size;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.StringUtils;

public class LingeringStackElementsException extends RuntimeException {
    public LingeringStackElementsException(Structure.Analysis structure, String identifier, Size stackSize) {
        super("Internal exception: lingering stack elements in " + StringUtils.nameNoPkg(structure.getClass()) +
                (identifier == null ? " " : " '" + identifier + "' ") + structure.startIndex + ": " + stackSize);
    }
    public LingeringStackElementsException(Structure.Analysis structure, String identifier, Size.Mutable stackSize) {
        this(structure, identifier, stackSize.capture());
    }
    public LingeringStackElementsException(Structure.Analysis structure, Size stackSize) {
        this(structure, null, stackSize);
    }
    public LingeringStackElementsException(Structure.Analysis structure, Size.Mutable stackSize) {
        this(structure, stackSize.capture());
    }
}
