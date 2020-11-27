package util;

public class UPair<T, R> {
    T first;
    R second;

    public static <T,R> UPair of(T t, R r) {
        return new UPair(t, t);
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
