package ru.auto.ara.core.utils

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TestJsonItemRepo<T>(
    private val key: String,
    private val prefs: SharedPreferences,
    private val typeToken: TypeToken<ArrayList<T>>,
    private val gson: Gson = Gson()
) {
    fun save(items: List<T>) {
        prefs.edit().putString(key, gson.toJson(items, typeToken.type)).commit()
    }

    fun get(): List<T> = if (prefs.contains(key)) {
        val json = prefs.getString(key, "")
        gson.fromJson<List<T>>(json, typeToken.type)
    } else emptyList()
}
