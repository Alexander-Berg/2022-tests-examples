package ru.yandex.auto.vin.decoder.partners.nbki

import auto.carfax.common.utils.http.TestHttpUtils
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

@Ignore
class NbkiClientIntTest extends AnyFunSuite with MockitoSupport {

  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val t = Traced.empty

  val remoteService = new RemoteHttpService(
    "nbki",
    new HttpEndpoint("collatauto.demo.nbki.ru", 8080, "http"),
    client = TestHttpUtils.DefaultHttpClient
  )
  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager

  val client = new NbkiClient(remoteService, "/CollatAuto/collatauto", partnerEventClient)

  test("get pledge") {
    val request = ResourceUtils.getStringFromResources("/nbki/valid_request.b64")
    val vin = VinCode("X4XKS694600K29362")

    val res = client.getPledges(vin, request).await
    println(res)
  }

}
