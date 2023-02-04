package ru.yandex.vos2.reviews.export

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.reviews.BaseReviewsTest
import ru.yandex.vos2.reviews.export.writer.ReviewsTopLikeExportWriter

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 07/08/2017.
  */
class ReviewsTopLikeExportWriterTest extends BaseReviewsTest {

  val exportWriter = new ReviewsTopLikeExportWriter(stream, 4, commentsDao, features)
  test("write top reviews by like"){

    val offer1 = createOffer1
    val offer2 = createOffer2
    val offer3 = createOffer3
    val offer4 = createOffer4

    exportWriter.onOffer(offer1)
    exportWriter.onOffer(offer2)
    exportWriter.onOffer(offer3)
    exportWriter.onOffer(offer4)
    exportWriter.finish()

    val is = new ByteArrayInputStream(stream.toByteArray)

    Offer.parseDelimitedFrom(is) shouldBe offer1
    Offer.parseDelimitedFrom(is) shouldBe offer4
    Offer.parseDelimitedFrom(is) shouldBe offer3
    Offer.parseDelimitedFrom(is) shouldBe offer2
    Offer.parseDelimitedFrom(is) shouldBe null

  }
}
