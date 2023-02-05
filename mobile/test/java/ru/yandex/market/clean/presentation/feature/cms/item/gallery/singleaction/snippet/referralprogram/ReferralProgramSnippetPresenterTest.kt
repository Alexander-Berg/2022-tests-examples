package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.referralprogram

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.facades.ReferralProgramAnalytics
import ru.yandex.market.clean.domain.model.referralprogram.ReferralProgramUserDescription
import ru.yandex.market.clean.presentation.feature.referralprogram.ReferralProgramTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.media.model.measuredImageReferenceTestInstance
import ru.yandex.market.presentationSchedulersMock

class ReferralProgramSnippetPresenterTest {

    private val viewObject = CmsReferralProgramVo(
        title = "title",
        subtitle = "subtitle",
        primaryButtonLabel = "primaryButtonLabel",
        secondaryButtonLabel = "secondaryButtonLabel",
        image = measuredImageReferenceTestInstance()
    )
    private val userDescription = mock<ReferralProgramUserDescription>()
    private val router = mock<Router>()
    private val viewState = mock<`ReferralProgramSnippetView$$State`>()
    private val useCases = mock<ReferralProgramSnippetUseCases>() {
        on { setReferralProgramWidgetWasClosed() } doReturn Completable.complete()
        on { getReferralProgramUserDescription() } doReturn Single.just(userDescription)
    }
    private val analytics = mock<ReferralProgramAnalytics>()
    private val presenter = ReferralProgramSnippetPresenter(
        presentationSchedulersMock(),
        viewObject,
        router,
        useCases,
        analytics
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
    fun `Navigate to referral program onboarding on primary button click`() {
        presenter.onPrimaryButtonClick()

        verify(router).navigateTo(ReferralProgramTargetScreen())
    }

    @Test
    fun `set close widget clicked on secondary button click`() {
        presenter.onSecondaryButtonClick()

        verify(useCases).setReferralProgramWidgetWasClosed()
    }

    @Test
    fun `send visible analytic on attach`() {
        presenter.attachView(viewState)

        verify(analytics).widgetVisible(userDescription)
    }

    @Test
    fun `send navigate analytic when primary button click`() {
        presenter.attachView(viewState)
        presenter.onPrimaryButtonClick()

        verify(analytics).widgetNavigate(userDescription, viewObject.primaryButtonLabel)
    }

    @Test
    fun `send navigate analytic when secondary button click`() {
        presenter.attachView(viewState)
        presenter.onSecondaryButtonClick()

        verify(analytics).widgetNavigate(userDescription, viewObject.secondaryButtonLabel)
    }
}