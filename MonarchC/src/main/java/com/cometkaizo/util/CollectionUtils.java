package com.cometkaizo.util;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class CollectionUtils {

    public static <E> boolean addUnique(Collection<E> c, E element) {
        if (!c.contains(element)) {
            c.add(element);
            return true;
        }
        return false;
    }

    public static boolean contains(Object[] array, Object ref) {
        if (array == null || ref != null && !array.getClass().getComponentType().isAssignableFrom(ref.getClass()))
            return false;
        for (Object element : array) {
            if (Objects.equals(element, ref)) return true;
        }
        return false;
    }

    public static <T, U> boolean anyMatch(T[] array, Predicate<T> condition) {
        return indexOf(array, condition) > -1;
    }
    public static <T, U> boolean anyMatch(List<T> list, Predicate<T> condition) {
        return indexOf(list, condition) > -1;
    }
    public static <T, U> boolean allMatch(T[] array, Predicate<T> condition) {
        for (T element : array) {
            if (!condition.test(element)) return false;
        }
        return true;
    }
    public static <T, U> boolean allMatch(List<T> list, Predicate<T> condition) {
        for (T element : list) {
            if (!condition.test(element)) return false;
        }
        return true;
    }
    public static <T, U> boolean noneMatch(T[] array, Predicate<T> condition) {
        return !anyMatch(array, condition);
    }
    public static <T, U> boolean noneMatch(List<T> list, Predicate<T> condition) {
        return !anyMatch(list, condition);
    }

    public static <T> int indexOf(T[] array, Predicate<T> condition) {
        for (int index = 0; index < array.length; index ++) {
            T element = array[index];
            if (condition.test(element)) return index;
        }
        return -1;
    }
    public static <T> int lastIndexOf(T[] array, Predicate<T> condition) {
        for (int index = array.length -1; index > -1; index --) {
            T element = array[index];
            if (condition.test(element)) return index;
        }
        return -1;
    }

    @SafeVarargs
    public static <T> Optional<T> firstNonNull(T... array) {
        return find(array, Objects::nonNull);
    }
    @SafeVarargs
    public static <T extends Optional<?>> Optional<T> firstPresent(T... array) {
        return find(array, Optional::isPresent);
    }
    public static <T> Optional<T> find(T[] array, Predicate<T> condition) {
        for (T element : array) {
            if (condition.test(element)) return Optional.ofNullable(element);
        }
        return Optional.empty();
    }
    public static <T> Optional<T> findLast(T[] array, Predicate<T> condition) {
        for (int index = array.length -1; index > -1; index --) {
            T element = array[index];
            if (condition.test(element)) return Optional.ofNullable(element);
        }
        return Optional.empty();
    }

    public static <T> int indexOf(List<T> list, Predicate<T> condition) {
        for (int index = 0; index < list.size(); index++) {
            T element = list.get(index);
            if (condition.test(element)) return index;
        }
        return -1;
    }
    public static <T> int lastIndexOf(List<T> list, Predicate<T> condition) {
        for (int index = list.size() -1; index > -1; index--) {
            T element = list.get(index);
            if (condition.test(element)) return index;
        }
        return -1;
    }
    public static <T> Optional<T> find(Collection<T> list, Predicate<T> condition) {
        for (T element : list) {
            if (condition.test(element)) return Optional.ofNullable(element);
        }
        return Optional.empty();
    }
    public static <T> Optional<T> findLast(List<T> list, Predicate<T> condition) {
        for (int index = list.size() -1; index > -1; index--) {
            T element = list.get(index);
            if (condition.test(element)) return Optional.ofNullable(element);
        }
        return Optional.empty();
    }

    public static <T> List<T> only(Collection<?> coll, Class<T> type) {
        List<T> result = new ArrayList<>(Math.min(10, coll.size()));
        for (var e : coll) {
            if (e != null && type.isAssignableFrom(e.getClass())) result.add(type.cast(e));
        }
        return result;
    }
    public static <T> List<T> only(Object[] arr, Class<T> type) {
        List<T> result = new ArrayList<>(Math.min(10, arr.length));
        for (var e : arr) {
            if (e != null && type.isAssignableFrom(e.getClass())) result.add(type.cast(e));
        }
        return result;
    }

    public static <T> T[] reverse(T[] array) {
        for (int i = 0; i < array.length / 2; i ++) {
            swap(array, i, array.length - i - 1);
        }
        return array;
    }
    public static <T> List<T> reverse(List<T> list) {
        for (int i = 0; i < list.size() / 2; i ++) {
            swap(list, i, list.size() - i - 1);
        }
        return list;
    }

    // does not deep copy array!
    public static <T> T[] reverse(T[] array, IntFunction<T[]> arrayGenerator) {
        T[] result = arrayGenerator.apply(array.length);
        for (int i = 0; i < array.length; i ++) {
            result[i] = array[array.length - 1 - i];
        }
        return result;
    }
    // does not deep copy list!
    public static <T> List<T> reverse(List<T> list, IntFunction<? extends List<T>> listGenerator) {
        List<T> result = listGenerator.apply(list.size());
        for (int i = 0; i < list.size(); i ++) {
            result.set(i, list.get(list.size() - 1 - i));
        }
        return result;
    }

    public static <T> void swap(T[] array, int sourceIndex, int destinationIndex) {
        T temp = array[sourceIndex];
        array[sourceIndex] = array[destinationIndex];
        array[destinationIndex] = temp;
    }
    public static <T> void swap(List<T> list, int sourceIndex, int destinationIndex) {
        T temp = list.get(sourceIndex);
        list.set(sourceIndex, list.get(destinationIndex));
        list.set(destinationIndex, temp);
    }

    public static <T> List<T> fill(List<T> list, T item, int startIndex) {
        for (int index = startIndex; index < list.size(); index ++) {
            list.set(index, item);
        }
        return list;
    }

    public static void requireNoNullElement(Object[] array, String message) {
        if (contains(array, null)) {
            throw new NullPointerException(message);
        }
    }

    public static <T> boolean containsDuplicates(List<T> list) {
        Set<T> lump = new HashSet<>();
        for (T element : list) {
            if (lump.contains(element)) return true;
            lump.add(element);
        }
        return false;
    }

    public static <T> Optional<T> findMax(Collection<T> collection, Function<T, Integer> valueFunction) {
        T largestElement = null;
        Integer largestValue = null;

        for (T element : collection) {
            int value = valueFunction.apply(element);
            if (largestValue == null || largestValue < value) {
                largestValue = value;
                largestElement = element;
            }
        }

        return Optional.ofNullable(largestElement);
    }
    public static <T> Optional<T> findLastMax(List<T> collection, Function<T, Integer> valueFunction) {
        T largestElement = null;
        Integer largestValue = null;

        for (T element : collection) {
            int value = valueFunction.apply(element);
            if (largestValue == null || largestValue <= value) {
                largestValue = value;
                largestElement = element;
            }
        }

        return Optional.ofNullable(largestElement);
    }

    public static <T> Optional<T> findMin(Collection<T> collection, Function<T, Integer> valueFunction) {
        T smallestElement = null;
        Integer smallestValue = null;

        for (T element : collection) {
            int value = valueFunction.apply(element);
            if (smallestValue == null || smallestValue > value) {
                smallestValue = value;
                smallestElement = element;
            }
        }

        return Optional.ofNullable(smallestElement);
    }
    public static <T> Optional<T> findLastMin(List<T> collection, Function<T, Integer> valueFunction) {
        T smallestElement = null;
        Integer smallestValue = null;

        for (T element : collection) {
            int value = valueFunction.apply(element);
            if (smallestValue == null || smallestValue >= value) {
                smallestValue = value;
                smallestElement = element;
            }
        }

        return Optional.ofNullable(smallestElement);
    }

    public static <T> void forEach(T[] array, Consumer<T> operation) {
        for (T element : array) {
            operation.accept(element);
        }
    }
    public static <T> void forEachIndexed(T[] a, BiConsumer<Integer, T> action) {
        for (int i = 0; i < a.length; i++) {
            action.accept(i, a[i]);
        }
    }
    public static <T> void forEachIndexed(List<T> l, BiConsumer<Integer, T> action) {
        for (int i = 0; i < l.size(); i++) {
            action.accept(i, l.get(i));
        }
    }

    public static <T, R> Object[] map(T[] array, Function<T, Object> function) {
        Object[] resultArray = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            T element = array[i];
            resultArray[i] = function.apply(element);
        }
        return resultArray;
    }

    public static <T, R> R[] map(T[] array, Function<T, R> function, IntFunction<R[]> arrayGenerator) {
        R[] resultArray = arrayGenerator.apply(array.length);
        for (int i = 0; i < array.length; i++) {
            T element = array[i];
            resultArray[i] = function.apply(element);
        }
        return resultArray;
    }

    public static <T, R> ArrayList<R> map(Collection<T> collection, Function<T, R> function) {
        ArrayList<R> resultList = new ArrayList<>(collection.size());
        for (var element : collection) {
            resultList.add(function.apply(element));
        }
        return resultList;
    }

    public static <T, R, C extends Collection<R>> C map(Collection<T> collection, Function<T, R> function, IntFunction<C> collectionGenerator) {
        C resultCollection = collectionGenerator.apply(collection.size());
        for (var element : collection) {
            resultCollection.add(function.apply(element));
        }
        return resultCollection;
    }

    public static <T, C extends Collection<T>> C filter(C collection, Predicate<T> condition, IntFunction<? extends C> collectionGenerator) {
        var result = collectionGenerator.apply(collection.size());
        for (T element : collection) {
            if (condition.test(element)) result.add(element);
        }
        return result;
    }

    public static <T> T pickRandom(List<T> list) {
        if (list.isEmpty()) return null;
        int index = (int) (Math.random() * list.size());
        return list.get(index);
    }

    public static <T> ArrayList<T> toArrayList(T[] a) {
        return Arrays.stream(a).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }


    public static Integer[] box(int[] ints) {
        var boxed = new Integer[ints.length];
        for (int index = 0; index < ints.length; index ++) {
            boxed[index] = ints[index];
        }
        return boxed;
    }
    public static Double[] box(double[] doubles) {
        var boxed = new Double[doubles.length];
        for (int index = 0; index < doubles.length; index ++) {
            boxed[index] = doubles[index];
        }
        return boxed;
    }
    public static Float[] box(float[] floats) {
        var boxed = new Float[floats.length];
        for (int index = 0; index < floats.length; index ++) {
            boxed[index] = floats[index];
        }
        return boxed;
    }
    public static Long[] box(long[] longs) {
        var boxed = new Long[longs.length];
        for (int index = 0; index < longs.length; index ++) {
            boxed[index] = longs[index];
        }
        return boxed;
    }
    public static Short[] box(short[] shorts) {
        var boxed = new Short[shorts.length];
        for (int index = 0; index < shorts.length; index ++) {
            boxed[index] = shorts[index];
        }
        return boxed;
    }
    public static Byte[] box(byte[] bytes) {
        var boxed = new Byte[bytes.length];
        for (int index = 0; index < bytes.length; index ++) {
            boxed[index] = bytes[index];
        }
        return boxed;
    }
    public static Character[] box(char[] chars) {
        var boxed = new Character[chars.length];
        for (int index = 0; index < chars.length; index ++) {
            boxed[index] = chars[index];
        }
        return boxed;
    }
    public static Boolean[] box(boolean[] booleans) {
        var boxed = new Boolean[booleans.length];
        for (int index = 0; index < booleans.length; index ++) {
            boxed[index] = booleans[index];
        }
        return boxed;
    }

    public static int[] unbox(Integer[] ints) {
        var unboxed = new int[ints.length];
        for (int index = 0; index < ints.length; index ++) {
            unboxed[index] = ints[index];
        }
        return unboxed;
    }
    public static double[] unbox(Double[] doubles) {
        var unboxed = new double[doubles.length];
        for (int index = 0; index < doubles.length; index ++) {
            unboxed[index] = doubles[index];
        }
        return unboxed;
    }
    public static float[] unbox(Float[] floats) {
        var unboxed = new float[floats.length];
        for (int index = 0; index < floats.length; index ++) {
            unboxed[index] = floats[index];
        }
        return unboxed;
    }
    public static long[] unbox(Long[] longs) {
        var unboxed = new long[longs.length];
        for (int index = 0; index < longs.length; index ++) {
            unboxed[index] = longs[index];
        }
        return unboxed;
    }
    public static short[] unbox(Short[] shorts) {
        var unboxed = new short[shorts.length];
        for (int index = 0; index < shorts.length; index ++) {
            unboxed[index] = shorts[index];
        }
        return unboxed;
    }
    public static byte[] unbox(Byte[] bytes) {
        var unboxed = new byte[bytes.length];
        for (int index = 0; index < bytes.length; index ++) {
            unboxed[index] = bytes[index];
        }
        return unboxed;
    }
    public static char[] unbox(Character[] chars) {
        var unboxed = new char[chars.length];
        for (int index = 0; index < chars.length; index ++) {
            unboxed[index] = chars[index];
        }
        return unboxed;
    }
    public static boolean[] unbox(Boolean[] booleans) {
        var unboxed = new boolean[booleans.length];
        for (int index = 0; index < booleans.length; index ++) {
            unboxed[index] = booleans[index];
        }
        return unboxed;
    }

    public static <T> T getOrAdd(List<T> list, int index, T defaultValue) {
        if (index >= 0 && index < list.size()) return list.get(index);
        list.add(defaultValue);
        return defaultValue;
    }

    @SafeVarargs
    public static <T> List<T> appendTo(List<T> list, T... others) {
        list.addAll(Arrays.asList(others));
        return list;
    }
    public static <T> List<T> appendTo(List<T> list, T other) {
        list.add(other);
        return list;
    }
    public static <T> List<T> appendTo(List<T> list, Collection<T> others) {
        list.addAll(others);
        return list;
    }

    @SafeVarargs
    public static <T> List<T> prependTo(List<T> list, T... others) {
        for (int i = 0; i < others.length; i++) {
            list.add(i, others[i]);
        }
        return list;
    }
    public static <T> List<T> prependTo(List<T> list, T other) {
        list.addFirst(other);
        return list;
    }
    public static <T> List<T> prependTo(List<T> list, Collection<T> others) {
        int i = 0;
        for (var it = others.iterator(); it.hasNext(); i++) {
            list.add(i, it.next());
        }
        return list;
    }

    public static <T> List<T> append(Collection<T> c, T other) {
        return new ArrayList<>(c) {{
            addLast(other);
        }};
    }
    public static <T> List<T> prepend(Collection<T> c, T other) {
        return new ArrayList<>(c) {{
            addFirst(other);
        }};
    }
    public static <T> T[] prepend(Function<Integer, T[]> generator, T[] a, T other) {
        var result = generator.apply(a.length + 1);
        result[0] = other;
        System.arraycopy(a, 0, result, 1, a.length);
        return result;
    }
    public static int[] prepend(int[] a, int other) {
        var result = new int[a.length + 1];
        result[0] = other;
        System.arraycopy(a, 0, result, 1, a.length);
        return result;
    }

    public static Stream<Byte> stream(byte[] arr) {
        var builder = Stream.<Byte>builder();
        for (byte b : arr) builder.add(b);
        return builder.build();
    }

    public static <T> Set<T> newIdentityHashSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
