package ru.yandex.disk.storage

import android.content.SharedPreferences
import java.lang.UnsupportedOperationException

open class MockSharedPreferences : SharedPreferences {
    private val map = HashMap<String, Any>()

    override fun getAll(): Map<String, *>? {
        return map
    }

    override fun getString(key: String, defValue: String?): String? {
        return map[key] as? String ?: defValue
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        return map[key] as? Set<String> ?: defValues
    }

    override fun getInt(key: String, defValue: Int): Int {
        return map[key] as? Int ?: defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        return map[key] as? Long ?: defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return map[key] as? Float ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return map[key] as? Boolean ?: defValue
    }

    override fun contains(key: String): Boolean {
        return map.contains(key)
    }

    override fun edit(): SharedPreferences.Editor? {
        return MockEditor(map)
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        throw UnsupportedOperationException()
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        throw UnsupportedOperationException()
    }

}

private class MockEditor constructor(private val map: MutableMap<String, Any>) : SharedPreferences.Editor {
    val putValues = HashMap<String, Any>()
    val removeKeys = HashSet<String>()
    var clear = false

    override fun putString(key: String, value: String?): SharedPreferences.Editor {
        if (value != null) putValues[key] = value else removeKeys.add(key)
        return this
    }

    override fun putStringSet(key: String, value: Set<String>?): SharedPreferences.Editor {
        if (value != null) putValues[key] = value else removeKeys.add(key)
        return this
    }

    override fun putInt(key: String, value: Int): SharedPreferences.Editor {
        putValues[key] = value
        return this
    }

    override fun putLong(key: String, value: Long): SharedPreferences.Editor {
        putValues[key] = value
        return this
    }

    override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
        putValues[key] = value
        return this
    }

    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
        putValues[key] = value
        return this
    }

    override fun remove(key: String): SharedPreferences.Editor {
        removeKeys.add(key)
        return this
    }

    override fun clear(): SharedPreferences.Editor {
        clear = true
        return this
    }

    override fun commit(): Boolean {
        if (!clear) {
            for ((key, value) in putValues.entries) {
                map[key] = value
            }
            for (key in removeKeys) {
                map.remove(key)
            }
        } else {
            map.clear()
        }
        return true
    }

    override fun apply() {
        commit()
    }
}