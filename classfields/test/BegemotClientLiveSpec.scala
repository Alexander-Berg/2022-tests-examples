package common.clients.begemot.test

import common.clients.begemot.model.Rule.{ExternalMarkup, GeoAddr, Tokens}
import common.clients.begemot.{BegemotClient, BegemotClientLive}
import common.clients.begemot.model.{GeoAddrResponse, Lemma, MarkupResponse, MarkupToken, TokenMorph}
import common.zio.sttp.endpoint.Endpoint
import common.zio.sttp.Sttp
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.Has
import zio.test.Assertion._
import zio.test._

import scala.io.Source

object BegemotClientLiveSpec extends DefaultRunnableSpec {

  private val singleGeoStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    Source.fromResource("geoaddr_single_geo_response.json")(scala.io.Codec.UTF8).getLines().mkString
  }

  private val multipleGeoStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    Source.fromResource("geoaddr_multiple_geo_response.json")(scala.io.Codec.UTF8).getLines().mkString
  }

  private val noGeoStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    Source.fromResource("geoaddr_no_geo_response.json")(scala.io.Codec.UTF8).getLines().mkString
  }

  private val noGeoStubV2 = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    Source.fromResource("geoaddr_no_geo_response_v2.json")(scala.io.Codec.UTF8).getLines().mkString
  }

  private val stubWithoutText = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    Source.fromResource("geoaddr_response_without_text.json")(scala.io.Codec.UTF8).getLines().mkString
  }

  private val markupStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    Source.fromResource("markup_response.json")(scala.io.Codec.UTF8).getLines().mkString
  }

  private val markupTokensStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    Source.fromResource("markup_tokens_response.json")(scala.io.Codec.UTF8).getLines().mkString
  }

  val config = Endpoint.testEndpointLayer.map(endpoint => Has(BegemotClientLive.Config(endpoint.get, "classifieds")))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("BegemotClientLive")(
      testM("parse single geo response") {
        for {
          result <- BegemotClient.requestRules("?????????????? apple ????????????", Set(GeoAddr))
        } yield assert(result.rules.geoAddr)(
          isSome(
            equalTo(
              GeoAddrResponse(
                nonGeoQuery = "?????????????? apple",
                bestInheritedGeo = List(Some(213)),
                bestGeo = List(Some(213)),
                weight = List(0.990827),
                pos = List(2),
                length = List(1),
                normalizedText = List("????????????")
              )
            )
          )
        )
      }.provideCustomLayer((config ++ Sttp.fromStub(singleGeoStub)) >>> BegemotClientLive.Live),
      testM("parse multiple geo response") {
        for {
          result <- BegemotClient.requestRules("?????????????? apple ???????????? ?????????????? ????????????????", Set(GeoAddr))
        } yield assert(result.rules.geoAddr)(
          isSome(
            equalTo(
              GeoAddrResponse(
                nonGeoQuery = "?????????????? apple",
                bestInheritedGeo = List(Some(213), Some(2)),
                bestGeo = List(Some(213), None),
                weight = List(0.984913, 0.845430),
                pos = List(2, 3),
                length = List(1, 2),
                normalizedText = List("????????????", "?????????????? ????????????????")
              )
            )
          )
        )
      }.provideCustomLayer((config ++ Sttp.fromStub(multipleGeoStub)) >>> BegemotClientLive.Live),
      testM("parse response without NonGeoQuery") {
        for {
          result <- BegemotClient.requestRules("????????", Set(GeoAddr))
        } yield assert(result.rules.geoAddr)(
          isSome(
            equalTo(
              GeoAddrResponse(
                nonGeoQuery = "",
                bestInheritedGeo = List(None),
                bestGeo = List(None),
                weight = List(0.856479),
                pos = List(0),
                length = List(1),
                normalizedText = List("????????")
              )
            )
          )
        )
      }.provideCustomLayer((config ++ Sttp.fromStub(stubWithoutText)) >>> BegemotClientLive.Live),
      testM("parse no geo response") {
        for {
          result <- BegemotClient.requestRules("?????????????? apple", Set(GeoAddr))
        } yield assert(result.rules.geoAddr)(isNone)
      }.provideCustomLayer((config ++ Sttp.fromStub(noGeoStub)) >>> BegemotClientLive.Live),
      testM("parse no geo response that has geoaddr information") {
        for {
          result <- BegemotClient.requestRules("??????????", Set(GeoAddr))
        } yield assert(result.rules.geoAddr)(isNone)
      }.provideCustomLayer((config ++ Sttp.fromStub(noGeoStubV2)) >>> BegemotClientLive.Live),
      testM("parse response in regular format with markup") {
        for {
          result <- BegemotClient.requestRules("???????????? ???????????????????????? ????????????", Set(ExternalMarkup(Set(Tokens))))
        } yield assert(result.markupResponse)(
          isSome(
            equalTo(
              MarkupResponse(List(MarkupToken("????????????"), MarkupToken("????????????????????????"), MarkupToken("????????????")))
            )
          )
        )
      }.provideCustomLayer((config ++ Sttp.fromStub(markupTokensStub)) >>> BegemotClientLive.Live),
      testM("parse markup response") {
        for {
          result <- BegemotClient.getLemmas("?????????? ???????????? ??????????????????")
        } yield assert(result)(
          equalTo(
            List(
              TokenMorph(List(Lemma("??????????"), Lemma("??????????"))),
              TokenMorph(List(Lemma("????????????"))),
              TokenMorph(List(Lemma("??????????????")))
            )
          )
        )
      }.provideCustomLayer((config ++ Sttp.fromStub(markupStub)) >>> BegemotClientLive.Live)
    )
  }
}
