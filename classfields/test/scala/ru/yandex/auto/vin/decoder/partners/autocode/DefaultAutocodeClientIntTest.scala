package ru.yandex.auto.vin.decoder.partners.autocode

import auto.carfax.common.utils.tracing.Traced
import io.lemonlabs.uri.Url
import org.scalatest.Ignore
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.auto.vin.decoder.model.IdentifierGenerators.{LicensePlateGen, VinCodeGen}
import ru.yandex.auto.vin.decoder.partners.event.NoopPartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

@Ignore
class DefaultAutocodeClientIntTest extends AsyncFunSuite with Matchers with MockitoSupport {

  val service = new RemoteHttpService(
    "autocode",
    new HttpEndpoint("b2bapi-autoru.avtocod.ru", 443, "https")
  )

  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val t: Traced = Traced.empty

  val partnerEventClient = new NoopPartnerEventManager

  private val config = AutocodeConfig(
    Url("https://b2bapi-autoru.avtocod.ru"),
    "aocheretko@autoru",
    "fbFMH8/Ts16ybEP/KbkLNw=="
  )

  val client = new DefaultAutocodeClient(config, service, partnerEventClient)

  val vinCode = VinCodeGen.sample.get
  val licensePlate = LicensePlateGen.sample.get

  test(s"get token") {
    client.login(60).map { token =>
      token.age shouldBe 60
    }
  }

  test("make report") {
    val token = client.login(60).await
    client.make(vinCode, token, AutocodeReportType.Main, regenerateIfExist = false).map { reportId =>
      partnerEventClient.event.getOrderId shouldBe reportId
    }
  }

  test("refresh report") {
    val token = client.login(60).await
    client.make(vinCode, token, AutocodeReportType.Main, regenerateIfExist = true).map { response =>
      assert(response.toString().nonEmpty)
    }
  }

  test("get result") {
    val token = client.login(60).await
    client
      .getResult(token, AutocodeRequest(s"autoru_main_report_$vinCode@autoru", vinCode, AutocodeReportType.Main))
      .map { report =>
        (report._2 \ "state").get.toString shouldBe "\"ok\""
      }
  }
}
