package ru.yandex.market.clean.presentation.feature.userpublications.questions

import android.os.Build
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.analitycs.AnalyticsService
import ru.yandex.market.analitycs.events.question.QuestionAnalyticsEvent
import ru.yandex.market.analitycs.health.MetricaSender
import ru.yandex.market.analytics.facades.health.UserPublicationsHealthFacade
import ru.yandex.market.domain.questions.model.ProductQuestion
import ru.yandex.market.clean.presentation.feature.question.single.ProductQuestionArguments
import ru.yandex.market.clean.presentation.feature.question.single.ProductQuestionTargetScreen
import ru.yandex.market.clean.presentation.feature.question.vo.ProductQaFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.paging.model.PageableResult
import ru.yandex.market.domain.questions.model.productQuestionTestInstance
import ru.yandex.market.presentationSchedulersMock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class UserQuestionsPresenterTest {

    private val router = mock<Router>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<AnalyticsService>()
    private val publicationsHealthFacade = mock<UserPublicationsHealthFacade>()
    private val metricaSender = mock<MetricaSender>()
    private val useCases = mock<UserQuestionsUseCases> {
        on { getUserQuestions(any(), any()) } doReturn Single.just(
            PageableResult
                .builder<ProductQuestion>()
                .currentPageIndex(0)
                .requestedItemsCount(productQuestions.size)
                .totalPagesCount(1)
                .totalItemsCount(productQuestions.size)
                .data(productQuestions)
                .build()
        )

        on { clearUserQuestions() } doReturn Completable.complete()
        on { likeQuestion(any()) } doReturn Completable.complete()
        on { removeQuestionVote(any()) } doReturn Completable.complete()
        on { observeUserQuestions() } doReturn Observable.just(productQuestions)
    }

    private val emptyUseCases = mock<UserQuestionsUseCases> {
        on { getUserQuestions(any(), any()) } doReturn Single.just(
            PageableResult
                .builder<ProductQuestion>()
                .currentPageIndex(0)
                .requestedItemsCount(0)
                .totalPagesCount(1)
                .totalItemsCount(0)
                .data(emptyList())
                .build()
        )

        on { clearUserQuestions() } doReturn Completable.complete()
        on { likeQuestion(any()) } doReturn Completable.complete()
        on { removeQuestionVote(any()) } doReturn Completable.complete()
        on { observeUserQuestions() } doReturn Observable.just(productQuestions)
    }
    private val productQaFormatter = mock<ProductQaFormatter> {
        on { formatUserQuestion(any(), any()) } doReturn userQuestionVoTestInstance()
    }

    private val schedulers = presentationSchedulersMock()

    private val view = mock<UserQuestionsView>()

    @Test
    fun `empty questions showEmpty`() {
        val presenter = UserQuestionsPresenter(
            schedulers, router, emptyUseCases, analyticsService, publicationsHealthFacade, metricaSender,
            productQaFormatter
        )
        presenter.attachView(view)
        verify(view, atLeast(1)).showEmpty()
    }

    @Test
    fun `questions showContent`() {
        val presenter = UserQuestionsPresenter(
            schedulers, router, useCases, analyticsService, publicationsHealthFacade, metricaSender,
            productQaFormatter
        )
        presenter.attachView(view)
        verify(view).showUserQuestions(userQuestions)
    }

    @Test
    fun `view questions comments`() {
        val presenter = UserQuestionsPresenter(
            schedulers, router, useCases, analyticsService, publicationsHealthFacade, metricaSender,
            productQaFormatter
        )
        presenter.attachView(view)
        presenter.onShowAnswersClick(userQuestions[0])
        val args = ProductQuestionArguments(
            questionId = userQuestions[0].questionVo.id,
            modelId = userQuestions[0].modelId.toString(),
            skuId = null
        )

        val event = QuestionAnalyticsEvent(
            skuId = null,
            modelId = userQuestions[0].modelId.toString(),
            questionId = userQuestions[0].questionVo.id,
            type = QuestionAnalyticsEvent.Type.ANSWERS_NAVIGATE
        )

        verify(analyticsService).report(
            event
        )

        verify(router).navigateTo(
            ProductQuestionTargetScreen(args)
        )
    }

    companion object {
        private val productQuestions = listOf(productQuestionTestInstance(), productQuestionTestInstance())
        private val userQuestions = listOf(userQuestionVoTestInstance(), userQuestionVoTestInstance())
    }
}