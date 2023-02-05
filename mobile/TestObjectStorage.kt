package com.yandex.frankenstein.agent.client

class TestObjectStorage {

    private val objects = mutableMapOf<String, Any>()
    private val lock = Object()

    operator fun set(key: String, value: Any) {
        synchronized(lock) {
            objects[key] = value
            lock.notifyAll()
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String): T {
        synchronized(lock) {
            return objects[key] as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrPut(key: String, defaultValue: () -> T): T {
        synchronized(lock) {
            return objects.getOrPut(key) {
                lock.notifyAll()
                defaultValue()
            } as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrWait(key: String): T {
        synchronized(lock) {
            var value = objects[key]
            while (value == null) {
                lock.wait()
                value = objects[key]
            }

            return value as T
        }
    }
}
