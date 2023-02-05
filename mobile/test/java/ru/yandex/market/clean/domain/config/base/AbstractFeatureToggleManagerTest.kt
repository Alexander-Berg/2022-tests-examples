package ru.yandex.market.clean.domain.config.base

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.common.featureconfigs.managers.base.AbstractFeatureConfigManager
import ru.yandex.market.common.featureconfigs.managers.base.AbstractFeatureToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.testcase.FeatureConfigMappingTestCase

@RunWith(Enclosed::class)
class AbstractFeatureToggleManagerTest {

    private class TestFeatureToggleManager(
        dependencies: Dependencies,
        override val isEnabledByDefault: Boolean
    ) : AbstractFeatureToggleManager(dependencies) {

        override val name = "Тестовый Конфиг"

        override val key = "test_config"

        override val description: String? = null

        override val ticketToRemove = "https://st.yandex-team.ru/BLUEMARKETAPPS-XXXX"

    }

    class EnabledFeatureToggleMappingTest : FeatureConfigMappingTestCase<FeatureToggle>() {

        override val json = """
            {
                "enabled": true
            }
        """.trimIndent()

        override val config = FeatureToggle(isEnabled = true)

        override fun createManager(dependencies: AbstractFeatureConfigManager.Dependencies): AbstractFeatureConfigManager<FeatureToggle> {
            return TestFeatureToggleManager(
                dependencies = dependencies,
                isEnabledByDefault = false
            )
        }
    }

    class DisabledFeatureToggleMappingTest : FeatureConfigMappingTestCase<FeatureToggle>() {

        override val json = """
            {
                "enabled": false
            }
        """.trimIndent()

        override val config = FeatureToggle(isEnabled = false)

        override fun createManager(dependencies: AbstractFeatureConfigManager.Dependencies): AbstractFeatureConfigManager<FeatureToggle> {
            return TestFeatureToggleManager(
                dependencies = dependencies,
                isEnabledByDefault = true
            )
        }
    }

    class IncorrectValueFeatureToggleMappingTest : FeatureConfigMappingTestCase<FeatureToggle>() {

        override val json = """
            {
                "enabled": 42L
            }
        """.trimIndent()

        override val config = FeatureToggle(isEnabled = false)

        override fun createManager(dependencies: AbstractFeatureConfigManager.Dependencies): AbstractFeatureConfigManager<FeatureToggle> {
            return TestFeatureToggleManager(
                dependencies = dependencies,
                isEnabledByDefault = true
            )
        }
    }

    class InvalidJsonFeatureToggleMappingTest : FeatureConfigMappingTestCase<FeatureToggle>() {

        override val json = "{"

        override val config = FeatureToggle(isEnabled = DEFAULT_VALUE)

        override fun createManager(dependencies: AbstractFeatureConfigManager.Dependencies): AbstractFeatureConfigManager<FeatureToggle> {
            return TestFeatureToggleManager(
                dependencies = dependencies,
                isEnabledByDefault = DEFAULT_VALUE
            )
        }

        companion object {
            private const val DEFAULT_VALUE = true
        }
    }

    class EmptyConfigFeatureToggleMappingTest : FeatureConfigMappingTestCase<FeatureToggle>() {

        override val json = ""

        override val config = FeatureToggle(isEnabled = DEFAULT_VALUE)

        override fun createManager(dependencies: AbstractFeatureConfigManager.Dependencies): AbstractFeatureConfigManager<FeatureToggle> {
            return TestFeatureToggleManager(
                dependencies = dependencies,
                isEnabledByDefault = DEFAULT_VALUE
            )
        }

        companion object {
            private const val DEFAULT_VALUE = true
        }
    }

    class AbsentConfigFeatureToggleMappingTest : FeatureConfigMappingTestCase<FeatureToggle>() {

        override val json: String? = null

        override val config = FeatureToggle(isEnabled = DEFAULT_VALUE)

        override fun createManager(dependencies: AbstractFeatureConfigManager.Dependencies): AbstractFeatureConfigManager<FeatureToggle> {
            return TestFeatureToggleManager(
                dependencies = dependencies,
                isEnabledByDefault = DEFAULT_VALUE
            )
        }

        companion object {
            private const val DEFAULT_VALUE = true
        }
    }

}