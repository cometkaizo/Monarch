package com.cometkaizo.parser.diagnostic;

import com.cometkaizo.util.CharIterator;
import com.cometkaizo.util.Diagnostic;

public class InvalidSyntaxErr implements Diagnostic {
    protected final String message;
    protected final int index;

    public InvalidSyntaxErr(String message, int startIndex, CharIterator chars) {
        int index = chars.cursor();

        // rewind to prev line if we started on a prev line and
        // there is only whitespace before the cursor on this line
        int lineIndex = chars.getLine();
        if (lineIndex > chars.getLineAt(startIndex)) {
            while (Character.isWhitespace(chars.get(index)) && chars.getLine() == lineIndex) index--;
        }

        String prefix = "Invalid syntax ";
        String position = "(" + (chars.getLineAt(index) + 1) + ":" + (chars.getColAt(index) + 1) + ")";
        String indent = "    ";

        String line = chars.getFullLineAt(index).stripTrailing();
        String trimmedLine = line.stripLeading();
        int trimAmt = line.length() - trimmedLine.length();
        int caretPos = chars.getColAt(index) + 1 - trimAmt + indent.length();

        this.index = index;
        this.message = message + ":\n" +
                prefix + position + "\n" + indent + trimmedLine + "\n" +
                " ".repeat(caretPos) + "^";
    }

    public InvalidSyntaxErr(Diagnostic diagnostic, int startIndex, CharIterator chars) {
        this(diagnostic.getString(), startIndex, chars);
    }

    @Override
    public String getString() {
        return message;
    }

    public int index() {
        return index;
    }
}
