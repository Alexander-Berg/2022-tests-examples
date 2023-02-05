package com.yandex.mail.asserts

import com.yandex.mail.provider.Constants
import com.yandex.mail.tools.TestWorkerFactory.WorkerInfo
import org.assertj.core.api.Condition

fun action(value: String) = keyWithStringValue(Constants.ACTION_EXTRA, value)

fun uid(value: Long) = keyWithLongValue(Constants.UID_EXTRA, value)

@JvmOverloads
fun keyWithLongValue(key: String, value: Long, defaultValue: Long = -1): Condition<WorkerInfo> {
    return object : Condition<WorkerInfo>() {
        override fun matches(worker: WorkerInfo): Boolean {
            return worker.workerParameters.inputData.getLong(key, defaultValue) == value
        }

        override fun toString(): String {
            return String.format("Have action <%s>", value)
        }
    }
}

fun keyWithStringValue(key: String, value: String): Condition<WorkerInfo> {
    return object : Condition<WorkerInfo>() {
        override fun matches(worker: WorkerInfo): Boolean {
            return worker.workerParameters.inputData.getString(key) == value
        }

        override fun toString(): String {
            return String.format("Have action <%s>", value)
        }
    }
}
