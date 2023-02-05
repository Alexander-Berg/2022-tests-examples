package ru.yandex.market.clean.data.mapper

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.analitycs.events.health.HealthEvent
import ru.yandex.market.analytics.health.HealthLevel
import ru.yandex.market.analytics.health.HealthName
import ru.yandex.market.analytics.health.HealthPortion
import ru.yandex.market.analitycs.events.health.additionalData.DuplicateWidgetIdInfo
import ru.yandex.market.clean.data.mapper.cms.CmsWidgetIdNormalizer
import ru.yandex.market.clean.domain.model.cms.CmsWidget

@RunWith(Parameterized::class)
class CmsWidgetIdNormalizerTest(
    private val originalWidgets: List<CmsWidget>,
    private val pageIds: List<String>,
    private val isAnalyticsServiceReported: Boolean
) {
    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val event = HealthEvent.builder()
        .name(HealthName.DUPLICATE_WIDGET_ID)
        .level(HealthLevel.ERROR)
        .portion(HealthPortion.CORE)
        .info(
            DuplicateWidgetIdInfo(
                pageIds
            )
        )
        .build()

    private val mapper = CmsWidgetIdNormalizer(
        analyticsService
    )

    @Test
    fun `Check mapper fixes duplicate ids and reports event`() {
        val fixedWidgets = mapper.fixIds(originalWidgets, pageIds)
        assert(fixedWidgets.distinctBy { it.id() }.size == originalWidgets.size)

        for (i in fixedWidgets.indices) {
            val originalWidget = originalWidgets[i]
            val fixedWidget = fixedWidgets[i]

            // здесь проверяем, что originalWidget и fixedWidget различаются только id
            if (originalWidget != fixedWidget) {
                assertEquals(fixedWidget.copy(id = originalWidget.id), originalWidget)
            }
        }

        if (isAnalyticsServiceReported) {
            verify(analyticsService, times(1)).report(event)
        } else {
            verify(analyticsService, never()).report(any<HealthEvent>())
        }
    }


    companion object {

        private const val ID = "repeated_id"
        private val REPEATED_ID_WIDGET = CmsWidget.testBuilder().id(ID).build()
        private val RANDOM_WIDGET = CmsWidget.testBuilder().id("test_id").build()
        private val HACKY_ID_WIDGET = REPEATED_ID_WIDGET.copy(id = "${ID}_1")

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            arrayOf(listOf(REPEATED_ID_WIDGET, RANDOM_WIDGET, HACKY_ID_WIDGET), emptyList<String>(), false),
            arrayOf(listOf(REPEATED_ID_WIDGET, REPEATED_ID_WIDGET), emptyList<String>(), true),
            arrayOf(listOf(REPEATED_ID_WIDGET, RANDOM_WIDGET, REPEATED_ID_WIDGET), listOf("1", "2"), true),
            arrayOf(
                listOf(REPEATED_ID_WIDGET, REPEATED_ID_WIDGET, REPEATED_ID_WIDGET, RANDOM_WIDGET),
                listOf("3"),
                true
            ),
            arrayOf(listOf(HACKY_ID_WIDGET, REPEATED_ID_WIDGET, REPEATED_ID_WIDGET), listOf("4"), true),
            arrayOf(
                listOf(REPEATED_ID_WIDGET, REPEATED_ID_WIDGET, HACKY_ID_WIDGET, REPEATED_ID_WIDGET, RANDOM_WIDGET),
                listOf("5"), true
            )
        )
    }
}