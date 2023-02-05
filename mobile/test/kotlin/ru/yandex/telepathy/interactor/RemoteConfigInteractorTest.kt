package ru.yandex.telepathy.interactor

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import ru.yandex.telepathy.ConfigSource
import ru.yandex.telepathy.MergeStrategy
import ru.yandex.telepathy.TelepathistAssignment
import ru.yandex.telepathy.model.JsonPath
import ru.yandex.telepathy.model.RemoteConfigEntry
import ru.yandex.telepathy.repository.RemoteConfigCache
import ru.yandex.telepathy.repository.RemoteConfigClient
import ru.yandex.telepathy.testutils.TestConfig

class RemoteConfigInteractorTest {

    private lateinit var remoteConfigInteractor: RemoteConfigInteractor

    private lateinit var remoteConfigCache: RemoteConfigCache

    private lateinit var remoteConfigClient: RemoteConfigClient

    private val configSource = ConfigSource.custom("https://ya.ru")

    private val assignment = TelepathistAssignment.compose()
        .loadFrom(configSource)
        .sealInEnvelope()

    private val assignmentWithOverride = TelepathistAssignment.compose()
        .loadFrom(configSource)
        .thenMerge(mapOf(TestConfig.override11 to TestConfig.override11), MergeStrategy.AppendOnly)
        .sealInEnvelope()

    @Before
    fun runBeforeAnyTest() {
        remoteConfigCache = mock()
        remoteConfigCache.stub {
            on { isConfigOutdated(any()) } doReturn true
            on { readConfig() } doReturn TestConfig.empty()
        }

        remoteConfigClient = mock()
        remoteConfigClient.stub {
            on { fetchConfig(any()) } doReturn TestConfig.map
        }

        remoteConfigInteractor = RemoteConfigInteractor(remoteConfigCache, remoteConfigClient)
        RemoteConfigInteractor.cache = null
    }

    @Test
    fun fetchConfig_whenConfigIsOutdated_mustLoadConfigFromApi() {
        remoteConfigInteractor.fetchConfig(assignment)
        verify(remoteConfigClient).fetchConfig(assignment)
    }

    @Test
    fun fetchConfig_whenConfigIsNotOutdated_mustLoadConfigFromApi() {
        remoteConfigCache.stub { on { isConfigOutdated(any()) } doReturn false }
        remoteConfigInteractor.fetchConfig(assignment)
        verify(remoteConfigClient, never()).fetchConfig(assignment)
        verify(remoteConfigCache, times(1)).readConfig()
    }

    @Test
    fun fetchConfig_whenLoadingFromApi_mustSaveConfigInCache() {
        remoteConfigInteractor.fetchConfig(assignment)
        verify(remoteConfigCache, times(1)).writeConfig(any())
        val config = remoteConfigInteractor.getCachedConfig()
        verify(remoteConfigCache, times(1)).readConfig()
        assertThat(config.getChildren(JsonPath())).isEqualTo(listOf(
            RemoteConfigEntry(TestConfig.obj1Key, TestConfig.object1, null, false, true),
            RemoteConfigEntry(TestConfig.obj2Key, TestConfig.object2, null, false, true)
        ))
    }

    @Test
    fun getCachedConfig_whenValueNotCachedInMemory_shouldLoadFromPrefs() {
        remoteConfigInteractor.getCachedConfig()
        verify(remoteConfigCache, times(1)).readConfig()
        remoteConfigInteractor.getCachedConfig()
        verify(remoteConfigCache, times(1)).readConfig()
    }

    @Test
    fun fetchConfig_whenConfigIsOutdated_shouldMergeWithAssignment() {
        val remoteConfig = remoteConfigInteractor.fetchConfig(assignmentWithOverride)
        assertThat(remoteConfig.getChildren(JsonPath())).isEqualTo(listOf(
            RemoteConfigEntry(TestConfig.obj1Key, TestConfig.object1, null, false, true),
            RemoteConfigEntry(TestConfig.obj2Key, TestConfig.object2, null, false, true),
            RemoteConfigEntry(TestConfig.override11, TestConfig.override11, null, false, true)
        ))
    }

    @Test
    fun fetchConfig_whenConfigIsNotOutdated_shouldMergeWithAssignment() {
        remoteConfigCache.stub { on { isConfigOutdated(any()) } doReturn false }
        RemoteConfigInteractor.cache = TestConfig.nonEmpty()
        val remoteConfig = remoteConfigInteractor.fetchConfig(assignmentWithOverride)
        assertThat(remoteConfig.getChildren(JsonPath())).isEqualTo(listOf(
            RemoteConfigEntry(TestConfig.obj1Key, TestConfig.object1, null, false, true),
            RemoteConfigEntry(TestConfig.obj2Key, TestConfig.object2, null, false, true),
            RemoteConfigEntry(TestConfig.override11, TestConfig.override11, null, false, true)
        ))
    }
}
