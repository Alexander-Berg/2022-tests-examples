package ru.yandex.telepathy.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import ru.yandex.telepathy.exception.EntryNotFoundException
import ru.yandex.telepathy.testutils.TestConfig
import ru.yandex.telepathy.testutils.jsonPath

class RemoteConfigTest {
    @Test
    fun getEntries_whenConfigIsEmpty_shouldReturnEmptyList() {
        assertThat(TestConfig.empty().getChildren(JsonPath())).isEmpty()
    }

    @Test
    fun getEntries_whenPathNotExist_shouldReturnEmptyList() {
        assertThat(TestConfig.empty().getChildren(jsonPath("a", "b", "c"))).isEmpty()
    }

    @Test
    fun getEntries_withEmptyPath_shouldReturnAllTopLevelEntries() {
        assertThat(TestConfig.nonEmpty().getChildren(jsonPath()))
            .isEqualTo(
                listOf(
                    RemoteConfigEntry(TestConfig.obj1Key, TestConfig.object1, null, false, true),
                    RemoteConfigEntry(TestConfig.obj2Key, TestConfig.object2, null, false, true)
                )
            )
    }

    @Test
    fun getEntries_withTwoValuesInPath_shouldReturnEntriesFromNestedObject() {
        assertThat(TestConfig.nonEmpty().getChildren(jsonPath(TestConfig.obj1Key, TestConfig.obj11Key)))
            .isEqualTo(
                listOf(
                    RemoteConfigEntry(TestConfig.obj111Key, TestConfig.object111, null, false, true),
                    RemoteConfigEntry(TestConfig.value111, TestConfig.value111, null, false, true)
                )
            )
    }

    @Test
    fun getEntries_withOverride_shouldReturnOverriddenValues() {
        val config = TestConfig.withOverride(
            mutableMapOf(
                TestConfig.obj1Key to mutableMapOf(
                    TestConfig.value11 to TestConfig.override11,
                    TestConfig.override12 to TestConfig.override12
                )
            )
        )
        assertThat(config.getChildren(jsonPath(TestConfig.obj1Key)))
            .isEqualTo(
                listOf(
                    RemoteConfigEntry(TestConfig.obj11Key, TestConfig.object11, null, false, true),
                    RemoteConfigEntry(TestConfig.obj12Key, TestConfig.object12, null, false, true),
                    RemoteConfigEntry(TestConfig.override12, null, TestConfig.override12, true, true),
                    RemoteConfigEntry(TestConfig.value11, TestConfig.value11, TestConfig.override11, true, true)
                )
            )
    }

    @Test
    fun getEntries_whenNullValue_shouldAlsoReturnEntry() {
        val config = RemoteConfig(JsonRoot.with(mapOf(TestConfig.value11 to null)), JsonRoot.empty())
        assertThat(config.getChildren(JsonPath())).isEqualTo(
            listOf(RemoteConfigEntry(TestConfig.value11, null, null, false, true))
        )
    }

    @Test
    fun getEntry_withEmptyPath_shouldReturnMap() {
        assertThat(TestConfig.nonEmpty().getElement(JsonPath()))
            .isEqualTo(RemoteConfigEntry("JSON Root", TestConfig.map, null, false, true))
    }

    @Test
    fun getEntry_forTopLevelObject() {
        assertThat(TestConfig.nonEmpty().getElement(jsonPath(TestConfig.obj1Key)))
            .isEqualTo(RemoteConfigEntry(TestConfig.obj1Key, TestConfig.object1, null, false, true))
    }

    @Test
    fun getEntry_forNestedObject() {
        val config = TestConfig.nonEmpty()
        val entry = config.getElement(jsonPath(TestConfig.obj1Key, TestConfig.obj11Key, TestConfig.value111))
        assertThat(entry).isEqualTo(RemoteConfigEntry(TestConfig.value111, TestConfig.value111, null, false, true))
    }

    @Test
    fun getEntry_forNonExistedObject() {
        val config = TestConfig.nonEmpty()
        val entry = config.getElement(jsonPath(TestConfig.override11))
        assertThat(entry).isEqualTo(RemoteConfigEntry(TestConfig.override11, null, null, false, false))
    }

    @Test
    fun getEntry_forOverriddenObject() {
        val config = TestConfig.withOverride(mutableMapOf(TestConfig.obj1Key to TestConfig.override11))
        assertThat(config.getElement(jsonPath(TestConfig.obj1Key))).isEqualTo(
            RemoteConfigEntry(TestConfig.obj1Key, TestConfig.object1, TestConfig.override11, true, true)
        )
    }

    @Test
    fun getEntry_whenParentIsOverridden_shouldReturnOverriddenValue() {
        val config = TestConfig.withOverride(
            mutableMapOf(
                TestConfig.obj1Key to mapOf(TestConfig.override11 to TestConfig.override11)
            )
        )
        assertThat(config.getElement(jsonPath(TestConfig.obj1Key, TestConfig.value11)))
            .isEqualTo(
                RemoteConfigEntry(TestConfig.value11, TestConfig.value11, null, false, true)
            )
    }

    @Test
    fun getEntry_forOverriddenObject_ifObjectNotExist() {
        val config = TestConfig.withOverride(mutableMapOf(TestConfig.override11 to TestConfig.override11))
        val entry = config.getElement(jsonPath(TestConfig.override11))
        assertThat(entry).isEqualTo(RemoteConfigEntry(TestConfig.override11, null, TestConfig.override11, true, true))
    }

    @Test
    fun override_topLevelValue() {
        val config = TestConfig.nonEmpty()
        config.override(jsonPath(TestConfig.obj1Key), TestConfig.override11)

        assertThat(config.getElement(jsonPath(TestConfig.obj1Key)))
            .isEqualTo(RemoteConfigEntry(TestConfig.obj1Key, TestConfig.object1, TestConfig.override11, true, true))

        assertThat(config.getChildren(JsonPath()))
            .isEqualTo(
                listOf(
                    RemoteConfigEntry(TestConfig.obj1Key, TestConfig.object1, TestConfig.override11, true, true),
                    RemoteConfigEntry(TestConfig.obj2Key, TestConfig.object2, null, false, true)
                )
            )
    }

    @Test
    fun override_nestedValue() {
        val config = TestConfig.nonEmpty()
        config.override(jsonPath(TestConfig.obj1Key, TestConfig.obj11Key), TestConfig.override11)

        assertThat(config.getElement(jsonPath(TestConfig.obj1Key, TestConfig.obj11Key)))
            .isEqualTo(RemoteConfigEntry(TestConfig.obj11Key, TestConfig.object11, TestConfig.override11, true, true))

        assertThat(config.getChildren(jsonPath(TestConfig.obj1Key)))
            .isEqualTo(
                listOf(
                    RemoteConfigEntry(TestConfig.obj11Key, TestConfig.object11, TestConfig.override11, true, true),
                    RemoteConfigEntry(TestConfig.obj12Key, TestConfig.object12, null, false, true),
                    RemoteConfigEntry(TestConfig.value11, TestConfig.value11, null, false, true)
                )
            )
    }

    @Test
    fun override_notExisting_topLevelValue() {
        val config = TestConfig.nonEmpty()
        config.override(jsonPath(TestConfig.override11), TestConfig.override11)

        assertThat(config.getElement(jsonPath(TestConfig.override11)))
            .isEqualTo(RemoteConfigEntry(TestConfig.override11, null, TestConfig.override11, true, true))

        assertThat(config.getChildren(JsonPath()))
            .isEqualTo(
                listOf(
                    RemoteConfigEntry(TestConfig.obj1Key, TestConfig.object1, null, false, true),
                    RemoteConfigEntry(TestConfig.obj2Key, TestConfig.object2, null, false, true),
                    RemoteConfigEntry(TestConfig.override11, null, TestConfig.override11, true, true)
                )
            )
    }

    @Test
    fun override_notExisting_nestedValue() {
        val config = TestConfig.nonEmpty()
        config.override(jsonPath(TestConfig.obj2Key, TestConfig.override11), TestConfig.override11)

        assertThat(config.getElement(jsonPath(TestConfig.obj2Key, TestConfig.override11)))
            .isEqualTo(RemoteConfigEntry(TestConfig.override11, null, TestConfig.override11, true, true))

        assertThat(config.getChildren(jsonPath(TestConfig.obj2Key)))
            .isEqualTo(
                listOf(
                    RemoteConfigEntry(TestConfig.override11, null, TestConfig.override11, true, true),
                    RemoteConfigEntry(TestConfig.value21, TestConfig.value21, null, false, true),
                    RemoteConfigEntry(TestConfig.value22, TestConfig.value22, null, false, true)
                )
            )
    }

    @Test
    fun reset_topLevelValue() {
        val config = TestConfig.withOverride(mutableMapOf(TestConfig.obj2Key to TestConfig.override12))
        config.reset(jsonPath(TestConfig.obj2Key))
        assertThat(config.getElement(jsonPath(TestConfig.obj2Key)))
            .isEqualTo(RemoteConfigEntry(TestConfig.obj2Key, TestConfig.object2, null, false, true))
    }

    @Test
    fun reset_nestedValue() {
        val config = TestConfig.nonEmpty()
        val path = jsonPath(TestConfig.obj1Key, TestConfig.obj11Key)
        config.override(path, TestConfig.override11)
        config.reset(path)
        assertThat(config.getElement(path))
            .isEqualTo(RemoteConfigEntry(TestConfig.obj11Key, TestConfig.object11, null, false, true))
    }

    @Test
    fun reset_notExisting_topLevelValue() {
        val config = TestConfig.withOverride(mutableMapOf(TestConfig.override12 to TestConfig.override12))
        config.reset(jsonPath(TestConfig.override12))
        assertThat(config.getElement(jsonPath(TestConfig.override12)))
            .isEqualTo(RemoteConfigEntry(TestConfig.override12, null, null, false, false))
    }

    @Test
    fun reset_notExisting_nestedValue() {
        val config = TestConfig.nonEmpty()
        val path = jsonPath(TestConfig.obj2Key, TestConfig.override11)
        config.override(path, TestConfig.override11)
        config.reset(path)
        assertThat(config.getElement(path))
            .isEqualTo(RemoteConfigEntry(TestConfig.override11, null, null, false, false))
    }

    @Test
    fun reset_shouldThrowException_whenValueNotFound() {
        val config = TestConfig.empty()
        val path = jsonPath(TestConfig.obj2Key, TestConfig.override11)
        assertThatThrownBy { config.reset(path) }.isExactlyInstanceOf(EntryNotFoundException::class.java)
    }
}
