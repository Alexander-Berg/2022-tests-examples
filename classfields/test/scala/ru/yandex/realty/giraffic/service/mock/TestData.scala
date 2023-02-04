package ru.yandex.realty.giraffic.service.mock

import play.api.libs.json.Json
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.application.service.Prometheus.Prometheus
import ru.yandex.realty.canonical.base.params.RequestParameter
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.clients.router.parser.ListingFiltersParser
import ru.yandex.realty.giraffic.service.CountManager.CountManager
import ru.yandex.realty.giraffic.service.CountManager
import ru.yandex.realty.giraffic.utils.ActionTimeObserver
import ru.yandex.realty.giraffic.utils.ActionTimeObserver.ActionTimeObserver
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.traffic.service.FrontendRouter
import ru.yandex.realty.traffic.service.FrontendRouter.FrontendRouter
import ru.yandex.realty.traffic.service.RegionService.RegionService
import ru.yandex.realty.urls.router.model.filter.ListingFilters
import ru.yandex.realty.util.IOUtil
import ru.yandex.vertis.ops.prometheus.SimpleCompositeCollector
import zio.clock.Clock
import zio.{ULayer, ZLayer}

import java.io.InputStream
import java.util.zip.GZIPInputStream

object TestData {

  lazy val testPrometheus: ULayer[Prometheus] = ZLayer.succeed(new SimpleCompositeCollector)
  lazy val actionObserver: ULayer[ActionTimeObserver] = testPrometheus ++ Clock.live >>> ActionTimeObserver.live

  private def processResource[T](name: String)(f: InputStream => T): T =
    IOUtil.using(getClass.getClassLoader.getResourceAsStream(name))(f)

  lazy val routerFilters: ListingFilters =
    processResource("filters.gzip.data") { is =>
      ListingFiltersParser
        .parse(
          Json.parse(
            new GZIPInputStream(is)
          )
        )
        .get
    }

  lazy val routerFiltersProvider: Provider[ListingFilters] = () => routerFilters

  lazy val regionServiceLayer: ULayer[RegionService] =
    ru.yandex.realty.traffic.TestData.regionServiceLayer

  lazy val siteService = ru.yandex.realty.traffic.TestData.sitesService

  val MoscowSellApartmentReq: Request = Request.Raw(
    RequestType.Search,
    Seq(
      RequestParameter.Rgid(NodeRgid.MOSCOW),
      RequestParameter.Type(OfferType.SELL),
      RequestParameter.Category(CategoryType.APARTMENT)
    )
  )

  val MoscowSellApartmentArbatReq: Request = Request.Raw(
    RequestType.Search,
    Seq(
      RequestParameter.Rgid(NodeRgid.MOSCOW),
      RequestParameter.Type(OfferType.SELL),
      RequestParameter.Category(CategoryType.APARTMENT),
      RequestParameter.StreetId(75348)
    )
  )

  val MoscowSellApartmentWithFurnitureReq: Request = Request.Raw(
    RequestType.Search,
    Seq(
      RequestParameter.Rgid(NodeRgid.MOSCOW),
      RequestParameter.Type(OfferType.SELL),
      RequestParameter.Category(CategoryType.APARTMENT),
      RequestParameter.HasFurniture(true)
    )
  )

  val MissingTypeApartmentMoscow: Request = Request.Raw(
    RequestType.Search,
    Seq(
      RequestParameter.Rgid(NodeRgid.MOSCOW),
      RequestParameter.Category(CategoryType.APARTMENT)
    )
  )

  val MoscowRentLotReq: Request = Request.Raw(
    RequestType.Search,
    Seq(
      RequestParameter.Rgid(NodeRgid.MOSCOW),
      RequestParameter.Type(OfferType.RENT),
      RequestParameter.Category(CategoryType.LOT)
    )
  )

  val MoscowWithStreetIdOnlyReq: Request =
    Request.Raw(
      RequestType.Search,
      Seq(
        RequestParameter.Rgid(NodeRgid.MOSCOW),
        RequestParameter.StreetId(123)
      )
    )

  case class KnownUrl(
    parsePath: String,
    request: Request,
    translatedPath: Option[String],
    offersCount: Int
  )

  object KnownUrl {

    def apply(parseAndTranslateUrl: String, request: Request, offersCount: Int): KnownUrl =
      new KnownUrl(parseAndTranslateUrl, request, Some(parseAndTranslateUrl), offersCount)
  }

  val MoscowSellApartmentKnown: KnownUrl = KnownUrl("/moskva/kupit/kvartira/", MoscowSellApartmentReq, 100)

  val MoscowSellApartmentArbat: KnownUrl =
    KnownUrl("/moskva/kupit/kvartira/st-ulica-arbat-75348/", MoscowSellApartmentArbatReq, 100)

  val MoscowSellApartmentArbatBuildings: Seq[KnownUrl] = Range.inclusive(1, 100).map { id =>
    KnownUrl(
      s"/moskva/kupit/kvartira/st-ulica-arbat-75348/dom-$id-$id/",
      Request.Raw(
        RequestType.Search,
        Seq(
          RequestParameter.Rgid(NodeRgid.MOSCOW),
          RequestParameter.Type(OfferType.SELL),
          RequestParameter.Category(CategoryType.APARTMENT),
          RequestParameter.StreetId(75348),
          RequestParameter.BuildingId(id.toLong),
          RequestParameter.HouseNumber(id.toString)
        )
      ),
      100
    )
  }

  val WithOffersAndNoCanonicalUrl: KnownUrl =
    KnownUrl("/moskva/?categoryType=APARTMENT", MissingTypeApartmentMoscow, None, 100)
  val CanonicalWithoutOffersUrl: KnownUrl = KnownUrl("/moskva/snyat/uchastok/", MoscowRentLotReq, offersCount = 0)

  val NoOffersNoCanonicalUrl: KnownUrl =
    KnownUrl("/moskva/?streetId=123", MoscowWithStreetIdOnlyReq, None, offersCount = 0)

  val TestKnownUrls = Seq(
    MoscowSellApartmentKnown,
    WithOffersAndNoCanonicalUrl,
    CanonicalWithoutOffersUrl,
    NoOffersNoCanonicalUrl,
    MoscowSellApartmentArbat
  ) ++ MoscowSellApartmentArbatBuildings

  val routerLive: ULayer[FrontendRouter] = {
    val translateMap =
      TestKnownUrls.map { url =>
        url.request -> url.translatedPath
      }.toMap

    val parseMap =
      TestKnownUrls.map { url =>
        url.parsePath -> url.request
      }.toMap
    ZLayer.succeed(
      new TestFrontendRouter(parseMap, translateMap): FrontendRouter.Service
    )
  }

  val countsLive: ULayer[CountManager] = {
    val countsMap =
      TestKnownUrls.map { url =>
        url.request -> url.offersCount
      }.toMap

    ZLayer.succeed(new TestOfferCountManager(countsMap): CountManager.Service)
  }
}
