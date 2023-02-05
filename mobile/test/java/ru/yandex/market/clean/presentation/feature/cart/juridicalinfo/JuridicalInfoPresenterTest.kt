package ru.yandex.market.clean.presentation.feature.cart.juridicalinfo

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.model.lavka2.LavkaJuridicalInfo
import ru.yandex.market.clean.domain.model.lavka2.LavkaServiceInfo
import ru.yandex.market.clean.presentation.feature.cart.CartType
import ru.yandex.market.clean.presentation.feature.cart.vo.MulticartJuridicalInfoVo
import ru.yandex.market.optional.Optional
import ru.yandex.market.presentationSchedulersMock

class JuridicalInfoPresenterTest {

    private val schedulers = presentationSchedulersMock()
    private val useCases = mock<JuridicalInfoUseCases>()
    private val juridicalInfo = mock<LavkaJuridicalInfo> {
        on { address } doReturn "address"
        on { partner } doReturn "partner"
    }
    private val lavkaJuridicalInfoVo = mock<MulticartJuridicalInfoVo> {
        on { cartType } doReturn CartType.Lavka
    }
    private val edaJuridicalInfoVo = mock<MulticartJuridicalInfoVo> {
        on { cartType } doReturn CartType.Retail("", "")
    }
    private val serviceInfo = mock<LavkaServiceInfo> {
        on { juridicalInfo } doReturn juridicalInfo
    }
    private val view = mock<JuridicalInfoView>()

    private val presenter = JuridicalInfoPresenter(
        schedulers, useCases
    )

    @Test
    fun `should show dialog if lavka juridical info exists`() {
        whenever(useCases.getLavkaServiceInfo()).thenReturn(Single.just(Optional.of(serviceInfo)))
        presenter.attachView(view)
        presenter.onJuridicalInfoClicked(lavkaJuridicalInfoVo)
        verify(view).showLavkaJuridicalDialog(any(), any())
        verify(view, never()).showRetailJuridicalDialog(any())
    }

    @Test
    fun `should do nothing if lavka juridical info not exists`() {
        whenever(useCases.getLavkaServiceInfo()).thenReturn(Single.just(Optional.empty()))
        presenter.attachView(view)
        presenter.onJuridicalInfoClicked(lavkaJuridicalInfoVo)
        verify(view, never()).showLavkaJuridicalDialog(any(), any())
    }

    @Test
    fun `should show dialog if eda juridical info exists`() {
        presenter.attachView(view)
        presenter.onJuridicalInfoClicked(edaJuridicalInfoVo)
        verify(view).showRetailJuridicalDialog(any())
        verify(view, never()).showLavkaJuridicalDialog(any(), any())
    }
}