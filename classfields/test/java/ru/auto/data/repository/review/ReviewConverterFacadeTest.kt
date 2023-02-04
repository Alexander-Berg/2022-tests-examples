package ru.auto.data.repository.review

import io.kotest.core.spec.style.DescribeSpec
import org.assertj.core.api.Assertions.assertThat
import ru.auto.data.model.network.scala.review.NWReview
import ru.auto.data.model.network.scala.review.NWReviewResponse
import ru.auto.data.stub.DictionaryRepositoryStub
import ru.auto.testextension.FileTestUtils
import ru.auto.testextension.testWithSubscriber

/**
 * @author dumchev on 2/7/19.
 */
@Suppress("UnstableApiUsage") // rxjava warnings
class ReviewConverterFacadeTest : DescribeSpec({

    describe("Review") {

        val converter = ReviewConverterFacade(DictionaryRepositoryStub())

        val nwReview: NWReview = FileTestUtils
            .readJsonAsset("/assets/reviews.json", NWReviewResponse::class.java)
            .reviews?.first() as NWReview
        val reviewSingle = converter.fromNetwork(nwReview)
        val review = reviewSingle.toBlocking().value()

        it("from network") {
            println(review)
            testWithSubscriber(reviewSingle)
            assertThat(review).hasNoNullFieldsOrProperties()
        }

        it("to network") {
            val convertedNwReview: NWReview = converter.toNetwork(review).toBlocking().value()
            assertThat(convertedNwReview.id).isNotBlank()
            assertThat(convertedNwReview.item?.auto).isNotNull()
        }
    }
})
