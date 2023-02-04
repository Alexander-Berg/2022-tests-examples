package ru.yandex.vos2.autoru.letters.renderers

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils

import scala.jdk.CollectionConverters._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-02-07.
  */
@RunWith(classOf[JUnitRunner])
class NewReviewPromocodeRendererTest extends AnyFunSuite with InitTestDbs {
  implicit val trace = Traced.empty

  private var testFinished = false
  private val nextInt = Stream.from(1)

  private val renderer = new NewReviewPromocodeRenderer(
    components.carsCatalog,
    components.motoCatalog,
    components.trucksCatalog,
    components.mdsPhotoUtils
  )

  test("photo url") {

    val offer = TestUtils.createOffer()
    offer.getOfferAutoruBuilder.addPhoto(TestUtils.createPhoto("1623995-68c8e8df9b314efbab9c7241a4ff992f"))
    val image = offer.getOfferAutoru.getPhotoList.asScala.head

    assert(components.mdsPhotoUtils.getSizes(image).get("832x624").nonEmpty)
  }

  test(s"review_content_test6") {
    while (!testFinished) {
      val offer = TestUtils.createOffer()
      offer.getOfferAutoruBuilder.addPhoto(TestUtils.createPhoto("1623995-68c8e8df9b314efbab9c7241a4ff992f"))
      val notification = renderer.render(offer.build())

      if (notification.name == "promokod_for.review_content_test6") {
        assert(
          notification.mail.get.payload.value
            .get("image")
            .map(_.as[String])
            .get
            .contains("1623995/68c8e8df9b314efbab9c7241a4ff992f")
        )
        testFinished = true
      }
    }
  }

}
