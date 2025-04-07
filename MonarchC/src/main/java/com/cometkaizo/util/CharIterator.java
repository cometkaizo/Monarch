package com.cometkaizo.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharIterator extends Triterator<Character> {

    private int line = 1;
    private int col = 1;

    public CharIterator(char[] chars) {
        this(CollectionUtils.box(chars));
    }
    public CharIterator(Character[] chars) {
        super(chars);
    }
    public CharIterator(List<Character> chars) {
        this(chars.toArray(Character[]::new));
    }
    public CharIterator(File file) throws IOException {
        this(readChars(file));
    }
    private static char[] readChars(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        String content = StringUtils.join(lines, "\n");
        return content.toCharArray();
    }


    @Override
    public void advance(int amt) {
        throwIfIllegalAdvance(amt);

        int amtSign = (int) Math.signum(amt); // -1, 0, 1
        while (amt != 0) {

            if (index != -1) {
                updateLineAndColumn(amtSign);
            }

            index += amtSign;
            amt -= amtSign;
        }
    }

    private void updateLineAndColumn(int amtSign) {
        if (amtSign > 0 && current() == '\n') {
            line ++;
            col = 1;
        } else if (amtSign < 0 && previousEndsWith("\n")) {
            line --;
            col = getCharsSinceLastNewline();
        } else if (index + amtSign != -1) { // index -1 does not count as a column
            col += amtSign;
        }
    }

    private int getCharsSinceLastNewline() {
        int charsSinceLastNewline = 1;
        while (index - charsSinceLastNewline - 1 >= 0 && peek(-charsSinceLastNewline - 1) != '\n') {
            charsSinceLastNewline ++;
        }
        return charsSinceLastNewline;
    }

    @Override
    public void jumpTo(int index) {
        throwIfIllegalIndex(index);

        advance(index - this.index);
    }

    @Override
    public void reset() {
        super.reset();
        line = 1;
        col = 1;
    }

    public String remaining() {
        return String.valueOf(CollectionUtils.unbox(remainingArray()));
    }

    public String subString(int start, int end) {
        return String.valueOf(CollectionUtils.unbox(subArray(start, end)));
    }

    /**
     * Checks if the remaining chars in this iterator starts with the given sequence, and if so, advances past them.
     * @param sequence the sequence to check
     * @return whether this method advanced past the given sequence
     */
    public boolean compareAndAdvance(CharSequence sequence) {
        boolean canAdvance = remainingStartsWith(sequence);
        if (canAdvance) advance(sequence.length());
        return canAdvance;
    }
    public boolean remainingStartsWith(CharSequence sequence) {
        return remaining().startsWith(sequence.toString());
    }

    public boolean compareAndGoBack(CharSequence sequence) {
        boolean canGoBack = previousEndsWith(sequence);
        if (canGoBack) back(sequence.length());
        return canGoBack;
    }
    public boolean previousEndsWith(CharSequence sequence) {
        return subString(0, index).endsWith(sequence.toString());
    }

    /**
     * Checks if the remaining chars in this iterator matches the given pattern, and if so, advances past the matched pattern.
     * @param regex the pattern to check
     * @return if the pattern was matched, returns the pattern that was matched, otherwise {@code null}
     */
    public String checkAndAdvance(Pattern regex) {
        Matcher matcher = regex.matcher(remaining());
        boolean found = matcher.find() && matcher.start() == 0;
        if (found) {
            String matched = matcher.group();
            advance(matched.length());
            return matched;
        }
        return null;
    }


    public int getLine() {
        return line;
    }

    public int getCol() {
        return col;
    }

    public int getLineAt(int index) {
        int lineCount = 0;
        for (int i = 0; i < array.length && i < index; i++) {
            if (array[i] == '\n') lineCount++;
        }
        return lineCount;
    }

    public int getColAt(int index) {
        int colCount = 0;
        for (int i = index-1; i > 0; i--) {
            if (array[i] == '\n') break;
            else colCount++;
        }
        return colCount;
    }

    public String getFullLine(int lineIndex) {
        var lineStr = new StringBuilder();
        int lineCount = 0;
        for (char c : array) {
            if (c == '\n') lineCount++;
            else if (lineCount == lineIndex) lineStr.append(c);
        }
        return lineStr.toString();
    }
    public String getFullLineAt(int index) {
        int start = Math.max(0, subString(0, index).lastIndexOf('\n') + 1);
        int end = subString(index, size()).indexOf('\n');
        if (end == -1) end = size();
        else end += index;
        return subString(start, end);
    }

    @Override
    public CharIterator fork() {
        CharIterator result = new CharIterator(array);
        result.index = index;
        result.col = col;
        result.line = line;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CharIterator that = (CharIterator) o;
        return line == that.line && col == that.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), line, col);
    }

    public CharPosition getPosition() {
        return new CharPosition(index, line, col);
    }
}
