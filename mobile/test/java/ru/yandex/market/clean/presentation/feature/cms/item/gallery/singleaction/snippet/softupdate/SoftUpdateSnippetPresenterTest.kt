package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.softupdate

import io.reactivex.Completable
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.presentation.feature.cms.model.SoftUpdateMergedWidgetCmsVo
import ru.yandex.market.presentationSchedulersMock

class SoftUpdateSnippetPresenterTest {

    private val viewObject = SoftUpdateMergedWidgetCmsVo(
        title = "title",
        subtitle = "sub title",
        buttonTitle = "button title",
        imageUrl = "imageUrl",
    )
    private val useCases = mock<SoftUpdateSnippetUseCases> {
        on { initiateSoftUpdate() } doReturn Completable.complete()
    }
    private val viewState = mock<`SoftUpdateSnippetView$$State`>()

    private val presenter = SoftUpdateSnippetPresenter(
        presentationSchedulersMock(),
        viewObject,
        useCases
    )

    @Before
    fun setup() {
        presenter.setViewState(viewState)
    }

    @Test
    fun `Show data on attach`() {
        presenter.attachView(viewState)

        verify(viewState).showData(viewObject)
    }

    @Test
    fun `Initiate update on button click`() {
        presenter.onButtonClick()

        verify(useCases).initiateSoftUpdate()
    }
}