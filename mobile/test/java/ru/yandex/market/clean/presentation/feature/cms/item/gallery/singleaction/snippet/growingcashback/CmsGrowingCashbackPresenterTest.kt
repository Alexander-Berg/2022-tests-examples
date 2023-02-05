package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.growingcashback

import io.reactivex.Completable
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.facades.GrowingCashbackAnalytics
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.GrowingCashbackTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.user.model.userProfileTestInstance
import ru.yandex.market.optional.Optional
import ru.yandex.market.presentationSchedulersMock

class CmsGrowingCashbackPresenterTest {

    private val userProfile = userProfileTestInstance()
    private val viewObject = mock<CmsGrowingCashbackVo>()
    private val router = mock<Router>()
    private val useCases = mock<CmsGrowingCashbackSnippetUseCases> {
        on { getCachedUserProfile() } doReturn Observable.just(Optional.of(userProfile))
        on { saveWasClosed() } doReturn Completable.complete()
    }
    private val analytics = mock<GrowingCashbackAnalytics>()
    private val view = mock<`CmsGrowingCashbackSnippetView$$State`>()
    private val presenter = CmsGrowingCashbackPresenter(
        presentationSchedulersMock(),
        viewObject,
        router,
        useCases,
        analytics
    )

    @Before
    fun setup() {
        presenter.attachView(view)
    }

    @Test
    fun `show vo on first attach`() {
        verify(view).showData(viewObject)
    }

    @Test
    fun `about button click navigate to about`() {
        presenter.onButtonClick(CmsGrowingCashbackVo.Button("", CmsGrowingCashbackVo.Action.ABOUT))
        verify(router).navigateTo(GrowingCashbackTargetScreen())
    }

    @Test
    fun `close button click save closed `() {
        presenter.onButtonClick(CmsGrowingCashbackVo.Button("", CmsGrowingCashbackVo.Action.CLOSE))
        verify(useCases).saveWasClosed()
    }

    @Test
    fun `send visible analytics on first attach`() {
        verify(analytics).cmsWidgetVisible(true, userProfile.hasYandexPlus)
    }

    @Test
    fun `send navigate analytics on about click`() {
        presenter.onButtonClick(CmsGrowingCashbackVo.Button("", CmsGrowingCashbackVo.Action.ABOUT))
        verify(analytics).cmsWidgetNavigate(true, userProfile.hasYandexPlus)
    }
}