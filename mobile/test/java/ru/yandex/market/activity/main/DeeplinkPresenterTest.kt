package ru.yandex.market.activity.main

import android.net.Uri
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.DeeplinkAnalytics
import ru.yandex.market.base.presentation.core.schedule.PresentationSchedulers
import ru.yandex.market.clean.presentation.navigation.Router

class DeeplinkPresenterTest {

    private val router = mock<Router>()

    private val schedulers = mock<PresentationSchedulers> {
        on { main } doReturn Schedulers.trampoline()
    }

    private val useCases = mock<DeeplinkUseCases>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<DeeplinkAnalytics>()

    private val deeplinkViewState = mock<`DeeplinkView$$State`>()

    private val presenter = DeeplinkPresenter(
        router = router,
        schedulers = schedulers,
        useCases = useCases,
        deeplinkAnalytics = analyticsService
    ).apply {
        setViewState(deeplinkViewState)
    }

    private val view = mock<DeeplinkView>()

    @Test
    fun `Presenter sets partner ids on new deeplink uri`() {
        val uri = mock<Uri>()
        whenever(useCases.applyPartnerIds(uri)) doReturn Completable.complete()
        whenever(useCases.applyAdjustParams(uri)) doReturn Completable.complete()

        presenter.attachView(view)
        presenter.onNewDeeplinkUri(uri, false)

        verify(useCases, times(1)).applyPartnerIds(uri)
        verify(useCases, times(1)).applyAdjustParams(uri)
    }

    @Test
    fun `Presenter notifies analytics service about handled deeplink`() {
        val uri = mock<Uri>()
        whenever(useCases.applyPartnerIds(uri)) doReturn Completable.complete()
        whenever(useCases.applyAdjustParams(uri)) doReturn Completable.complete()

        presenter.attachView(view)
        presenter.onNewDeeplinkUri(uri, false)

        verify(analyticsService, times(1)).sendEventHandleDeeplink(DeeplinkAnalytics.HandleAnalyticData(uri))
    }

    @Test
    fun `Presenter sets referral params from uir`() {
        val uri = mock<Uri>()

        whenever(useCases.setReferralParamsFromUri(uri)) doReturn Completable.complete()

        presenter.attachView(view)
        presenter.setReferralParams(uri)

        verify(useCases, times(1)).setReferralParamsFromUri(uri)
    }

    @Test
    fun `Presenter checks if referral params are set - true`() {
        whenever(useCases.checkIfReferralParamsAreSet()) doReturn Single.fromCallable { true }

        presenter.attachView(view)
        presenter.checkReferralParams()

        verify(useCases, times(1)).checkIfReferralParamsAreSet()
        verify(deeplinkViewState, never()).getReferralInfo()
    }

    @Test
    fun `Presenter checks if referral params are set - false`() {
        whenever(useCases.checkIfReferralParamsAreSet()) doReturn Single.fromCallable { false }

        presenter.attachView(view)
        presenter.checkReferralParams()

        verify(useCases, times(1)).checkIfReferralParamsAreSet()
        verify(deeplinkViewState, times(1)).getReferralInfo()
    }
}
