package auto.dealers.trade_in_notifier.storage.clients.test

import com.google.protobuf.ByteString
import auto.common.clients.auto_shard.AutoShardClient.AutoShardClient
import auto.common.clients.auto_shard.AutoShardClientLive
import common.zio.sttp.endpoint.Endpoint
import ru.auto.api.api_offer_model.{Offer, OfferStatus}
import auto.dealers.trade_in_notifier.storage.OfferToMatcherConverter
import auto.dealers.trade_in_notifier.storage.OfferToMatcherConverter.OfferToMatcherConverter
import auto.dealers.trade_in_notifier.storage.clients.OfferToMatcherConverterLive
import ru.yandex.vertis.subscriptions.model.{Document, Term}
import common.zio.sttp.Sttp
import common.zio.sttp.Sttp.ZioSttpBackendStub
import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.Method
import zio.ZLayer
import zio.test.Assertion._
import zio.test._
import zio.clock.Clock
import sttp.client3.ByteArrayBody

object OfferToMatcherConverterSpec extends DefaultRunnableSpec { self =>

  private val defaultOffer: Offer = Offer(id = "offer_11", status = OfferStatus.ACTIVE)

  private val defaultTerm: Term = Term(name = "model")

  private def makeDocument(offer: Offer): Document =
    Document(
      "offer_11",
      rawContent = ByteString.EMPTY,
      term = Seq(Term(name = "status", point = Some(Term.Point(offer.status.toString()))))
    )

  private val GetDocumentByOffer = "subscription/offer"

  private val responseStub =
    AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
      case r
          if r.uri.path.mkString("/") == GetDocumentByOffer &&
            r.uri.paramsMap.get("id").contains("offer_11") &&
            r.method == Method.POST =>
        r.body match {
          case byteArray: ByteArrayBody =>
            Response.ok(makeDocument(Offer.parseFrom(byteArray.b)).toByteArray)
          case _ =>
            throw new IllegalArgumentException(s"Illegal RequestBody: ${r.body}")
        }
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("OfferToMatcherConverterSpec")(
      testM("return code 200 - proto subscriptions.Model.Document") {
        for {
          response <- OfferToMatcherConverter.convert("offer_11", self.defaultOffer)
        } yield assert(response)(equalTo(makeDocument(self.defaultOffer)))
      }
    )
  }.provideLayerShared(createEnvironment(self.responseStub))

  def createEnvironment(
      stub: ZioSttpBackendStub): ZLayer[Any, TestFailure[Throwable], OfferToMatcherConverter with Clock] = {

    val autoShardClient: ZLayer[Any, Throwable, AutoShardClient] =
      (Sttp.fromStub(stub) ++ Endpoint.testEndpointLayer) >>> AutoShardClientLive.live

    val offerToMatcherConverter = autoShardClient >>> OfferToMatcherConverterLive.live

    offerToMatcherConverter.mapError(TestFailure.fail) ++ Clock.live
  }
}
