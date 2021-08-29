package util.counter;

import util.monoid.Monoid;

public class StringCounter<K> extends Counter<K, String, String> {
    public StringCounter() {
        super(Monoid.StringMonoid);
    }

    public StringCounter(String one) {
        super(Monoid.ofOne(one));
    }
}
