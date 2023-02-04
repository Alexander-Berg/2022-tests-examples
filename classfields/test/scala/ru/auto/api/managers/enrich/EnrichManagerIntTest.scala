package ru.auto.api.managers.enrich

import akka.actor.ActorSystem
import org.scalatest.Ignore
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.managers.aliases.AliasesManager
import ru.auto.api.managers.auction.DealerCallsAuctionManager
import ru.auto.api.managers.billing.schedule.ScheduleManager
import ru.auto.api.managers.calltracking.CalltrackingManager
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.managers.counters.CountersManager
import ru.auto.api.managers.delivery.DeliveryRegionProductPriceManager
import ru.auto.api.managers.dictionaries.DictionariesManager
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.favorite.FavoriteManager
import ru.auto.api.managers.incite.InciteManager
import ru.auto.api.managers.matchapplications.MatchApplicationsManager
import ru.auto.api.managers.offers.PhoneRedirectManager
import ru.auto.api.managers.price.{DealerPriceManager, UserPriceManager}
import ru.auto.api.model.moderation.Placeholders
import ru.auto.api.model.{ModelGenerators, RequestParams, UserRef}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.services.catalog.CatalogClient
import ru.auto.api.services.counter.CounterClient
import ru.auto.api.services.dealer_pony.DealerPonyClient
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.incite.InciteClient
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.safedeal.SafeDealClient
import ru.auto.api.services.salesman.{SalesmanClient, SalesmanUserClient}
import ru.auto.api.services.searcher.DefaultSearcherClient
import ru.auto.api.services.shark.SharkClient
import ru.auto.api.services.telepony.TeleponyClient
import ru.auto.api.services.web.MockedFeatureManager
import ru.auto.api.testkit.TestData
import ru.auto.api.util.concurrency.FutureTimeoutHandler
import ru.auto.api.util.{EmptyEnrichesCompletionMeter, Request, RequestImpl, UrlBuilder}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.auto.api.util.crypt.TypedCrypto
import ru.auto.api.model.AutoruUser

/**
  * Created by mcsim-gr on 09.08.17.
  */
@Ignore
class EnrichManagerIntTest extends HttpClientSuite with MockitoSupport with MockedFeatureManager {

  override protected def config: HttpClientConfig =
    HttpClientConfig("auto2-searcher-api.vrts-slb.test.vertis.yandex.net", 80)

  val catalogClient = mock[CatalogClient]
  val dictionariesManager = mock[DictionariesManager]
  val searcherClient = new DefaultSearcherClient(http, featureManager)
  val catalogManager = new CatalogManager(catalogClient, searcherClient, featureManager, dictionariesManager)

  private val aliasesManager = mock[AliasesManager]
  private val userPriceManager = mock[UserPriceManager]
  private val dealerPriceManager = mock[DealerPriceManager]
  private val dealerCallsAuctionManager = mock[DealerCallsAuctionManager]
  private val counterClient = mock[CounterClient]
  private val countersManger = mock[CountersManager]
  private val favoriteClient = mock[FavoriteManager]
  private val phoneRedirectManager = mock[PhoneRedirectManager]
  private val passportClient = mock[PassportClient]
  private val scheduleManager = mock[ScheduleManager]
  private val salesmanClient = mock[SalesmanClient]
  private val salesmanUserClient = mock[SalesmanUserClient]
  private val dealerPonyClient = mock[DealerPonyClient]
  private val calltrackingManager = mock[CalltrackingManager]
  private val productPriceManager = mock[DeliveryRegionProductPriceManager]
  private val geobaseClient = mock[GeobaseClient]
  private val sharkClient = mock[SharkClient]
  private val safeDealClient = mock[SafeDealClient]
  private val matchApplicationsManager = mock[MatchApplicationsManager]
  private val teleponyClient = mock[TeleponyClient]
  private val fakeManager = mock[FakeManager]
  private val inciteClient = mock[InciteClient]
  private val inciteManager = mock[InciteManager]
  private val apiWebAddress = "https://test.avto.ru"
  private val cryptoUserId = mock[TypedCrypto[AutoruUser]]
  when(cryptoUserId.encrypt(? : AutoruUser)).thenAnswer(_.getArgument[AutoruUser](0).toString.reverse)

  private val urlBuilder =
    new UrlBuilder(
      "http://www-desktop.test.autoru.yandex.net",
      "http://m.test.avto.ru",
      "https://parts.test.avto.ru",
      cryptoUserId
    )
  private val placeholderUtils = new Placeholders(urlBuilder)

  implicit private val system: ActorSystem = ActorSystem()

  private val manager = new EnrichManager(
    () => calltrackingManager,
    userPriceManager,
    dealerPriceManager,
    dealerCallsAuctionManager,
    countersManger,
    searcherClient,
    favoriteClient,
    phoneRedirectManager,
    scheduleManager,
    salesmanClient,
    dealerPonyClient,
    salesmanUserClient,
    urlBuilder,
    placeholderUtils,
    passportClient,
    TestData,
    catalogManager,
    featureManager,
    productPriceManager,
    geobaseClient,
    new EmptyEnrichesCompletionMeter(),
    sharkClient,
    safeDealClient,
    matchApplicationsManager,
    aliasesManager,
    teleponyClient,
    new FutureTimeoutHandler,
    fakeManager,
    inciteManager,
    apiWebAddress,
    cryptoUserId = cryptoUserId
  )

  implicit private val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setUser(UserRef.anon("42"))
    r
  }

  test("enrich car offer with TechParams") {
    val offer = ModelGenerators.OfferGen.next.toBuilder.setCategory(Category.CARS).build
    val feature: Feature[Boolean] = mock[Feature[Boolean]]
    when(feature.value).thenReturn(false)
    when(featureManager.catalogApiOfferEnrich).thenReturn(feature)

    val info = offer.getCarInfo
    assume(info.hasTechParamId, "Expected tech_param_id")
    assume(!info.hasMarkInfo, "mark_info not expected")
    assume(!info.hasModelInfo, "model_info not expected")

    val result = manager.enrich(offer, EnrichOptions(techParams = true)).futureValue

    result.getCarInfo.hasMarkInfo shouldBe true
    result.getCarInfo.hasModelInfo shouldBe true
    result.getCarInfo.hasSuperGen shouldBe true
    result.getCarInfo.hasConfiguration shouldBe true
    result.getCarInfo.hasTechParam shouldBe true
  }
}
