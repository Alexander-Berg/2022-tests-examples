package com.yandex.mail.tools

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

private fun processObject(jsonObject: JSONObject) {
    jsonObject.put("unknown_field_1", "unknown-value-1")
    jsonObject.keys().forEach { key ->
        val o = jsonObject.get(key)
        processUnknown(o)
    }
}

private fun processArray(jsonArray: JSONArray) {
    (0 until jsonArray.length()).forEach { i ->
        val o = jsonArray.get(i)
        processUnknown(o)
    }
}

private fun processUnknown(json: Any) {
    when (json) {
        is JSONObject -> processObject(json)
        is JSONArray -> processArray(json)
    }
}

fun fuzzJson(jsonString: String): String {
    val jsonTokener = JSONTokener(jsonString)
    val json = jsonTokener.nextValue()
    processUnknown(json)
    return json.toString()
}
