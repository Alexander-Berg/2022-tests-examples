package ru.yandex.realty.service

import org.junit.runner.RunWith
import ru.yandex.bolts.collection.Cf
import ru.yandex.inside.yt.kosher.impl.operations.StatisticsImpl
import ru.yandex.realty.canonical.base.params.RequestParameter.RoomsValue
import ru.yandex.realty.canonical.base.request.RequestType
import ru.yandex.realty.model.AdWithUrl
import ru.yandex.realty.service.live.LiveAdByUrlsGroupper
import ru.yandex.realty.traffic.model.ad.{AdWithUrls, GrouppedByUrlAds, MicroAd, MicroAdRelevance}
import ru.yandex.realty.traffic.model.urls.ExtractedSourceUrl
import ru.yandex.realty.traffic.model.urls.RequestMeta.AutoRequestMeta
import ru.yandex.realty.utils.SeqYield
import zio.test._
import zio.test.junit._
import eu.timepit.refined.auto._
import org.joda.time.Instant
import ru.yandex.realty.model.offer.CategoryType
import ru.yandex.realty.traffic.model.ad.MicroAdData.OfferData
import ru.yandex.realty.traffic.model.ad.MicroAdPrice.Direct
import ru.yandex.realty.traffic.model.offer.{OfferRooms, OfferType}
import ru.yandex.realty.traffic.model.relevance.NormalizedRelevance
import ru.yandex.realty.traffic.utils.CategoryTree

import scala.collection.JavaConverters._

@RunWith(classOf[ZTestJUnitRunner])
class AdByUrlsGroupperSpec extends JUnitRunnableSpec {

  import AdByUrlsGroupperSpecTestData._

  private def runMapReduce(ads: Seq[AdWithUrls]): Seq[GrouppedByUrlAds] = {
    val mapper = new LiveAdByUrlsGroupper.Mapper
    val reducer = new LiveAdByUrlsGroupper.Reducer

    val mapYield = new SeqYield[AdWithUrl]
    val reduceYield = new SeqYield[GrouppedByUrlAds]

    ads.foreach(mapper.map(_, mapYield))

    require(mapYield.forall(_._1 == 0), "Mapper. Required only 0 index output")

    reducer.reduce(
      Cf.wrap(
        mapYield
          .map(_._2)
          .sortBy(_.urlPath)
          .iterator
          .asJava
      ),
      reduceYield,
      new StatisticsImpl
    )

    require(reduceYield.forall(_._1 == 0), "Reducer. Required only 0 index output")
    reduceYield.map(_._2)
  }

  private def testCaseSpec(testCase: TestCase) =
    test(s"should correctly reduce `${testCase.name}`") {
      val res = runMapReduce(testCase.input)

      assertTrue(res == testCase.expected)
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("AdByUrlsGroupper")(
      testCaseSpec(NaberezhnyeChelnySnyatKvartiraDvuhkomnatnaya)
    )
}

object AdByUrlsGroupperSpecTestData {

  case class TestCase(
    name: String,
    input: Seq[AdWithUrls],
    expected: Seq[GrouppedByUrlAds]
  )

  val NaberezhnyeChelnySnyatKvartiraDvuhkomnatnaya: TestCase = {
    val url =
      ExtractedSourceUrl(
        requestKey = "5:1:\"566519\",2:2,4:\"RENT\",7:\"APARTMENT\"",
        urlPath = "/naberezhnye_chelny/snyat/kvartira/dvuhkomnatnaya/",
        text = "Снять 2-комнатную квартиру в Набережных Челнах",
        meta = AutoRequestMeta(
          rooms = Seq(RoomsValue.TwoRooms),
          requestType = RequestType.Search,
          categoryType = CategoryType.UNKNOWN,
          rgid = 0L,
          siteId = None,
          villageId = None
        ),
        extraUrlParams = Set.empty
      )

    val o7568657803187110639 =
      MicroAd(
        data = OfferData(
          offerId = "7568657803187110639",
          offerType = OfferType.Rent,
          updateTime = Instant.parse("2022-04-05T16:54:49.000Z"),
          offerRooms = Some(OfferRooms._2),
          category = CategoryTree.TwoRooms,
          price = Direct(12000),
          imageUrl =
            "http://avatars.mdst.yandex.net/get-realty/3019/offer.7568657803187110639.3244125970317423804/wiz_t2",
          flatType = None,
          title = "2-комнатная, 51 м²",
          area = Some(51),
          fromAgent = Some(true),
          isYandexRent = false,
          isExtendedOffer = false
        ),
        relevance = MicroAdRelevance(
          0.5010337681744792,
          0.18215125799179077
        )
      )

    val o7568657803187110514 =
      MicroAd(
        data = OfferData(
          offerId = "7568657803187110514",
          offerType = OfferType.Rent,
          updateTime = Instant.parse("2022-04-05T16:54:50.000Z"),
          offerRooms = Some(OfferRooms._2),
          category = CategoryTree.TwoRooms,
          price = Direct(21000),
          imageUrl = "http://avatars.mdst.yandex.net/get-realty/3274/w1296/wiz_t2",
          flatType = None,
          title = "2-комнатная, 50 м²",
          area = Some(50),
          fromAgent = Some(true),
          isYandexRent = false,
          isExtendedOffer = false
        ),
        relevance = MicroAdRelevance(
          0.5000162982614749,
          0.06000424921512604
        )
      )

    val o7568657803187135464 =
      MicroAd(
        data = OfferData(
          offerId = "7568657803187135464",
          offerType = OfferType.Rent,
          updateTime = Instant.parse("2022-04-05T16:54:48.000Z"),
          offerRooms = Some(OfferRooms._2),
          category = CategoryTree.TwoRooms,
          price = Direct(15000),
          imageUrl = "http://avatars.mdst.yandex.net/get-realty/3274/w1585/wiz_t2",
          flatType = None,
          title = "2-комнатная, 40 м²",
          area = Some(40),
          fromAgent = Some(true),
          isYandexRent = false,
          isExtendedOffer = false
        ),
        relevance = MicroAdRelevance(
          0.5009336509976847,
          0.11061692237854004
        )
      )

    val o7568657803187139432 =
      MicroAd(
        data = OfferData(
          offerId = "7568657803187139432",
          offerType = OfferType.Rent,
          updateTime = Instant.parse("2022-04-05T16:54:44.000Z"),
          offerRooms = Some(OfferRooms._2),
          category = CategoryTree.TwoRooms,
          price = Direct(13000),
          imageUrl = "http://avatars.mdst.yandex.net/get-realty/3274/w1592/wiz_t2",
          flatType = None,
          title = "2-комнатная, 40 м²",
          area = Some(40),
          fromAgent = Some(true),
          isYandexRent = false,
          isExtendedOffer = false
        ),
        relevance = MicroAdRelevance(
          0.5009592623684926,
          0.06018257513642311
        )
      )

    val offers = Seq(o7568657803187110639, o7568657803187110514, o7568657803187135464, o7568657803187139432)
    val input = offers.map(AdWithUrls(_, Seq(url)))

    val exp = offers
      .map { ad =>
        ad.copy(
          relevance = ad.relevance.copy(
            experiment = NormalizedRelevance.wrapUnsafe(1.0 - ad.relevance.experiment.value)
          )
        )
      }
      .sortBy(-_.relevance.experiment.value)

    val expected =
      GrouppedByUrlAds(
        url = url,
        topAds = offers.sortBy(-_.relevance.current.value),
        totalAds = offers.size.toLong,
        experimentTopAds = exp
      )

    TestCase("/naberezhnye_chelny/snyat/kvartira/dvuhkomnatnaya/", input, Seq(expected))
  }
}
