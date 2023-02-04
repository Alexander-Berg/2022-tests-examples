package ru.yandex.realty.clients.frontend.seo.model

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsValue, Json}
import ru.yandex.vertis.mockito.MockitoSupport

@RunWith(classOf[JUnitRunner])
class ResponseFormatSpec extends WordSpec with Matchers with MockitoSupport {

  private def json(resource: String): JsValue = {
    Json.parse(
      this.getClass.getClassLoader.getResourceAsStream(resource)
    )
  }

  "UrlWithRequestParams" should {
    "be correctly parsed" in {

      noException should be thrownBy json("response-formats/url-with-params.json").as[Seq[UrlWithRequestParams]]
    }
  }

  "UrlWithoutRequestParams" should {
    "be correctly parsed" in {

      noException should be thrownBy json("response-formats/url-wo-params.json").as[Seq[UrlWithoutParams]]
    }
  }
}
