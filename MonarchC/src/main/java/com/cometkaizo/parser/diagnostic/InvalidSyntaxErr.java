package com.cometkaizo.parser.diagnostic;

import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.CharIterator;
import com.cometkaizo.util.Diagnostic;
import com.cometkaizo.util.StringUtils;

public class InvalidSyntaxErr implements Diagnostic {
    protected final String message;
    protected final int index;

    public InvalidSyntaxErr(String message, CharIterator chars) {
        int index = chars.cursor() + 1;
        String prefix = "Invalid syntax ";
        String position = "(" + (chars.getLineAt(index) + 1) + ":" + (chars.getColAt(index) + 1) + ")";
        String indent = "    ";

        String line = chars.getFullLineAt(index);
        String trimmedLine = line.trim();
        int trimAmt = line.length() - trimmedLine.length();
        int caretPos = chars.getColAt(index) - (trimAmt - indent.length());

        this.index = index;
        this.message = message + ":\n" +
                prefix + position + "\n" + indent + trimmedLine + "\n" +
                " ".repeat(caretPos) + "^";
    }

    @Override
    public String getString() {
        return message;
    }

    public int index() {
        return index;
    }
}
