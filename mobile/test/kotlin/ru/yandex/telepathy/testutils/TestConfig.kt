package ru.yandex.telepathy.testutils

import ru.yandex.telepathy.model.JsonRoot
import ru.yandex.telepathy.model.RemoteConfig

/**
 * {
 *     "object1": {
 *         "value11": "value11",
 *         "object11": {
 *             "value111": "value111",
 *             "object111": {
 *                 "value1111": value1111"
 *             }
 *         }
 *         "object12": {
 *         }
 *     }
 *     "object2": {
 *         "value21": "value21",
 *         "value22": "value22"
 *     }
 * }
 */
object TestConfig {
    const val value11 = "value11"
    const val value21 = "value21"
    const val value22 = "value22"
    const val value111 = "value111"
    const val value1111 = "value1111"

    const val override11 = "override11"
    const val override12 = "override12"

    const val obj1Key = "object1"
    const val obj2Key = "object2"
    const val obj11Key = "object11"
    const val obj12Key = "object12"
    const val obj111Key = "object111"

    const val array1Key = "array1"
    const val array2Key = "array2"
    const val array3Key = "array3"

    val object111 = mapOf(value1111 to value1111)

    val object11 = mapOf(value111 to value111, obj111Key to object111)

    val object12 = emptyMap<String, Any?>()

    val object1 = mapOf(value11 to value11, obj11Key to object11, obj12Key to object12)

    val object2 = mapOf(value21 to value21, value22 to value22)

    val map = mapOf(obj1Key to object1, obj2Key to object2)

    val array1 = mapOf("[0]" to value11, "[1]" to value111)

    val array2 = mapOf("[0]" to object1, "[1]" to object2)

    val array3 = emptyMap<String, Any?>()

    fun withArrays() = mapOf(array1Key to array1, array2Key to array2, array3Key to array3)

    fun empty() = RemoteConfig(JsonRoot.empty(), JsonRoot.empty())

    fun nonEmpty() = RemoteConfig(JsonRoot.with(map), JsonRoot.empty())

    fun withOverride(override: Any?) = RemoteConfig(JsonRoot.with(map), JsonRoot.with(override))
}