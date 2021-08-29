package util.counter;

import util.monoid.Monoid;

import java.util.List;

public class ListCounter<K, V> extends Counter<K, List<V>, V> {
    public ListCounter() {
        super(Monoid.withList());
    }

    public ListCounter(V one) {
        super(Monoid.withList(one));
    }
}
