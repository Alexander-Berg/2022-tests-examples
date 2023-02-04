package common.clients.geocoder.test

import common.clients.geocoder.{GeocoderClient, GeocoderClientLive}
import common.zio.sttp.endpoint.Endpoint
import org.apache.commons.io.IOUtils
import common.zio.app.Application
import common.zio.sttp.Sttp
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import yandex.maps.proto.search.geocoder.geocoder._
import yandex.maps.proto.search.geocoder_internal.geocoder_internal.GeocoderInternalProto
import zio.test.Assertion._
import zio.test._

object GeocoderClientLiveSpec extends DefaultRunnableSpec {

  private val validStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond[Array[Byte]] {
    IOUtils.toByteArray(GeocoderClientLiveSpec.getClass.getResourceAsStream("/answer.bin"))
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("GeosuggestClientLive")(
      testM("parse response in reverseGeocode method") {
        for {
          response <- GeocoderClient.reverseGeocode(
            latitude = 55.748160,
            longitude = 37.587221
          )
          firstResultOpt = response.reply.flatMap(_.geoObject.headOption)
          firstResultNameOpt = firstResultOpt.flatMap(_.name)
          firstResultGeoIdOpt =
            firstResultOpt
              .flatMap(_.metadata.headOption)
              .flatMap(_.extension(GeocoderProto.gEOOBJECTMETADATA))
              .flatMap(_.extension(GeocoderInternalProto.tOPONYMINFO))
              .map(_.geoid)
        } yield assert(firstResultNameOpt)(isSome(equalTo("улица Арбат"))) &&
          assert(firstResultGeoIdOpt)(isSome(equalTo(213)))
      },
      testM("parse response in geocode method") {
        for {
          response <- GeocoderClient.geocode(
            text = "Арбат",
            limit = 1
          )
          firstResultOpt = response.reply.flatMap(_.geoObject.headOption)
          firstResultNameOpt = firstResultOpt.flatMap(_.name)
          firstResultGeoIdOpt =
            firstResultOpt
              .flatMap(_.metadata.headOption)
              .flatMap(_.extension(GeocoderProto.gEOOBJECTMETADATA))
              .flatMap(_.extension(GeocoderInternalProto.tOPONYMINFO))
              .map(_.geoid)
        } yield assert(firstResultNameOpt)(isSome(equalTo("улица Арбат"))) &&
          assert(firstResultGeoIdOpt)(isSome(equalTo(213)))
      }
    ).provideCustomLayer {
      (Endpoint.testEndpointLayer ++ Sttp.fromStub(validStub) ++ Application.live.orDie) >>> GeocoderClientLive.live
    }
  }
}
