package ru.yandex.market.clean.data.store

import com.annimon.stream.Optional
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.PulseConfigurator
import ru.yandex.market.clean.data.fapi.source.experiments.ExperimentsFapiClient
import ru.yandex.market.common.startapp.StartAppMetricaSender
import ru.yandex.market.common.startapp.StartupEventName
import ru.yandex.market.data.experiments.dto.ExperimentConfigDto
import ru.yandex.market.data.identifiers.repository.IdentifierRepository
import ru.yandex.market.domain.auth.model.YandexUid
import ru.yandex.market.experiment.config.ExperimentForced
import ru.yandex.market.feature.manager.FapiExperimentsFeatureManager
import ru.yandex.market.internal.PreferencesDataStore
import ru.yandex.market.net.experiment.StartupResponse
import ru.yandex.market.net.http.HttpClient

class StartupConfigDataStoreTest {

    private val capiClient = mock<HttpClient> {
        on { getStartupConfig(any(), any()) } doReturn CAPI_RESPONSE
    }

    private val fapiClient = mock<ExperimentsFapiClient> {
        on { getExperiments(any()) } doReturn FAPI_RESPONSE
    }

    private val fapiExperimentsFeatureManager = mock<FapiExperimentsFeatureManager> {
        on { isEnabledSync() } doReturn true
    }
    private val preferencesDataStore = mock<PreferencesDataStore> {
        on { forcedExperiments } doReturn Optional.empty()
    }
    private val identifierRepository = mock<IdentifierRepository>()
    private val startAppMetricaSender = mock<StartAppMetricaSender>()
    private val pulseConfigurator = mock<PulseConfigurator>()

    val dataStore = StartupConfigDataStore(
        httpClient = capiClient,
        experimentsFapiClient = fapiClient,
        fapiExperimentsFeatureManager = fapiExperimentsFeatureManager,
        identifierRepository = identifierRepository,
        startAppMetricaSender = startAppMetricaSender,
        preferencesDataStore = preferencesDataStore,
        pulseConfigurator = pulseConfigurator
    )

    @get:Rule
    var thrownExceptions: ExpectedException = ExpectedException.none()

    @Test
    fun `get config from cache if cache valid and fapi toggle is enabled`() {
        val cachedUid = dataStore.getStartupConfig(YandexUid("")).yandexUid
        val result = dataStore.getStartupConfig(YandexUid(requireNotNull(cachedUid)))

        assertThat(result).isEqualTo(FAPI_RESPONSE)
        verify(fapiClient).getExperiments(any())
    }

    @Test
    fun `get config from cache if cache valid and fapi toggle is disabled`() {
        whenever(fapiExperimentsFeatureManager.isEnabledSync()) doReturn false
        val cachedUid = dataStore.getStartupConfig(YandexUid("")).yandexUid
        val result = dataStore.getStartupConfig(YandexUid(requireNotNull(cachedUid)))

        assertThat(result).isEqualTo(CAPI_RESPONSE)
        verify(capiClient).getStartupConfig(any(), any())
    }

    @Test
    fun `get config from capi if cache invalid and fapi toggle is disabled`() {
        whenever(fapiExperimentsFeatureManager.isEnabledSync()) doReturn false
        val result = dataStore.getStartupConfig(YandexUid("test"))

        assertThat(result).isEqualTo(CAPI_RESPONSE)
    }

    @Test
    fun `get config from fapi if cache invalid and fapi toggle is enabled`() {
        val result = dataStore.getStartupConfig(YandexUid("test"))

        assertThat(result).isEqualTo(FAPI_RESPONSE)
    }

    @Test
    fun `send correct parameters to capi`() {
        whenever(fapiExperimentsFeatureManager.isEnabledSync()) doReturn false
        val uid = YandexUid("test")
        val forcedExp = listOf(
            ExperimentForced("forced exp 1"),
            ExperimentForced("forced exp 2")
        )
        whenever(preferencesDataStore.forcedExperiments) doReturn Optional.of(forcedExp)
        dataStore.getStartupConfig(uid)

        verify(capiClient).getStartupConfig(uid, forcedExp)
    }

    @Test
    fun `send empty forced experiments to capi if has no forced experiments in prefs`() {
        whenever(fapiExperimentsFeatureManager.isEnabledSync()) doReturn false
        whenever(preferencesDataStore.forcedExperiments) doReturn Optional.empty()
        dataStore.getStartupConfig(YandexUid("test"))

        verify(capiClient).getStartupConfig(any(), eq(emptyList()))
    }

    @Test
    fun `send correct paraments to fapi`() {
        val uid = YandexUid("test")
        val forcedExp = listOf(
            ExperimentForced("forced exp 1"),
            ExperimentForced("forced exp 2")
        )
        whenever(preferencesDataStore.forcedExperiments) doReturn Optional.of(forcedExp)
        dataStore.getStartupConfig(uid)

        verify(fapiClient).getExperiments(forcedExp)
    }

    @Test
    fun `send empty forced experiments to fapi if has no forced experiments in prefs`() {
        whenever(preferencesDataStore.forcedExperiments) doReturn Optional.empty()
        dataStore.getStartupConfig(YandexUid("test"))

        verify(fapiClient).getExperiments(emptyList())
    }

    @Test
    fun `send startup request metrics when use capi`() {
        whenever(fapiExperimentsFeatureManager.isEnabledSync()) doReturn false
        dataStore.getStartupConfig(YandexUid("test"))

        val inOrderChecks = inOrder(startAppMetricaSender)
        inOrderChecks.verify(startAppMetricaSender)
            .trackStartupEvent(StartupEventName.APPLICATION_CONFIG_REQUESTED_EVENT)
        inOrderChecks.verify(startAppMetricaSender)
            .trackStartupEvent(StartupEventName.APPLICATION_CONFIG_PERSISTED_EVENT)
    }

    @Test
    fun `send startup request metrics when use fapi`() {
        dataStore.getStartupConfig(YandexUid("test"))

        val inOrderChecks = inOrder(startAppMetricaSender)
        inOrderChecks.verify(startAppMetricaSender)
            .trackStartupEvent(StartupEventName.APPLICATION_CONFIG_REQUESTED_EVENT)
        inOrderChecks.verify(startAppMetricaSender)
            .trackStartupEvent(StartupEventName.APPLICATION_CONFIG_PERSISTED_EVENT)
    }

    @Test
    fun `save yandex uid from response when use capi`() {
        whenever(fapiExperimentsFeatureManager.isEnabledSync()) doReturn false
        dataStore.getStartupConfig(YandexUid("test"))

        verify(identifierRepository).setYandexUid(YandexUid(requireNotNull(CAPI_RESPONSE.yandexUid)))
    }

    @Test
    fun `save yandex uid from response when use fapi`() {
        dataStore.getStartupConfig(YandexUid("test"))

        verify(identifierRepository).setYandexUid(YandexUid(requireNotNull(FAPI_RESPONSE.yandexUid)))
    }

    @Test
    fun `save yandex uid from request if response uid is empty when use capi`() {
        whenever(fapiExperimentsFeatureManager.isEnabledSync()) doReturn false
        whenever(capiClient.getStartupConfig(any(), any())) doReturn StartupResponse(emptyList(), null)
        val uid = YandexUid("requestUid")
        dataStore.getStartupConfig(uid)

        verify(identifierRepository).setYandexUid(uid)
    }

    @Test
    fun `save yandex uid from request if response uid is empty when use fapi`() {
        whenever(fapiClient.getExperiments(any())) doReturn StartupResponse(emptyList(), null)
        val uid = YandexUid("requestUid")
        dataStore.getStartupConfig(uid)

        verify(identifierRepository).setYandexUid(uid)
    }

    @Test
    fun `set forced experiments`() {
        val forcedExp = "1;1;2;hello3;mo34mmy;1234567;;$(_99"
        dataStore.setForcedExperiments(forcedExp)
        verify(preferencesDataStore).setForcedExperiments(
            listOf(
                ExperimentForced("1"),
                ExperimentForced("1"),
                ExperimentForced("2"),
                ExperimentForced("3"),
                ExperimentForced("34"),
                ExperimentForced("1234567"),
                ExperimentForced("99"),
            )
        )
    }

    @Test
    fun `get forced experiments`() {
        whenever(preferencesDataStore.forcedExperiments) doReturn Optional.of(
            listOf(
                ExperimentForced("1"),
                ExperimentForced("2"),
                ExperimentForced("3"),
                ExperimentForced("34"),
                ExperimentForced("1234567"),
                ExperimentForced("99"),
            )
        )
        val result = dataStore.getForcedExperimentsFromString()
        assertThat(result).isEqualTo("1;2;3;34;1234567;99")
    }

    @Test
    fun `init pulse with experiments`() {
        val testIds = FAPI_RESPONSE.configsList?.mapNotNull { it.testId } ?: emptyList()

        whenever(fapiClient.getExperiments(emptyList())).thenReturn(
            FAPI_RESPONSE
        )

        dataStore.getStartupConfig(null)

        verify(pulseConfigurator).initPulseWithExperiments(testIds)
    }

    @Test
    fun `init pulse without experiments`() {
        whenever(fapiClient.getExperiments(emptyList())).thenThrow(RuntimeException::class.java)

        thrownExceptions.expect(RuntimeException::class.java)
        dataStore.getStartupConfig(null)

        verify(pulseConfigurator).initPulseWithoutExperiments()
    }

    companion object {
        private val CAPI_RESPONSE = StartupResponse(
            configsList = listOf(
                ExperimentConfigDto("capi config 1", null, null, null),
                ExperimentConfigDto("capi config 2", null, null, null)
            ),
            yandexUid = "uid from capi"
        )

        private val FAPI_RESPONSE = StartupResponse(
            configsList = listOf(
                ExperimentConfigDto("fapi config 1", null, null, null),
                ExperimentConfigDto("fapi config 2", null, null, null)
            ),
            yandexUid = "uid from fapi"
        )
    }

}
