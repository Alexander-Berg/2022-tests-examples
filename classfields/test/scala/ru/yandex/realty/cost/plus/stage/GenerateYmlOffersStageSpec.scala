package ru.yandex.realty.cost.plus.stage

import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import ru.yandex.inside.yt.kosher.operations.Yield
import ru.yandex.realty.canonical.base.request.RequestType
import ru.yandex.realty.cost.plus.model.yml.YmlOffer
import ru.yandex.realty.cost.plus.service.stage.live.GenerateYmlOffersStage
import ru.yandex.realty.cost.plus.stage.GenerateYmlOffersStageSpec.{CollectingYield, RealCase1}
import ru.yandex.realty.model.offer.CategoryType
import ru.yandex.realty.traffic.model.ad.{GrouppedByUrlAds, MicroAd}
import ru.yandex.realty.traffic.model.converter.ad.MicroAdProtoConverter
import ru.yandex.realty.traffic.model.urls.ExtractedSourceUrl
import ru.yandex.realty.traffic.model.urls.RequestMeta.AutoRequestMeta
import ru.yandex.realty.traffic.proto.ad.MicroAdListMessage
import ru.yandex.vertis.protobuf.{ProtoInstanceProvider, ProtobufUtils}
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[ZTestJUnitRunner])
class GenerateYmlOffersStageSpec extends JUnitRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("GenerateYmlOffersStage") {
      test("should correctly process singe groupped ads") {
        val mapper = new GenerateYmlOffersStage.Mapper()
        val y = new CollectingYield[YmlOffer]

        mapper.map(RealCase1, y)

        assertTrue(y.firstOnly.size == 7)
      }
    }
}

object GenerateYmlOffersStageSpec extends ProtoInstanceProvider {

  final class CollectingYield[T] extends Yield[T] {

    private val result: ArrayBuffer[(Int, T)] = ArrayBuffer.empty

    override def `yield`(index: Int, value: T): Unit =
      result.append((index, value))

    override def close(): Unit = ()

    def firstOnly: Seq[T] =
      result.map {
        case (0, x) =>
          x
        case (i, _) =>
          throw new RuntimeException(s"Expected yield only for first index(0), but found $i")
      }
  }

  private def readAds(name: String): Seq[MicroAd] = {
    val json = IOUtils.toString(this.getClass.getClassLoader.getResourceAsStream(s"$name.json"))

    ProtobufUtils
      .fromJson[MicroAdListMessage](MicroAdListMessage.getDefaultInstance, json)
      .getAdsList
      .asScala
      .map(MicroAdProtoConverter.fromProto)
  }

  lazy val RealCase1: GrouppedByUrlAds =
    GrouppedByUrlAds(
      url = ExtractedSourceUrl(
        requestKey = "5:1:\"475709\",4:\"SELL\",7:\"APARTMENT\"",
        urlPath = "/liskinskiy_rayon/kupit/kvartira/",
        text = "Купить квартиру в Лискинском районе",
        meta = AutoRequestMeta(
          rooms = Seq.empty,
          requestType = RequestType.Search,
          categoryType = CategoryType.APARTMENT,
          rgid = 475709,
          siteId = None,
          villageId = None
        ),
        extraUrlParams = Set.empty
      ),
      topAds = readAds("liskinskiy_rayon_kupit_kvartira_top_ads"),
      totalAds = 7,
      experimentTopAds = readAds("liskinskiy_rayon_kupit_kvartira_top_exp_ads")
    )

  println(RealCase1)
}
