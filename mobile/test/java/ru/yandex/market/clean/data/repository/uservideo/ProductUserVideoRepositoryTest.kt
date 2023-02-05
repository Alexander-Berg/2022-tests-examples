package ru.yandex.market.clean.data.repository.uservideo

import android.net.Uri
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import junit.framework.TestCase
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.fapi.dto.white.whiteFrontApiMergedCommentaryDtoTestInstance
import ru.yandex.market.clean.data.fapi.source.review.video.UserVideoFapiClient
import ru.yandex.market.clean.data.mapper.CommentMapper
import ru.yandex.market.clean.data.mapper.ProductUgcVideoMapper
import ru.yandex.market.clean.data.mapper.uservideo.UserVideoMapper
import ru.yandex.market.clean.data.model.dto.uservideo.uploadVideoUrlDtoTestInstance
import ru.yandex.market.clean.data.repository.AuthRepositoryImpl
import ru.yandex.market.clean.domain.model.uservideo.uploadVideoInfoTestInstance
import ru.yandex.market.domain.ugc.model.commentaryTestInstance
import ru.yandex.market.internal.pageable.PageableMapper

class ProductUserVideoRepositoryTest : TestCase() {

    private val uri = mock<Uri>()

    private val userVideoCacheDataStore: UserVideoCacheDataStore = mock() {
        on { getCommentsForVideo(MOCK_VIDEO_ID) } doReturn Maybe.empty()
        on { observeCommentsForVideoChanges(MOCK_VIDEO_ID) } doReturn Observable.empty()
        on { setComments(any(), any()) } doReturn Completable.complete()
    }
    private val userVideoMapper: UserVideoMapper = mock() {
        on { mapUploadVideoInfo(any()) } doReturn uploadVideoInfoTestInstance(videoId = MOCK_VIDEO_ID)
    }
    private val tusRepository: VideoTusRepository = mock() {
        on { uploadTus(any(), any()) } doReturn Single.just(true)
    }
    private val commentMapper: CommentMapper = mock() {
        on { map(any()) } doReturn commentaryTestInstance()
    }
    private val userProductVideoMapper = mock<ProductUgcVideoMapper>()
    private val authenticationRepository = mock<AuthRepositoryImpl>()
    private val pagerMapper = mock<PageableMapper>()

    fun testVideoCommentLoaded() {
        val userVideoFapiClient: UserVideoFapiClient = mock() {
            on { resolveVideoComments(MOCK_VIDEO_ID, 1) } doReturn Single.just(
                Pair(
                    listOf(
                        whiteFrontApiMergedCommentaryDtoTestInstance()
                    ),
                    null
                )
            )
            on { resolveVideoComments(MOCK_VIDEO_ID, null) } doReturn Single.just(
                Pair(
                    listOf(
                        whiteFrontApiMergedCommentaryDtoTestInstance()
                    ), 1
                )
            )
        }
        val productUserVideoRepository = ProductUserVideoRepositoryImpl(
            userVideoFapiClient,
            userVideoCacheDataStore,
            userVideoMapper,
            userProductVideoMapper,
            tusRepository,
            commentMapper,
            pagerMapper,
            authenticationRepository,
        )
        val list = productUserVideoRepository.resolveVideoComments(MOCK_VIDEO_ID).blockingLast()
        assertEquals(2, list.size)
    }

    fun `test resolve videoId`() {
        val userVideoFapiClient: UserVideoFapiClient = mock() {
            on { resolveVideoUploadUrl() } doReturn Single.just(uploadVideoUrlDtoTestInstance(videoId = MOCK_VIDEO_ID))
        }
        val productUserVideoRepository = ProductUserVideoRepositoryImpl(
            userVideoFapiClient,
            userVideoCacheDataStore,
            userVideoMapper,
            userProductVideoMapper,
            tusRepository,
            commentMapper,
            pagerMapper,
            authenticationRepository,
        )
        val videoId = productUserVideoRepository.uploadVideo(uri).blockingGet()
        assertEquals(videoId, MOCK_VIDEO_ID)
    }


    companion object {
        private const val MOCK_VIDEO_ID = "videoId"
    }
}