package ru.yandex.market.analitycs.events.morda.widget

import org.junit.Assert
import org.junit.Test
import ru.yandex.market.analitycs.events.morda.widget.entity.skuEntityTestInstance
import ru.yandex.market.clean.data.mapper.cms.CmsWidgetTypeMapper

class WidgetEventTest {

    @Test
    fun `Test offer show event`() {
        val eventName =
            WidgetEvent.testInstance().copy(
                snippet = Snippet.testInstance().copy(entity = skuEntityTestInstance()),
                widgetName = CmsWidgetTypeMapper.SCROLLBOX_NAME
            )
                .getEventTitle()

        Assert.assertEquals("CMS-PAGE_SCROLLBOX_SNIPPET_OFFER_SHOW_VISIBLE", eventName)
    }

    @Test
    fun `Test offer visible event`() {
        val eventName =
            WidgetEvent.testInstance().copy(
                snippet = Snippet.testInstance()
                    .copy(entity = skuEntityTestInstance(), eventType = Snippet.EventType.NAVIGATE),
                widgetName = CmsWidgetTypeMapper.SCROLLBOX_NAME
            )
                .getEventTitle()

        Assert.assertEquals("CMS-PAGE_SCROLLBOX_SNIPPET_OFFER_SHOW_NAVIGATE", eventName)
    }
}