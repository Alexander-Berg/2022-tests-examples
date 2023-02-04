package ru.auto.api.managers.enrich

import akka.actor.ActorSystem
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito._
import org.mockito.internal.stubbing.defaultanswers.ReturnsMocks
import org.scalacheck.Gen
import org.scalactic.source.Position
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.time.{Millis, Second, Seconds, Span}
import org.scalatest.{Inspectors, OptionValues}
import ru.auto.api.ApiOfferModel._
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.PaidService
import ru.auto.api.CommonModel.PaidService.Activator
import ru.auto.api.CommonModel.PaidService.Activator.{OWNER, PACKAGE_TURBO}
import ru.auto.api.auth.Application
import ru.auto.api.currency.CurrencyRates
import ru.auto.api.extdata.DataService
import ru.auto.api.features.FeatureManager
import ru.auto.api.features.FeatureManager.DealerVasProductsFeatures
import ru.auto.api.managers.aliases.AliasesManager
import ru.auto.api.managers.auction.DealerCallsAuctionManager
import ru.auto.api.managers.billing.schedule.ScheduleManager
import ru.auto.api.managers.billing.schedule.ScheduleManager.ForOffers
import ru.auto.api.managers.calltracking.CalltrackingManager
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.managers.counters.CountersManager
import ru.auto.api.managers.delivery.DeliveryRegionProductPriceManager
import ru.auto.api.managers.enrich.EnrichManagerSpec.AllowedUserOffersShowTag
import ru.auto.api.managers.enrich.enrichers.DailyCountersEnricher.{DailyCountersParams, DefaultDailyCountersPeriod}
import ru.auto.api.managers.enrich.enrichers.PositionsEnricher
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.favorite.FavoriteManager
import ru.auto.api.managers.incite.InciteManager
import ru.auto.api.managers.matchapplications.MatchApplicationsManager
import ru.auto.api.managers.offers.PhoneRedirectManager
import ru.auto.api.managers.price.{DealerPriceManager, UserPriceManager}
import ru.auto.api.metro.MetroBase
import ru.auto.api.model.AutoruProduct._
import ru.auto.api.model.CommonModelUtils._
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.model.gen.SalesmanModelGenerators._
import ru.auto.api.model.moderation.Placeholders
import ru.auto.api.model.searcher.CatalogUtils._
import ru.auto.api.services.dealer_pony.DealerPonyClient
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.incite.InciteClient
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.safedeal.SafeDealClient
import ru.auto.api.services.salesman.{SalesmanClient, SalesmanUserClient}
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.shark.SharkClient
import ru.auto.api.services.telepony.TeleponyClient
import ru.auto.api.testkit.TestData
import ru.auto.api.util._
import ru.auto.api.util.concurrency.FutureTimeoutHandler
import ru.auto.api.util.crypt.TypedCrypto
import ru.auto.salesman.model.user.ApiModel
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import java.time.{LocalDate, ZoneId}
import java.util.Currency
import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import scala.util.Random

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 02.03.17
  */
class EnrichManagerSpec extends BaseSpec with MockitoSupport with OptionValues with ScalaCheckPropertyChecks {

  private val aliasesManager = mock[AliasesManager]
  private val userPriceManager = mock[UserPriceManager]
  private val dealerPriceManager = mock[DealerPriceManager]
  private val dealerCallsAuctionManager = mock[DealerCallsAuctionManager]
  private val countersManager = mock[CountersManager]
  private val searcherClient = mock[SearcherClient]
  private val favoriteManager = mock[FavoriteManager]
  private val phoneRedirectManager = mock[PhoneRedirectManager]
  private val scheduleManager = mock[ScheduleManager]
  private val salesmanClient = mock[SalesmanClient]
  private val dealerPonyClient = mock[DealerPonyClient]
  private val salesmanUserClient = mock[SalesmanUserClient]
  private val passportClient = mock[PassportClient]
  private val calltrackingManager = mock[CalltrackingManager]
  private val ownershipChecker = mock[OwnershipChecker]
  private val catalogManager = mock[CatalogManager]
  private val featureManager = mock[FeatureManager]
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

  implicit private val actorSystem: ActorSystem = ActorSystem()
  private val futureTimeoutHandler = new FutureTimeoutHandler

  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  when(featureManager.autoComputeMaxDiscount).thenReturn(feature)

  when(featureManager.chatsDontTextMe).thenReturn {
    new Feature[Boolean] {
      override def name: String = "chats_dont_text_me"
      override def value: Boolean = true
    }
  }

  when(featureManager.enrichOnlyForHumans).thenReturn {
    new Feature[Boolean] {
      override def name: String = "enrich_only_for_humans"
      override def value: Boolean = false
    }
  }

  when(featureManager.dealerVasFeatures).thenReturn(Seq.empty[Feature[String]])

  when(featureManager.enableShowInStories).thenReturn {
    new Feature[Boolean] {
      override def name: String = "use_show_in_stories_vos"
      override def value: Boolean = true
    }
  }

  when(featureManager.calltrackingCountersDedicatedEnricher).thenReturn {
    new Feature[Boolean] {
      override def name: String = "calltracking_counters_dedicated_enricher"
      override def value: Boolean = true
    }
  }

  when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
  when(fakeManager.shouldTakeFakeOfferPhone(?)(?)).thenReturn(false)

  private val urlBuilder =
    new UrlBuilder(
      "http://www-desktop.test.autoru.yandex.net",
      "http://m.test.avto.ru",
      "https://parts.test.avto.ru",
      cryptoUserId
    )

  private val placeholders = new Placeholders(urlBuilder)

  implicit private val trace: Traced = Traced.empty

  implicit private val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(SessionID("test_session"))))
    r.setUser(UserRef.anon("42"))
    r.setApplication(Application.desktop)
    r
  }

  private val userRequest = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(SessionID("test_session"))))
    val user = AutoruUser(42)
    r.setUser(user)
    r.setApplication(Application.desktop)
    r
  }

  private val dealerRequest = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(SessionID("test_session"))))
    val dealer = AutoruDealer(42)
    r.setUser(dealer)
    r.setDealer(dealer)
    r.setApplication(Application.desktop)
    r
  }

  private def enrichManager(userPriceManager: UserPriceManager = userPriceManager,
                            scheduleManager: ScheduleManager = scheduleManager,
                            dataService: DataService = TestData) = new EnrichManager(
    () => calltrackingManager,
    userPriceManager,
    dealerPriceManager,
    dealerCallsAuctionManager,
    countersManager,
    searcherClient,
    favoriteManager,
    phoneRedirectManager,
    scheduleManager,
    salesmanClient,
    dealerPonyClient,
    salesmanUserClient,
    urlBuilder,
    placeholders,
    passportClient,
    dataService,
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
    futureTimeoutHandler,
    fakeManager,
    inciteManager,
    apiWebAddress,
    cryptoUserId = cryptoUserId
  )

  private val manager = enrichManager()

  implicit override def patienceConfig: PatienceConfig = PatienceConfig(interval = Span(400, Millis))

  "EnrichManager" should {
    //todo(darl) enrich with region info

    "enrich single offer with counters" in {
      val offer = ModelGenerators.OfferGen.next
      val callsCountersPattern = offer.getCounters
      val counters = ModelGenerators.CountersGen.next
      val id = offer.id

      when(countersManager.getCounters(?, ?, ?, ?)(?)).thenReturnF(Map(id -> counters))

      val result = manager.enrich(offer, EnrichOptions(counters = true)).futureValue
      result.getCounters.getAll shouldBe counters.getAll
      result.getCounters.getDaily shouldBe counters.getDaily
      result.getCounters.getPhoneAll shouldBe counters.getPhoneAll
      result.getCounters.getPhoneDaily shouldBe counters.getPhoneDaily
      result.getCounters.getCallsAll shouldBe callsCountersPattern.getCallsAll
      result.getCounters.getCallsDaily shouldBe callsCountersPattern.getCallsDaily

      verify(countersManager).getCounters(eq(Seq(offer)), eq(false), eq(false), ?)(?)
    }

    "enrich with unescaped offer description" in {
      val offer = ModelGenerators.OfferGen.next.toBuilder.setDescription("&amp;&quot;").build()
      val result = manager.enrich(offer, EnrichOptions()).futureValue
      result.getDescription shouldBe "&\""
    }

    "return unmodified offer if counters request returns an error" in {
      val offer = ModelGenerators.OfferGen.next
      val callsCountersPattern = offer.getCounters

      when(countersManager.getCounters(?, ?, ?, ?)(?)).thenThrowF(new Exception())

      val result = manager.enrich(offer, EnrichOptions(counters = true)).futureValue
      result.getCounters.hasAll shouldBe false
      result.getCounters.hasDaily shouldBe false
      result.getCounters.hasPhoneAll shouldBe false
      result.getCounters.hasPhoneDaily shouldBe false
      result.getCounters.getCallsAll shouldBe callsCountersPattern.getCallsAll
      result.getCounters.getCallsDaily shouldBe callsCountersPattern.getCallsDaily

      verify(countersManager).getCounters(eq(Seq(offer)), eq(false), eq(false), ?)(?)
    }

    "enrich single user non-prolongable offer with paid service prices" in {
      val offer = ModelGenerators.PrivateOfferGen.next
        .updated(_.clearServicePrices())
      val servicePrices = ModelGenerators.PaidNonProlongableServicePriceGen.list

      when(userPriceManager.getMultipleOffersPrices(?, ?, ?, ?)(?)).thenReturnF(Map(offer.id -> servicePrices))

      val result = manager.enrich(offer, EnrichOptions(paidServicePrices = true)).futureValue

      result.getServicePricesList.asScala.map(_.toBuilder.setRecommendationPriority(0).build) shouldBe servicePrices

      verify(userPriceManager)
        .getMultipleOffersPrices(
          Seq(offer),
          autoruUserProducts(featureManager.enableShowInStories),
          applyMoneyFeature = false,
          isNewDraft = false
        )(request)
    }

    "enrich single user non-prolongable offer with paid service prices excluding ShowInStories" in {
      when(featureManager.enableShowInStories).thenReturn {
        new Feature[Boolean] {
          override def name: String = "enable_show_in_stories"
          override def value: Boolean = false
        }
      }

      val offer = ModelGenerators.PrivateOfferGen.next
        .updated(_.clearServicePrices())
      val servicePrices = ModelGenerators.PaidNonProlongableServicePriceGen.list

      when(userPriceManager.getMultipleOffersPrices(?, ?, ?, ?)(?)).thenReturnF(Map(offer.id -> servicePrices))

      val result = manager.enrich(offer, EnrichOptions(paidServicePrices = true)).futureValue

      result.getServicePricesList.asScala.map(_.toBuilder.setRecommendationPriority(0).build) shouldBe servicePrices

      verify(userPriceManager)
        .getMultipleOffersPrices(
          Seq(offer),
          autoruUserProducts(featureManager.enableShowInStories),
          applyMoneyFeature = false,
          isNewDraft = false
        )(request)
    }

    "enrich single user prolongable with original price with paid service prices" in {
      pending // Тест падает всегда, потому что цена в AutoProlongPrice не проставляется.
      val offer = ModelGenerators.PrivateOfferGen.next
        .updated(_.clearServicePrices())
      val servicePrices = ModelGenerators.PaidProlongableServicePriceGen.list

      when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
      when(userPriceManager.getMultipleOffersPrices(?, ?, ?, ?)(?)).thenReturnF(Map(offer.id -> servicePrices))

      val result = manager.enrich(offer, EnrichOptions(paidServicePrices = true)).futureValue

      Inspectors.forEvery(servicePrices.zip(result.getServicePricesList.asScala)) {
        case (was, is) =>
          was.getOriginalPrice shouldBe is.getAutoProlongPrice.getValue
      }

      val resultBuilder = result.toBuilder
      resultBuilder.getServicePricesBuilderList.asScala.foreach(b => {
        b.clearAutoProlongPrice
        b.setRecommendationPriority(0)
      })
      resultBuilder.build().getServicePricesList.asScala shouldBe servicePrices

      verify(userPriceManager).getMultipleOffersPrices(
        Seq(offer),
        autoruUserProducts(featureManager.enableShowInStories),
        applyMoneyFeature = false,
        isNewDraft = false
      )
    }

    "enrich single user prolongable without original price with paid service prices" in {
      pending // Тест падает всегда, потому что цена в AutoProlongPrice не проставляется.
      val offer = ModelGenerators.PrivateOfferGen.next
        .updated(_.clearServicePrices())
      val servicePrices = ModelGenerators.NoOriginalPricePaidProlongableServicePriceGen.list

      when(fakeManager.shouldFakeRequest(?)).thenReturn(false)

      when(userPriceManager.getMultipleOffersPrices(?, ?, ?, ?)(?)).thenReturnF(Map(offer.id -> servicePrices))

      val result = manager.enrich(offer, EnrichOptions(paidServicePrices = true)).futureValue

      Inspectors.forEvery(servicePrices.zip(result.getServicePricesList.asScala)) {
        case (was, is) =>
          was.getPrice shouldBe is.getAutoProlongPrice.getValue
      }

      val resultBuilder = result.toBuilder
      resultBuilder.getServicePricesBuilderList.asScala.foreach(b => {
        b.clearAutoProlongPrice
        b.setRecommendationPriority(0)
      })
      resultBuilder.build().getServicePricesList.asScala shouldBe servicePrices

      verify(userPriceManager).getMultipleOffersPrices(
        Seq(offer),
        autoruUserProducts(featureManager.enableShowInStories),
        applyMoneyFeature = false,
        isNewDraft = false
      )
    }

    "enrich single dealer cars:new offer with paid service prices" in {
      val offer = ModelGenerators.DealerCarsNewOfferGen.next
        .updated(_.clearServicePrices())

      val regionId = TestData.tree.unsafeFederalSubject(offer.getSalon.getPlace.getGeobaseId).id

      val servicePrices = ModelGenerators.PaidServicePriceGen.list
      when(dealerPriceManager.getDealerPrices(?, ?)(?)).thenReturnF(servicePrices)
      when(salesmanClient.getAvailableCallTariffs(?)(?)).thenReturnF(List())
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List(regionId))
        )
      )

      val result = manager.enrich(offer, EnrichOptions(paidServicePrices = true))(dealerRequest).futureValue
      result.getServicePricesList.asScala shouldBe servicePrices

      verify(dealerPriceManager).getDealerPrices(
        offer,
        List(Boost, Premium, SpecialOffer, Badge)
      )(dealerRequest)
    }

    "enrich single dealer cars:used offer with paid service prices including turbo package" in {
      val offer = ModelGenerators.DealerCarsUsedOfferGen.next
        .updated(_.clearServicePrices())

      val regionId = TestData.tree.unsafeFederalSubject(offer.getSalon.getPlace.getGeobaseId).id

      val servicePrices = ModelGenerators.PaidServicePriceGen.list
      when(dealerPriceManager.getDealerPrices(?, ?)(?)).thenReturnF(servicePrices)
      when(salesmanClient.getAvailableCallTariffs(?)(?)).thenReturnF(List())
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List(regionId))
        )
      )

      val result = manager.enrich(offer, EnrichOptions(paidServicePrices = true))(dealerRequest).futureValue
      result.getServicePricesList.asScala shouldBe servicePrices

      verify(dealerPriceManager).getDealerPrices(
        offer,
        List(Reset, Boost, PackageTurbo, Premium, SpecialOffer, Badge)
      )(dealerRequest)
    }

    "enrich single dealer moto offer with paid service prices including turbo package" in {
      val offer = ModelGenerators.DealerMotoOfferGen.next
        .updated(_.clearServicePrices())

      val servicePrices = ModelGenerators.PaidServicePriceGen.list
      when(dealerPriceManager.getDealerPrices(?, ?)(?)).thenReturnF(servicePrices)
      when(salesmanClient.getAvailableCallTariffs(?)(?)).thenReturnF(List())
      when(featureManager.disableVasForSingleWithCallsPaymentModel).thenReturn(Feature("", _ => true))
      val result = manager.enrich(offer, EnrichOptions(paidServicePrices = true))(dealerRequest).futureValue
      result.getServicePricesList.asScala shouldBe servicePrices

      verify(dealerPriceManager).getDealerPrices(
        eq(offer),
        argThat[Seq[AutoruProduct]](_.toSet == Set(Boost, Premium, Badge, SpecialOffer, PackageTurbo))
      )(eq(dealerRequest))
    }

    "enrich single car offer with tech params" in {
      val offer = ModelGenerators.OfferGen.next.toBuilder.setCategory(Category.CARS).build
      val info = offer.getCarInfo
      val breadcrumbs = ModelGenerators.breadcrumbsResponseGen(info).next

      assume(info.hasTechParamId, "Expected tech_param_id")
      assume(!info.hasMarkInfo, "mark_info not expected")
      assume(!info.hasModelInfo, "model_info not expected")

      when(fakeManager.shouldFakeRequest(?)).thenReturn(false)
      when(fakeManager.shouldFakeTechparams(?)).thenReturn(false)

      when(fakeManager.getBreadcrumbsWithFakeCheck(any[Seq[Offer]]())(?))
        .thenReturnF(Map(Category.CARS -> new RichBreadcrumbs(breadcrumbs)))
      when(fakeManager.getTechParamsWithFakeCheck(any[CatalogInfoProvider](), ?, ?)(?))
        .thenReturn(
          breadcrumbs
            .getTechParams(info.getTechParamId)
        )
      val result = manager.enrich(offer, EnrichOptions(techParams = true)).futureValue
      result.getCarInfo.getMarkInfo shouldBe breadcrumbs.getOptMark(info.getMark, None).get
      result.getCarInfo.getModelInfo shouldBe breadcrumbs.getOptModel(info.getMark, info.getModel, None).get
      result.getCarInfo.getSuperGen shouldBe breadcrumbs.getOptSuperGen(info.getSuperGenId).get
      result.getCarInfo.getConfiguration shouldBe breadcrumbs.getConfiguration(info.getConfigurationId).get
      result.getCarInfo.getTechParam shouldBe breadcrumbs.getTechParams(info.getTechParamId).get

      verify(fakeManager).getBreadcrumbsWithFakeCheck(Seq(offer))
    }

    "enrich single moto offer with tech params" in {
      val offer = ModelGenerators.MotoOfferGen.next
      val info = offer.getMotoInfo
      val breadcrumbs = ModelGenerators.breadcrumbsResponseGen(info.getMark, info.getModel).next

      assume(!info.hasMarkInfo, "mark_info not expected")
      assume(!info.hasModelInfo, "model_info not expected")

      when(fakeManager.getBreadcrumbsWithFakeCheck(any[Seq[Offer]]())(?))
        .thenReturnF(Map(Category.MOTO -> new RichBreadcrumbs(breadcrumbs)))
      val result = manager.enrich(offer, EnrichOptions(techParams = true)).futureValue
      result.getMotoInfo.getMarkInfo shouldBe breadcrumbs.getOptMark(info.getMark, None).get
      result.getMotoInfo.getModelInfo shouldBe breadcrumbs.getOptModel(info.getMark, info.getModel, None).get

      verify(fakeManager).getBreadcrumbsWithFakeCheck(Seq(offer))
    }

    "enrich single truck offer with tech params" in {
      val offer = ModelGenerators.TruckOfferGen.next
      val info = offer.getTruckInfo
      val breadcrumbs = ModelGenerators.breadcrumbsResponseGen(info.getMark, info.getModel).next

      assume(!info.hasMarkInfo, "mark_info not expected")
      assume(!info.hasModelInfo, "model_info not expected")

      when(fakeManager.getBreadcrumbsWithFakeCheck(any[Seq[Offer]]())(?))
        .thenReturnF(Map(Category.TRUCKS -> new RichBreadcrumbs(breadcrumbs)))
      val result = manager.enrich(offer, EnrichOptions(techParams = true)).futureValue
      result.getTruckInfo.getMarkInfo shouldBe breadcrumbs.getOptMark(info.getMark, None).get
      result.getTruckInfo.getModelInfo shouldBe breadcrumbs.getOptModel(info.getMark, info.getModel, None).get

      verify(fakeManager).getBreadcrumbsWithFakeCheck(Seq(offer))
    }

    "enrich single offer with offer position" in {
      val offer = ModelGenerators.OfferGen.next.updated(_.setStatus(OfferStatus.ACTIVE))
      val position = ModelGenerators.offerPositionGen.next

      assert(offer.getSearchPosition == 0, "search_position not expected")

      when(searcherClient.offerPosition(?, ?, ?)(?)).thenReturnF(position)
      when(searcherClient.simpleOfferPosition(?, ?, ?)(?)).thenReturnF(position)

      val result = manager.enrich(offer, EnrichOptions(offerPosition = true)).futureValue
      result.getSearchPosition shouldBe position.position
      result.getAdditionalInfo.getSimilarOffersCount shouldBe position.totalCount
      result.getAdditionalInfo.getSearchPositions(0).getTotalCount shouldBe position.totalCount
      result.getAdditionalInfo.getSearchPositions(0).getPositions(0).getPosition shouldBe position.position
      result.getAdditionalInfo.getSearchPositions(0).getPositions(1).getPosition shouldBe position.position

      verify(searcherClient).simpleOfferPosition(offer, PositionsEnricher.RelevanceSorting, true)
      verify(searcherClient).offerPosition(offer, PositionsEnricher.PriceSorting, true)
      verify(searcherClient).offerPosition(offer, PositionsEnricher.PriceSorting, false)
    }

    "enrich single offer with favorite info and notes" in {
      val note = "note"
      val fav = true
      val offer = ModelGenerators.OfferGen.next.updated(_.setIsFavorite(false).clearNote)
      val offerFromPersonalApi = offer.updated(_.setIsFavorite(true).setNote(note))
      when(favoriteManager.getFavoritesAndNotesByIds(?, ?)(?))
        .thenReturnF(Seq(offerFromPersonalApi))

      val result = manager.enrich(offer, EnrichOptions(notesAndFavorites = true)).futureValue
      result.getIsFavorite shouldBe fav
      result.getNote shouldBe note
      verify(favoriteManager).getFavoritesAndNotesByIds(request.user.personalRef, Seq(offer.id))(request)
    }

    "return unmodified offer if favorites request returns an error" in {
      val offer = ModelGenerators.OfferGen.next.updated(_.setIsFavorite(false).clearNote)

      when(favoriteManager.getFavoritesAndNotesByIds(?, ?)(?))
        .thenThrowF(new Exception())

      val result = manager.enrich(offer, EnrichOptions(notesAndFavorites = true)).futureValue
      result.getIsFavorite shouldBe false
      result.getNote shouldBe ""

      verify(favoriteManager).getFavoritesAndNotesByIds(request.user.personalRef, Seq(offer.id))(request)
    }

    "enrich single offer with price in currency" in {
      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      offerBuilder.getPriceInfoBuilder.setPrice(1000).setCurrency("RUR")
      val offer = offerBuilder.build()

      val result = manager.enrich(offer, EnrichOptions()).futureValue
      result.getPriceInfo.selectRurPrice shouldEqual 1000
      val expected = 17d // math.rint(1000 / 58.3370)
      result.getPriceInfo.selectUsdPrice shouldEqual (expected +- 0.01f)
      result.getPriceInfo.selectEurPrice shouldEqual 0 // exchange rate undefined
    }

    "correctly handle offer with non RUR price" in {
      val USD = Currency.getInstance("USD")
      val EUR = Currency.getInstance("EUR")
      val RUR = Currency.getInstance("RUR")

      val offerBuilder = ModelGenerators.OfferGen.next.toBuilder
      offerBuilder.getPriceInfoBuilder.setPrice(1000).setCurrency("USD")
      val offer = offerBuilder.build()

      val currency = CurrencyRates(Map(USD -> 55d, EUR -> 60d, RUR -> 1d))
      val testData = mock[DataService](new ReturnsMocks)
      when(testData.currency).thenReturn(currency)
      when(testData.tree).thenReturn(TestData.tree)
      val manager = enrichManager(dataService = testData)

      val result = manager.enrich(offer, EnrichOptions()).futureValue
      result.getPriceInfo.selectUsdPrice shouldEqual result.getPriceInfo.selectPrice
      val expectedRur = 1000 * 55d
      val expectedEur = 917d // 1000 * 55 / 60D
      result.getPriceInfo.selectRurPrice shouldEqual (expectedRur +- 0.01f)
      result.getPriceInfo.selectEurPrice shouldEqual (expectedEur +- 0.01f)
    }

    "enrich salon info" in {
      val offerBuilder = ModelGenerators.DealerOfferGen.next.toBuilder
      offerBuilder.getSalonBuilder.setPlace(ModelGenerators.LocationGen.next)
      val offer = offerBuilder.build()
      val salon = Salon.newBuilder().setOffersCount(120).build()
      when(searcherClient.getSalon(?)(?)).thenReturnF(Some(salon))

      val result = manager.enrich(offer, EnrichOptions(salon = true)).futureValue
      result.getSalon.getOffersCount shouldEqual 120
      verify(searcherClient).getSalon(offer)
    }

    "don't enrich salon info for private sellers" in {
      val offer = ModelGenerators.PrivateOfferGen.next
      assume(offer.getPrivateSeller != PrivateSeller.getDefaultInstance, "expected not default private_seller")

      val result = manager.enrich(offer, EnrichOptions(salon = true)).futureValue
      result.getSalon shouldEqual Salon.getDefaultInstance
      result.getPrivateSeller shouldBe offer.getPrivateSeller
    }

    "enrich single offer with url" in {
      val offer = ModelGenerators.OfferGen.next

      val result = manager.enrich(offer, EnrichOptions()).futureValue
      result.getUrl shouldEqual urlBuilder.offerUrl(offer)
    }

    "enrich single offer with metro" in {
      val station = ModelGenerators.MetroStationGen.next
      val lines = ModelGenerators.MetroLinesListGen.next
      val offer = ModelGenerators.OfferGen.next

      val metroBase = mock[MetroBase]
      val testData = mock[DataService](new ReturnsMocks)
      when(metroBase.station(?)).thenReturn(Some(station))
      when(metroBase.stationLines(?)).thenReturn(lines)
      when(testData.metroBase).thenReturn(metroBase)
      when(testData.tree).thenReturn(TestData.tree)
      val manager = enrichManager(dataService = testData)

      val result = manager.enrich(offer, EnrichOptions(regionInfo = true)).futureValue

      val resultMetro = if (offer.getSellerType == SellerType.PRIVATE) {
        result.getSeller.getLocation.getMetroList.asScala
      } else {
        result.getSalon.getPlace.getMetroList.asScala
      }
      val resultStation = resultMetro.headOption.get
      resultStation.getLocation shouldBe station.location
      resultStation.getLinesCount shouldBe lines.size
      resultStation.getLinesList.asScala.map(_.getName).toSet shouldEqual lines.map(_.names.ru).toSet
      resultStation.getLinesList.asScala.map(_.getColor).toSet shouldEqual lines.map(_.color).toSet
    }

    "enrich single user offer with service schedules" in {
      val offer = ModelGenerators.OfferGen.next
      val scheduleResponse = scheduleResponseGen(withIds = Seq(offer.getId)).next
      when(scheduleManager.getSchedules(ForOffers(List(offer.id)), checkOffersOwnership = false)(userRequest))
        .thenReturnF(scheduleResponse)
      val enriched = manager.enrich(offer, EnrichOptions(serviceSchedules = true))(userRequest).futureValue
      enriched.getServiceSchedules shouldBe scheduleResponse.getOffersMap.get(offer.getId)
    }

    "enrich single dealer offer with service schedules" in {
      val offer = ModelGenerators.OfferGen.next
      val scheduleResponse = scheduleResponseGen(withIds = Seq(offer.getId)).next
      when(scheduleManager.getSchedules(ForOffers(List(offer.id)), checkOffersOwnership = false)(dealerRequest))
        .thenReturnF(scheduleResponse)
      val enriched = manager.enrich(offer, EnrichOptions(serviceSchedules = true))(dealerRequest).futureValue
      enriched.getServiceSchedules shouldBe scheduleResponse.getOffersMap.get(offer.getId)
    }

    "not enrich single offer with service schedules" in {
      val offer = ModelGenerators.OfferGen.next
      val scheduleResponse = scheduleResponseGen(withoutIds = Seq(offer.getId)).next
      when(scheduleManager.getSchedules(ForOffers(List(offer.id)), checkOffersOwnership = false)(userRequest))
        .thenReturnF(scheduleResponse)
      val enriched = manager.enrich(offer, EnrichOptions(serviceSchedules = true))(userRequest).futureValue
      enriched.hasServiceSchedules shouldBe false
    }

    "enrich several offers with service schedules" in {
      implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(1, Second))
      val offers = Gen.nonEmptyListOf(ModelGenerators.OfferGen).next
      val scheduledIds = Random.shuffle(offers).take(Random.nextInt(offers.size + 1)).map(_.getId)
      val nonScheduledIds = offers.map(_.getId).filterNot(scheduledIds.contains)
      val scheduleResponse = scheduleResponseGen(withIds = scheduledIds, withoutIds = nonScheduledIds).next
      when(scheduleManager.getSchedules(ForOffers(offers.map(_.id)))(userRequest)).thenReturnF(scheduleResponse)
      val enriched = manager.enrich(offers, EnrichOptions(serviceSchedules = true))(userRequest).futureValue
      val scheduledIdsSet = scheduledIds.toSet
      for (offer <- enriched) {
        if (scheduledIdsSet.contains(offer.getId)) {
          offer.getServiceSchedules shouldBe scheduleResponse.getOffersMap.get(offer.getId)
        } else {
          offer.hasServiceSchedules shouldBe false
        }
      }
    }

    "not check offers ownership" in {
      val scheduleManager = new ScheduleManager(salesmanClient, salesmanUserClient, ownershipChecker)
      val manager = enrichManager(scheduleManager = scheduleManager)
      val offer = ModelGenerators.DealerOfferGen.next
      val scheduleResponse = scheduleResponseGen(withIds = Seq(offer.getId)).next
      when(scheduleManager.getSchedules(ForOffers(List(offer.id)))(dealerRequest)).thenReturnF(scheduleResponse)
      when(salesmanClient.getSchedules(?, ?, ?)(?)).thenReturnF(scheduleResponse)
      val enriched = manager.enrich(offer, EnrichOptions(serviceSchedules = true))(dealerRequest).futureValue
      enriched.getServiceSchedules shouldBe scheduleResponse.getOffersMap.get(offer.getId)
      verifyNoMoreInteractions(ownershipChecker)
    }

    "enrich single offer with autostrategies" in {
      val offer = ModelGenerators.OfferGen.next
      val autostrategies = offerAutostrategiesListGen(List(offer.id)).next
      when(salesmanClient.getAutostrategies(List(offer.id))(dealerRequest)).thenReturnF(autostrategies)
      val enriched = manager.enrich(offer, EnrichOptions(autostrategies = true))(dealerRequest).futureValue
      enriched.getAutostrategiesList shouldBe autostrategies.head.getAutostrategiesList
    }

    "not enrich single offer with autostrategies" in {
      val offer = ModelGenerators.OfferGen.next
      when(salesmanClient.getAutostrategies(List(offer.id))(dealerRequest)).thenReturnF(Nil)
      val enriched = manager.enrich(offer, EnrichOptions(autostrategies = true))(dealerRequest).futureValue
      enriched.getAutostrategiesCount shouldBe 0
    }

    "not enrich single offer with autostrategies for user" in {
      val offer = ModelGenerators.OfferGen.next
      val enriched = manager.enrich(offer, EnrichOptions(autostrategies = true))(userRequest).futureValue
      enriched.getAutostrategiesCount shouldBe 0
    }

    "enrich several offers with autostrategies" in {
      implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(1, Second))
      val offers = Gen.nonEmptyListOf(ModelGenerators.OfferGen).next.distinctBy(_.id).toSeq
      val offerIds = offers.map(_.id).toList
      val idsWith = offerIds.take(Random.nextInt(offerIds.size + 1))
      val autostrategies = offerAutostrategiesListGen(idsWith).next
      when(salesmanClient.getAutostrategies(offerIds)(dealerRequest)).thenReturnF(autostrategies)
      val enriched = manager.enrich(offers, EnrichOptions(autostrategies = true))(dealerRequest).futureValue
      for (offer <- enriched) {
        if (idsWith.contains(offer.id)) {
          val expected =
            autostrategies
              .filter(_.getOfferId == offer.getId)
              .head
              .getAutostrategiesList
          offer.getAutostrategiesList shouldBe expected
        } else {
          offer.getAutostrategiesCount shouldBe 0
        }
      }
    }

    "not enrich several offers with autostrategies for user" in {
      val offers = Gen.nonEmptyListOf(ModelGenerators.OfferGen).next
      val enriched = manager.enrich(offers, EnrichOptions(autostrategies = true))(userRequest).futureValue
      for (offer <- enriched) {
        offer.getAutostrategiesCount shouldBe 0
      }
    }

    "add to failed flags when counters are failed" in {
      val offer = ModelGenerators.OfferGen.next
      when(countersManager.getCounters(?, ?, ?, ?)(?)).thenThrowF(new Exception())
      val result = manager.enrich(offer, EnrichOptions(counters = true)).futureValue
      result.getEnrichFailedFlagsList should contain theSameElementsAs Seq(EnrichFailedFlag.COUNTERS)
    }

    "fail when counters are required and unavailable" in {
      val offer = ModelGenerators.OfferGen.next
      val opts = EnrichOptions(counters = true, required = Set(EnrichFailedFlag.COUNTERS))
      when(countersManager.getCounters(?, ?, ?, ?)(?)).thenThrowF(new RuntimeException("counters unavailable"))
      intercept[RuntimeException] {
        manager.enrich(offer, opts).futureValue
      }
    }

    "enrich with user pic" in {
      val offer = ModelGenerators.PrivateOfferGen.next
      val opts = EnrichOptions(userPic = true)
      val imageUrl = ModelGenerators.ImageUrlGenerator.next
      val profile = ModelGenerators.PassportAutoruProfileGen.next.toBuilder
        .setUserpic(imageUrl)
        .build()
      when(passportClient.getUserProfile(?)(?)).thenReturnF(profile)
      val result = manager.enrich(offer, opts).futureValue
      result.getSeller.getUserpic shouldBe imageUrl
    }

    "not enrich with user pic if offer is from dealer" in {
      val offer = ModelGenerators.DealerOfferGen.next.updated { builder =>
        builder.getSellerBuilder.clearUserpic()
      }
      val opts = EnrichOptions(userPic = true)
      val imageUrl = ModelGenerators.ImageUrlGenerator.next
      val profile = ModelGenerators.PassportAutoruProfileGen.next.toBuilder
        .setUserpic(imageUrl)
        .build()
      when(passportClient.getUserProfile(?)(?)).thenReturnF(profile)
      val result = manager.enrich(offer, opts).futureValue
      result.getSeller.hasUserpic shouldBe false
    }

    "respond quickly if enricher is slow" in {
      val offer = ModelGenerators.PrivateOfferGen.next
      val manager = enrichManager(new SlowlyFailingUserPriceManager(timeout = 10.seconds))
      manager
        .enrich(offer, EnrichOptions(paidServicePrices = true))
        // здесь проверяем, что менеджер отработал быстрее, чем SlowlyFailingUserPriceManager
        .futureValue(PatienceConfig(timeout = Span(3, Seconds)), implicitly[Position])
    }

    "enrich calltracking counters" in {
      forAll(ModelGenerators.OfferGen, Gen.posNum[Int], Gen.posNum[Int]) { (offer0, views, calls) =>
        reset(calltrackingManager)
        when(calltrackingManager.countCallsBatch(?)(?)).thenReturnF(Map(offer0.id -> calls))
        val offer = offer0.toBuilder.setCounters(offer0.getCounters.toBuilder.setAll(views)).build
        val result = manager.enrich(offer, EnrichOptions(calltracking = true))(dealerRequest).futureValue
        result.getCounters.getCallsAll shouldBe calls
        result.getCounters.getCardViewCallConversionAll shouldBe ModelUtils.calcConversion(views, calls)
      }
    }
  }

  "Prolongable enricher" should {

    def enrich(offer: Offer, req: Request = userRequest): Offer =
      manager.enrich(offer, EnrichOptions(prolongable = true))(req).futureValue

    "enrich private offer with prolongable for one active product" in {
      forAll(ModelGenerators.PrivateOfferGen, productResponseGen()) { (offer, productResponse) =>
        val offerProductResponse = productResponse.toBuilder.setOffer(offer.id.toPlain).build()
        when(salesmanUserClient.getActiveAutoruProducts(?)(?)).thenReturnF(
          ApiModel.ProductResponses.newBuilder.addAllProductResponses(List(offerProductResponse).asJava).build
        )
        val service = PaidService
          .newBuilder()
          .setService(AutoruProduct.forSalesmanService(offerProductResponse).value.salesName)
          .setIsActive(true)
          .build()
        val forEnrich = offer.updated {
          _.clearServices()
            .addServices(service)
            .setUserRef(userRequest.user.privateRef.toPlain)
        }
        val enriched = enrich(forEnrich)
        val enrichedService = enriched.getServices(0)
        enrichedService.getService shouldBe service.getService
        enrichedService.getIsActive shouldBe service.getIsActive
        enrichedService.getProlongable shouldBe productResponse.getProlongable
        verify(salesmanUserClient).getActiveAutoruProducts(
          ApiModel.ActiveOffersProductsRequest.newBuilder
            .addAllActiveOfferProductRequests(
              List(
                ApiModel.ActiveOfferProductsRequest.newBuilder
                  .setOfferId(offer.getId)
                  .setGeoId(offer.getSeller.getLocation.getGeobaseId)
                  .build
              ).asJava
            )
            .build
        )(userRequest)
        reset(salesmanUserClient)
      }
    }

    "do nothing if only inactive products" in {
      forAll(ModelGenerators.OfferGen, Gen.listOf(ProductGen).map(_.distinct)) { (offer, products) =>
        val forEnrich = offer.updated {
          _.clearServices()
            .setUserRef(userRequest.user.privateRef.toPlain)
        }
        enrich(forEnrich).getServicesList shouldBe forEnrich.getServicesList
      }
    }

    "do nothing if dealer offer" in {
      forAll(ModelGenerators.DealerOfferGen) { offer =>
        enrich(offer, dealerRequest).getServicesList shouldBe offer.getServicesList
      }
    }

    "do nothing if no products" in {
      forAll(ModelGenerators.OfferGen)(offer => enrich(offer).getServicesList shouldBe offer.getServicesList)
    }
  }

  "Daily counters enricher" should {

    def enrich(offer: Offer): Offer =
      manager
        .enrich(offer, EnrichOptions(dailyCounters = DailyCountersParams.Enabled(DefaultDailyCountersPeriod)))
        .futureValue

    "enrich offer with daily counters" in {
      forAll(ModelGenerators.OfferGen, Gen.listOf(DailyCounterGen)) { (offer, dailyCounters) =>
        reset(countersManager)
        when(countersManager.getCountersByDay(?, ?, ?, ?)(?)).thenReturnF(Map(offer.id -> dailyCounters))
        val forEnrich = offer.updated(_.clearDailyCounters())
        val enriched = enrich(forEnrich)

        val callsByDay = offer.getDailyCountersList.asScala.map(dc => (dc.getDate, dc.getPhoneCalls)).toMap
        val dailyCountersWithCalls = dailyCounters.map(dc => {
          dc.toBuilder
            .setPhoneCalls(callsByDay.getOrElse(dc.getDate, 0))
            .setCardViewCallsConversion(0d)
            .build
        })
        enriched.getDailyCountersList.asScala should contain theSameElementsAs dailyCountersWithCalls

        val to = LocalDate.now(ZoneId.of("Europe/Moscow"))
        val from = to.minusWeeks(2)
        verify(countersManager).getCountersByDay(eq(offer.getCategory), eq(Seq(offer.id)), eq(from), eq(to))(?)
        verifyNoMoreInteractions(countersManager)
      }
    }

    "not enrich offer with other offer daily counters" in {
      forAll(ModelGenerators.OfferGen, Gen.choose(1, 10000), Gen.listOf(DailyCounterGen)) {
        (offer, offerIdDiff, dailyCounters) =>
          reset(countersManager)
          val counterOfferId = offer.id.copy(id = offer.id.id + offerIdDiff)
          when(countersManager.getCountersByDay(?, ?, ?, ?)(?)).thenReturnF(Map(counterOfferId -> dailyCounters))
          val forEnrich = offer.updated(_.clearDailyCounters())
          val enriched = enrich(forEnrich)
          enriched.getDailyCountersList.asScala shouldBe empty
          val to = LocalDate.now(ZoneId.of("Europe/Moscow"))
          val from = to.minusWeeks(2)
          verify(countersManager).getCountersByDay(eq(offer.getCategory), eq(Seq(offer.id)), eq(from), eq(to))(?)
          verifyNoMoreInteractions(countersManager)
      }
    }
  }

  "Dealer service enricher" should {

    def enrich(offer: Offer): Offer =
      manager.enrich(offer, EnrichOptions(dealerServices = true)).futureValue

    "enrich dealer offer services" in {
      // выбран premium во избежание генерации turbo: для turbo enrichedServices.size == 3
      forAll(DealerOfferGen, activePaidAutoruServiceGen(autoruProductGen(Premium))) { (baseOffer, service) =>
        val offer = baseOffer.toBuilder.clearServices().addServices(service).build()
        val enriched = enrich(offer)
        val enrichedServices = enriched.getServicesList.asScala
        enrichedServices should have size 1
        val enrichedService = enrichedServices.headOption.value
        enrichedService.getService shouldBe service.getService
        enrichedService.getIsActive shouldBe service.getIsActive
        enrichedService.getCreateDate shouldBe service.getCreateDate
        enrichedService.getExpireDate shouldBe service.getExpireDate
        enrichedService.hasDeactivationAllowed shouldBe true
        enrichedService.getActivatedBy shouldBe OWNER
      }
    }

    "rm redundant subproduct" in {
      forAll(
        DealerOfferGen,
        activePaidAutoruServiceGen(autoruProductGen(Premium)),
        activePaidAutoruServiceGen(autoruProductGen(PackageTurbo))
      ) { (baseOffer, premium, turbo) =>
        val offer = baseOffer.toBuilder.clearServices().addServices(premium).addServices(turbo).build()

        val enriched = enrich(offer)

        val enrichedServices = enriched.getServicesList.asScala
        enrichedServices.map(_.getService) should contain theSameElementsAs Seq(
          "package_turbo",
          "all_sale_special",
          "all_sale_premium"
        )
      }
    }

    "return exactly premium activated by turbo instead of premium activated by owner" in {
      forAll(
        DealerOfferGen,
        activePaidAutoruServiceGen(autoruProductGen(Premium)),
        activePaidAutoruServiceGen(autoruProductGen(PackageTurbo))
      ) { (baseOffer, premium, turbo) =>
        val offer = baseOffer.toBuilder.clearServices().addServices(premium).addServices(turbo).build()

        val enriched = enrich(offer)

        val enrichedServices = enriched.getServicesList.asScala
        val turboPremium = enrichedServices.find(_.getService == "all_sale_premium").value
        turboPremium.getIsActive shouldBe turbo.getIsActive
        turboPremium.getCreateDate shouldBe turbo.getCreateDate
        turboPremium.getExpireDate shouldBe turbo.getExpireDate
        turboPremium.hasDeactivationAllowed shouldBe true
        turboPremium.getDeactivationAllowed.getValue shouldBe false
        turboPremium.getActivatedBy shouldBe PACKAGE_TURBO
      }
    }

    "not enrich user offer services" in {
      forAll(PrivateOfferGen, ActivePaidAutoruServiceGen) { (baseOffer, service) =>
        val offer = baseOffer.toBuilder.clearServices().addServices(service).build()
        val enriched = enrich(offer)
        val enrichedServices = enriched.getServicesList.asScala
        enrichedServices should have size 1
        val enrichedService = enrichedServices.headOption.value
        enrichedService.getService shouldBe service.getService
        enrichedService.getIsActive shouldBe service.getIsActive
        enrichedService.getCreateDate shouldBe service.getCreateDate
        enrichedService.getExpireDate shouldBe service.getExpireDate
        enrichedService.hasDeactivationAllowed shouldBe false
        enrichedService.getActivatedBy shouldBe Activator.UNKNOWN
      }
    }

    "not enrich on default options" in {
      forAll(DealerOfferGen, ActivePaidAutoruServiceGen) { (baseOffer, service) =>
        val offer = baseOffer.toBuilder.clearServices().addServices(service).build()
        manager.enrich(offer, EnrichOptions()).futureValue.getServicesList shouldBe offer.getServicesList
      }
    }

    "fail on wrong product" in {
      forAll(DealerOfferGen) { baseOffer =>
        val builder = baseOffer.toBuilder
        builder.addServicesBuilder().setService("wrong")
        val offer = builder.build()
        val result = enrich(offer)
        result.getEnrichFailedFlagsList.asScala should contain only EnrichFailedFlag.DEALER_SERVICES
        result.getServicesList shouldBe offer.getServicesList
      }
    }

    "fail on inactive product" in {
      forAll(DealerOfferGen, ActivePaidAutoruServiceGen) { (baseOffer, baseService) =>
        val service = baseService.toBuilder.setIsActive(false).build()
        val offer = baseOffer.toBuilder.addServices(service).build()
        val result = enrich(offer)
        result.getEnrichFailedFlagsList.asScala should contain only EnrichFailedFlag.DEALER_SERVICES
        result.getServicesList shouldBe offer.getServicesList
      }
    }
  }

  "Encrypted user ID enricher" should {
    "add encrypted user ID to private offers if exists tag" in {
      forAll(PrivateOfferGen, Gen.posNum[Long]) { (baseOffer, uid) =>
        val userId = AutoruUser(uid)
        val offer = baseOffer.toBuilder
          .setUserRef(userId.toPlain)
          .addTags(AllowedUserOffersShowTag)
          .build()
        val expected = cryptoUserId.encrypt(userId)
        val result = manager.enrich(offer, EnrichOptions()).futureValue
        result.getAdditionalInfo.getOtherOffersShowInfo.getEncryptedUserId shouldBe expected
      }
    }
    "do nothing for private offers if not exists tag " in {
      forAll(DealerOfferGen, Gen.posNum[Long]) { (baseOffer, uid) =>
        val offer = baseOffer.toBuilder
          .setUserRef(AutoruUser(uid).toPlain)
          .clearTags()
          .build()
        val result = manager.enrich(offer, EnrichOptions()).futureValue
        result.getAdditionalInfo.getOtherOffersShowInfo.getEncryptedUserId shouldBe empty
        result.getEnrichFailedFlagsList shouldBe empty
      }
    }
    "do nothing for dealer offers" in {
      forAll(DealerOfferGen, Gen.posNum[Long]) { (baseOffer, uid) =>
        val offer = baseOffer.toBuilder.setUserRef(AutoruDealer(uid).toPlain).build()
        val result = manager.enrich(offer, EnrichOptions()).futureValue
        result.getAdditionalInfo.getOtherOffersShowInfo.getEncryptedUserId shouldBe empty
        result.getEnrichFailedFlagsList shouldBe empty
      }
    }
  }

  private def haveCertPlanned: Matcher[Offer] =
    Matcher(offer => MatchResult(offer.hasCertPlanned, "Offer don't have CertPlanned", "Offer have CertPlanned"))

}

object EnrichManagerSpec {
  private val AllowedUserOffersShowTag: String = "allowed_user_offers_show"
}
