package util;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class U {
    public static void main(String[] args) {
        List<List<String>> list = List.of(List.of("a"), List.of("b"));
        var flat = list
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

    }

    public static <T, R> List<R> map(List<T> input, Function<T, R> map) {
        List result = null;
        try {
            var clazz = input.getClass();
            result = clazz.getConstructor().newInstance();
            for (var t : input) {
                result.add(map.apply(t));
            }
        } catch (Exception e) {
            System.out.println("Bad usage");
        }
        return result;
    }

    public static <T> T reduce(List<T> input, BiFunction<T, T, T> combine) {
        if (input.isEmpty()) return null;
        T acc = input.get(0);
        for (int i = 1; i < input.size(); i++) {
            acc = combine.apply(acc, input.get(i));
        }
        return acc;
    }

    public static <T, R> R reduce(List<T> input, BiFunction<R, T, R> combine, R base) {
        if (input.isEmpty()) return null;
        R acc = base;
        for (int i = 0; i < input.size(); i++) {
            acc = combine.apply(acc, input.get(i));
        }
        return acc;
    }

    public static <T,R,S> List<S> zipMap(List<T> inputT, List<R> inputR, BiFunction<T,R,S> combine) {
        var result = new ArrayList<S>();
        for (int i = 0; i < Math.max(inputR.size(), inputT.size()); i++) {
            result.add(combine.apply(getOrNull(inputT, i), getOrNull(inputR, i)));
        }
        return result;
    }

    public static <T,R> List<UPair<T,R>> zip(List<T> inputT, List<R> inputR) {
        return zipMap(inputT, inputR, UPair::<T,R>of);
    }

    public static <T> List<T> filter(List<T> input, Predicate<T> pred) {
        List result = null;
        try {
            var clazz = input.getClass();
            result = clazz.getConstructor().newInstance();
            for (var t : input) {
                if (pred.test(t))
                    result.add(t);
            }
        } catch (Exception e) {
            System.out.println("Bad usage");
        }
        return result;
    }

    public static Comparable getMinimum(List<Comparable> input) {
        return getExtremum(true, input, c -> c);
    }

    public static Comparable getMaximum(List<Comparable> input) {
        return getExtremum(false, input, c -> c);
    }

    public static <T> T getMinimum(List<T> input, Function<T, Comparable> map) {
        return getExtremum(true, input, map);
    }

    public static <T> T getMaximum(List<T> input, Function<T, Comparable> map) {
        return getExtremum(false, input, map);
    }

    private static <T> T getExtremum(boolean isMinimum, List<T> input, Function<T, Comparable> map) {
        if (input.isEmpty()) return null;
        var bestT = input.get(0);
        for (int i = 1; i < input.size(); i++) {
            var curT = input.get(i);
            if (isMinimum ? map.apply(bestT).compareTo(map.apply(curT)) > 0 : map.apply(bestT).compareTo(map.apply(curT)) < 0) {
                bestT = curT;
            }
        }
        return bestT;
    }

    public static <T> T getAny(List<T> input, Predicate<T> pred) {
        for (var t : input) {
            if (pred.test(t)) return t;
        }
        return null;
    }

    public static <T> boolean all(List<T> input, Predicate<T> pred) {
        return !any(input, pred.negate());
    }

    public static <T> boolean any(List<T> input, Predicate<T> pred) {
        for (var t : input) {
            if (pred.test(t)) return true;
        }
        return false;
    }

    private static void _log(PrintStream out, Object... objects) {
        int callersLineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();
        String className = Thread.currentThread().getStackTrace()[3].getClassName();
        out.println('[' + className + ":" + callersLineNumber + ']' + " " +
                join(new ArrayList<>(Arrays.asList(objects))));
    }

    public static void log(PrintStream out, Object... objects) {
        _log(out, objects);
    }

    public static void log(Object... objects) {
        log(System.out, objects);
    }

    public static <T> String join(List<T> input) {
        return join(input, " ; ");
    }

    public static <T> String join(List<T> input, String delimiter) {
        if (input == null || input.isEmpty()) return "";
        return mapReduceSkipNull(input, T::toString, (acc, s) -> acc + delimiter + s);
    }

    public static <T, R> R mapSum(List<T> input, Function<T, R> map) {
        if (input.isEmpty()) return null;
        return mapSum(input.subList(1, input.size()), map, map.apply(input.get(0)));
    }

    public static <T, R> R mapSum(List<T> input, Function<T, R> map, R rBase) {
        if (input.isEmpty()) return rBase;
        try {
            BiFunction<R, R, R> add =
                    (a1, a2) -> {
                        if (a1 instanceof String) {
                            return (R) ((String) a1 + (String) a2);
                        }
                        if (a1 instanceof Double) {
                            return (R) Double.valueOf((Double) a1 + (Double) a2);
                        }
                        if (a1 instanceof Integer) {
                            return (R) Integer.valueOf((Integer) a1 + (Integer) a2);
                        }
                        try {
                            Method mAdd = rBase.getClass().getMethod("add", rBase.getClass());
                            var acc = rBase;
                            return (R) mAdd.invoke(a1, a2);
                        } catch (Exception e) {
                            System.out.println("Bad usage");
                            return a1;
                        }
                    };
            return mapReduce(input, map, add, rBase);
        } catch (Exception e) {
            System.out.println("Bad usage");
            return null;
        }
    }

    public static <T, R> R addAll(List<T> input, R rBase) {
        if (input.isEmpty()) return null;
        Method[] methods = rBase.getClass().getMethods();
        Method mAdd = null;
        for (var m : methods) {
            if (m != null && m.getParameterCount() == 1 && m.getName().equals("add")) mAdd = m;
        }
        Method finalMAdd = mAdd;
        BiFunction<R, T, R> add =
                (a1, a2) -> {
                    try {
                        finalMAdd.invoke(a1, a2);
                        return a1;
                    } catch (Exception e) {
                        return a1;
                    }
                };
        return mapReduce(input, t -> t, add, rBase);
    }


    public static <T, R> R mapReduce(List<T> input, Function<T, R> map, BiFunction<R, R, R> reduce) {
        if (input.isEmpty()) return null;
        return mapReduceSkipNull(input.subList(1, input.size()), map, reduce, map.apply(input.get(0)));
    }

    public static <T, R, U> U mapReduce(List<T> input, Function<T, R> map, BiFunction<U, R, U>reduce, U base) {
        return mapConditionalReduce(input, t -> true, map, t -> 0, reduce, (u, r) -> u, base);
    }

    public static <T, R> R mapReduceSkipNull(List<T> input, Function<T, R> map, BiFunction<R, R, R>reduce) {
        return mapConditionalReduce(input.subList(1, input.size()), Objects::nonNull, map, t -> 0, reduce, (u, r) -> u, map.apply(input.get(0)));
    }

    public static <T, R, U> U mapReduceSkipNull(List<T> input, Function<T, R> map, BiFunction<U, R, U>reduce, U base) {
        return mapConditionalReduce(input, Objects::nonNull, map, t -> 0, reduce, (u, r) -> u, base);
    }

    public static <T, R, U> U mapConditionalReduce(List<T> input, Predicate<T> pred,
                                                      BiFunction<T, Boolean, R> map,
                                                      BiFunction<U, R, U> reduce,
                                                      U base) {
        return mapConditionalReduce(input, pred, (T t) -> map.apply(t, true), (T t) -> map.apply(t, false), reduce, reduce, base);
    }

    public static <T, R, S, U> U mapConditionalReduce(List<T> input, Predicate<T> pred,
                                                      Function<T, R> mapTrue,
                                                      Function<T, S> mapFalse,
                                                      BiFunction<U, R, U> reduceTrue,
                                                      BiFunction<U, S, U> reduceFalse,
                                                      U base) {
        var acc = base;
        for (var t : input) {
            acc = pred.test(t) ? reduceTrue.apply(acc, mapTrue.apply(t))
                    : reduceFalse.apply(acc, mapFalse.apply(t));
        }
        return acc;
    }

    public static <T, R, S, V> U mapConditionalReduce(List<T> input, Predicate<T> pred,
                                                      Function<T, R> mapTrue,
                                                      Function<T, S> mapFalse,
                                                      BiFunction<U, R, U> reduceTrue,
                                                      BiFunction<U, S, U> reduceFalse,
                                                      Supplier<U> baseSup) {
        return mapConditionalReduce(input, pred, mapTrue, mapFalse, reduceTrue, reduceFalse, baseSup.get());
    }



    public static <T> T getOrNull(List<T> inputT, int i) {
        return i < inputT.size() ? inputT.get(i) : null;
    }

    public static <T> Stream<UEnumerator<T>> enumerate(List<T> c) {
        return IntStream.range(0, c.size()).mapToObj(i -> UEnumerator.of(i, c.get(i)));
    }

    public static <T> T randomEntry(List<T> inputT) {
        return inputT.get(ThreadLocalRandom.current().nextInt(inputT.size()));
    }

    public static void repeat(int n, Runnable r) {
        for (int i = 0; i < n; i++) {
            r.run();
        }
    }

    public static String fill(String s, int length) {
        return s + ("                                                                              " +
                "                                    ").substring(0, length - s.length());
    }

    public static <T> List<T> sort(List<T> inputT, Comparator<T> comparator) {
        var result = new ArrayList<>(inputT);
        result.sort(comparator);
        return result;
    }

    public static <T, R> void forEach(Collection<T> inputT, Collection<R> inputR, BiConsumer<T, R> biConsumer) {
        Iterator<R> rit = inputR.iterator();
        Iterator<T> tit = inputT.iterator();
        while (tit.hasNext() && rit.hasNext()) {
            biConsumer.accept(tit.next(), rit.next());
        }
    }

    public static <T, R, S> void forEach(Collection<T> inputT, Collection<R> inputR, Collection<S> inputS, TriConsumer<T, R, S> triConsumer) {
        Iterator<R> rit = inputR.iterator();
        Iterator<T> tit = inputT.iterator();
        Iterator<S> sit = inputS.iterator();
        while (tit.hasNext() && rit.hasNext()) {
            triConsumer.accept(tit.next(), rit.next(), sit.next());
        }
    }


    public static <T, R> void forEach(List<UPair<T, R>> pairs, BiConsumer<T, R> biConsumer) {
        pairs.forEach(p -> biConsumer.accept(p.first, p.second));
    }


}
