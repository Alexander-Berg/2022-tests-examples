package ru.auto.api.auth

import ru.auto.api.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 25.02.17
  */
class ApplicationTokenSpec extends BaseSpec {

  "ApplicationToken" should {
    "be parsed from string" in {
      val token = ApplicationToken("123abcABC")
      token.value shouldBe "123abcABC"
    }
    "throw exception on malformed token" in {
      intercept[IllegalArgumentException] {
        ApplicationToken("abc/3")
      }
    }
  }
}
