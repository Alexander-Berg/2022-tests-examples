package common.autoru.clients.public_api.test

import common.autoru.clients.public_api.{PublicApiClient, SttpPublicApiClient}
import common.autoru.clients.public_api.SttpPublicApiClient.SttpPublicApiConfig
import common.autoru.clients.public_api.model.{PublicApiError, SearchGroupBy}
import common.zio.sttp.Sttp
import common.zio.sttp.Sttp.Sttp
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.ResponseModel.OfferListingResponse
import ru.auto.api.search.SearchModel.SearchRequestParameters
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.{basicRequest, Response, SttpClientException, UriContext}
import sttp.model.{Method, StatusCode}
import zio.{ULayer, ZIO, ZLayer}
import zio.test._
import zio.test.Assertion._

object SttpPublicApiClientSpec extends DefaultRunnableSpec {
  private val testListing = OfferListingResponse.newBuilder().addOffers(Offer.getDefaultInstance).build()
  private val testPage = 2
  private val testPageSize = 10
  private val testGrouping = SearchGroupBy.DealerId

  private val testCfg = ZLayer.succeed {
    SttpPublicApiConfig("url", 80, "http", "token")
  }

  private val searchUri = "1.0/search/cars"

  private val okStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case req
        if req.uri.path.mkString("/") == searchUri &&
          req.uri.paramsMap.get("page_size").contains(testPageSize.toString) &&
          req.uri.paramsMap.get("page").contains(testPage.toString) &&
          req.uri.paramsMap.get("group_by").contains(testGrouping.name) &&
          req.method == Method.POST =>
      Response.ok(testListing.toByteArray)
  }

  private val badRequestStub =
    AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond("bla", StatusCode.BadRequest)

  private val exceptionStub =
    AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond(
      throw new SttpClientException.ConnectException(basicRequest.get(uri"http://example.com"), new Exception)
    )

  private def testEnv(sttp: ULayer[Sttp]) = (testCfg ++ sttp) >>> SttpPublicApiClient.live

  private val searchTest = for {
    client <- ZIO.service[PublicApiClient.Service]
    res <- client.searchCars(SearchRequestParameters.getDefaultInstance, testPage, testPageSize, testGrouping)
  } yield res

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("SttpPublicApiClient")(
      testM("should search cars properly") {
        assertM(searchTest)(equalTo(testListing))
      }.provideCustomLayer(testEnv(Sttp.fromStub(okStub))),
      testM("should fail with PublicApiError on non 2xx code") {
        assertM(searchTest.run)(fails(isSubtype[PublicApiError](anything)))
      }.provideCustomLayer(testEnv(Sttp.fromStub(badRequestStub))),
      testM("should fail with PublicApiError on sttp errors") {
        assertM(searchTest.run)(fails(isSubtype[PublicApiError](anything)))
      }.provideCustomLayer(testEnv(Sttp.fromStub(exceptionStub)))
    )
}
