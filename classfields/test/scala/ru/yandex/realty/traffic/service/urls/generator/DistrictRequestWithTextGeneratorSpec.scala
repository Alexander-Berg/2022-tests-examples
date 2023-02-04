package ru.yandex.realty.traffic.service.urls.generator

import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.params.RequestParameter
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.giraffic.BySourceUrlsRequest
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.proto.unified.offer.address.LocationUnified
import ru.yandex.realty.traffic.TestData
import ru.yandex.realty.traffic.model.RequestWithText
import ru.yandex.realty.traffic.service.urls.generator.live.DistrictRequestWithTextGenerator
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

import scala.collection.JavaConverters._

@RunWith(classOf[ZTestJUnitRunner])
class DistrictRequestWithTextGeneratorSpec extends JUnitRunnableSpec {

  private val serviceLayer =
    TestData.regionServiceLayer >>> DistrictRequestWithTextGenerator.live

  private def offerRequest(rgid: Long, districts: Seq[Long]): BySourceUrlsRequest = {
    BySourceUrlsRequest.ByOfferRequest(
      UnifiedOffer
        .newBuilder()
        .setLocation(
          LocationUnified
            .newBuilder()
            .setRgid(rgid)
            .addAllDistricts(districts.map(Long.box).asJava)
        )
        .build()
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("DistrictRequestWithTextGenerator")(
      testM("not generate anything when offer geo is greater than city") {
        GeneratorSpecCommon
          .testNoGenerated(offerRequest(NodeRgid.MOSCOW_AND_MOS_OBLAST, Seq.empty))
          .provideLayer(serviceLayer)
      },
      testM("generate all districts request when offer in city and no known districts") {
        GeneratorSpecCommon
          .testGeneratorGenerateExpected(offerRequest(NodeRgid.MOSCOW, Seq.empty))(
            RequestWithText(
              Request.Raw(RequestType.Districts, Seq(RequestParameter.Rgid(NodeRgid.MOSCOW))),
              s"Недвижимость с выбором по районам в Москве"
            )
          )
          .provideLayer(serviceLayer)
      },
      testM("generate all districts request when offer in city and with districts") {
        val dist = 193391L
        GeneratorSpecCommon
          .testGeneratorGenerateExpected(offerRequest(NodeRgid.MOSCOW, Seq(dist)))(
            RequestWithText(
              Request.Raw(RequestType.Districts, Seq(RequestParameter.Rgid(NodeRgid.MOSCOW))),
              s"Недвижимость с выбором по районам в Москве"
            ),
            RequestWithText(
              Request.Raw(
                RequestType.District,
                Seq(RequestParameter.Rgid(NodeRgid.MOSCOW), RequestParameter.District(dist))
              ),
              "Недвижимость в Богородском в Москве"
            )
          )
          .provideLayer(serviceLayer)
      }
    )

}
