package util;

public class UPair<T, R> {
    public T first;
    public R second;

    public static <T, R> UPair of(T t, R r) {
        return new UPair<T, R>(t, r);
    }

    @Override
    public String toString() {
        return "UPair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }

    public UPair(T first, R second) {
        this.first = first;
        this.second = second;
    }
}
