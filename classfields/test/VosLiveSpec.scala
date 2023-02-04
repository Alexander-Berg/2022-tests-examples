package auto.common.clients.vos.test

import auto.common.clients.vos.Vos.OwnerId.DealerId
import auto.common.clients.vos.Vos.{OffersCountFilter, SectionFilter, StatusFilter}
import auto.common.clients.vos.{Vos, VosLive}
import common.zio.sttp.endpoint.Endpoint
import ru.auto.api.api_offer_model.{Category, OfferStatus, Section}
import common.zio.sttp.Sttp
import sttp.client3.{Request, Response}
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.Assertion._

object VosLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("VosClientLive") {
      testM("proper uri in getOffersCount") {

        for {
          count <- Vos.getOffersCount(Some(category), DealerId(dealerId), filters)
        } yield assert(count)(equalTo(expectedOffersCount))

      }
    }.provideCustomLayerShared((Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpStub) >>> VosLive.live))
  }

  private val dealerId = 11L
  private val category = Category.CARS
  private val expectedOffersCount = 111

  private val sttpStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case req if validOffersCountUri(req) =>
      Response.ok(expectedOffersCount.toString)
  }

  private def validOffersCountUri(req: Request[_, _]) = {
    val properPathString = s"api/v1/offers/${category.name.toLowerCase}/dealer:$dealerId/count"
    val paramsMap = req.uri.paramsMap
    req.uri.path.mkString("/") == properPathString &&
    filters.forall(f => paramsMap.get(f.name).contains(f.renderedValue))
  }

  private val filters: List[OffersCountFilter] =
    List(SectionFilter(Section.NEW), StatusFilter(OfferStatus.EXPIRED))

}
