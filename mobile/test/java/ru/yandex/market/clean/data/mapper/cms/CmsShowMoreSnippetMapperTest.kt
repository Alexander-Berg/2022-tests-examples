package ru.yandex.market.clean.data.mapper.cms

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.mock
import ru.yandex.market.base.network.common.address.HttpAddressParser
import ru.yandex.market.clean.domain.model.cms.CmsWidget
import ru.yandex.market.clean.domain.model.cms.CmsWidgetShowMoreSnippet
import ru.yandex.market.clean.domain.model.cms.WidgetData
import ru.yandex.market.clean.presentation.feature.cms.item.carousel.CarouselProductItemSnippetTheme
import ru.yandex.market.data.deeplinks.DeeplinkParser
import ru.yandex.market.common.android.ResourcesManager

class CmsShowMoreSnippetMapperTest {

    private val deeplinkParser = mock<DeeplinkParser> {}
    private val httpAddressParser = HttpAddressParser()
    private val resourceDataStore = mock<ResourcesManager>()
    private val cmsShowMoreSnippetMapper =
        CmsShowMoreSnippetMapper(httpAddressParser, deeplinkParser, resourceDataStore)

    @Test
    fun `Test mapFromShowMoreSnippetButton`() {
        val cmsWidget = CmsWidget.testInstance()
        val widgetData = WidgetData.EMPTY.copy(titleLink = "title_link")
        val mappedResult = cmsShowMoreSnippetMapper.mapFromShowMoreSnippetButton(widgetData, cmsWidget)
        Assertions.assertThat(mappedResult).isNotNull
        Assertions.assertThat(mappedResult?.snippetTheme).isEqualTo(cmsWidget.snippetTheme())
        Assertions.assertThat(mappedResult?.title).isNull()
        Assertions.assertThat(mappedResult?.url).isNull()
        Assertions.assertThat(mappedResult?.uriDeeplink).isEqualTo(widgetData.titleLink)
        Assertions.assertThat(mappedResult?.linkFromHandle).isTrue
    }

    @Test
    fun `Test mapFromShowMoreSnippetBottom`() {
        val builderShowMoreSnippet = CmsWidgetShowMoreSnippet(
            title = "title",
            uriDeeplink = "some_deeplink",
            snippetTheme = CarouselProductItemSnippetTheme.UNO
        )
        val cmsWidget = CmsWidget.testBuilder().showMoreSnippetBottom(builderShowMoreSnippet).build()
        val snippetBottom = cmsWidget.showMoreSnippetBottom()
        Assertions.assertThat(snippetBottom).isEqualTo(builderShowMoreSnippet)
        val mappedResult = cmsShowMoreSnippetMapper.mapFromShowMoreSnippetBottom(cmsWidget)
        Assertions.assertThat(mappedResult).isNotNull
        Assertions.assertThat(snippetBottom).isNotNull
        Assertions.assertThat(mappedResult?.snippetTheme).isEqualTo(snippetBottom?.snippetTheme)
        Assertions.assertThat(mappedResult?.title).isEqualTo(snippetBottom?.title)
        Assertions.assertThat(mappedResult?.url).isEqualTo(snippetBottom?.url)
        Assertions.assertThat(mappedResult?.uriDeeplink).isEqualTo(snippetBottom?.uriDeeplink)
        Assertions.assertThat(mappedResult?.targetScreen).isEqualTo(snippetBottom?.targetScreen)
    }

}