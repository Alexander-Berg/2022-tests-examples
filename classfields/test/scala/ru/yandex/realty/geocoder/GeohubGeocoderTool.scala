package ru.yandex.realty.geocoder

import com.google.protobuf.util.JsonFormat
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import ru.yandex.realty.http.{ApacheHttpClient, HttpEndpoint, RemoteHttpService}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object GeohubGeocoderTool extends App {

  implicit private val trace: Traced = Traced.empty

  val httpClient = new ApacheHttpClient({
    val client = HttpAsyncClientBuilder
      .create()
      .setMaxConnPerRoute(1024)
      .setMaxConnTotal(40 * 1024)
      .build()
    client.start()
    sys.addShutdownHook(client.close())
    client
  })

  val g = new GeohubGeocoder(
    new RemoteHttpService(
      "geohub-geocoder-tool",
      HttpEndpoint("realty-geohub-api.vrts-slb.test.vertis.yandex.net"),
      httpClient
    ),
    GeocoderOrigins.UNIFICATION
  )

  println(
    Await
      .result(g.find("Пискаревский 2к2Щ"), 5.seconds)
      .map(JsonFormat.printer().print(_))
  )

}
