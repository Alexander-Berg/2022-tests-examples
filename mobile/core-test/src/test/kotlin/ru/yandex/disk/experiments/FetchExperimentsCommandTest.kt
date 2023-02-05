package ru.yandex.disk.experiments

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import ru.yandex.disk.remote.ErrorMapper
import ru.yandex.disk.remote.ExperimentsApi
import ru.yandex.disk.remote.RemoteRepo
import ru.yandex.disk.stats.AnalyticsAgent
import ru.yandex.disk.stats.EventLog
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.util.ScopedKeyValueStore
import ru.yandex.disk.util.UserAgentProvider
import rx.Single

@RunWith(RobolectricTestRunner::class)
class FetchExperimentsCommandTest {
    private val remoteRepo = mock<RemoteRepo>()
    private val userAgentProvider = mock<UserAgentProvider> {
        on { deviceId } doReturn ("testId")
    }

    private val experimentsSettings = ExperimentsSettings(
        ScopedKeyValueStore(TestObjectsFactory.createSettings(RuntimeEnvironment.application!!), "test"),
        ExperimentsWhitelistProvider { emptyList() }
    )
    private val command = FetchExperimentsCommand(userAgentProvider, experimentsSettings, remoteRepo, mock(), mock())

    private val analyticsAgent = mock<AnalyticsAgent>()

    init {
        EventLog.init(true, mock(), analyticsAgent)
    }

    @Test
    fun `should put experiment config to ExperimentSettings`() {
        val mockConfig = createSimpleMockConfig()
        whenever(remoteRepo.listExperiments()).thenReturn(Single.just(mockConfig))

        command.execute(FetchExperimentsCommandRequest())

        assertThat(experimentsSettings.getTestIds()[0], equalTo("testId1"))
        assertThat(experimentsSettings.getFlags()[0], equalTo("flag1"))
    }

    @Test
    fun `should not fail on Exceptions`() {
        whenever(remoteRepo.listExperiments()).thenReturn(ErrorMapper.mapSingleError(NullPointerException()))

        command.execute(FetchExperimentsCommandRequest())

        verify(remoteRepo).listExperiments()
    }

    @Test
    fun `should setup experiment EventLog`() {

        val mockConfig = createSimpleMockConfig()
        whenever(remoteRepo.listExperiments()).thenReturn(Single.just(mockConfig))

        command.execute(FetchExperimentsCommandRequest())

        verify(analyticsAgent).putAppExperiments(listOf("testId1"))
    }

    @Test
    fun `should work with empty testId`() {
        val mockConfig = createMockConfig(emptyList(), emptyList())
        whenever(remoteRepo.listExperiments()).thenReturn(Single.just(mockConfig))

        command.execute(FetchExperimentsCommandRequest())

        verify(analyticsAgent).putAppExperiments(emptyList())
        assertThat(experimentsSettings.getFlags(), empty())
        assertThat(experimentsSettings.getTestIds(), empty())
    }

}

private fun createSimpleMockConfig(): ExperimentsApi.Config {
    return createMockConfig(listOf("flag1"), listOf("testId1"))
}

private fun createMockConfig(flags: List<String>, testIds: List<String>): ExperimentsApi.Config {
    val mockDiskContext = mock<ExperimentsApi.DiskContext> {
        on { this.flags } doReturn flags
        on { this.testIds } doReturn testIds
    }
    val mockContext = mock<ExperimentsApi.Context> {
        on { diskContext } doReturn mockDiskContext
    }
    val mockExperiment = mock<ExperimentsApi.Experiment> {
        on { context } doReturn mockContext
    }
    return mock {
        on { items } doReturn listOf(mockExperiment)
    }
}
