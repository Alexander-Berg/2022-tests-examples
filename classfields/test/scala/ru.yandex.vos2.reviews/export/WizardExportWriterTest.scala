package ru.yandex.vos2.reviews.export

import ru.yandex.vos2.reviews.BaseReviewsTest
import ru.yandex.vos2.reviews.export.writer.WizardExportWriter

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 26/10/2017.
  */
class WizardExportWriterTest extends BaseReviewsTest {
  val exportWriter = new WizardExportWriter(stream)
  test("write counters") {

    val res = "mark;model;c;rating\n" + "Audu;TT;1;3.3\n" + "BMW;X5;3;3.9\n"

    exportWriter.onOffer(createOffer1)
    exportWriter.onOffer(createOffer2)
    exportWriter.onOffer(createOffer3)
    exportWriter.onOffer(createOffer4)
    exportWriter.finish()

    val resCounter = stream.toString

    resCounter shouldBe res
  }
}
