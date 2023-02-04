package ru.yandex.realty.traffic.service.urls.generator

import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.params.RequestParameter
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.giraffic.BySourceUrlsRequest
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.proto.unified.offer.address.LocationUnified
import ru.yandex.realty.traffic.TestData
import ru.yandex.realty.traffic.service.urls.generator.live.RailwayStationRequestWithTextGenerator
import ru.yandex.realty.proto.unified.offer.address.Station
import ru.yandex.realty.traffic.model.RequestWithText
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

@RunWith(classOf[ZTestJUnitRunner])
class RailwayStationRequestWithTextGeneratorSpec extends JUnitRunnableSpec {

  private val serviceLayer =
    TestData.regionServiceLayer >>> RailwayStationRequestWithTextGenerator.live

  private def offerRequest(rgid: Long, esr: Option[Long]): BySourceUrlsRequest = {
    val station = esr.map(id => Station.newBuilder().setName("").setEsr(id).build())
    BySourceUrlsRequest.ByOfferRequest(
      UnifiedOffer
        .newBuilder()
        .setLocation {
          val location = LocationUnified.newBuilder().setRgid(rgid)
          station.foreach(s => location.addStation(s))
          location
        }
        .build()
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("RailwayStationRequestWithTextGenerator")(
      testM("not generate anything when offer geo is greater than city") {
        GeneratorSpecCommon
          .testNoGenerated(offerRequest(NodeRgid.MOSCOW_AND_MOS_OBLAST, None))
          .provideLayer(serviceLayer)
      },
      testM("generate common request for city when offer has no railway stations") {
        GeneratorSpecCommon
          .testGeneratorGenerateExpected(offerRequest(NodeRgid.MOSCOW, None))(
            RequestWithText(
              Request.Raw(RequestType.Railways, Seq(RequestParameter.Rgid(NodeRgid.MOSCOW))),
              s"Недвижимость с выбором по станциям пригородных поездов в Москве"
            )
          )
          .provideLayer(serviceLayer)
      },
      testM("generate common request and railway station request when offer has railway station") {
        GeneratorSpecCommon
          .testGeneratorGenerateExpected(offerRequest(NodeRgid.MOSCOW, Some(TestData.station.getEsr)))(
            RequestWithText(
              Request.Raw(RequestType.Railways, Seq(RequestParameter.Rgid(NodeRgid.MOSCOW))),
              s"Недвижимость с выбором по станциям пригородных поездов в Москве"
            ),
            RequestWithText(
              Request.Raw(
                RequestType.Railway,
                Seq(RequestParameter.Rgid(NodeRgid.MOSCOW), RequestParameter.StationId(TestData.station.getEsr))
              ),
              s"Недвижимость у станции ${TestData.station.getTitle} в Москве"
            )
          )
          .provideLayer(serviceLayer)
      }
    )

}
