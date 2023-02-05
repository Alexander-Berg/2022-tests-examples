package ru.yandex.market.clean.presentation.feature.cms.item.carousel.livestream

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CarouselOnFirstVisibleItemListenerTest {

    private val layoutManager = mock<LinearLayoutManager>()
    private val recyclerView = mock<RecyclerView> {
        on { layoutManager } doReturn layoutManager
    }
    private val carouselLiveStreamWidgetItem = mock<CarouselLiveStreamHorizontalSnippetItem>()
    private val carouselLiveStreamWidgetItem1 = mock<CarouselLiveStreamHorizontalSnippetItem>()

    private val items = listOf(
        carouselLiveStreamWidgetItem,
        carouselLiveStreamWidgetItem1,
    )
    private val scrollListener = CarouselOnFirstVisibleItemListener(items)

    @Test
    fun `Should allow play video for first item when never scrolled before`() {
        scrollListener.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE)

        verify(items.getOrNull(0))?.isItemAllowedToPlayVideo = true
    }

    @Test
    fun `Should allow play video for second item and forbid for first when scrolled before and first visible position = 1`() {
        whenever(layoutManager.findFirstCompletelyVisibleItemPosition()).thenReturn(1)
        scrollListener.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE)
        scrollListener.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE)

        verify(items.getOrNull(0))?.isItemAllowedToPlayVideo = false
        verify(items.getOrNull(1))?.isItemAllowedToPlayVideo = true
    }
}