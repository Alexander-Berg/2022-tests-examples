package ru.yandex.market.clean.data.store

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import dagger.MembersInjector
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.TestApplication
import ru.yandex.market.TestComponent
import ru.yandex.market.common.experiments.AbstractExperiment
import ru.yandex.market.common.experiments.ExperimentSplit
import ru.yandex.market.common.experiments.config.ExperimentConfig
import ru.yandex.market.common.experiments.registry.ExperimentRegistry
import ru.yandex.market.common.preferences.CommonPreferences
import ru.yandex.market.data.experiments.mapper.ExperimentConfigMapper
import ru.yandex.market.data.experiments.pref.ExperimentConfigDao
import ru.yandex.market.data.experiments.pref.IgnoreRemoteExperimentConfigsDao
import ru.yandex.market.data.experiments.source.LocalExperimentConfigDataSource
import ru.yandex.market.di.TestScope
import ru.yandex.market.processor.experiments.annotations.Experiment
import ru.yandex.market.rx.schedulers.YSchedulers
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class LocalExperimentConfigDataSourceTest {

    private val experimentConfigMapper = ExperimentConfigMapper()

    @Inject
    lateinit var commonPreferences: CommonPreferences

    lateinit var source: LocalExperimentConfigDataSource

    @Before
    fun setUp() {
        YSchedulers.setTestMode()
        DaggerLocalExperimentConfigDataSourceTest_Component.builder()
            .testComponent(TestApplication.instance.component)
            .build()
            .injectMembers(this)

        source = LocalExperimentConfigDataSource(
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

    }

    @Test
    fun `put experiment configs without local override await last configs`() {
        source.putExperimentConfigs(
            listOf(configTestIdWithoutOverride)
        )

        val expectConfigs = source.putExperimentConfigs(
            listOf(configTestIdWithoutOverrideWithRear)
        )
        val actualConfigs = source.getExperimentConfigs()
        assertEquals(expectConfigs, actualConfigs)
    }

    @Test
    fun `put experimen configs with local override await local config and last remote config`() {
        source.putExperimentConfigs(
            listOf(configTestIdWithoutOverride, configAnotherTestExperiment)
        )

        val expectConfigs = source.putExperimentConfigs(
            listOf(configTestIdWithoutOverrideWithRear)
        )
        val actualCongigs = source.getExperimentConfigs()

        assertEquals(expectConfigs, actualCongigs)
    }


    @Test
    fun `remove configs await configs where is override true `() {
        source.putExperimentConfigs(
            listOf(configTestIdWithoutOverride, configTestIdWithoutOverrideWithRear, configSomeIdWithOverride)
        )

        source.removeRemoteExperimentConfigs()

        val actualCongigs = source.getExperimentConfigs()
        val expectConfigs = listOf(configSomeIdWithOverride)

        assertEquals(actualCongigs, expectConfigs)
    }

    @Test
    fun `rewrite local override config await modified override config`() {
        source.putExperimentConfigs(
            listOf(configAnotherTestExperiment)
        )

        val newConfigAnotherTestExperiment = configAnotherTestExperiment.copy(
            testId = "newTestId",
            bucketId = "newBucketId",
            rearrFlags = emptyList()
        )

        val actual = source.putExperimentConfigs(
            listOf(newConfigAnotherTestExperiment)
        )

        val expect = listOf(newConfigAnotherTestExperiment)

        assertEquals(expect, actual)
    }

    @Test
    fun `rewrite local override config await first config`() {
        val actual = source.putExperimentConfigs(
            listOf(configAnotherTestExperiment)
        )

        val newConfigAnotherTestExperiment = configAnotherTestExperiment.copy(
            testId = "newTestId",
            bucketId = "newBucketId",
            rearrFlags = emptyList(),
            isOverride = false
        )

        source.putExperimentConfigs(
            listOf(newConfigAnotherTestExperiment)
        )

        val expect = listOf(configAnotherTestExperiment)

        assertEquals(expect, actual)
    }

    @Test
    fun `write config when ignore remote experiment configs await empty configs`() {
        source.ignoreRemoteExperimentConfigs = true
        source.putExperimentConfigs(
            listOf(configTestIdWithoutOverride, configTestIdWithoutOverrideWithRear)
        )

        val actual = source.getExperimentConfigs()

        assertEquals(emptyList<ExperimentConfig>(), actual)
    }


    @Test
    fun `rewrite split config by admin await admin config `() {
        val configs = source.putExperimentConfigs(
            listOf(configAnotherTestExperiment)
        )
        val adminConfig = configAnotherTestExperiment.copy(
            alias = "test-experiment_control"
        )
        source.applyLocalOverrides(listOf(adminConfig), emptyList())

        val actual = source.getExperimentConfigs()
        val expect = listOf(adminConfig)
        assertEquals(expect, actual)
    }

    @Test
    fun `clear config by remote await empty configs`() {
        source.putExperimentConfigs(
            listOf(configAnotherTestExperiment)
        )
        source.applyLocalOverrides(emptyList(), listOf(configAnotherTestExperiment))

        val actual = source.getExperimentConfigs()
        assertEquals(emptyList<ExperimentConfig>(), actual)
    }

    companion object {

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
    interface Component : MembersInjector<LocalExperimentConfigDataSourceTest>

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
