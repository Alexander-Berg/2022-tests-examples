package ru.yandex.market.common.experiments.registry

import android.content.Context
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import ru.yandex.market.common.experiments.AbstractExperiment
import ru.yandex.market.common.experiments.Experiment
import ru.yandex.market.common.experiments.ExperimentSplit

class ExperimentRegistryTest {

    private fun setupRegistry(experiments: List<Experiment<*>>): ExperimentRegistry {
        return ExperimentRegistry(experiments)
    }

    @Test
    fun `test register the same experiments`() {
        val experiments = listOf(TestExperiment(), TestExperiment())
        try {
            setupRegistry(experiments)
            fail("Experiment registry allowed registering the same experiment more than once!")
        } catch (t: Throwable) {
            assertThat(t).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun `test duplicate splits registration handling`() {
        val experiments = listOf(AnotherTestExperiment(), DuplicateSplitNameTestExperiment())
        try {
            setupRegistry(experiments)
            fail("Experiment registry allowed registering experiment using alias name from another experiment!")
        } catch (t: Throwable) {
            assertThat(t).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun `test handling of the attempt to get experiment for unregistered split`() {
        val registry = setupRegistry(listOf())
        try {
            registry.getExperiment<TestExperiment.Split>(TestExperiment.Split::class.java)
            fail("Experiment registry should have thrown exception while trying to access unregistered experiment!")
        } catch (t: Throwable) {
            assertThat(t).isInstanceOf(RuntimeException::class.java)
        }
    }

    @Test
    fun `test handling of the attempt to get experiment for registered split`() {
        val experiments = listOf(TestExperiment())
        val registry = setupRegistry(experiments)
        val experiment = registry.getExperiment<TestExperiment.Split>(TestExperiment.Split::class.java)
        assertThat(experiment).isNotNull
    }

    @Test
    fun `test experiments registration and access operations`() {
        val experiments = listOf(TestExperiment(), AnotherTestExperiment())
        val registry = setupRegistry(experiments)

        val testExperiment = registry.getExperiment<TestExperiment.Split>(TestExperiment.Split::class.java)
        assertThat(testExperiment).isInstanceOf(TestExperiment::class.java)

        val anotherTestExperiment =
            registry.getExperiment<AnotherTestExperiment.Split>(AnotherTestExperiment.Split::class.java)
        assertThat(anotherTestExperiment).isInstanceOf(AnotherTestExperiment::class.java)

        val allRegisteredExperiments = registry.experiments
        assertThat(allRegisteredExperiments)
            .hasSize(2)
            .containsOnlyOnce(testExperiment)
            .containsOnlyOnce(anotherTestExperiment)
    }


    class TestExperiment : AbstractExperiment<TestExperiment.Split>() {

        override val experimentName = "Test experiment name displayed in the debug screen"

        override val ticketToRemove = "https://st.yandex-team.ru/BLUEMARKETAPPS-XXXXX"

        override fun registerAliases(registry: AliasRegistry<Split>) {
            registry
                .add(SPLIT_CONTROL, controlSplit)
                .add(SPLIT_TEST1, testStrategy1)
                .add(SPLIT_TEST2, testStrategy2)
                .add(SPLIT_TEST3, testStrategy3)
        }

        override fun getDefaultSplit(context: Context) = controlSplit

        override fun splitType(): Class<out Split> = Split::class.java

        class Split(val flag: Boolean, val strategy: TestStrategy) : ExperimentSplit

        companion object {
            private const val SPLIT_CONTROL = "test-experiment_control"
            private const val SPLIT_TEST1 = "test-experiment_test1"
            private const val SPLIT_TEST2 = "test-experiment-test2"
            private const val SPLIT_TEST3 = "test-experiment-test3"

            enum class TestStrategy {
                DEFAULT,
                STRATEGY_1,
                STRATEGY_2,
                STRATEGY_3,
            }

            private val controlSplit = Split(flag = false, strategy = TestStrategy.DEFAULT)
            private val testStrategy1 = Split(flag = true, strategy = TestStrategy.STRATEGY_1)
            private val testStrategy2 = Split(flag = true, strategy = TestStrategy.STRATEGY_2)
            private val testStrategy3 = Split(flag = true, strategy = TestStrategy.STRATEGY_3)
        }
    }

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

        class Split(val isFeatureEnabled: Boolean) : ExperimentSplit

        companion object {
            private const val SPLIT_CONTROL = "another-test-experiment_control"
            private const val SPLIT_TEST = "another-test-experiment_test"

            private val controlSplit = Split(isFeatureEnabled = false)
            private val testSplit = Split(isFeatureEnabled = true)
        }
    }

    class DuplicateSplitNameTestExperiment : AbstractExperiment<DuplicateSplitNameTestExperiment.Split>() {

        override val experimentName = "Test experiment that shares split name with another experiment"

        override val ticketToRemove = "https://st.yandex-team.ru/BLUEMARKETAPPS-AbCdE"

        override fun registerAliases(registry: AliasRegistry<Split>) {
            registry
                .add(SPLIT_CONTROL, controlSplit)
                .add(SPLIT_TEST, testSplit)
        }

        override fun getDefaultSplit(context: Context) = controlSplit

        override fun splitType(): Class<out Split> = Split::class.java

        class Split(val isFeatureEnabled: Boolean) : ExperimentSplit

        companion object {
            private const val SPLIT_CONTROL = "duplicate-split-name-test-experiment_control"
            private const val SPLIT_TEST = "another-test-experiment_test"

            private val controlSplit = Split(isFeatureEnabled = false)
            private val testSplit = Split(isFeatureEnabled = true)
        }
    }
}