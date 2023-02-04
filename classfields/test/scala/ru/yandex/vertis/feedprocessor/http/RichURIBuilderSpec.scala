package ru.yandex.vertis.feedprocessor.http

import org.apache.http.client.utils.URIBuilder
import ru.yandex.vertis.feedprocessor.BaseSpec

class RichURIBuilderSpec extends BaseSpec {

  "URIBuilder" should {
    "build url with number parameters" in {
      val uri = new URIBuilder("/test")
      uri.addParameter("intId", 123)
      uri.addParameter("longId", 456L)

      uri.build().toString shouldBe "/test?intId=123&longId=456"
    }

    "build url with boolean parameter" in {
      val uri = new URIBuilder("/test")
      uri.addParameter("isTrue", true)
      uri.addParameter("isFalse", false)

      uri.build().toString shouldBe "/test?isTrue=true&isFalse=false"
    }

    "build url with multiplied parameter" in {
      val uri = new URIBuilder("/test")
      uri.addMultipliedParameter("tag", Seq("tagA", "tagB"))

      uri.build().toString shouldBe "/test?tag=tagA&tag=tagB"
    }

    "build url with optional parameter" in {
      val uri = new URIBuilder("/test")
      uri.addOptionalParameter("paramA", Some("valueA"))
      uri.addOptionalParameter("paramB", None)

      uri.build().toString shouldBe "/test?paramA=valueA"
    }

    "build url with optional multiple parameter" in {
      val uri = new URIBuilder("/test")
      uri.addOptionalMultipliedParameter("paramA", Some(Seq("valueA", "valueB")))
      uri.addOptionalMultipliedParameter("paramB", None)

      uri.build().toString shouldBe "/test?paramA=valueA&paramA=valueB"
    }
  }

}
