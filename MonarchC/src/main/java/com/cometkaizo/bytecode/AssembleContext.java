package com.cometkaizo.bytecode;

import com.cometkaizo.analysis.Size;
import com.cometkaizo.parser.Context;
import com.cometkaizo.parser.Structure;
import com.cometkaizo.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AssembleContext extends Context {
    private final Interpreters interpreters = new Interpreters();
    private final Chunk data = new Chunk();
    private final Size.Mutable stackSize = new Size.Mutable();

    public Chunk assemble() {
        var interpretersChunk = interpreters.assemble();
        return interpretersChunk.plus(data);
    }

    public Interpreters interpreters() {
        return interpreters;
    }
    public Chunk data() {
        return data;
    }


    public Size.Mutable stackSize() {
        return stackSize;
    }


    public static class Interpreters {
        private final List<Structure.Interpreter> interpreters = new ArrayList<>();
        public Structure.Interpreter add(Structure.Interpreter interpreter) {
            this.interpreters.add(interpreter);
            return interpreter;
        }
        @SuppressWarnings("unchecked")
        public <T extends Structure.Interpreter> T get(Class<? extends T> type, Supplier<Structure.Interpreter> getter) {
            return (T) get(type::isInstance, getter);
        }
        public Structure.Interpreter get(Predicate<Structure.Interpreter> filter, Supplier<Structure.Interpreter> getter) {
            var existing = CollectionUtils.find(interpreters, filter);
            return existing.orElseGet(() -> add(getter.get()));
        }

        private Chunk assemble() {
            var port = new Chunk();
            var portSwitch = new Chunk.JumpArrSwitchBuilder();
            var interpreters = new Chunk();

            for (var i : this.interpreters) {
                var iLoc = interpreters.createLabel();
                interpreters.writeLabel(iLoc);
                portSwitch.addBranch(i.name(), iLoc);

                i.assemble(interpreters);
            }

            portSwitch.apply(port);

            return port.plus(interpreters);
        }
    }
}
