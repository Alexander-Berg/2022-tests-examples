package ru.yandex.realty.traffic.service.urls.generator

import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.request.{RealtyNearbyExactMetroStationRequest, RealtyNearbyMetroStationsRequest}
import ru.yandex.realty.giraffic.BySourceUrlsRequest
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.proto.unified.offer.address.{LocationUnified, Metro}
import ru.yandex.realty.traffic.TestData
import ru.yandex.realty.traffic.model.RequestWithText
import ru.yandex.realty.traffic.service.urls.generator.live.MetroRequestWithTextGenerator
import zio.test.ZSpec
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

import scala.collection.JavaConverters._

@RunWith(classOf[ZTestJUnitRunner])
class MetroRequestWithTextGeneratorSpec extends JUnitRunnableSpec {
  private val serviceLayer =
    TestData.regionServiceLayer >>> MetroRequestWithTextGenerator.live

  private def offerRequest(rgid: Long, geoId: Int, metroStationIds: Seq[Int]): BySourceUrlsRequest = {
    val stations = metroStationIds.map { id =>
      Metro.newBuilder().setGeoId(id).build()
    }
    BySourceUrlsRequest.ByOfferRequest(
      UnifiedOffer
        .newBuilder()
        .setLocation(
          LocationUnified
            .newBuilder()
            .setRgid(rgid)
            .setGeoId(geoId)
            .addAllMetro(stations.asJava)
        )
        .build()
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("MetroRequestWithTextGenerator")(
      testM("generate no requests for city without metro stations") {
        GeneratorSpecCommon
          .testNoGenerated(offerRequest(NodeRgid.CHELYABINSKAYA_OBLAST, Regions.CHELYABINSK, Seq.empty))
          .provideLayer(serviceLayer)
      },
      testM("generate common metro requests for city and federation subject if there is only rgid and geoId") {
        GeneratorSpecCommon
          .testGeneratorGenerateExpected(offerRequest(NodeRgid.MOSCOW, Regions.MOSCOW, Seq.empty))(
            RequestWithText(
              RealtyNearbyMetroStationsRequest(NodeRgid.MOSCOW),
              "Купить или арендовать недвижимость от частных лиц и агентств с выбором по станциям метро в Москве на Яндекс Недвижимости"
            ),
            RequestWithText(
              RealtyNearbyMetroStationsRequest(NodeRgid.MOSCOW_AND_MOS_OBLAST),
              "Купить или арендовать недвижимость от частных лиц и агентств с выбором по станциям метро в Москве и МО на Яндекс Недвижимости"
            )
          )
          .provideLayer(serviceLayer)
      },
      testM(
        "generate common metro requests and metro station requests for city and federation subject if there are rgid and metro ids"
      ) {
        val metroId = 20433
        GeneratorSpecCommon
          .testGeneratorGenerateExpected(offerRequest(NodeRgid.MOSCOW, Regions.MOSCOW, Seq(metroId)))(
            RequestWithText(
              RealtyNearbyMetroStationsRequest(NodeRgid.MOSCOW),
              "Купить или арендовать недвижимость от частных лиц и агентств с выбором по станциям метро в Москве на Яндекс Недвижимости"
            ),
            RequestWithText(
              RealtyNearbyMetroStationsRequest(NodeRgid.MOSCOW_AND_MOS_OBLAST),
              "Купить или арендовать недвижимость от частных лиц и агентств с выбором по станциям метро в Москве и МО на Яндекс Недвижимости"
            ),
            RequestWithText(
              RealtyNearbyExactMetroStationRequest(NodeRgid.MOSCOW, metroId),
              "Купить или арендовать недвижимость от частных лиц и агентств у станции метро Ленинский проспект в Москве на Яндекс Недвижимости"
            ),
            RequestWithText(
              RealtyNearbyExactMetroStationRequest(NodeRgid.MOSCOW_AND_MOS_OBLAST, metroId),
              "Купить или арендовать недвижимость от частных лиц и агентств у станции метро Ленинский проспект в Москве и МО на Яндекс Недвижимости"
            )
          )
          .provideLayer(serviceLayer)
      }
    )
}
