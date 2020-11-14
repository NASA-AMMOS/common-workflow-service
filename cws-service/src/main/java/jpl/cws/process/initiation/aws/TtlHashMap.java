package jpl.cws.process.initiation.aws;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;

/**
 * A hash map that expires and removes items if they are older than a given
 * time-to-live.
 * Modified From: https://gist.github.com/joelittlejohn/5565410
 * <p>
 * The expiry is a passive process, items aren't removed until they are
 * retrieved and deemed to be expired by {@link #get(Object)}.
 */
public class TtlHashMap<K, V> implements Map<K, V> {

	private final HashMap<K, V> store = new HashMap<>();
	private final HashMap<K, Long> timestamps = new HashMap<>();
	private final long ttl;

	public TtlHashMap(TimeUnit ttlUnit, long ttlValue) {
		this.ttl = ttlUnit.toNanos(ttlValue);
	}

	@Override
	public V get(Object key) {
		V value = this.store.get(key);

		if (value != null && expired(key, value)) {
			store.remove(key);
			timestamps.remove(key);
			return null;
		} else {
			return value;
		}
	}

	private boolean expired(Object key, V value) {
		return (System.nanoTime() - timestamps.get(key)) > this.ttl;
	}

	@Override
	public V put(K key, V value) {
		timestamps.put(key, System.nanoTime());
		return store.put(key, value);
	}

	@Override
	public int size() {
		return store.size();
	}

	@Override
	public boolean isEmpty() {
		return store.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return store.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return store.containsValue(value);
	}

	@Override
	public V remove(Object key) {
		timestamps.remove(key);
		return store.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<? extends K, ? extends V> e : m.entrySet()) {
			this.put(e.getKey(), e.getValue());
		}
	}

	@Override
	public void clear() {
		timestamps.clear();
		store.clear();
	}

	@Override
	public Set<K> keySet() {
		clearExpired();
		return unmodifiableSet(store.keySet());
	}

	@Override
	public Collection<V> values() {
		clearExpired();
		return unmodifiableCollection(store.values());
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		clearExpired();
		return unmodifiableSet(store.entrySet());
	}

	private void removeIfExpired(Object key) {
		V value = this.store.get(key);

		if (value != null && expired(key, value)) {
			store.remove(key);
			timestamps.remove(key);
		}
	}

	public void clearExpired() {
		for (Object k : store.keySet().toArray()) {
			this.removeIfExpired(k);
		}
	}

}