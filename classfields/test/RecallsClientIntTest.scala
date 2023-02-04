package auto.carfax.common.clients.recalls.test

import auto.carfax.common.clients.recalls.{RecallsCardCreated, RecallsCardDeleted, RecallsClient}
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.{UserRef, VinCode}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

@Ignore
class RecallsClientIntTest extends AnyFunSuite {

  val remoteService = new RemoteHttpService(
    "recalls",
    new HttpEndpoint("recalls-api-http-api.vrts-slb.test.vertis.yandex.net", 80, "http")
  )

  implicit val t: Traced = Traced.empty
  val client = new RecallsClient(remoteService)

  test("add card by vin") {
    val vin = VinCode("WBAUE11040E238571")
    val user = UserRef.parseOrThrow("user:26186324")

    val recalls = client.addUserCard(user, vin, true).await

    assert(recalls.asInstanceOf[RecallsCardCreated].card.getSerializedSize != 0)
  }

  test("delete card") {
    val user = UserRef.parseOrThrow("user:26186324")

    val recalls = client.deleteUserCard(user, 815).await

    assert(recalls == RecallsCardDeleted)
  }

}
