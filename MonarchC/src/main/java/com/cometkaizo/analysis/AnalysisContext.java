package com.cometkaizo.analysis;

import com.cometkaizo.analysis.diagnostic.InvalidSemanticsErr;
import com.cometkaizo.monarch.structure.CompilationUnit;
import com.cometkaizo.parser.Context;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.CharIterator;
import com.cometkaizo.util.Diagnostic;

import java.util.*;

public class AnalysisContext extends Context {
    private final Deque<Structure.Analysis> structures = new ArrayDeque<>();

    private final CharIterator chars;
    private final Map<String, CompilationUnit.Raw> compilationUnits;

    public AnalysisContext(Map<String, CompilationUnit.Raw> compilationUnits, CharIterator chars) {
        this.compilationUnits = compilationUnits;
        this.chars = chars;
    }

    @Deprecated
    public <A extends Structure.Analysis> A pushStructure(A structure) {
        structures.addFirst(structure);
        return structure;
    }
    @Deprecated
    public void popStructure() {
        structures.pollFirst();
    }
    public Structure.Analysis topStructure() {
        return structures.peekFirst();
    }

    public Optional<CompilationUnit.Raw> getCompilationUnit(String name) {
        return Optional.ofNullable(compilationUnits.get(name));
    }

    public <A extends Structure.Analysis> List<A> analyze(Collection<? extends Structure.Raw<? extends A>> raw) {
        List<A> result = new ArrayList<>(raw.size());
        raw.stream().map(s -> s.analyze(this)).forEach(result::add);
        return result;
    }

    public void report(Diagnostic diagnostic, Structure.Analysis reportingStructure) {
        super.report(new InvalidSemanticsErr(diagnostic, reportingStructure, chars));
    }
}
