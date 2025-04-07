package com.cometkaizo.analysis;

import com.cometkaizo.bytecode.AssembleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public interface StackResource {
    Size footprint();
    void assembleSetup(AssembleContext ctx);
    default void assembleCleanup(AssembleContext ctx) {
        Size footprint = footprint();
        ctx.data().opPopAll(footprint);
        ctx.stackSize().subtract(footprint);
    }

    interface Manager {
        default Size offsetOf(Class<? extends StackResource> type, AssembleContext ctx) {
            return offsetOf(type::isInstance, ctx);
        }
        Size offsetOf(Predicate<? super StackResource> condition, AssembleContext ctx);
        Size offset(AssembleContext ctx);
        <T extends StackResource> T getOrCreate(Class<T> type, Function<StackResource.Manager, T> generator);
        <T extends StackResource> Optional<T> get(Class<T> type);

        class Simple implements Manager {
            private final List<StackResource> resources = new ArrayList<>();
            private Size stackLoc;

            public void assembleSetup(AssembleContext ctx) {
                resources.forEach(res -> res.assembleSetup(ctx));
                stackLoc = ctx.stackSize().capture();
            }

            public void assembleCleanup(AssembleContext ctx) {
                resources.reversed().forEach(res -> res.assembleCleanup(ctx)); // reversed but doesn't actually matter (can't skip popping bytes)
            }

            @Override
            public Size offsetOf(Predicate<? super StackResource> condition, AssembleContext ctx) {
                var total = Size.ZERO;
                for (var res : resources.reversed()) {
                    if (condition.test(res)) return total.plus(offset(ctx));
                    total = total.plus(res.footprint());
                }
                return Size.INVALID;
            }

            public Size offset(AssembleContext ctx) {
                return ctx.stackSize().capture().minus(stackLoc);
            }

            @Override
            public <T extends StackResource> T getOrCreate(Class<T> type, Function<StackResource.Manager, T> generator) {
                return get(type).orElseGet(() -> add(generator.apply(this)));
            }
            @Override
            public <T extends StackResource> Optional<T> get(Class<T> type) {
                return resources.stream().filter(type::isInstance).limit(1).map(type::cast).findAny();
            }

            public Size footprint() {
                return resources.stream().map(StackResource::footprint).reduce(Size.ZERO, Size::plus);
            }

            private <T extends StackResource> T add(T resource) {
                resources.add(resource);
                return resource;
            }
        }
    }
}
