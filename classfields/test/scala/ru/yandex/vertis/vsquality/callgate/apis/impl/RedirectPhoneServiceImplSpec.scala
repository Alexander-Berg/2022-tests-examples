package ru.yandex.vertis.vsquality.callgate.apis.impl

import cats.effect.IO
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.vsquality.callgate.apis.GeneralGatewayApiClient
import ru.yandex.vertis.vsquality.callgate.apis.GeneralGatewayApiClient.Contacts
import ru.yandex.vertis.vsquality.callgate.generators.Arbitraries._
import ru.yandex.vertis.vsquality.callgate.model.Payload
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase

class RedirectPhoneServiceImplSpec extends SpecBase {

  private val payload = generate[Payload.GeneralOfferCallInfo].copy(phone = "old-phone".taggedWith)

  "RedirectPhoneServiceImpl" should {
    "fails if redirect fails" in {
      val gateway = gatewayFailing
      callDelegateApi(gateway).shouldFail
    }

    "doesn't change payload if phone is not redirected" in {
      val gateway =
        gatewayReturning(
          Contacts(
            phone = "".taggedWith,
            origPhone = "".taggedWith,
            isRedirectPhone = false
          )
        )

      val ret = callDelegateApi(gateway).unsafeRunSync()

      ret shouldBe payload
    }

    "changes payload if phone is redirected" in {
      val gateway =
        gatewayReturning(
          Contacts(
            phone = "new-phone".taggedWith,
            origPhone = "orig-phone".taggedWith,
            isRedirectPhone = true
          )
        )

      val ret = callDelegateApi(gateway).unsafeRunSync()

      val expected = payload.copy(phone = "new-phone".taggedWith)
      ret shouldBe expected
    }
  }

  private def callDelegateApi(gateway: GeneralGatewayApiClient[F]): F[Payload.GeneralOfferCallInfo] = {
    val api = new RedirectPhoneServiceImpl(gateway)
    api.redirectPhone(payload)
  }

  private def gatewayReturning(contacts: Contacts): GeneralGatewayApiClient[F] = { _ => IO.pure(contacts) }
  private def gatewayFailing: GeneralGatewayApiClient[F] = { _ => IO.raiseError(new RuntimeException) }
}
