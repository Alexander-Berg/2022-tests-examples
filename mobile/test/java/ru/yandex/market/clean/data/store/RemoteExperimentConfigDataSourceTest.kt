package ru.yandex.market.clean.data.store

import com.annimon.stream.Optional
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.analytics.PulseConfigurator
import ru.yandex.market.clean.data.fapi.source.experiments.ExperimentsFapiClient
import ru.yandex.market.clean.data.source.experiment.RemoteExperimentConfigDataSource
import ru.yandex.market.common.experiments.config.ExperimentConfig
import ru.yandex.market.common.startapp.StartAppMetricaSender
import ru.yandex.market.data.experiments.dto.ExperimentConfigDto
import ru.yandex.market.data.experiments.mapper.ExperimentConfigMapper
import ru.yandex.market.data.identifiers.repository.IdentifierRepository
import ru.yandex.market.domain.auth.model.YandexUid
import ru.yandex.market.feature.manager.FapiExperimentsFeatureManager
import ru.yandex.market.internal.PreferencesDataStore
import ru.yandex.market.net.experiment.StartupResponse
import ru.yandex.market.net.http.HttpClient

class RemoteExperimentConfigDataSourceTest {

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

    val source = RemoteExperimentConfigDataSource(
        dataStore,
        ExperimentConfigMapper()
    )

    @Test
    fun `get configs`() {
        val actualConfigs = source.getExperimentConfigs(YandexUid(""))
        assertEquals(expectConfigs, actualConfigs)
    }

    companion object {
        private val CAPI_RESPONSE = StartupResponse(
            configsList = emptyList(),
            yandexUid = "uid from capi"
        )

        private val FAPI_RESPONSE = StartupResponse(
            configsList = listOf(
                ExperimentConfigDto("fapi config 3", null, "", emptyList()),
                ExperimentConfigDto("fapi config 4", "", null, null),
                ExperimentConfigDto("", null, null, null)
            ),
            yandexUid = "uid from fapi"
        )

        val expectConfigs = listOf(
            ExperimentConfig(
                testId = "fapi config 3",
                alias = null,
                bucketId = "",
                rearrFlags = emptyList(),
                isOverride = false,
            ),
            ExperimentConfig(
                testId = "fapi config 4",
                alias = "",
                bucketId = null,
                rearrFlags = null,
                isOverride = false,
            )
        )
    }
}
