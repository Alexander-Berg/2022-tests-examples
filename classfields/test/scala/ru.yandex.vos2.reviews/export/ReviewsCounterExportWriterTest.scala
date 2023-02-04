package ru.yandex.vos2.reviews.export

import ru.yandex.vos2.reviews.BaseReviewsTest
import ru.yandex.vos2.reviews.export.writer.ReviewsCounterExportWriter

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 04/08/2017.
  */
class ReviewsCounterExportWriterTest extends BaseReviewsTest {

  val exportWriter = new ReviewsCounterExportWriter(stream)
  test("write counters"){

    val res = "BMW,X5,1,1\nBMW,X5,2,2\nAudu,TT,1,1\n"

    exportWriter.onOffer(createOffer1)
    exportWriter.onOffer(createOffer2)
    exportWriter.onOffer(createOffer3)
    exportWriter.onOffer(createOffer4)
    exportWriter.finish()

    val resCounter = stream.toString

    resCounter shouldBe res
  }
}
