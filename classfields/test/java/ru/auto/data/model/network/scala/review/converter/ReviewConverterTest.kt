package ru.auto.data.model.network.scala.review.converter

import io.kotest.core.spec.style.DescribeSpec
import org.assertj.core.api.Assertions.assertThat
import ru.auto.data.model.network.scala.review.NWContentType
import ru.auto.data.model.network.scala.review.NWReview
import ru.auto.data.model.network.scala.review.NWReviewResponse
import ru.auto.data.model.review.Review
import ru.auto.data.repository.IDictionaryRepository
import ru.auto.data.stub.DictionaryRepositoryStub
import ru.auto.testextension.FileTestUtils

/**
 * @author dumchev on 2/8/19.
 */
class ReviewConverterTest : DescribeSpec({

    @Suppress("UnstableApiUsage")
    fun createConverter(
        dictionaryRepo: IDictionaryRepository,
        nwReview: NWReview
    ): ReviewConverter = dictionaryRepo
        .getDictionariesForCategory(nwReview.item?.auto?.category?.name?.toLowerCase() ?: "cars")
        .map { dictionaries -> ReviewConverter(dictionaries) }
        .toBlocking()
        .value()

    fun getNWReviews(): List<NWReview>? = FileTestUtils
        .readJsonAsset("/assets/reviews.json", NWReviewResponse::class.java)
        .reviews


    describe("having converter") {

        val nwReviews = getNWReviews()
        val dictionaryRepo: IDictionaryRepository = DictionaryRepositoryStub()

        nwReviews?.forEach { nwReview ->

            val reviewConverter = createConverter(dictionaryRepo, nwReview)
            val convertedReview: Review = reviewConverter.fromNetwork(nwReview)


            context("from network") {

                it("pros, cons, contents are not empty") {
                    assertThat(convertedReview.cons).isNotEmpty
                    assertThat(convertedReview.pros).isNotEmpty
                    assertThat(convertedReview.reviewContents).isNotEmpty
                }

                it("all fields are not-null") {
                    assertThat(convertedReview).hasNoNullFieldsOrProperties()
                }
            }


            context("to network") {
                val nwConvertedReview: NWReview = reviewConverter.toNetwork(convertedReview)

                it("check basic fields converted") {
                    assertThat(nwConvertedReview)
                        .hasNoNullFieldsOrPropertiesExcept("like_num", "dislike_num", "published", "updated", "views_count")
                }

                it("reviewer") {
                    assertThat(nwConvertedReview.reviewer?.id).isNotNull() // the only field needed
                }

                it("tags") {
                    assertThat(nwConvertedReview.tags?.map { it.name }).containsExactlyInAnyOrder(
                        ReviewRateConverter.APPEARANCE,
                        ReviewRateConverter.COMFORT,
                        ReviewRateConverter.SAFETY,
                        ReviewRateConverter.RELIABILITY,
                        ReviewRateConverter.DRIVEABILITY
                    )
                }

                it("content") {
                    assertThat(nwConvertedReview.content).isNotEmpty
                    assertThat(nwConvertedReview.content).hasSize(nwReview.content?.size ?: 0)
                    nwConvertedReview.content?.forEach { nwContent ->
                        nwContent.content_value?.forEach { assertThat(it.value).isNotBlank() }
                        if (nwContent.type == NWContentType.VIDEO) assertThat(nwContent.video).isNotNull()
                    }
                }

                it("item.auto") {
                    assertThat(nwConvertedReview.item?.auto).isNotNull()
                    with(nwConvertedReview.item?.auto) {
                        assertThat(this?.category?.name).isNotNull()
                        assertThat(this?.mark).isNotNull()
                        assertThat(this?.model).isNotNull()
                        assertThat(this?.configuration_id).isNotNull()
                        // also need to check DictionaryBased values
                    }
                }
            }
        }
    }
})
