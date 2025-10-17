package de.liforra.namehistory.cache

import java.util.concurrent.ConcurrentHashMap

class SimpleCache<V>(private val ttlMillis: Long, private val maxEntries: Int = 64) {
    private data class Entry<V>(val value: V, val expiresAt: Long)
    private val map = ConcurrentHashMap<String, Entry<V>>()

    fun get(key: String): V? {
        val e = map[key] ?: return null
        if (System.currentTimeMillis() > e.expiresAt) {
            map.remove(key)
            return null
        }
        return e.value
    }

    fun put(key: String, value: V) {
        if (map.size >= maxEntries) {
            val firstKey = map.keys.firstOrNull()
            if (firstKey != null) map.remove(firstKey)
        }
        map[key] = Entry(value, System.currentTimeMillis() + ttlMillis)
    }

    fun clear() = map.clear()
}


