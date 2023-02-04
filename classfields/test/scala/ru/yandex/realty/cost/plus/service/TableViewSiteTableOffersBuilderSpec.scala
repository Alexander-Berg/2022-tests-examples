package ru.yandex.realty.cost.plus.service

import org.joda.time.Instant
import org.junit.runner.RunWith
import ru.yandex.realty.cost.plus.logic.{Hashing, OfferMicroAdToYmlOffer}
import ru.yandex.realty.cost.plus.model.yml.YmlOfferData.{
  ExtendedNewbuildingSearchData,
  NewbuildingSearchData,
  OfferSearchData
}
import ru.yandex.realty.cost.plus.model.yml.{RawYmlOffer, WeakYmlSet, YmlPrice}
import ru.yandex.realty.cost.plus.service.builder.UrlViewBuilder.BuilderContext
import ru.yandex.realty.cost.plus.service.builder.impl.{CarouselViewUrlViewBuilder, SiteTablesUrlViewBuilder}
import ru.yandex.realty.cost.plus.service.builder.impl.SiteTablesUrlViewBuilder.MinRoomsToNameWithCategory
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.traffic.model.ad.MicroAdData.OfferData
import ru.yandex.realty.traffic.model.ad.{GrouppedByUrlAds, MicroAd, MicroAdData, MicroAdPrice, MicroAdRelevance}
import ru.yandex.realty.traffic.model.offer.{OfferRooms, OfferType}
import ru.yandex.realty.traffic.model.offer.OfferRooms.OfferRooms
import ru.yandex.realty.traffic.model.relevance.NormalizedRelevance
import ru.yandex.realty.traffic.model.urls.{ExtractedSourceUrl, RequestMeta}
import ru.yandex.realty.traffic.utils.CategoryTree
import zio.test.Assertion._
import zio.test._
import zio.test.junit._

@RunWith(classOf[ZTestJUnitRunner])
class TableViewSiteTableOffersBuilderSpec extends JUnitRunnableSpec {

  import ru.yandex.realty.cost.plus.testdata.TestMetaStorageSupplier._

  private val slobodaSet = {
    val meta = metaStorage.getForSite(ZhkSloboda.getId).flatMap(_.metaData).get
    WeakYmlSet(
      meta.total.primaryUrlPath,
      meta.tableTitle
    )
  }

  private val hashing = Hashing.Default

  private val justSet =
    WeakYmlSet(
      "/url/",
      "Some url"
    )

  private def offer(
    id: Long
  ): MicroAd =
    MicroAd(
      MicroAdData.OfferData(
        id.toString,
        offerType = OfferType.Sell,
        updateTime = Instant.ofEpochMilli(id),
        offerRooms = None,
        category = CategoryTree.Commercial,
        price = MicroAdPrice.Direct(10000),
        imageUrl = "image.url",
        flatType = None,
        title = s"commercial $id",
        area = None,
        fromAgent = None,
        isYandexRent = false,
        isExtendedOffer = false
      ),
      MicroAdRelevance.currentOnly(NormalizedRelevance.wrapUnsafe(id.toDouble / 100.0))
    )

  private def baseSpec(site: Site, expected: Seq[RawYmlOffer], toSet: WeakYmlSet, topAds: Seq[MicroAd] = Seq.empty) =
    test(s"Correctly return ${expected.size} offers for ${site.getName}") {
      assert {
        new SiteTablesUrlViewBuilder(new CarouselViewUrlViewBuilder(hashing), hashing)
          .buildYmlOffers(
            GrouppedByUrlAds(
              ExtractedSourceUrl(
                "key",
                toSet.path,
                toSet.title,
                RequestMeta
                  .SiteMainPageRequestMeta(site.getId, metaStorage.getForSite(site.getId).flatMap(_.metaData).get)
              ),
              topAds = topAds,
              experimentTopAds = Seq.empty,
              totalAds = topAds.size
            ),
            BuilderContext.Empty
          )
      }(hasSameElements(expected))
    }

  private def slobodaExpected(headerSet: WeakYmlSet): Seq[RawYmlOffer] = {
    val meta = metaStorage.getForSite(ZhkSloboda.getId).flatMap(_.metaData).get

    def roomsOffer(rooms: OfferRooms) = {
      val roomMeta = meta.byRooms(rooms)

      val (category, name) = MinRoomsToNameWithCategory(rooms)

      RawYmlOffer(
        name = name,
        url = RawYmlOffer.Url(roomMeta.primaryUrlPath, anchor = Some("flats"), from = Set(hashing.get(headerSet.path))),
        price = YmlPrice(roomMeta.stat.primary.get.minPrice.value, isFrom = true),
        categoryId = category.id,
        imageUrl = s"https:${ZhkSloboda.getMainPhoto.getUrlPrefix}/wiz_t2",
        additionalData = NewbuildingSearchData(
          offersCount = roomMeta.stat.primary.get.offersCount.value,
          areaFrom = roomMeta.stat.primary.get.areaFrom,
          rooms = Some(rooms),
          isTableRow = true
        ),
        setRequired = true
      )
    }

    Seq(
      RawYmlOffer(
        name = meta.tableTitle,
        url = RawYmlOffer.Url(headerSet.path),
        price = YmlPrice(meta.total.stat.primary.get.minPrice.value, isFrom = true),
        categoryId = CategoryTree.Site.id,
        imageUrl = meta.mainPhoto,
        additionalData = ExtendedNewbuildingSearchData(meta, meta.total.stat.primary.get.offersCount.value),
        setRequired = false
      )
    ) ++ Seq(OfferRooms._1, OfferRooms._2, OfferRooms._3, OfferRooms.Studio).map(roomsOffer)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("TableViewSiteTableOffersBuilder")(
      baseSpec(ZhkLyublinskijPark, Seq.empty, justSet),
      baseSpec(ZhkTest, Seq.empty, justSet),
      baseSpec(ZhkSloboda, slobodaExpected(slobodaSet), SlobodaHeaderSet),
      suite("correctly return carousel fallback") {
        val inputOffers =
          (1 to 10).map(offer(_))

        val meta = metaStorage.getForSite(ZhkLyublinskijPark.getId).get.metaData.get

        def makeExpected(ad: MicroAd) =
          Some(ad.data).collectFirst {
            case o: OfferData =>
              val converted = OfferMicroAdToYmlOffer
                .toYmlOffer(
                  justSet.path,
                  RequestMeta.SiteMainPageRequestMeta(
                    ZhkLyublinskijPark.getId,
                    meta
                  ),
                  o,
                  hashing
                )

              converted.copy(
                url = RawYmlOffer.Url(
                  meta.total.offersListingUrlPath,
                  pinnedOfferId = Some(o.offerId),
                  from = Set("table_view_type", hashing.get(justSet.path))
                ),
                additionalData = converted.additionalData
                  .asInstanceOf[OfferSearchData]
                  .copy(
                    offerForSiteTable = true
                  ),
                setRequired = true
              )
          }

        val expected = inputOffers
          .sortBy(_.relevance.current.value)
          .reverse
          .flatMap(makeExpected)

        baseSpec(ZhkLyublinskijPark, expected, justSet, topAds = inputOffers)
      }
    )
}
