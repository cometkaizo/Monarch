package com.cometkaizo.parser;

import com.cometkaizo.parser.diagnostic.InvalidSyntaxErr;
import com.cometkaizo.util.CharIterator;
import com.cometkaizo.util.DiagnosticList;

import java.util.*;
import java.util.regex.Pattern;

import static com.cometkaizo.util.CollectionUtils.findMax;

public class ParseContext extends Context {
    private static final Pattern WORD_FMT = Pattern.compile("\\w+"),
            WHITESPACE_FMT = Pattern.compile("\\s+"),
            INTEGER_FMT = Pattern.compile("-?\\d+");
    public final CharIterator chars;
    private final Deque<Frame> frames = new ArrayDeque<>();
    private final Deque<Structure.Raw<?>> structures = new ArrayDeque<>();
    private final Set<InvalidSyntaxErr> syntaxProblems = new HashSet<>();
    private final Map<String, Structure.Parser<?>> parsers;

    // TODO: 2024-09-05 right now no problems other than syntaxProblems show up

    public ParseContext(CharIterator chars, DiagnosticList problems, Map<String, Structure.Parser<?>> parsers) {
        super(problems);
        this.chars = chars;
        this.parsers = parsers;
    }

    public boolean literal(CharSequence literal) {
        return chars.compareAndAdvance(literal);
    }
    public String word() {
        return chars.checkAndAdvance(WORD_FMT);
    }
    public boolean whitespace() {
        return chars.checkAndAdvance(WHITESPACE_FMT) != null;
    }
    public Integer integer() {
        String intStr = chars.checkAndAdvance(INTEGER_FMT);
        if (intStr == null) return null;
        return Integer.parseInt(intStr);
    }

    public Optional<Structure.Parser<?>> getParser(String name) {
        return Optional.ofNullable(parsers.get(name));
    }

    public void reportInvalidSyntax(String message) {
        syntaxProblems.add(new InvalidSyntaxErr(message, chars));
    }
    public Optional<InvalidSyntaxErr> syntaxProblem() {
        return findMax(syntaxProblems, InvalidSyntaxErr::index);
    }

    public <T extends Structure.Raw<?>> T pushStructure(T raw) {
        structures.addFirst(raw);
        return raw;
    }
    public void popStructure() {
        structures.pollFirst();
    }
    public Structure.Raw<?> topStructure() {
        return structures.peekFirst();
    }

    public void enterFrame() {
        frames.addFirst(new Frame(chars.cursor(), structures.size()));
    }
    public void exitFrameSuccess() {
        frames.removeFirst();
    }
    public void exitFrameFail(String message) {
        var frame = frames.removeFirst();
        chars.jumpTo(frame.cursor);
        reverseStructuresTo(frame.rawCount);
        reportInvalidSyntax(message);
    }
    private void reverseStructuresTo(int count) {
        for (int i = 0; i < structures.size() - count; i++) popStructure();
    }

    private record Frame(int cursor, int rawCount) {}
}
