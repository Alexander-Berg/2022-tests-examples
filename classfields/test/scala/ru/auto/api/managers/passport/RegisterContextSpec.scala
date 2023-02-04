package ru.auto.api.managers.passport

import ru.auto.api.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 08.06.18
  */
class RegisterContextSpec extends BaseSpec {
  "RegisterContext.withName" should {
    "resolve enum from snake_case name" in {
      RegisterContext.withName("search_confirm") shouldBe RegisterContext.SearchConfirm
    }

    "throw IllegalArgumentException on unknown value" in {
      an[IllegalArgumentException] should be thrownBy {
        RegisterContext.withName("xxx")
      }
    }
  }
}
