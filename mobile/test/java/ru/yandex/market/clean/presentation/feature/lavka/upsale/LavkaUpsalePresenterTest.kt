package ru.yandex.market.clean.presentation.feature.lavka.upsale

import io.reactivex.Observable
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.activity.searchresult.LavkaSearchResultItemProductVo
import ru.yandex.market.activity.searchresult.LavkaSearchResultVoFormatter
import ru.yandex.market.clean.domain.model.lavka2.product.LavkaSearchResultItem
import ru.yandex.market.clean.presentation.feature.lavka.upsale.vo.LavkaUpsaleVo
import ru.yandex.market.presentationSchedulersMock

class LavkaUpsalePresenterTest {

    private val schedulers = presentationSchedulersMock()
    private val lavkaUpsaleUseCases = mock<LavkaUpsaleUseCases> {
        on { observeUpsale() } doReturn Observable.just(emptyList())
        on { isLavkaComboEnabled() } doReturn Observable.just(false)
    }
    private val lavkaSearchResultVoFormatter = mock<LavkaSearchResultVoFormatter>()
    private val view = mock<LavkaUpsaleView>()

    private val product = mock<LavkaSearchResultItem>()
    private val productVo = mock<LavkaSearchResultItemProductVo.ProductVo>()
    private val staticUpsaleVo = LavkaUpsaleVo.StaticUpsale(DUMMY_TITLE, listOf(productVo))
    private val dynamicUpsaleVo = LavkaUpsaleVo.DynamicUpsale(DUMMY_TITLE)


    @Test
    fun `should set title on attach`() {
        val presenter = createPresenter(staticUpsaleVo)
        presenter.attachView(view)
        verify(view).showTitle(DUMMY_TITLE)
    }

    @Test
    fun `should set static content when static vo`() {
        val presenter = createPresenter(staticUpsaleVo)
        presenter.attachView(view)
        verify(view).showUpsale(listOf(productVo))
    }

    @Test
    fun `should observe upsale and set result as content`() {
        whenever(lavkaUpsaleUseCases.observeUpsale()).thenReturn(Observable.just(listOf(product)))
        whenever(lavkaSearchResultVoFormatter.format(any(), any(), anyString(), any())).thenReturn(
            productVo
        )

        val presenter = createPresenter(dynamicUpsaleVo)
        presenter.attachView(view)
        verify(view).showUpsale(listOf(productVo))
    }

    @Test
    fun `should set content invisible when upsale is empty`() {
        val presenter = createPresenter(staticUpsaleVo)
        presenter.attachView(view)
        verify(view).showUpsale(listOf(productVo))
    }

    private fun createPresenter(vo: LavkaUpsaleVo) = LavkaUpsalePresenter(
        schedulers, vo, lavkaUpsaleUseCases, lavkaSearchResultVoFormatter
    )

    private companion object {
        const val DUMMY_TITLE = "TITLE"
    }
}
