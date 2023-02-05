package ru.yandex.market.clean.presentation.feature.userpublications.reviews

import android.os.Build
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.analitycs.health.MetricaSender
import ru.yandex.market.analytics.facades.ReviewPhotosAnalytics
import ru.yandex.market.analytics.facades.health.UserPublicationsHealthFacade
import ru.yandex.market.clean.data.mapper.UserReviewMapper
import ru.yandex.market.clean.domain.model.review.ProductReview
import ru.yandex.market.clean.domain.model.review.productReviewTestInstance
import ru.yandex.market.clean.presentation.feature.avatar.avatarImageSourceTestInstance
import ru.yandex.market.clean.presentation.feature.review.ProductReviewFormatter
import ru.yandex.market.clean.presentation.feature.review.ProductReviewVo
import ru.yandex.market.clean.presentation.feature.review.comments.ReviewCommentsArguments
import ru.yandex.market.clean.presentation.feature.review.comments.ReviewCommentsScroll
import ru.yandex.market.clean.presentation.feature.review.comments.ReviewCommentsSource
import ru.yandex.market.clean.presentation.feature.review.comments.ReviewCommentsTargetScreen
import ru.yandex.market.clean.presentation.feature.review.create.text.reviewPhotoVo_RemoteTestInstance
import ru.yandex.market.clean.presentation.feature.review.shortModelInfoVoTestInstance
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.paging.model.PageableResult
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.utils.EquableCharSequence

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class UserReviewsPresenterTest : TestCase() {
    private val router = mock<Router>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val publicationsHealthFacade = mock<UserPublicationsHealthFacade>()
    private val metricaSender = mock<MetricaSender>()
    private val useCases = mock<UserReviewsUseCases> {
        on { getReviews(any(), any()) } doReturn Single.just(
            PageableResult
                .builder<ProductReview>()
                .currentPageIndex(0)
                .requestedItemsCount(userReviews.size)
                .totalPagesCount(1)
                .totalItemsCount(userReviews.size)
                .data(userReviews)
                .build()
        )

        on { clearUserReviews() } doReturn Completable.complete()
        on { observeUserReviews() } doReturn Observable.just(userReviews)
        on { deleteUserReview(any()) } doReturn Completable.complete()
        on { checkHasOrders() } doReturn Single.just(true)
    }

    private val emptyUseCases = mock<UserReviewsUseCases> {
        on { getReviews(any(), any()) } doReturn Single.just(
            PageableResult
                .builder<ProductReview>()
                .currentPageIndex(0)
                .requestedItemsCount(0)
                .totalPagesCount(1)
                .totalItemsCount(0)
                .data(emptyList())
                .build()
        )

        on { clearUserReviews() } doReturn Completable.complete()
        on { observeUserReviews() } doReturn Observable.just(emptyList())
        on { checkHasOrders() } doReturn Single.just(true)
    }

    private val productReviewFormatter = mock<ProductReviewFormatter> {
        on { formatUserReview(any(), any()) } doReturn generateUserReviewVoTestInstance()
    }

    private val schedulers = presentationSchedulersMock()

    private val view = mock<UserReviewsView>()

    private val reviewPhotosAnalytics = mock<ReviewPhotosAnalytics>()

    private val userReviewMapper = mock<UserReviewMapper>()

    @Test
    fun `empty reviews showEmpty`() {
        val presenter = UserReviewsPresenter(
            schedulers, router, emptyUseCases, productReviewFormatter, publicationsHealthFacade, metricaSender,
            reviewPhotosAnalytics, userReviewMapper
        )
        presenter.attachView(view)
        verify(view, atLeast(1)).showEmpty(true)
    }

    @Test
    fun `reviews showContent`() {
        val presenter = UserReviewsPresenter(
            schedulers, router, useCases, productReviewFormatter, publicationsHealthFacade, metricaSender,
            reviewPhotosAnalytics, userReviewMapper
        )
        presenter.attachView(view)
        verify(view).showReviews(userReviewVos)
    }

    @Test
    fun `view review comments`() {
        val presenter = UserReviewsPresenter(
            schedulers, router, useCases, productReviewFormatter, publicationsHealthFacade, metricaSender,
            reviewPhotosAnalytics, userReviewMapper
        )
        presenter.attachView(view)
        val modelName = userReviewVos[0].productVo?.modelName ?: ""
        presenter.onShowAllCommentsClicked(userReviewVos[0].modelId.toString(), modelName, userReviewVos[0].reviewVo.id)
        verify(router).navigateTo(
            ReviewCommentsTargetScreen(
                ReviewCommentsArguments(
                    userReviewVos[0].modelId.toString(),
                    userReviewVos[0].reviewVo.id,
                    null,
                    modelName,
                    commentToScroll = ReviewCommentsScroll.FirstComment,
                    source = ReviewCommentsSource.USER_REVIEWS,
                )
            )
        )
    }

    companion object {
        private val userReviews = listOf(productReviewTestInstance(), productReviewTestInstance())
        private val userReviewVos = listOf(generateUserReviewVoTestInstance(), generateUserReviewVoTestInstance())

        private fun generateUserReviewVoTestInstance(): UserReviewVo {
            return UserReviewVo(
                isPending = false,
                isRejected = false,
                modelId = 0L,
                userReviewInfoVo = userReviewInfoVoTestInstance(),
                reviewVo = ProductReviewVo(
                    id = "id",
                    authorName = "authorName",
                    displayAuthorName = "displayAuthorName",
                    photos = listOf(reviewPhotoVo_RemoteTestInstance(), reviewPhotoVo_RemoteTestInstance()),
                    avatar = avatarImageSourceTestInstance(),
                    usagePeriod = EquableCharSequence("usagePeriod", "usagePeriod"),
                    starsCount = 5,
                    productGrade = "productGrade",
                    isVerified = true,
                    showAllCommentsButton = true,
                    commentsCount = 2,
                    showDatePlaceInfo = true,
                    date = "date",
                    isTextExpanded = true,
                    fullReviewText = EquableCharSequence("fulltext", "fulltext"),
                    userLiked = false,
                    userDisliked = false,
                    likeCount = 0,
                    dislikeCount = 0
                ),
                productVo = shortModelInfoVoTestInstance()
            )
        }
    }
}