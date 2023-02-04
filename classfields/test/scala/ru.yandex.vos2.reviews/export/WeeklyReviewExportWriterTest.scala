package ru.yandex.vos2.reviews.export

import java.io.ByteArrayInputStream

import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.reviews.BaseReviewsTest
import ru.yandex.vos2.reviews.export.writer.WeeklyReviewExportWriter

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 23/08/2017.
  */
class WeeklyReviewExportWriterTest extends BaseReviewsTest {

  val exportWriter = new WeeklyReviewExportWriter(stream, commentsDao, features)

  test("weekly reviews") {

    println(System.nanoTime())

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
    Offer.parseDelimitedFrom(is) shouldBe null

  }
}
