package ru.yandex.vertis.telepony.util

import ru.yandex.vertis.telepony.SpecBase

/**
  * @author tolmach
  */
class HttpUtilsSpec extends SpecBase {

  "HttpUtils" should {
    "remove credentials from url" when {
      "url with credentials passed" in {
        val sampleUrl = "https://username:password@telepony.test.vertis.yandex.net/reactive-api/operators/operator"
        val expectedUrl = "https://***:***@telepony.test.vertis.yandex.net/reactive-api/operators/operator"
        val actualUrl = HttpUtils.hideCredentials(sampleUrl)
        actualUrl shouldBe expectedUrl
      }
    }
  }

}
