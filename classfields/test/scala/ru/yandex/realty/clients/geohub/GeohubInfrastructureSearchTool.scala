package ru.yandex.realty.clients.geohub

import com.google.protobuf.util.JsonFormat
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import ru.yandex.realty.buildinginfo.model.auxsources.NearbyInfrastructure
import ru.yandex.realty.buildinginfo.model.storagemodel.SimpleGeoPoint
import ru.yandex.realty.http.{ApacheHttpClient, HttpEndpoint, RemoteHttpService}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.Mappings.MapAny

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object GeohubInfrastructureSearchTool extends App {

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

  val g = new GeohubInfrastructureSearchClient(
    new RemoteHttpService(
      "geohub-infrastructure-search",
      HttpEndpoint("realty-geohub-api.vrts-slb.test.vertis.yandex.net"),
      httpClient
    )
  )

  println(
    Await
      .result(
        g.omnisearch(
          NearbyInfrastructure
            .newBuilder()
            .setGeoPoint(SimpleGeoPoint.newBuilder().setLat(59.997076f).setLon(30.272850f).build())
            .applySideEffect(_.getMetroBuilder)
            .applySideEffect(_.getIsochronesBuilder)
            .build()
        ),
        5.seconds
      )
      .applySideEffect(JsonFormat.printer().print(_))
  )

}
