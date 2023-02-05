package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction

import io.reactivex.Completable
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.model.cms.CmsItem
import ru.yandex.market.clean.domain.model.cms.CmsWidget
import ru.yandex.market.clean.presentation.feature.cms.model.CmsViewObject
import ru.yandex.market.presentationSchedulersMock

class SingleActionGalleryWidgetPresenterTest {

    private val widget = CmsWidget.testInstance()
    private val widgetDataSubject: BehaviorSubject<List<CmsItem>> = BehaviorSubject.create()
    private val useCases = mock<SingleActionGalleryWidgetUseCases> {
        on { loadData(eq(widget), any()) } doReturn widgetDataSubject
        on { saveAgitationWidgetsIds(listOf(widget.id())) } doReturn Completable.complete()
        on { setAgitationWidgetItemsLoaded(eq(widget.id()), any()) } doReturn Completable.complete()
        on { onWidgetAttachedToScreen(eq(widget.id()), any()) } doReturn Completable.complete()
    }
    private val firstWidgetData = listOf(mock<CmsItem>())
    private val secondWidgetData = listOf(mock<CmsItem>())
    private val firstWidgetItems = listOf(mock<CmsViewObject>())
    private val secondWidgetItems = listOf(mock<CmsViewObject>())
    private val formatter = mock<SingleActionSnippetsFormatter> {
        on { format(firstWidgetData) } doReturn firstWidgetItems
        on { format(secondWidgetData) } doReturn secondWidgetItems
    }
    private val viewState = mock<`SingleActionGalleryView$$State`>()

    private val presenter = SingleActionGalleryWidgetPresenter(
        presentationSchedulersMock(),
        widget,
        useCases,
        formatter,
        mock()
    )

    @Before
    fun setup() {
        presenter.attachView(viewState)
    }

    @Test
    fun `Observe and show data`() {
        val inOrderCheck = inOrder(viewState)
        widgetDataSubject.onNext(firstWidgetData)
        widgetDataSubject.onNext(secondWidgetData)

        inOrderCheck.verify(viewState).showContent(firstWidgetItems)
        inOrderCheck.verify(viewState).showContent(secondWidgetItems)
    }

    @Test
    fun `Hide widget on first attach`() {
        presenter.attachView(viewState)
        verify(viewState).hide()
    }

    @Test
    fun `Hide widget on empty data`() {
        widgetDataSubject.onNext(emptyList())

        verify(viewState, atLeast(2)).hide()
    }


    @Test
    fun `Hide widget on error`() {
        widgetDataSubject.onError(Error())

        verify(viewState, atLeast(2)).hide()
    }

    @Test
    fun `Save agitation widget id on firs attach`() {
        verify(useCases).saveAgitationWidgetsIds(listOf(widget.id()))
    }

    @Test
    fun `Report agitation widget has data`() {
        widgetDataSubject.onNext(firstWidgetData)
        verify(useCases).setAgitationWidgetItemsLoaded(widget.id(), false)
    }

    @Test
    fun `Report agitation widget has no data`() {
        widgetDataSubject.onNext(emptyList())
        verify(useCases).setAgitationWidgetItemsLoaded(widget.id(), true)
    }

    @Test
    fun `Report agitation widget error`() {
        widgetDataSubject.onError(Error())
        verify(useCases).setAgitationWidgetItemsLoaded(widget.id(), true)
    }

    @Test
    fun `Report agitation widget attached to screen`() {
        presenter.onWidgetAttachedToWindow(true)
        verify(useCases).onWidgetAttachedToScreen(widget.id(), true)
    }

    @Test
    fun `Report agitation widget detached from screen`() {
        presenter.onWidgetAttachedToWindow(false)
        verify(useCases).onWidgetAttachedToScreen(widget.id(), false)
    }
}