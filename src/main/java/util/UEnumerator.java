package util;

public class UEnumerator<T> extends UPair<Integer, T> {

    public UEnumerator(Integer first, T second) {
        super(first, second);
    }
    public static <T> UEnumerator<T> of(Integer i, T t) {
        return new UEnumerator<T>(i, t);
    }

    public int i() { return first; }
    public T val() { return second; }

}
