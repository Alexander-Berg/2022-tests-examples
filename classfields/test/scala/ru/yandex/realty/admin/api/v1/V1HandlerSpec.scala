package ru.yandex.realty.admin.api.v1

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.admin.api.ApiHandlerSpecBase
import ru.yandex.realty.admin.proto.api.feature.{Feature, Features}

@Ignore
@RunWith(classOf[JUnitRunner])
class V1HandlerSpec extends ApiHandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.OK

  override def basePath: String = "/api/1.x"

  "GET /features" should {

    "return all features" in {
      Get(url("/features")) ~> route ~> check {
        status shouldBe OK
        val features = responseAs[Features]
        features.getFeaturesList.isEmpty should be(false)
      }
    }

  }

  "PUT /features" should {

    "update all features" in {
      val features = Features
        .newBuilder()
        .addFeatures(
          Feature
            .newBuilder()
            .setName("big_b_logging")
            .setEnabled(false)
            .build()
        )
        .build()
      Put(url("/features"), features) ~> route ~> check {
        status shouldBe OK
      }
    }

    "update feature by name" in {
      val feature = Feature
        .newBuilder()
        .setName("big_b_logging")
        .setEnabled(false)
        .build()
      Put(url(s"/features/${feature.getName}"), feature) ~> route ~> check {
        status shouldBe OK
      }
    }

  }

}
