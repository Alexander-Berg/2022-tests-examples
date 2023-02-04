package ru.yandex.vos2.reviews.export

import ru.yandex.vos2.reviews.BaseReviewsTest
import ru.yandex.vos2.reviews.export.writer.RatingExportWriter

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 07/08/2017.
  */
class RatingExportWriterTest extends BaseReviewsTest {

  val exportWriter = new RatingExportWriter(stream)
  test("write counters"){

    val res = "BMW,X5,1/kuzov:3,podveska:4,dvigatel:1/1\n" +
      "BMW,X5,2/kuzov:9,podveska:9,dvigatel:9/2\n" +
      "Audu,TT,1/kuzov:5,podveska:3,dvigatel:2/1\n"

    exportWriter.onOffer(createOffer1)
    exportWriter.onOffer(createOffer2)
    exportWriter.onOffer(createOffer3)
    exportWriter.onOffer(createOffer4)
    exportWriter.finish()

    val resCounter = stream.toString

    resCounter shouldBe res
  }

}
