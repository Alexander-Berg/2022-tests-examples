package com.yandex.mail.asserts.data

import androidx.work.Data
import com.yandex.mail.provider.Constants
import org.assertj.core.api.Condition

fun action(value: String) = keyWithStringValue(Constants.ACTION_EXTRA, value)

fun uid(value: Long) = keyWithLongValue(Constants.UID_EXTRA, value)

@JvmOverloads
fun keyWithLongValue(key: String, value: Long, defaultValue: Long = -1): Condition<Data> {
    return object : Condition<Data>() {
        override fun matches(data: Data): Boolean {
            return data.getLong(key, defaultValue) == value
        }

        override fun toString(): String {
            return String.format("Have action <%s>", value)
        }
    }
}

fun keyWithStringValue(key: String, value: String): Condition<Data> {
    return object : Condition<Data>() {
        override fun matches(data: Data): Boolean {
            return data.getString(key) == value
        }

        override fun toString(): String {
            return String.format("Have action <%s>", value)
        }
    }
}
