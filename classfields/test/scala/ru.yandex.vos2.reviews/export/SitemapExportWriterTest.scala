package ru.yandex.vos2.reviews.export

import ru.yandex.vos2.reviews.BaseReviewsTest
import ru.yandex.vos2.reviews.export.writer.SitemapExportWriter

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 28/12/2017.
  */
class SitemapExportWriterTest extends BaseReviewsTest {

  val pathHead = "https://media.auto.ru/reviews/"
  val isMobile = false
  val exportWriter = new SitemapExportWriter(stream, pathHead, isMobile)

  test("sitemap") {

    exportWriter.start()
    exportWriter.onOffer(createOffer1)
    exportWriter.onOffer(createOffer2)
    exportWriter.onOffer(createOffer3)
    exportWriter.onOffer(createOffer4)
    exportWriter.onOffer(createOffer5)
    exportWriter.finish()

    val res = stream.toString

    res shouldBe ""
  }
}
