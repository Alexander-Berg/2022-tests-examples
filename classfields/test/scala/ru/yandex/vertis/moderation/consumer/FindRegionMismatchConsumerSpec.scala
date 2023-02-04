package ru.yandex.vertis.moderation.consumer

import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.service.geo.impl.{RegionGeneralizerServiceImpl, RegionTreeFactory}
import ru.yandex.vertis.moderation.service.geo.{
  LoggingRegionGeneralizerService,
  RegionGeneralizerService,
  RegionTreeService
}
import ru.yandex.vertis.moderation.httpclient.geobase.impl.http.HttpGeobaseClient
import ru.yandex.vertis.moderation.httpclient.telepony.impl.http.HttpTeleponyPhoneUnifierClient
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.InstanceGen
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, RealtyEssentials}
import ru.yandex.vertis.moderation.model.realty
import ru.yandex.vertis.moderation.proto.{Autoru, RealtyLight}
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.SellerType
import ru.yandex.vertis.moderation.scheduler.task.region.RegionMismatchDecider.Source

import scala.concurrent.ExecutionContext

/**
  * Specs for [[FindRegionMismatchConsumer]] logic
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class FindRegionMismatchConsumerSpec extends SpecBase {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val regionTree: RegionTreeService = RegionTreeFactory.buildFromResource("geobase.xml")

  def regionGeneralizer: RegionGeneralizerService =
    new RegionGeneralizerServiceImpl(regionTree) with LoggingRegionGeneralizerService

  private val httpClient = new DefaultAsyncHttpClient()

  lazy val geobaseClient = new HttpGeobaseClient("http://geobase-test.qloud.yandex.ru/v1/", httpClient)
  lazy val teleponyClient =
    new HttpTeleponyPhoneUnifierClient("http://hydra-01-myt.test.vertis.yandex.net:35530/api/1.x", httpClient)

  "FindRegionMismatchConsumer.asSource" should {
    "return None if resolve ip to country" in {
      val instance = {
        val i = InstanceGen.suchThat(_.essentials.isInstanceOf[AutoruEssentials]).next
        val ess = i.essentials.asInstanceOf[AutoruEssentials]
        i.copy(essentials =
          ess.copy(
            source = Autoru.AutoruEssentials.Source.AUTO_RU,
            isCallCenter = false,
            sellerType = Some(SellerType.PRIVATE),
            geobaseId = Seq(11463),
            ip = Some("176.101.1.139"),
            phones =
              Map(
                "79787621879" -> ""
              )
          )
        )
      }
      FindRegionMismatchConsumer
        .asSource(
          geobaseClient,
          teleponyClient,
          regionGeneralizer
        )(instance)
        .futureValue match {
        case Some(Source(977, Some(977), Seq(10995))) => ()
        case other                                    => fail(s"Unexpected $other")
      }
    }
    "be correct with phone geoid" in {
      val instance = {
        val i = InstanceGen.suchThat(_.essentials.isInstanceOf[RealtyEssentials]).next
        val ess = i.essentials.asInstanceOf[RealtyEssentials]
        i.copy(essentials =
          ess.copy(
            source = Some(RealtyLight.RealtyEssentials.Source.YANDEX_REALTY),
            isCallCenter = Some(false),
            authorInfo =
              Some(realty.AuthorInfo(Some(RealtyLight.AuthorType.PRIVATE_OWNER), None, None, Seq.empty, None, None)),
            geoInfo = Some(realty.GeoInfo(Seq.empty, Seq(11463), None, None, None, None, None, None)),
            ip = Some("77.88.8.1"),
            phones =
              Seq(
                "79787621879",
                "79213882934"
              )
          )
        )
      }
      FindRegionMismatchConsumer
        .asSource(
          geobaseClient,
          teleponyClient,
          regionGeneralizer
        )(instance)
        .futureValue match {
        case Some(Source(977, Some(10174), Seq(10995, 10174))) => ()
        case other                                             => fail(s"Unexpected $other")
      }
    }
    "return None if has phone geoid resolved to country" in {
      val instance = {
        val i = InstanceGen.suchThat(_.essentials.isInstanceOf[AutoruEssentials]).next
        val ess = i.essentials.asInstanceOf[AutoruEssentials]
        i.copy(essentials =
          ess.copy(
            source = Autoru.AutoruEssentials.Source.AUTO_RU,
            isCallCenter = false,
            sellerType = Some(SellerType.PRIVATE),
            geobaseId = Seq(11463),
            ip = Some("77.88.8.1"),
            phones =
              Map(
                "79787621879" -> "",
                "+78002509639" -> ""
              )
          )
        )
      }
      FindRegionMismatchConsumer
        .asSource(
          geobaseClient,
          teleponyClient,
          regionGeneralizer
        )(instance)
        .futureValue match {
        case None  => ()
        case other => fail(s"Unexpected $other")
      }
    }
  }

}
