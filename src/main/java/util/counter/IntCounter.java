package util.counter;

import util.monoid.Monoid;

public class IntCounter<K> extends Counter<K, Integer, Integer> {
    public IntCounter() {
        super(Monoid.IntMonoid);
    }

    public IntCounter(Integer one) {
        super(Monoid.ofOne(one));
    }
}
