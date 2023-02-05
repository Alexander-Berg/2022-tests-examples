// Copyright (c) 2019 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.telepathy.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import ru.yandex.telepathy.exception.EntryNotFoundException
import ru.yandex.telepathy.exception.IllegalTypeException
import ru.yandex.telepathy.testutils.TestConfig
import ru.yandex.telepathy.testutils.jsonPath

class JsonPathTest {
    @Test
    fun getElement_whenEmpty_shouldThrowException() {
        assertThatThrownBy { JsonPath().getElement(JsonRoot.empty()) }
            .isExactlyInstanceOf(EntryNotFoundException::class.java)
    }

    @Test
    fun getElement_whenNullJson_isWorking() {
        assertThat(JsonPath().getElement(JsonRoot.with(null))).isNull()
    }

    @Test
    fun getElement_whenSingleIntJson_isWorking() {
        assertThat(JsonPath().getElement(JsonRoot.with(1))).isEqualTo(1)
    }

    @Test
    fun getElement_whenSingleStringJson_isWorking() {
        assertThat(JsonPath().getElement(JsonRoot.with(""))).isEqualTo("")
    }

    @Test
    fun getElement_whenSingleMapJson_isWorking() {
        assertThat(JsonPath().getElement(JsonRoot.with(emptyMap<Any, Any>()))).isEqualTo(emptyMap<Any, Any>())
    }

    @Test
    fun getElement_objectFromMap() {
        assertThat(jsonPath(TestConfig.obj1Key).getElement(JsonRoot.with(TestConfig.map)))
            .isEqualTo(TestConfig.object1)
    }

    @Test
    fun getElement_objectFromNestedMap() {
        assertThat(jsonPath(TestConfig.obj1Key, TestConfig.value11).getElement(JsonRoot.with(TestConfig.map)))
            .isEqualTo(TestConfig.value11)
    }

    @Test
    fun getElement_objectFromArray() {
        assertThat(jsonPath(TestConfig.array1Key, 0).getElement(JsonRoot.with(TestConfig.withArrays())))
            .isEqualTo(TestConfig.value11)
    }

    @Test
    fun getElement_objectFromMapNestedInArray() {
        val path = jsonPath(TestConfig.array2Key, 1, TestConfig.value22)
        assertThat(path.getElement(JsonRoot.with(TestConfig.withArrays()))).isEqualTo(TestConfig.value22)
    }

    @Test
    fun getElement_whenElementNotExist_shouldThrowEntryNotFoundException() {
        assertThatThrownBy { jsonPath(TestConfig.value11).getElement(JsonRoot.with(emptyMap<String, Any?>())) }
            .isExactlyInstanceOf(EntryNotFoundException::class.java)
    }

    @Test
    fun getElement_whenElementHasWrongType_shouldThrowIllegalTypeException() {
        assertThatThrownBy { jsonPath(0).getElement(JsonRoot.with(emptyList<Any>())) }
            .isExactlyInstanceOf(IllegalTypeException::class.java)
    }

    @Test
    fun put_whenRootElementIsNull_shouldWork() {
        val json = JsonRoot.with(null)
        JsonPath().put(json, 1)
        assertThat(json.value).isEqualTo(1)
    }

    @Test
    fun put_whenRootElementIsEmpty_shouldWork() {
        val json = JsonRoot.empty()
        JsonPath().put(json, 1)
        assertThat(json.value).isEqualTo(1)
    }

    @Test
    fun put_whenRootElementIsMap_shouldWork() {
        val map = mutableMapOf<String, Any>()
        val json = JsonRoot.with(map)
        jsonPath(TestConfig.value11).put(json, 1)
        assertThat(map[TestConfig.value11]).isEqualTo(1)
    }

    @Test
    fun put_whenRootElementIsArray_shouldWork() {
        val list = mutableMapOf<String, Any>()
        val json = JsonRoot.with(list)
        jsonPath(42).put(json, 1)
        assertThat(list["[42]"]).isEqualTo(1)
    }

    @Test
    fun put_whenRootElementIsEmpty_shouldBuildPath() {
        val json = JsonRoot.with(null)
        jsonPath(TestConfig.value11, 0, TestConfig.value11, 0).put(json, 1)
        assertThat(json.value).isInstanceOf(MutableMap::class.java)
        val map1 = json.value as MutableMap<String, Any?>
        assertThat(map1[TestConfig.value11]).isInstanceOf(MutableMap::class.java)
        val array1 = map1[TestConfig.value11] as MutableMap<String, Any?>
        assertThat(array1["[0]"]).isInstanceOf(MutableMap::class.java)
        val map2 = array1["[0]"] as MutableMap<String, Any?>
        assertThat(map2[TestConfig.value11]).isInstanceOf(MutableMap::class.java)
        val array2 = map2[TestConfig.value11] as MutableMap<String, Any?>
        assertThat(array2["[0]"]).isEqualTo(1)
    }

    @Test
    fun put_shouldReplaceExistingValueInJsonMap() {
        val map = mutableMapOf(TestConfig.value11 to 0)
        val json = JsonRoot.with(mapOf(TestConfig.value11 to mapOf("[0]" to map)))
        jsonPath(TestConfig.value11, 0, TestConfig.value11).put(json, 1)
        assertThat(map[TestConfig.value11]).isEqualTo(1)
    }

    @Test
    fun put_shouldReplaceExistingValueInJsonArray() {
        val array = mutableMapOf("[0]" to 0)
        val json = JsonRoot.with(mapOf(TestConfig.value11 to mapOf("[0]" to array)))
        jsonPath(TestConfig.value11, 0, 0).put(json, 1)
        assertThat(array["[0]"]).isEqualTo(1)
    }

    @Test
    fun put_shouldThrowException_whenGivenInvalidPath() {
        val json = JsonRoot.with(mutableListOf(1, 2, 3))
        val path = jsonPath(TestConfig.value11)
        assertThatThrownBy { path.getElement(json) }.isExactlyInstanceOf(IllegalTypeException::class.java)
    }
}