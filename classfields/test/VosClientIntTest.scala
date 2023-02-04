package auto.carfax.common.clients.vos.test

import auto.carfax.common.clients.vos.VosClient
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, Ignore}
import ru.yandex.auto.vin.decoder.model.AutoruUser
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Ignore
class VosClientIntTest extends AnyWordSpec with MockitoSupport with BeforeAndAfter {

  val service = new RemoteHttpService(
    "vos",
    new HttpEndpoint("vos2-autoru-api.vrts-slb.test.vertis.yandex.net", 80, "http")
  )

  "VosClient" should {
    val vos = new VosClient(service)
    val validUser = AutoruUser(10591660)
    val validOffer = "1043045004"

    "get offer hash by user id and offer" in {
      val maybeId = Await.result(vos.getAutoRuIdWithHash(validOffer, validUser)(Traced.empty), 10.seconds)
      assert(maybeId.nonEmpty)
      val maybeOffer = Await.result(vos.getOffer(maybeId.get)(Traced.empty), 10.seconds)
      assert(maybeOffer.nonEmpty)
    }
  }

}
