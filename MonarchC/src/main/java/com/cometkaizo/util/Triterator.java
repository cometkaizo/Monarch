package com.cometkaizo.util;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Triterator<T> implements Iterator<T> {

    protected final T[] array;
    protected int index = -1;

    protected Triterator(T[] array) {
        Objects.requireNonNull(array, "Array cannot be null");
        this.array = array;
    }

    @SuppressWarnings("unchecked")
    public static <E> Triterator<E> of(List<E> list) {
        Objects.requireNonNull(list, "List cannot be null");
        return new Triterator<>((E[]) list.toArray());
    }
    public static <E> Triterator<E> of(E[] array) {
        return new Triterator<>(array);
    }
    public static <E> Triterator<E> copyOf(Triterator<E> Triterator) {
        var result = new Triterator<>(Arrays.copyOf(Triterator.array, Triterator.array.length));
        result.index = Triterator.index;
        return result;
    }
    public static <E> Triterator<E> forkOf(Triterator<E> Triterator) {
        if (Triterator == null) return null;
        var result = new Triterator<>(Triterator.array);
        result.index = Triterator.index;
        return result;
    }


    public List<T> toList() {
        return List.of(array);
    }

    public T[] toArray() {
        return Arrays.copyOf(array, array.length);
    }

    public Stream<T> stream() {
        return Stream.of(array);
    }

    public List<T> getListRange(int start, int end) {
        return toList().subList(start, end);
    }
    public T[] getRange(int start, int end) {
        return Arrays.copyOfRange(array, start, end);
    }

    public List<T> getListRange(int start) {
        int min = Math.min(start, cursor());
        int max = Math.max(start, cursor());
        return getListRange(min, max);
    }
    public T[] getRange(int start) {
        int min = Math.min(start, cursor());
        int max = Math.max(start, cursor());
        return getRange(min, max);
    }


    /**
     * Returns an unmodifiable list of all remaining elements.
     * <p>
     * Does not include current element.
     * @return an unmodifiable list of all remaining elements
     */
    public List<T> remainingList() {
        return List.of(remainingArray());
    }

    /**
     * Returns an array of all remaining elements.
     * <p>
     * Does not include current element.
     * @return an array of all remaining elements
     */
    public T[] remainingArray() {
        return Arrays.copyOfRange(array, cursor() + 1, array.length);
    }

    public T[] subArray(int end) {
        return subArray(0, end);
    }

    public T[] subArray(int start, int end) {
        if (end > size() || start < 0) throw new IllegalArgumentException("Range [" + start + "," + end + ") is out of bounds for length " + size());
        return Arrays.copyOfRange(array, start, end);
    }

    public T peek() {
        return peek(1);
    }

    public T peek(int amt) {
        return safePeek(amt).orElseThrow(() -> new NoSuchElementException("No element to peek at index " + index + " + amount " + amt));
    }

    public Optional<T> safePeek() {
        return safePeek(1);
    }

    public Optional<T> safePeek(int amt) {
        if (!hasElementAt(index + amt)) return Optional.empty();
        return Optional.ofNullable(array[index + amt]);
    }
    public Optional<T> safeNext() {
        if (!hasNext()) return Optional.empty();
        return Optional.ofNullable(next());
    }

    @Override
    public T next() {
        advance();
        return current();
    }

    public T previous() {
        back();
        return current();
    }

    public T current() {
        return array[index];
    }

    public T get(int index) {
        return array[index];
    }

    public int cursor() {
        return index;
    }

    @Override
    public boolean hasNext() {
        return hasNext(1);
    }
    public boolean hasNext(int amt) {
        return isValidIndex(index + amt);
    }

    public boolean hasCurrent() {
        return hasElementAt(index);
    }
    public boolean hasPrevious() {
        return hasPrevious(1);
    }
    public boolean hasPrevious(int amt) {
        return hasNext(-amt);
    }

    public boolean hasElementAt(int index) {
        return index >= 0 && index < array.length;
    }

    public boolean isValidIndex(int index) {
        return index >= -1 && index < array.length;
    }



    public void advance() {
        advance(1);
    }

    public void advance(int amt) {
        throwIfIllegalAdvance(amt);
        index += amt;
    }

    protected void throwIfIllegalAdvance(int amt) {
        if (!isValidIndex(index + amt))
            throw new IllegalArgumentException("Invalid index " + index + " + amount " + amt);
    }

    protected void throwIfIllegalIndex(int index) {
        if (!isValidIndex(index))
            throw new IllegalArgumentException("Invalid index " + index);
    }


    public void back() {
        back(1);
    }

    public void back(int amt) {
        advance(-amt);
    }

    public void jumpTo(int index) {
        throwIfIllegalIndex(index);
        this.index = index;
    }

    public void reset() {
        index = 0;
    }

    public int size() {
        return array.length;
    }

    public Triterator<T> fork() {
        return forkOf(this);
    }

    public void merge(Triterator<T> other) {
        if (!Arrays.equals(array, other.array)) throw new IllegalArgumentException("Cannot merge two iterators with different contents " + this + " and " + other);
        index = other.index;
    }

    /**
     * Advances past the next remaining consecutive elements that the given condition returns true to such that
     * {@code condition.test(next())} returns false and {@code condition.test(current())} may return true or false.
     * @param condition a consistent condition accepting an element and returning whether it should be skipped
     * @return whether this method advanced
     */
    public boolean checkAndAdvance(Predicate<T> condition) {
        boolean advanced = false;
        while (hasNext() && condition.test(peek())) {
            advance();
            advanced = true;
        }
        return advanced;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triterator<?> that = (Triterator<?>) o;
        return index == that.index && Arrays.equals(array, that.array);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(index);
        result = 31 * result + Arrays.hashCode(array);
        return result;
    }
}
