package ru.yandex.market.clean.data.store

import android.content.Context
import android.os.Build
import com.annimon.stream.Optional
import com.google.gson.Gson
import dagger.MembersInjector
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.TestApplication
import ru.yandex.market.TestComponent
import ru.yandex.market.analytics.PulseConfigurator
import ru.yandex.market.base.presentation.core.schedule.PresentationSchedulers
import ru.yandex.market.clean.data.fapi.source.experiments.ExperimentsFapiClient
import ru.yandex.market.clean.data.source.experiment.RemoteExperimentConfigDataSource
import ru.yandex.market.common.experiments.AbstractExperiment
import ru.yandex.market.common.experiments.ExperimentSplit
import ru.yandex.market.common.experiments.config.ExperimentConfig
import ru.yandex.market.common.experiments.registry.ExperimentRegistry
import ru.yandex.market.common.experiments.service.ExperimentConfigService
import ru.yandex.market.common.preferences.CommonPreferences
import ru.yandex.market.common.startapp.StartAppMetricaSender
import ru.yandex.market.data.experiments.dto.ExperimentConfigDto
import ru.yandex.market.data.experiments.mapper.ExperimentConfigMapper
import ru.yandex.market.data.experiments.pref.ExperimentConfigDao
import ru.yandex.market.data.experiments.pref.IgnoreRemoteExperimentConfigsDao
import ru.yandex.market.data.experiments.source.LocalExperimentConfigDataSource
import ru.yandex.market.data.experiments.store.ActualizedExperimentConfigsDataStore
import ru.yandex.market.data.identifiers.repository.IdentifierRepository
import ru.yandex.market.di.TestScope
import ru.yandex.market.domain.auth.model.Uuid
import ru.yandex.market.experiment.config.ExperimentConfigServiceImpl
import ru.yandex.market.feature.manager.FapiExperimentsFeatureManager
import ru.yandex.market.internal.PreferencesDataStore
import ru.yandex.market.net.experiment.StartupResponse
import ru.yandex.market.net.http.HttpClient
import ru.yandex.market.processor.experiments.annotations.Experiment
import ru.yandex.market.rx.schedulers.YSchedulers
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ExperimentConfigServiceImplTest {

    private val experimentConfigMapper = ExperimentConfigMapper()
    private val actualizedStore = ActualizedExperimentConfigsDataStore()

    private val schedulers = mock<PresentationSchedulers> {
        on { coroutineMainContext } doReturn Dispatchers.Main.immediate
    }

    private val identifierRepository = mock<IdentifierRepository> {
        on { getUuid() } doReturn Uuid("test")
    }

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
    private val startAppMetricaSender = mock<StartAppMetricaSender>()
    private val pulseConfigurator = mock<PulseConfigurator>()

    private val dataStore = StartupConfigDataStore(
        httpClient = capiClient,
        experimentsFapiClient = fapiClient,
        fapiExperimentsFeatureManager = fapiExperimentsFeatureManager,
        identifierRepository = identifierRepository,
        startAppMetricaSender = startAppMetricaSender,
        preferencesDataStore = preferencesDataStore,
        pulseConfigurator = pulseConfigurator
    )

    private val remoteSource = RemoteExperimentConfigDataSource(
        dataStore,
        ExperimentConfigMapper()
    )

    @Inject
    lateinit var commonPreferences: CommonPreferences

    private lateinit var localSource: LocalExperimentConfigDataSource

    private lateinit var service: ExperimentConfigService

    @Before
    fun setUp() {
        YSchedulers.setTestMode()
        DaggerExperimentConfigServiceImplTest_Component.builder()
            .testComponent(TestApplication.instance.component)
            .build()
            .injectMembers(this)

        localSource = LocalExperimentConfigDataSource(
            ExperimentRegistry(listOf(AnotherTestExperiment())),
            ExperimentConfigDao(
                commonPreferences,
                Gson()
            ),
            IgnoreRemoteExperimentConfigsDao(
                commonPreferences
            ),
            experimentConfigMapper
        )


        service = ExperimentConfigServiceImpl(
            schedulers,
            identifierRepository,
            remoteSource,
            localSource,
            actualizedStore
        )
    }

    @Test
    fun `config is not empty await config is loaded`() {
        service.setConfigs(listOf(configTestIdWithoutOverride))
        val isConfigLoaded = service.isConfigLoaded()
        assertTrue(isConfigLoaded)
    }

    @Test
    fun `config is  empty await config is not loaded`() {
        val isConfigLoaded = service.isConfigLoaded()
        assertFalse(isConfigLoaded)
    }

    @Test
    fun `get config cache from actualized`() {
        service.setConfigs(listOf(configTestIdWithoutOverride))
        val actualConfigs = service.getConfigs()
        assertEquals(listOf(configTestIdWithoutOverride), actualConfigs)
    }

    @Test
    fun `configs is empty await default config`() {
        val actualConfigs = service.getConfigs()
        assertEquals(listOf(ExperimentConfig.DEFAULT_CONFIG), actualConfigs)
    }

    @Test
    fun `get local config`() {
        localSource.putExperimentConfigs(listOf(configTestIdWithoutOverride))
        val actualConfigs = service.getConfigs()
        assertEquals(listOf(configTestIdWithoutOverride), actualConfigs)
    }

    @Test
    fun `get actualized configs await default config`() {
        val actualConfigs = service.getActualizedConfigs()
        assertEquals(listOf(ExperimentConfig.DEFAULT_CONFIG), actualConfigs)
    }

    @Test
    fun `get actualized configs await cache config`() {
        actualizedStore.putExperimentSplitConfigs(listOf(configTestIdWithoutOverride))
        val actualConfigs = service.getActualizedConfigs()
        assertEquals(listOf(configTestIdWithoutOverride), actualConfigs)
    }

    @Test
    fun `get empty saved configs`() {
        val configs = service.getSavedConfigs()
        assertEquals(emptyList<ExperimentConfig>(), configs)
    }

    @Test
    fun `get saved configs`() {
        val localConfigs = localSource.putExperimentConfigs(listOf(configTestIdWithoutOverride))
        val configs = service.getSavedConfigs()
        assertEquals(localConfigs, configs)
    }

    @Test
    fun `is ignore remote experiment configs await true`() {
        localSource.ignoreRemoteExperimentConfigs = true
        val isIgnore = service.isIgnoreRemoteExperimentConfigs()
        assertTrue(isIgnore)
    }

    @Test
    fun `is ignore remote experiment configs default await false`() {
        val isIgnore = service.isIgnoreRemoteExperimentConfigs()
        assertFalse(isIgnore)
    }


    @Test
    fun `set true in ignore remote experiment configs await clear experiments without local override`() {
        localSource.putExperimentConfigs(
            listOf(configTestIdWithoutOverride, configTestIdWithoutOverrideWithRear, configSomeIdWithOverride)
        )
        service.setIgnoreRemoteExperimentConfigs(true)
        val actualConfigs = service.getConfigs()
        val expectConfigs = listOf(configSomeIdWithOverride)
        assertEquals(expectConfigs, actualConfigs)
    }

    @Test
    fun `set false in ignore remote experiment configs await clear experiments without local override`() {
        val configs = listOf(configTestIdWithoutOverride, configTestIdWithoutOverrideWithRear, configSomeIdWithOverride)
        localSource.putExperimentConfigs(configs)
        service.setIgnoreRemoteExperimentConfigs(false)
        val actualConfigs = service.getConfigs()
        assertEquals(configs, actualConfigs)
    }

    @Test
    fun `set invalidate configs`() {
        service.setConfigs(listOf(configTestIdWithoutOverride))
        val actualConfigs = service.getConfigs()
        assertEquals(listOf(configTestIdWithoutOverride), actualConfigs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `set not valid configs await error`() {
        service.setConfigs(listOf(incorrectConfig))
    }

    @Test
    fun `rewrite split config by admin await admin config `() {
        service.setConfigs(
            listOf(configAnotherTestExperiment)
        )
        val adminConfig = configAnotherTestExperiment.copy(
            alias = "test-experiment_control"
        )
        service.applyLocalOverrides(listOf(adminConfig), emptyList())

        val actual = service.getConfigs()
        val expect = listOf(adminConfig)
        assertEquals(expect, actual)
    }

    @Test
    fun `clear config by remote await default config`() {
        service.setConfigs(
            listOf(configAnotherTestExperiment)
        )
        service.applyLocalOverrides(emptyList(), listOf(configAnotherTestExperiment))

        val actual = service.getConfigs()
        assertEquals(listOf(ExperimentConfig.DEFAULT_CONFIG), actual)
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

        val incorrectConfig = ExperimentConfig(
            "",
            "alias",
            "bucketId",
            emptyList(),
            false
        )

        val configTestIdWithoutOverride = ExperimentConfig(
            "testId",
            "alias",
            "bucketId",
            emptyList(),
            false
        )
        val configTestIdWithoutOverrideWithRear = ExperimentConfig(
            "moreTestId",
            "more-alias",
            "moreBucketId",
            listOf("test_rearr"),
            false
        )
        val configSomeIdWithOverride = ExperimentConfig(
            "someId",
            null,
            "bucketId",
            emptyList(),
            true
        )
        val configAnotherTestExperiment = ExperimentConfig(
            "test-experiment",
            "test-experiment_test",
            "bucketId",
            listOf("my_rearr"),
            true
        )
    }

    @dagger.Component(dependencies = [TestComponent::class])
    @TestScope
    interface Component : MembersInjector<ExperimentConfigServiceImplTest>

    @Experiment
    class AnotherTestExperiment : AbstractExperiment<AnotherTestExperiment.Split>() {

        override val experimentName = "Another Test experiment name"

        override val ticketToRemove = "https://st.yandex-team.ru/BLUEMARKETAPPS-12345"

        override fun registerAliases(registry: AliasRegistry<Split>) {
            registry
                .add(SPLIT_CONTROL, controlSplit)
                .add(SPLIT_TEST, testSplit)
        }

        override fun getDefaultSplit(context: Context) = controlSplit

        override fun splitType(): Class<out Split> = Split::class.java

        class Split() : ExperimentSplit

        companion object {
            private const val SPLIT_CONTROL = "test-experiment_control"
            private const val SPLIT_TEST = "test-experiment_test"

            private val controlSplit = Split()
            private val testSplit = Split()
        }
    }
}
