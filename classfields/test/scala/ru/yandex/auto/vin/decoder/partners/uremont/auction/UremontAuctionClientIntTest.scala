package ru.yandex.auto.vin.decoder.partners.uremont.auction

import auto.carfax.common.clients.hydra.HydraClient
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.{BeforeAndAfterAll, Ignore}
import ru.yandex.auto.vin.decoder.model.{CommonVinCode, VinCode}
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.{ExecutionContext, Future}

@Ignore
class UremontAuctionClientIntTest extends AsyncFunSuite with MockitoSupport with BeforeAndAfterAll {

  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val t = Traced.empty

  val hydraClient: HydraClient = mock[HydraClient]
  when(hydraClient.getClicker(?, ?)(?)).thenReturn(Future.successful(1))
  when(hydraClient.incClicker(?, ?)(?)).thenReturn(Future.unit)

  val remoteService = new RemoteHttpService(
    "uremont",
    new HttpEndpoint("data.uremont.dev", 443, "https")
  )

  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager

  val client = new UremontAuctionClient(
    httpService = remoteService,
    token = "5d749bbf641ad1a8fc7810b10ccebbff",
    hydraClient = hydraClient,
    partnerEventClient = partnerEventClient
  )(ExecutionContext.global)

  val vins: Seq[String] = Seq(
    "2T2BK1BA5AC006409"
  )

  for (vin <- vins) {
    test(s"get vin data for $vin") {
      client.getRawVinData(CommonVinCode(vin)).map { response =>
        assert(response.data.migtorg.isDefined)
      }
    }
  }

  test("incorrect vin") {
    client.getRawVinData(CommonVinCode("QQQQQQ")).map { response =>
      assert(response.data.migtorg.isDefined)
    }
  }

  test("raw") {
    client.getRawVinData(VinCode("2T2BK1BA5AC006409")).await

    assert(2 + 2 == 4)
  }
}
