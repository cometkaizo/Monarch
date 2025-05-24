package com.cometkaizo.parser;

import com.cometkaizo.monarch.structure.CompilationUnit;
import com.cometkaizo.parser.diagnostic.InvalidSyntaxErr;
import com.cometkaizo.util.CharIterator;
import com.cometkaizo.util.Diagnostic;
import com.cometkaizo.util.DiagnosticList;

import java.util.*;
import java.util.regex.Pattern;

import static com.cometkaizo.util.CollectionUtils.findLastMax;
import static com.cometkaizo.util.CollectionUtils.only;

public class ParseContext extends Context {
    private static final Pattern WORD_FMT = Pattern.compile("\\w+"),
            WHITESPACE_FMT = Pattern.compile("\\s+"),
            INTEGER_FMT = Pattern.compile("-?\\d+"),
            DECIMAL_FMT = Pattern.compile("-?\\d+(\\.\\d+)?");
    public final CharIterator chars;
    private final Deque<Frame> frames = new ArrayDeque<>();
    public final Map<String, CompilationUnit.Raw> compilationUnits = new HashMap<>(1);
    private final Deque<Structure.Raw<?>> structures = new ArrayDeque<>();
    private final List<InvalidSyntaxErr> syntaxProblems = new ArrayList<>();
    private final Map<String, Structure.Parser<?>> parsers;

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
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) { return null; }
    }
    public Long longInteger() {
        String intStr = chars.checkAndAdvance(INTEGER_FMT);
        if (intStr == null) return null;
        try {
            return Long.parseLong(intStr);
        } catch (NumberFormatException e) { return null; }
    }
    public Double decimal() {
        String decimalStr = chars.checkAndAdvance(DECIMAL_FMT);
        if (decimalStr == null) return null;
        try {
            return Double.parseDouble(decimalStr);
        } catch (NumberFormatException e) { return null; }
    }

    public void addCompilationUnit(CompilationUnit.Raw unit) {
        this.compilationUnits.put(unit.name, unit);
    }
    public Optional<CompilationUnit.Raw> getCompilationUnit(String name) {
        return Optional.ofNullable(compilationUnits.get(name));
    }
    public CompilationUnit.Raw getCurrentCompilationUnit() {
        return only(structures, CompilationUnit.Raw.class).getLast();
    }

    public Optional<Structure.Parser<?>> getParser(String name) {
        return Optional.ofNullable(parsers.get(name));
    }

    public void reportInvalidSyntax(int startIndex, String message) {
        syntaxProblems.add(new InvalidSyntaxErr(message, startIndex, chars));
    }
    public void report(Diagnostic diagnostic) {
        super.report(new InvalidSyntaxErr(diagnostic, frames.getFirst().cursor, chars));
    }
    public Optional<InvalidSyntaxErr> syntaxProblem() {
        return findLastMax(syntaxProblems, InvalidSyntaxErr::index);
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
        reportInvalidSyntax(frame.cursor, message);
        chars.jumpTo(frame.cursor);
        reverseStructuresTo(frame.rawCount);
    }
    private void reverseStructuresTo(int count) {
        for (int i = 0; i < structures.size() - count; i++) popStructure();
    }

    private record Frame(int cursor, int rawCount) {}
}
