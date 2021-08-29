package util.counter;

import util.monoid.Monoid;

public class DoubleCounter<K> extends Counter<K, Double, Double> {
    public DoubleCounter() {
        super(Monoid.DoubleMonoid);
    }

    public DoubleCounter(Double one) {
        super(Monoid.ofOne(one));
    }
}
