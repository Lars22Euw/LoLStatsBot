package util.monoid;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class Monoid<V, W> {
    private final V zero;
    private final Supplier<V> zeroSup;
    public W one;
    public final BiFunction<V, W, V> add;
    public final BiFunction<V, W, V> sub;

    public Monoid(V zero, Supplier<V> zeroSup, W one, BiFunction<V, W, V> add, BiFunction<V, W, V> sub) {
        this.zero = zero;
        this.zeroSup = zeroSup;
        this.one = one;
        this.add = add;
        this.sub = sub;
    }

    public V zero() {
        if (zero != null) {
            return zero;
        } else {
            return zeroSup.get();
        }
    }

    public static Monoid<Integer, Integer> IntMonoid = new Monoid<>(0, null, 1, Integer::sum, (minuend, subtrahend) -> minuend - subtrahend);
    public static Monoid<String, String> StringMonoid = new Monoid<>(null, () -> "", " ", (s1, s2) -> s1 + s2, null);
    public static Monoid<Double, Double> DoubleMonoid = new Monoid<>(0.0, null, 1.0, Double::sum, (minuend, subtrahend) -> minuend - subtrahend);


    public static <T> Monoid<T, T> ofOne(T one) {
        if (one instanceof String) {
            return (Monoid<T, T>) new Monoid<>(null, StringMonoid.zeroSup, (String) one, StringMonoid.add, null);
        } else if (one instanceof Integer) {
            return (Monoid<T, T>) new Monoid<>(IntMonoid.zero, null, (Integer) one, IntMonoid.add, null);
        } else if (one instanceof Double) {
            return (Monoid<T, T>) new Monoid<>(DoubleMonoid.zero, null, (Double) one, DoubleMonoid.add, null);
        } else {
            throw new IllegalArgumentException("For one of type " + one.getClass().getSimpleName() + " no monoid is prepared.");
        }
    }

    public static <T> Monoid<List<T>, T> withList(T one) {
        return new Monoid<>(null, ArrayList::new, one, (lst, ele) -> {
            lst.add(ele);
            return lst;
        }, (lst, ele) -> {
            lst.remove(ele);
            return lst;
        });
    }

    public static <T> Monoid<List<T>, T> withList() {
        return withList(null);
    }
}
