package com.cometkaizo.analysis;

import com.cometkaizo.bytecode.AssembleContext;
import com.cometkaizo.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.cometkaizo.util.CollectionUtils.forEachIndexed;
import static com.cometkaizo.util.CollectionUtils.prepend;

public interface StructResource {
    int[] footprint();

    interface Manager {
        default int indexOf(Class<? extends StructResource> type) {
            return indexOf(type::isInstance);
        }
        int indexOf(Predicate<? super StructResource> type);
        <T extends StructResource> T get(Class<T> type, Supplier<T> resourceCreator);

        class Simple implements Manager {
            private final List<StructResource> resources = new ArrayList<>();

            public void assembleSetup(AssembleContext ctx) {
                var c = ctx.data();
                c.opPushAll(prepend(new int[resources.size()], resources.size()));
                c.opStructCreate();

                forEachIndexed(resources, (i, resource) -> {
                    int[] resFootprint = resource.footprint();
                    c.opPushAll(prepend(resFootprint, resFootprint.length));
                    c.opStructCreate();

                    c.opCopy(0, 1, 0, 0);
                    c.opStructSet(i);
                });
            }

            public void assembleCleanup(AssembleContext ctx) {
                var c = ctx.data();
                forEachIndexed(resources, (i, resource) -> {
                    c.opCopy(0, 0, 0, 0);
                    c.opStructGet(i);
                    c.opFree();
                });
                c.opFree();
            }

            public int indexOf(Predicate<? super StructResource> type) {
                return CollectionUtils.indexOf(resources, type::test);
            }

            public <T extends StructResource> T get(Class<T> type, Supplier<T> resourceCreator) {
                return get(type).orElseGet(() -> add(resourceCreator.get()));
            }

            private <T> Optional<T> get(Class<T> type) {
                return resources.stream().filter(type::isInstance).limit(1).map(type::cast).findAny();
            }
            private <T extends StructResource> T add(T resource) {
                resources.add(resource);
                return resource;
            }
        }
    }
}
