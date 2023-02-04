package common.clients.geosuggest.test

import common.clients.geosuggest.model.{Position, SuggestBases, SuggestItem}
import common.clients.geosuggest.{GeosuggestClient, GeosuggestClientLive}
import common.zio.sttp.endpoint.Endpoint
import common.zio.sttp.Sttp
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.test.Assertion._
import zio.test._

import scala.io.Source

object GeosuggestClientLiveSpec extends DefaultRunnableSpec {

  private val validStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    Source.fromResource("answer.json")(scala.io.Codec.UTF8).getLines().mkString
  }

  import SuggestBases._

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("GeosuggestClientLive") {
      testM("parse response") {
        for {
          result <- GeosuggestClient.suggest("Москва", Seq.empty, None, Seq.empty, 2)
        } yield assert(result)(
          equalTo(
            Seq(
              SuggestItem(
                "Москва",
                "Россия",
                "Россия, Москва ",
                List(GeobaseCityBase),
                Position(55.755817, 37.617645),
                213,
                None
              ),
              SuggestItem(
                "Москва-Сити",
                "Московский международный деловой центр, Пресненский район, Центральный административный округ, Москва, Россия",
                "Россия, Москва, Центральный административный округ, Пресненский район, Московский международный деловой центр Москва-Сити ",
                List.empty,
                Position(55.749451, 37.542824),
                120538,
                Some(120538)
              )
            )
          )
        )
      }
    }.provideCustomLayer((Endpoint.testEndpointLayer ++ Sttp.fromStub(validStub)) >>> GeosuggestClientLive.live)
  }
}
