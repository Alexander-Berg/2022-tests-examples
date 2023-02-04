package ru.auto.data.repository.review

import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import ru.auto.data.model.network.scala.review.NWReview
import ru.auto.data.model.network.scala.review.NWReviewResponse
import ru.auto.data.model.network.scala.review.NWReviewSaveResponse
import ru.auto.data.model.network.scala.review.NWStatus
import ru.auto.data.model.review.Review
import ru.auto.data.model.review.Status
import ru.auto.data.network.scala.ScalaApi
import ru.auto.data.stub.DictionaryRepositoryStub
import ru.auto.testextension.FileTestUtils
import ru.auto.testextension.testWithSubscriber
import rx.Single

/**
 * @author dumchev on 2/11/19.
 */
@Suppress("UnstableApiUsage") //hiding rxjava urine
class ReviewDraftRepoTest : DescribeSpec({

    describe("having dependencies") {


        val nwReview: NWReview = (FileTestUtils
            .readJsonAsset("/assets/reviews.json", NWReviewResponse::class.java)
            .reviews?.first() as NWReview)

        val api: ScalaApi = mock()
        whenever(api.getAnonReviewDraft()).thenAnswer { Single.just(nwReview) }
        whenever(api.updateReviewDraft(any(), any())).thenAnswer { Single.just(NWReviewSaveResponse(nwReview.id, emptyList())) }

        val converter = ReviewConverterFacade(DictionaryRepositoryStub())

        val repo = OldReviewDraftRepository(api, converter)

        it("publishing changes status to ENABLED") {
            val reviewSingle: Single<Review> = repo.getAnonDraft()
            val publishReviewSingle = reviewSingle
                .flatMap { review ->
                    val disabledReview = review.copy(status = Status.DISABLED)
                    repo.publishDraft(disabledReview)
                }
                .doOnSuccess { review -> review.status shouldBe Status.ENABLED }

            testWithSubscriber(publishReviewSingle)

            val reviewBodyCaptor = argumentCaptor<NWReview>()
            verify(api, times(1)).updateReviewDraft(any(), reviewBodyCaptor.capture())

            reviewBodyCaptor.lastValue.status shouldBe NWStatus.ENABLED
        }
    }
})
