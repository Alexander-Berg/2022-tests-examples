package ru.auto.feature.onboarding

import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock
import ru.auto.ara.RxTest
import ru.auto.core_logic.Analyst
import ru.auto.core_logic.router.listener.buildChooseListener
import ru.auto.data.prefs.MemoryReactivePrefsDelegate
import ru.auto.data.repository.IAppInfoRepository
import ru.auto.feature.onboarding.data.OnboardingAction
import ru.auto.feature.onboarding.data.OnboardingFinalAction
import ru.auto.feature.onboarding.data.repository.IOnboardingShowingRepository
import ru.auto.feature.onboarding.data.repository.OnboardingShowingRepository

@RunWith(AllureParametrizedRunner::class)
class OnboardingFeatureTest(private val param: TestParam) : RxTest() {

    private val prefsMock = MemoryReactivePrefsDelegate()
    private val onboardingRepositoryMock: IOnboardingShowingRepository = OnboardingShowingRepository(prefsMock)
    private val appInfoRepositoryMock = mock<IAppInfoRepository>()

    private val feature by lazy {
        OnboardingProvider(
            args = IOnboardingProvider.Args(buildChooseListener { }),
            dependencies = object : OnboardingProvider.Dependencies {
                override val onboardingShowingRepository: IOnboardingShowingRepository = onboardingRepositoryMock
                override val appInfoRepository: IAppInfoRepository = appInfoRepositoryMock
            }
        ).feature
    }

    @Before
    fun setup() {
        Analyst.init(mock())
        feature.run {
            param.selectedAction?.let {
                feature.accept(Onboarding.Msg.ClickedAction(it))
            }
            accept(param.msg)
        }
    }

    @Test
    fun testMessage() {
        when (param.msg) {
            is Onboarding.Msg.FirstSlideAppearShown -> shouldSaveShownEventToPrefs()
            is Onboarding.Msg.ClickedAction -> shouldSaveSelectedActionToPerfs()
            is Onboarding.Msg.ClickedSlideBottomButton -> shouldSaveSelectedFinalActionToPrefs()
        }
    }

    private fun shouldSaveShownEventToPrefs() =
        onboardingRepositoryMock.hasOnboardingShown()
            .test()
            .assertValue(true)

    private fun shouldSaveSelectedActionToPerfs() =
        onboardingRepositoryMock.getSelectedAction()
            .test()
            .assertValue((param.msg as Onboarding.Msg.ClickedAction).action)

    private fun shouldSaveSelectedFinalActionToPrefs() {
        val position = (param.msg as Onboarding.Msg.ClickedSlideBottomButton).pos
        val finalAction = getFinalActionForPosition(position, param.selectedAction)
        onboardingRepositoryMock.getSelectedFinalAction()
            .test()
            .assertValue(finalAction)
    }

    private fun getFinalActionForPosition(position: Int, action: OnboardingAction?): OnboardingFinalAction =
        when {
            action == OnboardingAction.WANT_TO_SELL && position == 3 -> OnboardingFinalAction.CREATE_OFFER
            action == OnboardingAction.WANT_TO_BUY && position == 2 -> OnboardingFinalAction.WATCH_BETS
            action == OnboardingAction.WANT_TO_BUY && position == 3 -> OnboardingFinalAction.LETS_GO
            action == OnboardingAction.SEARCH_FOR_SOMETHING && position == 3 -> OnboardingFinalAction.TO_GARAGE
            else -> error("No final action found for action $action")
        }


    companion object {
        data class TestParam(
            val msg: Onboarding.Msg,
            val selectedAction: OnboardingAction? = null,
        )

        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data(): Collection<TestParam> = listOf(
            TestParam(Onboarding.Msg.FirstSlideAppearShown),
            TestParam(Onboarding.Msg.ClickedAction(OnboardingAction.WANT_TO_SELL)),
            TestParam(Onboarding.Msg.ClickedAction(OnboardingAction.WANT_TO_BUY)),
            TestParam(Onboarding.Msg.ClickedAction(OnboardingAction.SEARCH_FOR_SOMETHING)),
            TestParam(Onboarding.Msg.ClickedSlideBottomButton(3), OnboardingAction.WANT_TO_SELL),
            TestParam(Onboarding.Msg.ClickedSlideBottomButton(2), OnboardingAction.WANT_TO_BUY),
            TestParam(Onboarding.Msg.ClickedSlideBottomButton(3), OnboardingAction.WANT_TO_BUY),
            TestParam(Onboarding.Msg.ClickedSlideBottomButton(3), OnboardingAction.SEARCH_FOR_SOMETHING),
        )
    }
}
