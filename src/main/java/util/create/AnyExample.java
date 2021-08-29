package util.create;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnyExample<T, R> {
    private String a;
    private T b;
    private List<T> c;
    private Map<R, Integer> d;
    private Set<List<T>> e;

    public AnyExample(String a, T b, List<T> c, Map<R, Integer> d, Set<List<T>> e) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }

    @Override
    public String toString() {
        return "AnyExample{" +
                "a='" + a + '\'' +
                ", b=" + b +
                ", c=" + c +
                ", d=" + d +
                ", e=" + e +
                '}';
    }
}
