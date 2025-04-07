/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2020  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.cometkaizo.util;

import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 *
 * @author huangyuhui
 */
public final class Listener<T> {

    private final SimpleMultimap<EventPriority, Consumer<T>> handlers
            = new SimpleMultimap<>(() -> new EnumMap<>(EventPriority.class), CopyOnWriteArraySet::new);

    public Consumer<T> weakOnUpdate(Consumer<T> consumer) {
        onUpdate(new WeakListener(consumer));
        return consumer;
    }

    public Consumer<T> weakOnUpdate(Consumer<T> consumer, EventPriority priority) {
        onUpdate(new WeakListener(consumer), priority);
        return consumer;
    }

    public void onUpdate(Consumer<T> consumer) {
        onUpdate(consumer, EventPriority.NORMAL);
    }

    public synchronized void onUpdate(Consumer<T> consumer, EventPriority priority) {
        if (!handlers.get(priority).contains(consumer))
            handlers.put(priority, consumer);
    }

    public void onUpdate(Runnable runnable) {
        onUpdate(t -> runnable.run());
    }

    public void onUpdate(Runnable runnable, EventPriority priority) {
        onUpdate(t -> runnable.run(), priority);
    }

    public synchronized void update(T event) {
        for (EventPriority priority : EventPriority.values()) {
            for (Consumer<T> handler : handlers.get(priority))
                handler.accept(event);
        }
    }

    public synchronized void stopUpdating(Consumer<T> consumer) {
        handlers.removeValue(consumer);
    }

    private class WeakListener implements Consumer<T> {
        private final WeakReference<Consumer<T>> ref;

        public WeakListener(Consumer<T> listener) {
            this.ref = new WeakReference<>(listener);
        }

        @Override
        public void accept(T t) {
            Consumer<T> listener = ref.get();
            if (listener == null) {
                stopUpdating(this);
            } else {
                listener.accept(t);
            }
        }
    }
}
