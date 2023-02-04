package ru.auto.api.model

import ru.auto.api.BaseSpec

class SessionIDSpec extends BaseSpec {
  "SessionID" should {
    "mangle session id" in {
      val sessionId =
        "3:1618943746.5.0.1618943746264:20ywsg:82.1|1412811062.0.2|233399.539164.i96gHog44em_YgJEFMD0pWNl2dI"
      SessionID.mangle(sessionId) shouldBe "3:1618943746.5.0.1618943746264:20ywsg:82.1|1412811062.0.2|233399.539164.***"
    }
  }
}
