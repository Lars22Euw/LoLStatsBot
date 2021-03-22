package util;

public class Triple<T, S, R> {
    public T first;
    public S second;

    public R third;

    public static <T, S, R> Triple of(T t, S s, R r) {
        return new Triple<T, S, R>(t, s, r);
    }

    @Override
    public String toString() {
        return "Triple{" +
                "first=" + first +
                ", second=" + second +
                ", third=" + third +
                '}';
    }

    public Triple(T first, S second, R third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }
}
