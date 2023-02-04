package ru.yandex.realty.cost.plus.service

import eu.timepit.refined.auto._
import org.joda.time.Instant
import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.request.RequestType
import ru.yandex.realty.cost.plus.logic.Hashing
import ru.yandex.realty.cost.plus.logic.ToSortableItemConverter.OffersDescFeature
import ru.yandex.realty.cost.plus.model.yml.RawYmlOffer
import ru.yandex.realty.cost.plus.service.builder.UrlViewBuilder.BuilderContext
import ru.yandex.realty.cost.plus.service.builder.impl.CarouselViewUrlViewBuilder
import ru.yandex.realty.cost.plus.utils.CustomAssertions._
import ru.yandex.realty.model.offer.CategoryType
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.traffic.model.ad._
import ru.yandex.realty.traffic.model.offer.OfferType
import ru.yandex.realty.traffic.model.relevance.NormalizedRelevance
import ru.yandex.realty.traffic.model.relevance.NormalizedRelevance.NormalizedRelevance
import ru.yandex.realty.traffic.model.urls.{ExtractedSourceUrl, RequestMeta}
import ru.yandex.realty.traffic.utils.CategoryTree
import ru.yandex.vertis.mockito.MockitoSupport
import zio.test.Assertion._
import zio.test._
import zio.test.junit._

@RunWith(classOf[ZTestJUnitRunner])
class CarouselViewUrlBuilderSpec extends JUnitRunnableSpec with MockitoSupport {

  private val million = 1000000

  private val url = ExtractedSourceUrl(
    "key",
    "/moskva/kupit/",
    "Купить в Москве",
    RequestMeta.AutoRequestMeta(
      Seq.empty,
      RequestType.Search,
      categoryType = CategoryType.UNKNOWN,
      rgid = NodeRgid.MOSCOW,
      siteId = None,
      villageId = None
    )
  )

  private def withPinnedOffer(url: String)(id: Int) =
    RawYmlOffer.Url(
      url,
      pinnedOfferId = Some(id.toString)
    )

  private def grouppedAds(ads: MicroAd*) =
    GrouppedByUrlAds(url, ads, ads.size, Seq.empty)

  private def runBuild(grouppedAds: GrouppedByUrlAds) =
    new CarouselViewUrlViewBuilder(Hashing.Default).buildYmlOffers(
      grouppedAds,
      BuilderContext.CarouselContext(OffersDescFeature.UpdateTime, filterNonSuitablePrices = true)
    )

  private def offer(
    id: Long,
    relev: MicroAdRelevance,
    price: Long = 100000,
    isYandexRent: Boolean = false,
    isExtended: Boolean = false
  ) =
    MicroAd(
      MicroAdData.OfferData(
        id.toString,
        offerType = OfferType.Sell,
        updateTime = Instant.ofEpochMilli((relev.current.toDouble * Long.MaxValue).toLong),
        offerRooms = None,
        category = CategoryTree.Commercial,
        price = MicroAdPrice.Direct(price),
        imageUrl = "image.url",
        flatType = None,
        title = s"commercial $id",
        area = None,
        fromAgent = None,
        isYandexRent = isYandexRent,
        isExtendedOffer = isExtended
      ),
      relev
    )

  private def village(id: Long, relev: NormalizedRelevance, convertible: Boolean = true) = {
    val data = mock[MicroAdData.VillageData]
    when(data.villageId).thenReturn(id)
    when(data.name).thenReturn(if (convertible) "." else "")

    MicroAd(
      MicroAdData.VillageData(
        villageId = id,
        name = s"village $id",
        price = MicroAdPrice.From(10000),
        imageUrl = "image.url",
        mainPageUrlPath = s"/village/$id"
      ),
      MicroAdRelevance.currentOnly(relev)
    )
  }

  private def makeCurrentOnlyRelevance(i: Int, max: Int = 10): MicroAdRelevance =
    MicroAdRelevance.currentOnly(
      NormalizedRelevance.wrapUnsafe(i.toDouble / max.toDouble)
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CarouselViewUrlBuilder")(
      test("should return empty when comes less than 3 offers") {
        def shouldBeEmptySpec(offers: MicroAd*) =
          assertTrue(runBuild(grouppedAds(offers: _*)).isEmpty)

        shouldBeEmptySpec() &&
        shouldBeEmptySpec(offer(1, MicroAdRelevance.currentOnly(0.1))) &&
        shouldBeEmptySpec(offer(1, MicroAdRelevance.currentOnly(0.1)), offer(2, MicroAdRelevance.currentOnly(0.2)))
      },
      test("should return non-empty when comes enough offers with different types") {
        assertTrue(
          runBuild(
            grouppedAds(
              offer(1, MicroAdRelevance.currentOnly(0.1)),
              offer(2, MicroAdRelevance.currentOnly(0.2)),
              offer(3, MicroAdRelevance.currentOnly(0.3)),
              village(1, 1.0)
            )
          ).nonEmpty
        )
      },
      test("return non empty when more than 2 offers with same type") {
        assert(
          runBuild(
            grouppedAds(
              village(1, 0.1),
              village(2, 0.2),
              village(3, 0.3)
            )
          )
        )(hasSize(equalTo(3)))
      },
      test("should filter offers by prices") {
        // один оффер с вбросовой ценой, но с хорошей релевантностью
        val badMinPriceOffer = offer(1, MicroAdRelevance.currentOnly(0.9), price = million)

        // два офффера с очень большой ценой, но хорошей релевантностью
        val badHigh = offer(2, MicroAdRelevance.currentOnly(0.91), price = 50 * million)
        val badHigh2 = offer(3, MicroAdRelevance.currentOnly(0.92), price = 48 * million)

        val normalPrices =
          (4 to 26).map { i =>
            offer(
              i,
              makeCurrentOnlyRelevance(i, 100),
              price = i * million
            )
          }

        val offers = Seq(badMinPriceOffer, badHigh, badHigh2) ++ normalPrices

        val reduced = runBuild(grouppedAds(offers: _*))

        // релевантность == id + 50, а отдаем всего 20 офферов
        val expectedUrls = (7 to 26).reverse.map(withPinnedOffer(url.urlPath))

        assert(reduced.map(_.url))(hasSameElementsAndOrder(expectedUrls))
      },
      test("should not filter offers by prices when too few offers") {
        val badMinPriceOffer = offer(1, MicroAdRelevance.currentOnly(0.9), price = million)
        val badHigh = offer(2, MicroAdRelevance.currentOnly(0.91), price = 50 * million)
        val badHigh2 = offer(3, MicroAdRelevance.currentOnly(0.92), price = 48 * million)

        val normalPrices =
          (4 to 10).map { i =>
            offer(
              i,
              makeCurrentOnlyRelevance(i, 100),
              price = i * million
            )
          }

        val offers = Seq(badMinPriceOffer, badHigh, badHigh2) ++ normalPrices
        val reduced = runBuild(grouppedAds(offers: _*))
        val expectedUrls = ((3 to 1 by -1) ++ (10 to 4 by -1)).map(withPinnedOffer(url.urlPath))

        assert(reduced.map(_.url))(hasSameElementsAndOrder(expectedUrls))
      },
      test("return correct boost YandexRent") {

        // обычные оффера. При этом релевантность их по отношению к офферам аренды больше
        val normalOffers =
          (4 to 10).map { id =>
            offer(
              id,
              makeCurrentOnlyRelevance(id)
            )
          }

        val rentOffers =
          (1 to 3).map { id =>
            offer(
              id,
              makeCurrentOnlyRelevance(id),
              isYandexRent = true
            )
          }

        val actual =
          runBuild(grouppedAds(normalOffers ++ rentOffers: _*)).map(_.url)

        val expectedUrls =
          (3 to 1 by -1).map(withPinnedOffer(url.urlPath)) ++
            (10 to 4 by -1).map(withPinnedOffer(url.urlPath))

        assert(actual)(hasSameElementsAndOrder(expectedUrls))
      },
      test("should place extended offers at the end") {

        val normalOffers =
          (2 to 8).map { id =>
            offer(
              id,
              makeCurrentOnlyRelevance(id)
            )
          }

        val offers =
          Seq(offer(1, makeCurrentOnlyRelevance(1), isExtended = true)) ++
            normalOffers ++
            Seq(
              offer(9, makeCurrentOnlyRelevance(9), isExtended = true),
              offer(10, makeCurrentOnlyRelevance(10), isExtended = true)
            )

        val actual = runBuild(grouppedAds(offers: _*)).map(_.url)

        val expected =
          ((8 to 2 by -1) ++ Seq(10, 9, 1)).map(withPinnedOffer(url.urlPath))

        assert(actual)(hasSameElementsAndOrder(expected))
      }
    )
}
