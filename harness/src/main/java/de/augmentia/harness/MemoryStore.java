package de.augmentia.harness;

public interface MemoryStore {
    void save(String key, String value);
    String load(String key);

    static MemoryStore inMemory() {
        return new MemoryStore() {
            private final java.util.Map<String, String> store = new java.util.concurrent.ConcurrentHashMap<>();
            public void save(String key, String value) { store.put(key, value); }
            public String load(String key) { return store.get(key); }
        };
    }
}