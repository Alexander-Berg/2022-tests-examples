package ru.yandex.market.clean.data.repository.config

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.common.config.ConfigKeys
import ru.yandex.market.common.config.PersistentConfigDataStore

class ConfigRepositoryTest {

    private val persistentConfigDataStore = mock<PersistentConfigDataStore>()
    private val repository = ConfigRepository(persistentConfigDataStore)

    @Before
    fun before() {
        whenever(persistentConfigDataStore.getPersistedString(any()))
            .thenReturn(null)
        whenever(persistentConfigDataStore.getPersistedBoolean(any()))
            .thenReturn(null)
        TEST_STRING_DESCRIPTOR.override = null
        TEST_BOOLEAN_DESCRIPTOR.override = null
    }

    @Test
    fun `Return default string if there is no persisted string`() {
        val defaultValue = "default string value"

        val value = repository.getString(TEST_STRING_DESCRIPTOR, defaultValue)

        assertEquals(defaultValue, value)
    }

    @Test
    fun `Return default true if there is no persisted boolean`() {
        val defaultValue = true

        val value = repository.getBoolean(TEST_BOOLEAN_DESCRIPTOR, defaultValue)

        assertEquals(defaultValue, value)
    }

    @Test
    fun `Return default false if there is no persisted boolean`() {
        val defaultValue = false

        val value = repository.getBoolean(TEST_BOOLEAN_DESCRIPTOR, defaultValue)

        assertEquals(defaultValue, value)
    }

    @Test
    fun `Return persisted string value`() {
        val persistedValue = "persisted string value"
        val defaultValue = "default value"
        whenever(persistentConfigDataStore.getPersistedString(TEST_STRING_DESCRIPTOR))
            .thenReturn(persistedValue)

        val value = repository.getString(TEST_STRING_DESCRIPTOR, defaultValue)

        assertEquals(persistedValue, value)
    }

    @Test
    fun `Return persisted boolean value`() {
        val persistedValue = true
        val defaultValue = !persistedValue
        whenever(persistentConfigDataStore.getPersistedBoolean(TEST_BOOLEAN_DESCRIPTOR))
            .thenReturn(persistedValue)

        val value = repository.getBoolean(TEST_BOOLEAN_DESCRIPTOR, defaultValue)

        assertEquals(persistedValue, value)
    }

    @Test
    fun `Return overridden string value if there is no persisted`() {
        val defaultValue = "default string value"
        val overriddenValue = "overridden string value"
        TEST_STRING_DESCRIPTOR.override = overriddenValue

        val value = repository.getString(TEST_STRING_DESCRIPTOR, defaultValue)

        assertEquals(overriddenValue, value)
    }

    @Test
    fun `Return overridden true if there is no persisted`() {
        val overriddenValue = true
        val defaultValue = !overriddenValue
        TEST_BOOLEAN_DESCRIPTOR.override = overriddenValue

        val value = repository.getBoolean(TEST_BOOLEAN_DESCRIPTOR, defaultValue)

        assertEquals(overriddenValue, value)
    }

    @Test
    fun `Return overridden false if there is no persisted`() {
        val overriddenValue = false
        val defaultValue = !overriddenValue
        TEST_BOOLEAN_DESCRIPTOR.override = overriddenValue

        val value = repository.getBoolean(TEST_BOOLEAN_DESCRIPTOR, defaultValue)

        assertEquals(overriddenValue, value)
    }

    companion object {
        private val TEST_STRING_DESCRIPTOR = ConfigKeys.Strings.BLUE_CAPI_ENDPOINT
        private val TEST_BOOLEAN_DESCRIPTOR = ConfigKeys.Booleans.SEND_METRICS
    }

}
