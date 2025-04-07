package com.cometkaizo.analysis;

import com.cometkaizo.monarch.structure.CompilationUnit;
import com.cometkaizo.monarch.structure.resource.Type;
import com.cometkaizo.parser.Context;
import com.cometkaizo.parser.Structure;

import java.util.*;

public class AnalysisContext extends Context {
    private final Deque<Structure.Analysis> structures = new ArrayDeque<>();

    private final Map<String, Type> types;
    private final Map<String, CompilationUnit.Raw> compilationUnits = new HashMap<>(1);

    public AnalysisContext(Map<String, Type> types) {
        this.types = types;
    }

    public <A extends Structure.Analysis> A pushStructure(A structure) {
        structures.addFirst(structure);
        return structure;
    }
    public void popStructure() {
        structures.pollFirst();
    }
    public Structure.Analysis topStructure() {
        return structures.peekFirst();
    }

    public Optional<Type> getType(String name) {
        return Optional.ofNullable(types.get(name));
    }

    public void addCompilationUnit(CompilationUnit.Raw unit) {
        this.compilationUnits.put(unit.name, unit);
    }
    public Optional<CompilationUnit.Raw> getCompilationUnit(String name) {
        return Optional.ofNullable(compilationUnits.get(name));
    }

    public <A extends Structure.Analysis> List<A> analyze(Collection<? extends Structure.Raw<? extends A>> raw) {
        List<A> result = new ArrayList<>(raw.size());
        raw.stream().map(s -> s.analyze(this)).forEach(result::add);
        return result;
    }
}
