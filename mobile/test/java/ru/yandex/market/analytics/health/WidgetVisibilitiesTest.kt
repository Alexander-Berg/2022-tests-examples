package ru.yandex.market.analytics.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.common.WidgetVisibilities

class WidgetVisibilitiesTest {

    private val widgetVisibilities = WidgetVisibilities()

    @Test
    fun `Method countShownFromStart returns visible widgets count`() {
        widgetVisibilities.setVisibility(0, true)
        widgetVisibilities.setVisibility(1, true)

        val shownFromStart = widgetVisibilities.countShownFromStart()
        assertThat(shownFromStart).isEqualTo(2)
    }

    @Test
    fun `Method countShownFromStart continues counting after hidden widget`() {
        widgetVisibilities.setVisibility(0, true)
        widgetVisibilities.setVisibility(1, false)
        widgetVisibilities.setVisibility(2, true)

        val shownFromStart = widgetVisibilities.countShownFromStart()
        assertThat(shownFromStart).isEqualTo(2)
    }

    @Test
    fun `Method countShownFromStart breaks counting on first unknown widget`() {
        widgetVisibilities.setVisibility(0, true)
        widgetVisibilities.setVisibility(2, true)

        val shownFromStart = widgetVisibilities.countShownFromStart()
        assertThat(shownFromStart).isEqualTo(1)
    }

    @Test
    fun `Method countKnownFromStart returns known widgets count`() {
        widgetVisibilities.setVisibility(0, true)
        widgetVisibilities.setVisibility(1, false)

        val knownFromStart = widgetVisibilities.countKnownFromStart()
        assertThat(knownFromStart).isEqualTo(2)
    }

    @Test
    fun `Method countKnownFromStart breaks counting on first unknown widget`() {
        widgetVisibilities.setVisibility(0, true)
        widgetVisibilities.setVisibility(2, true)

        val knownFromStart = widgetVisibilities.countKnownFromStart()
        assertThat(knownFromStart).isEqualTo(1)
    }

    @Test
    fun `There is no crash for index out of supported size`() {
        widgetVisibilities.setVisibility(9999999, true)
    }

}