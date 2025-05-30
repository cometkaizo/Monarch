package com.cometkaizo.monarch.structure.resource;

import com.cometkaizo.analysis.Size;
import com.cometkaizo.analysis.StackResource;
import com.cometkaizo.bytecode.AssembleContext;

import java.util.ArrayList;
import java.util.List;

// Make an equivalent to this for types
public class Vars implements StackResource {
    private final List<Var> vars = new ArrayList<>(), params = new ArrayList<>();
    private final Manager manager;

    public Vars(StackResource.Manager manager) {
        this.manager = manager;
    }

    public boolean addVar(Var var) {
        if (has(var.name())) {
            return false;
        } else {
            vars.add(var);
            return true;
        }
    }
    public boolean addParam(Var var) {
        manager.getOrCreate(Params.class, Params::new).add(var.footprint());
        if (has(var.name())) {
            return false;
        } else {
            params.add(var);
            return true;
        }
    }
    public Size offsetOf(String name, AssembleContext ctx) {
        var varOff = manager.offsetOf(r1 -> r1 == this, ctx);
        for (var var : vars.reversed()) {
            if (var.name().equals(name)) return varOff;
            varOff = varOff.plus(var.footprint());
        }

        var paramOff = manager.offsetOf(Params.class, ctx);
        for (var param : params.reversed()) {
            if (param.name().equals(name)) return paramOff;
            paramOff = paramOff.plus(param.footprint());
        }

        return Size.INVALID;
    }
    public boolean has(String name) {
        return get(name) != null;
    }

    public Var get(String name) {
        return vars.stream().filter(v -> v.name().equals(name)).findFirst()
                .orElseGet(() -> params.stream().filter(v -> v.name().equals(name)).findFirst().orElse(null));
    }

    @Override
    public Size footprint() {
        return vars.stream().map(Var::footprint).reduce(Size.ZERO, Size::plus);
    }

    @Override
    public void assembleSetup(AssembleContext ctx) {
        var c = ctx.data();
        for (var var : vars.reversed()) {
            var footprint = var.footprint();
            if (footprint.byteAmt() != 0) c.opPushAll(new int[footprint.byteAmt()]); // byteAmt == 0 => 1 ptrAmt
            for (int i = 0; i < footprint.ptrAmt(); i++) {
                c.opPushAll(new int[0]);
            }
        }
        ctx.stackSize().add(footprint());
    }

    public static class Params implements StackResource {
        Size footprint = Size.ZERO;

        public Params() {}
        public Params(StackResource.Manager manager) {}

        void add(Size footprint) {
            this.footprint = this.footprint.plus(footprint);
        }

        @Override
        public Size footprint() {
            return footprint;
        }

        @Override
        public void assembleSetup(AssembleContext ctx) {
            ctx.stackSize().add(footprint);
        }
    }
}
