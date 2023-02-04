package ru.yandex.vertis.passport.api.v1

import ru.yandex.vertis.passport.model.SessionResult
import ru.yandex.vertis.passport.view.SessionResultView
import org.scalatest.Matchers._
import ru.yandex.vertis.passport.api.ServiceBackendProvider

/**
  *
  * @author zvez
  */
trait SessionTestUtils { this: ServiceBackendProvider =>

  def checkSessionResult(expected: SessionResult, response: SessionResultView): Unit = {
    response shouldBe SessionResultView.asView(expected)
    response.id shouldBe expected.session.id
    response.deviceUid shouldBe expected.session.deviceUid
    response.ttlSec shouldBe expected.session.ttl.getStandardSeconds
    response.userId shouldBe expected.session.userId
  }
}
