package ru.yandex.market.clean.presentation.feature.uservideo.single

import io.reactivex.Observable
import io.reactivex.Single
import junit.framework.TestCase
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import ru.yandex.market.domain.user.video.model.UgcVideoSource
import ru.yandex.market.clean.presentation.feature.question.comment.add.AddCommentTargetScreen
import ru.yandex.market.clean.presentation.feature.question.vo.ProductCommentFormatter
import ru.yandex.market.clean.presentation.feature.question.vo.ProductQaEventFormatter
import ru.yandex.market.clean.presentation.feature.question.vo.productCommentDataVoTestInstance
import ru.yandex.market.clean.presentation.feature.question.vo.productCommentVo_CommentTestInstance
import ru.yandex.market.clean.presentation.feature.review.photos.gallery.ugcVideoVoTestInstance
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.ugc.model.commentaryTestInstance
import ru.yandex.market.domain.user.video.model.ugcVideoTestInstance
import ru.yandex.market.feature.addcomment.ui.AddCommentArguments
import ru.yandex.market.presentationSchedulersMock

class ProductVideoPresenterTest : TestCase() {

    private val useCases = mock<ProductVideoUseCases> {
        on { observeVideoEvents() } doReturn Observable.empty()
        on { getCachedProductVideo(MOCK_MODEL_ID, MOCK_VIDEO_ID, SOURCE) } doReturn Single.just(MOCK_VIDEO)
        on { observeProductVideoComments(MOCK_VIDEO_ID) } doReturn Observable.just(MOCK_COMMENTS)
    }

    private val router = mock<Router>()

    private val schedulers = presentationSchedulersMock()

    private val commentFormatter = mock<ProductCommentFormatter> {
        on { formatCommentList(any(), any()) } doReturn MOCK_COMMENTS_VO
    }
    private val formatter = mock<UgcVideoFormatter> {
        on { formatVideo(MOCK_VIDEO) } doReturn MOCK_VIDEO_VO
    }

    private val qaEventFormatter = mock<ProductQaEventFormatter>()

    private val args = ProductVideoArguments(
        id = MOCK_VIDEO_ID,
        modelId = MOCK_MODEL_ID,
        modelName = MOCK_MODEL_NAME,
        skuId = MOCK_SKU_ID,
        source = SOURCE
    )

    private val presenter =
        ProductVideoPresenter(schedulers, router, useCases, args, formatter, qaEventFormatter, commentFormatter)

    private val view = mock<ProductVideoView>()

    @Test
    fun `test initial loading`() {
        presenter.attachView(view)
        verify(useCases).observeVideoEvents()
        verify(useCases).getCachedProductVideo(MOCK_MODEL_ID, MOCK_VIDEO_ID, SOURCE)
        verify(useCases).observeProductVideoComments(MOCK_VIDEO_ID)
    }

    @Test
    fun `test add comment show input`() {
        presenter.addComment()
        verify(router).navigateTo(
            AddCommentTargetScreen(
                AddCommentArguments(
                    target = AddCommentArguments.Target.VideoComment(
                        videoId = MOCK_VIDEO_ID,
                        parentCommentId = null
                    ), replyToName = null, modelId = MOCK_MODEL_ID, skuId = MOCK_SKU_ID
                )
            )
        )
    }

    @Test
    fun `test refresh screen after comment expanding`() {
        presenter.attachView(view)
        presenter.expandComment(PARENT_COMMENT_ID)
        verify(view, times(2)).showContent(MOCK_VIDEO_VO, MOCK_COMMENTS_VO, emptySet())
    }

    @Test
    fun `test refresh screen after comment childs showing`() {
        presenter.attachView(view)
        presenter.switchCommentAnswers(PARENT_COMMENT_ID)
        verify(view, times(2)).showContent(MOCK_VIDEO_VO, MOCK_COMMENTS_VO, setOf(PARENT_COMMENT_ID))
    }


    companion object {
        private const val MOCK_VIDEO_ID = "video_id"
        private const val MOCK_MODEL_ID = "123"
        private const val MOCK_MODEL_NAME = "model_name"
        private const val MOCK_SKU_ID = "12345"
        private const val PARENT_COMMENT_ID = 1L
        private const val CHILD_COMMENT_ID = 2L
        private val SOURCE = UgcVideoSource.GALLERY
        private val MOCK_VIDEO = ugcVideoTestInstance()
        private val MOCK_COMMENTS = listOf(commentaryTestInstance())
        private val MOCK_VIDEO_VO = ugcVideoVoTestInstance()
        private val MOCK_COMMENTS_VO = listOf(
            productCommentVo_CommentTestInstance(
                parentId = null,
                info = productCommentDataVoTestInstance(id = PARENT_COMMENT_ID)
            ),
            productCommentVo_CommentTestInstance(
                info = productCommentDataVoTestInstance(id = CHILD_COMMENT_ID), parentId = PARENT_COMMENT_ID
            )
        )
    }
}