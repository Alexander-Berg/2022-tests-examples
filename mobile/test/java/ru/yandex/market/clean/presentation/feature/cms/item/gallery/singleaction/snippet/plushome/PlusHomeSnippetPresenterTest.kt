package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.plushome

import io.reactivex.Completable
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.domain.user.model.userProfileTestInstance
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeArguments
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeFlowAnalyticsInfo
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.optional.Optional
import ru.yandex.market.presentationSchedulersMock

class PlusHomeSnippetPresenterTest {

    private val plusHomeVo = cmsPlusHomeNavigationVoTestInstance()
    private val userProfile = userProfileTestInstance()
    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.HOME
    }
    private val useCases = mock<PlusHomeSnippetUseCases> {
        on { saveWasInteracted(plusHomeVo.tag) } doReturn Completable.complete()
        on { getCurrentUser() } doReturn Observable.just(Optional.of(userProfile))
    }
    private val analytics = mock<PlusHomeWidgetAnalytics>()
    private val viewState = mock<`PlusHomeSnippetView$$State`>()
    private val presenter = PlusHomeSnippetPresenter(
        presentationSchedulersMock(),
        plusHomeVo,
        router,
        useCases,
        analytics
    )

    @Before
    fun setup() {
        presenter.attachView(viewState)
    }

    @Test
    fun `show vo on first attach`() {
        verify(viewState).showData(plusHomeVo)
    }

    @Test
    fun `navigate to plus home on click`() {
        presenter.onButtonClick()
        verify(router).navigateTo(
            PlusHomeTargetScreen(
                PlusHomeArguments(PlusHomeFlowAnalyticsInfo(Screen.HOME.toString()))
            )
        )
    }

    @Test
    fun `save widget was interacted on click`() {
        presenter.onButtonClick()
        verify(useCases).saveWasInteracted(plusHomeVo.tag)
    }

    @Test
    fun `send visible event on widget visible`() {
        presenter.attachView(viewState)
        verify(analytics).widgetVisible(true, userProfile.hasYandexPlus, plusHomeVo.tag)
    }

    @Test
    fun `send navigate event on widget navigate`() {
        presenter.onButtonClick()
        verify(analytics).widgetNavigate(true, userProfile.hasYandexPlus, plusHomeVo.tag)
    }
}