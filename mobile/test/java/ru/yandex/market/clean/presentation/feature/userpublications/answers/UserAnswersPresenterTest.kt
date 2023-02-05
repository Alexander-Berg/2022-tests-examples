package ru.yandex.market.clean.presentation.feature.userpublications.answers

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
import ru.yandex.market.activity.model.SkuTargetScreen
import ru.yandex.market.analitycs.health.MetricaSender
import ru.yandex.market.analytics.facades.health.UserPublicationsHealthFacade
import ru.yandex.market.domain.questions.model.ProductAnswer
import ru.yandex.market.clean.presentation.feature.product.ProductFragment
import ru.yandex.market.clean.presentation.feature.question.remove.RemoveContentBottomSheetFragment
import ru.yandex.market.clean.presentation.feature.question.remove.RemoveContentBottomSheetTargetScreen
import ru.yandex.market.clean.presentation.feature.question.single.ProductQuestionArguments
import ru.yandex.market.clean.presentation.feature.question.single.ProductQuestionTargetScreen
import ru.yandex.market.clean.presentation.feature.question.vo.ProductQaFormatter
import ru.yandex.market.clean.presentation.feature.review.shortModelInfoVoTestInstance
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.paging.model.PageableResult
import ru.yandex.market.domain.questions.model.productAnswerTestInstance
import ru.yandex.market.presentationSchedulersMock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class UserAnswersPresenterTest {
    private val router = mock<Router>()

    @Suppress("DEPRECATION")
    private val metricaSender = mock<MetricaSender>()
    private val publicationsHealthFacade = mock<UserPublicationsHealthFacade>()
    private val useCases = mock<UserAnswersUseCases> {
        on { getUserAnswers(any(), any()) } doReturn Single.just(
            PageableResult
                .builder<ProductAnswer>()
                .currentPageIndex(0)
                .requestedItemsCount(productAnswers.size)
                .totalPagesCount(1)
                .totalItemsCount(productAnswers.size)
                .data(productAnswers)
                .build()
        )

        on { clearUserAnswers() } doReturn Completable.complete()
        on { likeAnswer(any(), any()) } doReturn Completable.complete()
        on { dislikeAnswer(any(), any()) } doReturn Completable.complete()
        on { removeAnswerVote(any(), any()) } doReturn Completable.complete()
        on { observeUserAnswers() } doReturn Observable.just(productAnswers)
    }

    private val emptyUseCases = mock<UserAnswersUseCases> {
        on { getUserAnswers(any(), any()) } doReturn Single.just(
            PageableResult
                .builder<ProductAnswer>()
                .currentPageIndex(0)
                .requestedItemsCount(0)
                .totalPagesCount(1)
                .totalItemsCount(0)
                .data(emptyList())
                .build()
        )

        on { clearUserAnswers() } doReturn Completable.complete()
        on { likeAnswer(any(), any()) } doReturn Completable.complete()
        on { dislikeAnswer(any(), any()) } doReturn Completable.complete()
        on { removeAnswerVote(any(), any()) } doReturn Completable.complete()
        on { observeUserAnswers() } doReturn Observable.just(productAnswers)
    }
    private val productQaFormatter = mock<ProductQaFormatter> {
        on { formatUserAnswer(any(), any(), any()) } doReturn userAnswerVoTestInstance()
    }

    private val schedulers = presentationSchedulersMock()

    private val view = mock<UserAnswersView>()


    @Test
    fun `empty answers showEmpty`() {
        val presenter = UserAnswersPresenter(
            schedulers, emptyUseCases, metricaSender, publicationsHealthFacade, productQaFormatter, router,
        )
        presenter.attachView(view)
        verify(view).showEmpty()
    }

    @Test
    fun `answers showContent`() {
        val presenter =
            UserAnswersPresenter(
                schedulers,
                useCases,
                metricaSender,
                publicationsHealthFacade,
                productQaFormatter,
                router
            )
        presenter.attachView(view)
        verify(view).showAnswers(userAnswers)
    }


    @Test
    fun `expand answer`() {
        val presenter =
            UserAnswersPresenter(
                schedulers,
                useCases,
                metricaSender,
                publicationsHealthFacade,
                productQaFormatter,
                router
            )
        presenter.attachView(view)
        presenter.expandAnswer(userAnswers[0].answerVo)
        verify(productQaFormatter, atLeast(1)).formatUserAnswer(
            productAnswer = productAnswers[0],
            isQuestionExpanded = false,
            isAnswerExpanded = true
        )
        verify(view, atLeast(1)).showAnswers(userAnswers)
    }

    @Test
    fun `expand question`() {
        val presenter =
            UserAnswersPresenter(
                schedulers,
                useCases,
                metricaSender,
                publicationsHealthFacade,
                productQaFormatter,
                router
            )
        presenter.attachView(view)
        presenter.expandQuestion(userAnswers[0])
        verify(productQaFormatter, atLeast(1)).formatUserAnswer(
            productAnswer = productAnswers[0],
            isQuestionExpanded = true,
            isAnswerExpanded = false
        )
        verify(view, atLeast(1)).showAnswers(userAnswers)
    }

    @Test
    fun `go to product`() {
        val presenter =
            UserAnswersPresenter(
                schedulers,
                useCases,
                metricaSender,
                publicationsHealthFacade,
                productQaFormatter,
                router
            )
        presenter.attachView(view)
        presenter.onProductClicked(shortModelInfoVo)
        verify(router).navigateTo(
            SkuTargetScreen(
                ProductFragment.Arguments(
                    productId = shortModelInfoVo.productId,
                    offerCpc = shortModelInfoVo.defaultOfferCpc
                )
            )
        )
    }

    @Test
    fun `delete answer`() {
        val presenter =
            UserAnswersPresenter(
                schedulers,
                useCases,
                metricaSender,
                publicationsHealthFacade,
                productQaFormatter,
                router
            )
        presenter.attachView(view)
        presenter.onAnswerDelete(userAnswers[0])
        val args = RemoveContentBottomSheetFragment.Arguments(
            content = RemoveContentBottomSheetFragment.Content.Answer(
                questionId = userAnswers[0].answerVo.questionId,
                answerId = userAnswers[0].answerVo.id
            )
        )
        verify(router).navigateTo(RemoveContentBottomSheetTargetScreen(args))
    }

    @Test
    fun `click comments`() {
        val presenter =
            UserAnswersPresenter(
                schedulers,
                useCases,
                metricaSender,
                publicationsHealthFacade,
                productQaFormatter,
                router
            )
        presenter.attachView(view)
        presenter.onCommentsClicked(userAnswers[0])

        val modelId = userAnswers[0].modelId
        val args = ProductQuestionArguments(
            questionId = userAnswers[0].answerVo.questionId,
            modelId = modelId.toString(),
            skuId = null,
            answerIdToOpen = userAnswers[0].answerVo.id
        )
        verify(router).navigateTo(ProductQuestionTargetScreen(args))
    }

    @Test
    fun `like answer`() {
        val presenter =
            UserAnswersPresenter(
                schedulers,
                useCases,
                metricaSender,
                publicationsHealthFacade,
                productQaFormatter,
                router
            )
        presenter.attachView(view)
        presenter.onLikeAnswer(userAnswers[0].answerVo.copy(userLiked = false))
        verify(useCases).likeAnswer(userAnswers[0].answerVo.questionId, userAnswers[0].answerVo.id)
        presenter.onLikeAnswer(userAnswers[0].answerVo.copy(userLiked = true))
        verify(useCases).removeAnswerVote(userAnswers[0].answerVo.questionId, userAnswers[0].answerVo.id)
    }


    @Test
    fun `dislike answer`() {
        val presenter =
            UserAnswersPresenter(
                schedulers,
                useCases,
                metricaSender,
                publicationsHealthFacade,
                productQaFormatter,
                router
            )
        presenter.attachView(view)
        presenter.onDislikeAnswer(userAnswers[0].answerVo.copy(userDisliked = false))
        verify(useCases).dislikeAnswer(userAnswers[0].answerVo.questionId, userAnswers[0].answerVo.id)
        presenter.onDislikeAnswer(userAnswers[0].answerVo.copy(userDisliked = true))
        verify(useCases).removeAnswerVote(userAnswers[0].answerVo.questionId, userAnswers[0].answerVo.id)
    }

    companion object {
        private val productAnswers = listOf(productAnswerTestInstance(), productAnswerTestInstance())
        private val userAnswers = listOf(userAnswerVoTestInstance(), userAnswerVoTestInstance())
        private val shortModelInfoVo = shortModelInfoVoTestInstance()
    }
}