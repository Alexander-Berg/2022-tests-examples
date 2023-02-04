package ru.yandex.vertis.passport

import org.scalatest.FreeSpec
import ru.yandex.vertis.passport.config.SessionIdConfig
import ru.yandex.vertis.passport.model.{SessionId, SessionUser}
import ru.yandex.vertis.passport.service.SessionIdService.Valid
import ru.yandex.vertis.passport.service.impl.RichSessionIdService
import ru.yandex.vertis.passport.test.SpecBase
import ru.yandex.vertis.passport.util.crypt.HmacSigner

/**
  * Tests to make sure session ids, generated in the past still valid
  *
  * @author zvez
  */
//scalastyle:off
class SessionIdLongevityTest extends FreeSpec with SpecBase {

  val signer = new HmacSigner(SessionIdConfig.readFromResource("/crypt/session.secret").secret)

  val sessionIdService = new RichSessionIdService(signer)

  "initial release sessions" in {
    val sid = SessionId.parse(
      "21503284|1490180812326.604800.ajC8B4qaXvx8dcZU5fXkuw.hqygx_8NBGFCV6oBaMro4MamAqGVISpOzb64E6hWklc"
    )
    sessionIdService.validate(sid) match {
      case Valid(data) =>
        data.owner shouldBe SessionUser("21503284")
        data.ttl.getStandardSeconds shouldBe 604800L
        data.creationDate.getMillis shouldBe 1490180812326L
      case other => fail("Unexpected result: " + other)
    }
  }

  "test" in {
    val sid = SessionId.parse(
      "21506632|1495784671474.7776000.hAh7mSM_eqdprwQdPiFY5A.d3RFv8BrpWk_-wl_A40PfSryjbh5tUtbLSeOMBlyB_E"
    )
    sessionIdService.validate(sid) match {
      case Valid(data) =>
        println(data)
      case other => fail("Unexpected result: " + other)
    }
  }

}
