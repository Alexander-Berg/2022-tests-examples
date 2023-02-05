package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class TicketToRemoveRuleTest {

    @Test
    fun `Does not Report issue if class is not toggle or experiment`() {
        val findings = """
            
            class Sample {
            
            override val ticketToRemove = "https://st.yandex-team.ru/BLUEMARKETAPPS-31427"
            
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Reports issue if class is AbstractFeatureToggleManager and ticket is bad`() {
        val findings = """
            
            class Sample : AbstractFeatureToggleManager {
            
            override val ticketToRemove = "some text"
            
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The ticket to remove must be like `https://st.yandex-team.ru/BLUEMARKETAPPS-XXXX`"
        )
    }

    @Test
    fun `Reports issue if class is AbstractFeatureConfigManager and ticket is bad`() {
        val findings = """
            
            class Sample : AbstractFeatureConfigManager {
            
            override val ticketToRemove = "some text"
            
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The ticket to remove must be like `https://st.yandex-team.ru/BLUEMARKETAPPS-XXXX`"
        )
    }

    @Test
    fun `Reports issue if class is Experiment and ticket is bad`() {
        val findings = """
            
            class Sample : Experiment {
            
            override val ticketToRemove = "some text"
            
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The ticket to remove must be like `https://st.yandex-team.ru/BLUEMARKETAPPS-XXXX`"
        )
    }

    @Test
    fun `Does not reports issue if class is Experiment and ticket is good`() {
        val findings = """
            
            class Sample : Experiment {
            
            override val ticketToRemove = "https://st.yandex-team.ru/BLUEMARKETAPPS-XXXX"
            
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    private fun String.toFindings(): List<Finding> = TicketToRemoveRule().lint(trimIndent())
}