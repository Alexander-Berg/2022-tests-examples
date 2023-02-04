package ru.auto.feature.appreview

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Test
import org.junit.runner.RunWith
 import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.router.Navigator
import ru.auto.data.prefs.MemoryReactivePrefsDelegate
import ru.auto.experiments.Experiments
import ru.auto.experiments.ExperimentsManager
import ru.auto.experiments.inAppReview
import ru.auto.feature.appreview.data.FakeReviewManagerFactory
import ru.auto.feature.appreview.data.ReviewInteractor
import ru.auto.feature.appreview.data.ReviewInteractor.Companion.SHOW_TIMES_COUNT
import ru.auto.feature.appreview.data.ReviewPrefRepository
import ru.auto.feature.appreview.router.command.ShowInAppReviewCommand
import ru.auto.feature.appreview.router.command.ShowRateDialogCommand
import ru.auto.test.core.RxTestAndroid

@RunWith(AllureRunner::class) class ReviewCoordinationDelegateTest : RxTestAndroid() {

    private val navigator: Navigator = mock()

    private val delegate = ReviewCoordinationDelegate(
        reviewInteractor = ReviewInteractor(
            reviewPrefRepository = ReviewPrefRepository(MemoryReactivePrefsDelegate())
        ),
        navigator = navigator,
        inAppReviewController = InAppReviewController(
            reviewManagerFactory = FakeReviewManagerFactory(),
            navigator = navigator
        ),
        sendFeedbackInteractor = mock(),
        strings = mock(),
        userRepository = mock(),
        rateAnalyst = mock(),
        rateDialogCoordinator = mock()
    )

    @Test
    fun `should open custom dialog after 10th card opened`() {
        setupExp(ReviewInteractor.ReviewOpenResult.CUSTOM_DIALOG)
        for (i in 0..100) {
            delegate.tryToShowReviewDialogFromOffer()
            val performCommandTimesCount = if (i < SHOW_TIMES_COUNT - 1) 0 else 1
            verify(navigator, times(performCommandTimesCount))
                .perform(argWhere { command -> command is ShowRateDialogCommand })
        }
    }

    @Test
    fun `should open in app review after 10th card opened`() {
        setupExp(ReviewInteractor.ReviewOpenResult.IN_APP_REVIEW)
        for (i in 0..100) {
            delegate.tryToShowReviewDialogFromOffer()
            val performCommandTimesCount = if (i < SHOW_TIMES_COUNT - 1) 0 else 1
            verify(navigator, times(performCommandTimesCount))
                .perform(argWhere { command -> command is ShowInAppReviewCommand })
        }
    }

    @Test
    fun `should not open any review after 10th card opened`() {
        setupExp(ReviewInteractor.ReviewOpenResult.NONE)
        for (i in 0..100) {
            delegate.tryToShowReviewDialogFromOffer()
            verify(navigator, never()).perform(any())
        }
    }

    private fun setupExp(reviewResult: ReviewInteractor.ReviewOpenResult) {
        val expManager: Experiments = mock()
        whenever(expManager.inAppReview()).thenReturn(reviewResult.abUserGroup)
        ExperimentsManager.setInstance(expManager)
    }
}
