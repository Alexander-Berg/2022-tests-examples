package ru.yandex.realty.misc.action.deeplink

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.application.ng.favorites.FavoritesServiceClient
import ru.yandex.realty.clients.MortgagesClient
import ru.yandex.realty.clients.searcher.SearcherClient
import ru.yandex.realty.clients.suggest.GeoSuggestClient
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.geocoder.LocationUnifierService
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.http.HttpClientMock
import ru.yandex.realty.managers.favorites.FavoriteShortLinkService
import ru.yandex.realty.misc.servantlets.autocomplete.MobileAutocompleteResponseBuilder
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.sites.{CompaniesStorage, SitesGroupingService}
import ru.yandex.realty.storage.AgencyProfileStorage
import ru.yandex.realty.tags.TagsRuntime
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.HighwayLocator
import ru.yandex.realty.util.geo.GeoHelper
import ru.yandex.vertis.util.concurrent.Threads

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class ParseDeeplinkActionSpec extends SpecBase with HttpClientMock {
  implicit val ex: ExecutionContext = Threads.SameThreadEc

  val regionGraphProvider: Provider[RegionGraph] = mock[Provider[RegionGraph]]
  val geoHelperProvider: Provider[GeoHelper] = mock[Provider[GeoHelper]]
  val tagsProvider: Provider[TagsRuntime] = mock[Provider[TagsRuntime]]
  val sitesGroupingService: SitesGroupingService = mock[SitesGroupingService]
  val highwayLocatorProvider: Provider[HighwayLocator] = mock[Provider[HighwayLocator]]
  val responseBuilder: MobileAutocompleteResponseBuilder = mock[MobileAutocompleteResponseBuilder]
  val locationUnifierService: LocationUnifierService = mock[LocationUnifierService]
  val companiesProvider: Provider[CompaniesStorage] = mock[Provider[CompaniesStorage]]
  val geoSuggestClient: GeoSuggestClient = mock[GeoSuggestClient]
  val agencyProfileStorage: Provider[AgencyProfileStorage] = mock[Provider[AgencyProfileStorage]]
  val vosClient: VosClientNG = mock[VosClientNG]
  val favoriteShortLinkService: FavoriteShortLinkService = mock[FavoriteShortLinkService]
  val favoritesServiceClient: FavoritesServiceClient = mock[FavoritesServiceClient]
  val mortgagesClient: MortgagesClient = mock[MortgagesClient]
  val searcherClient: SearcherClient = mock[SearcherClient]

  val parseDeeplinkAction = new ParseDeeplinkAction(
    regionGraphProvider,
    geoHelperProvider,
    tagsProvider,
    sitesGroupingService,
    highwayLocatorProvider,
    responseBuilder,
    locationUnifierService,
    companiesProvider,
    geoSuggestClient,
    agencyProfileStorage,
    vosClient,
    favoriteShortLinkService,
    favoritesServiceClient,
    mortgagesClient,
    searcherClient
  )(ex)

  "parseDeeplinkAction " should {
    "return only offers from lucene when actualized " in {
      val offerIds = Seq("01", "02", "03")

      val searcherFullResponse =
        Seq(
          UnifiedOffer.getDefaultInstance.toBuilder.setOfferId("01").build(),
          UnifiedOffer.getDefaultInstance.toBuilder.setOfferId("02").build()
        )

      (searcherClient
        .getUnifiedOffersByIds(_: Seq[String], _: Boolean, _: Boolean)(_: Traced))
        .expects(*, *, *, *)
        .returning(Future(searcherFullResponse))
        .once()
      val actualizedOffers = Await.result(parseDeeplinkAction.actualizeOfferIds(offerIds)(Traced.empty), 1.seconds)

      actualizedOffers shouldBe searcherFullResponse.map(_.getOfferId)
    }
    "don't send request to searcher when offers seq is empty " in {
      val offerIds = Seq.empty[String]
      val actualizedOffers = Await.result(parseDeeplinkAction.actualizeOfferIds(offerIds)(Traced.empty), 1.seconds)
      actualizedOffers shouldBe offerIds
    }
  }
}
