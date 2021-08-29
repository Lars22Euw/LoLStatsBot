package util.counter;

import util.monoid.Monoid;

import java.util.*;

public class Counter<K, V, W> {
    private final Monoid<V, W> monoid;
    public Map<K, V> content = new HashMap<>();

    public Counter(Monoid<V, W> monoid) {
        this.monoid = monoid;
    }

    public int size() {
        return content.size();
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public boolean containsKey(Object key) {
        return content.containsKey((K) key);
    }

    public boolean containsValue(Object value) {
        return content.containsValue((V) value);
    }

    public V get(K key) {
        return content.getOrDefault(key, monoid.zero());
    }

    public V put(K key, W value) {
        return content.put(key, monoid.add.apply(monoid.zero(), value));
    }

    public V remove(K key) {
        return content.remove(key);
    }

    public void putAll(@org.jetbrains.annotations.NotNull Map<? extends K, ? extends V> m) {
        content.putAll(m);
    }

    public void clear() {
        content.clear();
    }

    public Set<K> keySet() {
        return content.keySet();
    }

    public Collection<V> values() {
        return content.values();
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return content.entrySet();
    }

    public void increment(K key, W inc) {
        if (monoid.add == null) {
            throw new UnsupportedOperationException("No add defined for the Monoid " + this.getClass().getSimpleName() + "!");
        }
        content.compute(key, (k, v) -> (v == null) ? monoid.add.apply(monoid.zero(), inc) : monoid.add.apply(v, inc));
    }

    public void decrement(K key, W dec) {
        if (monoid.sub == null) {
            throw new UnsupportedOperationException("No sub defined for the Monoid " + this.getClass().getSimpleName() + "!");
        }
        content.compute(key, (k, v) -> (v == null) ? monoid.sub.apply(monoid.zero(), dec) : monoid.sub.apply(v, dec));
    }

    public void increment(K key) {
        if (monoid.one == null) {
            throw new UnsupportedOperationException("No one defined for the Monoid " + this.getClass().getSimpleName() + "!");
        }
        increment(key, monoid.one);
    }

    public void decrement(K key) {
        if (monoid.one == null) {
            throw new UnsupportedOperationException("No one defined for the Monoid " + this.getClass().getSimpleName() + "!");
        }
        decrement(key, monoid.one);
    }

    @Override
    public String toString() {
        return "Counter" + content;
    }
}
