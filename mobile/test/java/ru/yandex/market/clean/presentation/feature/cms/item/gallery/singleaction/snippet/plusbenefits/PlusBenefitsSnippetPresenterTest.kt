package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.plusbenefits

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.presentation.feature.cms.model.PlusBenefitsWidgetCmsVo
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeArguments
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeFlowAnalyticsInfo
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeOnboardingRequest
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.presentationSchedulersMock

class PlusBenefitsSnippetPresenterTest {

    private val viewObject = PlusBenefitsWidgetCmsVo(
        title = "title",
        subtitle = "subtitle",
        buttonText = "button text"
    )
    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.HOME
    }
    private val viewState = mock<`PlusBenefitsSnippetView$$State`>()

    private val presenter = PlusBenefitsSnippetPresenter(
        presentationSchedulersMock(),
        viewObject,
        router
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
    fun `Navigate to plus home screen on button click`() {
        presenter.onButtonClick()

        val expectedTarget = PlusHomeTargetScreen(
            PlusHomeArguments(
                analyticsInfo = PlusHomeFlowAnalyticsInfo((router.currentScreen.toString())),
                plusHomeOnboardingRequest = PlusHomeOnboardingRequest.BENEFITS
            )
        )

        verify(router).navigateTo(expectedTarget)
    }
}