package ru.yandex.realty.traffic.service.urls.generator

import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.request.IndexRequest
import ru.yandex.realty.giraffic.BySourceUrlsRequest
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.proto.unified.offer.address.LocationUnified
import ru.yandex.realty.traffic.TestData
import ru.yandex.realty.traffic.model.RequestWithText
import ru.yandex.realty.traffic.service.urls.generator.live.IndexRequestWithTextGenerator
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

@RunWith(classOf[ZTestJUnitRunner])
class IndexRequestWithTextGeneratorSpec extends JUnitRunnableSpec {

  private val serviceLayer =
    TestData.regionServiceLayer >>> IndexRequestWithTextGenerator.live

  private def offerRequest(rgid: Long): BySourceUrlsRequest = {

    BySourceUrlsRequest.ByOfferRequest(
      UnifiedOffer
        .newBuilder()
        .setLocation(LocationUnified.newBuilder().setRgid(rgid))
        .build()
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("IndexRequestWithTextGenerator")(
      testM("should correctly generate requests when no known districts") {
        GeneratorSpecCommon
          .testGeneratorGenerateExpected(offerRequest(NodeRgid.MOSCOW))(
            RequestWithText(
              IndexRequest(NodeRgid.MOSCOW),
              s"Яндекс.Недвижимость — свежие объявления в Москве"
            ),
            RequestWithText(
              IndexRequest(NodeRgid.MOSCOW_AND_MOS_OBLAST),
              s"Яндекс.Недвижимость — свежие объявления в Москве и МО"
            ),
            RequestWithText(
              IndexRequest(NodeRgid.RUSSIA),
              s"Яндекс.Недвижимость — свежие объявления в России"
            )
          )
          .provideLayer(serviceLayer)
      }
    )
}
