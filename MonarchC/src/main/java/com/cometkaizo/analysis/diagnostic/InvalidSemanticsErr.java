package com.cometkaizo.analysis.diagnostic;

import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.CharIterator;
import com.cometkaizo.util.Diagnostic;
import com.cometkaizo.util.StringUtils;

public record InvalidSemanticsErr(Diagnostic diagnostic, Structure.Analysis reportingStructure, CharIterator chars) implements Diagnostic {
    @Override
    public String getString() {
        int startIndex = reportingStructure.startIndex.index() + 1;

        String prefix = "Invalid semantics ";
        String position = "(" + (chars.getLineAt(startIndex) + 1) + ":" + (chars.getColAt(startIndex) + 1) + ")";
        String indent = "    ";

        String line = chars.getFullLineAt(startIndex).stripTrailing();
        String trimmedLine = line.trim().stripLeading();
        int trimAmt = line.length() - trimmedLine.length();

        int caretPos = chars.getColAt(startIndex) - trimAmt + indent.length();

        return StringUtils.nameNoPkg(reportingStructure.getClass()) + ":\n" +
                diagnostic.getString() + ":\n" +
                prefix + position + "\n" + indent + trimmedLine + "\n" +
                " ".repeat(caretPos) + "^";
    }
}
