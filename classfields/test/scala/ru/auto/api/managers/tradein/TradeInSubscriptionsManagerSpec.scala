package ru.auto.api.managers.tradein

import org.mockito.Mockito._
import org.scalacheck.Gen
import ru.auto.api.BaseSpec
import ru.auto.api.SearchesModel.SavedSearchCreateParams
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.managers.enrich.SearchItemManager
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.managers.favorite.PersonalSavedSearch
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.events.SavedSearchEvents
import ru.auto.api.model.favorite.{OfferSearchesDomain, SavedSearchFactoryProvider}
import ru.auto.api.model.subscriptions.AutoruTradeInSubscriptionsDomain
import ru.auto.api.model.{ModelGenerators, RequestParams}
import ru.auto.api.services.subscriptions.SubscriptionClient
import ru.auto.api.util.search.SearchMappings
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.catalog.model.api.ApiModel.RawCatalog
import ru.yandex.vertis._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.subscriptions.Model.RequestSource
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class TradeInSubscriptionsManagerSpec extends BaseSpec with MockitoSupport {
  private val subscriptionClient = mock[SubscriptionClient]
  private val searchItemManager = mock[SearchItemManager]
  private val statEventManager: StatEventsManager = mock[StatEventsManager]
  private val catalogManager = mock[CatalogManager]

  private val featureManager: FeatureManager = mock[FeatureManager]
  private val feature: Feature[Boolean] = mock[Feature[Boolean]]

  when(feature.value).thenReturn(false)
  when(featureManager.oldOptionsSearchMapping).thenReturn(feature)
  when(featureManager.allowSearcherRequestEnrichmentWithExpFlags).thenReturn(Feature("", _ => true))
  when(featureManager.allowSearcherRequestEnrichmentWithGlobalFlags).thenReturn(Feature("", _ => true))
  when(featureManager.dealerBoostCoefficient).thenReturn(Feature("", _ => 1.1f))

  private val defaultsMapper = new DefaultsMapper(featureManager)
  private val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)
  private val savedSearchFactoryProvider = new SavedSearchFactoryProvider(searchMappings)

  private val tradeInSubscriptions = new TradeInSubscriptionsManager(
    statEventManager,
    subscriptionClient,
    searchItemManager,
    savedSearchFactoryProvider
  )

  private def toSraasAny(enrichedPersonal: PersonalSavedSearch)(implicit request: Request) = {
    val message = enrichedPersonal.convertToApiSearch

    val context = sraas.Any
      .newBuilder()
      .setValue(message.toByteString)
      .build()

    context
  }

  private def newRequest(implicit trace: Traced): RequestImpl = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r
  }

  before {
    reset(subscriptionClient, searchItemManager)
  }

  implicit val traced: Traced = Traced.empty

  "TradeInSubscriptionsManager" should {
    "add new subscription" in {
      implicit val request: RequestImpl = newRequest

      val user = ModelGenerators.DealerUserRefGen.next
      val savedSearch = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain, optCategory = Some(Cars)).next
      val createParams = SavedSearchCreateParams
        .newBuilder()
        .setHttpQuery(savedSearch.queryString)
        .setTitle(savedSearch.title)
        .build()
      val subscription = ModelGenerators.SubscriptionGen.next

      when(subscriptionClient.upsertSubscription(?, ?, ?)(?)).thenReturnF(subscription)
      when(statEventManager.logEvent(?)(?)).thenReturnF(())
      when(catalogManager.exactByCatalogFilter(?, ?, ?, ?, ?)(?)).thenReturnF(RawCatalog.getDefaultInstance)
      when(searchItemManager.getSearchTitleSafe(?)(?)).thenReturnF("Все марки автомобиля")

      stub(
        searchItemManager
          .enrichSearch[PersonalSavedSearch](_: PersonalSavedSearch, _: Boolean, _: Boolean)(_: RequestImpl)
      ) { case (s, _, _, _) => Future.successful(s) }

      val res = tradeInSubscriptions.add(user, savedSearch.category, createParams).futureValue

      res.getSearch.getDeliveries.getEmailDelivery.getEnabled shouldBe true
      res.getSearch.getDeliveries.getPushDelivery.getEnabled shouldBe true
      res.getSearch.getTitle shouldBe "Все марки автомобиля"

      verify(subscriptionClient).upsertSubscription(eq(user), ?, ?)(eq(traced))
      verify(statEventManager).logEvent(any[SavedSearchEvents.AddEvent]())(eq(request))
      verify(searchItemManager).getSearchTitleSafe(?)(eq(request))
    }

    "delete subscription by subscriptionId" in {
      implicit val request: RequestImpl = newRequest

      val user = ModelGenerators.DealerUserRefGen.next
      val savedSearch = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain, optCategory = Some(Cars)).next
      val requestSource = RequestSource.newBuilder().setHttpQuery(savedSearch.queryString)

      val subscription = {
        val subscriptionBuilder = ModelGenerators.SubscriptionGen.next.toBuilder
        savedSearch.qualifier.foreach(subscriptionBuilder.setQualifier)
        subscriptionBuilder.setId(savedSearch.id)
        subscriptionBuilder.setRequest(requestSource).build()
      }

      when(subscriptionClient.deleteSubscription(?, ?, ?)(?)).thenReturnF(())
      when(statEventManager.logEvent(any[SavedSearchEvents.DeleteEvent]())(?)).thenReturnF(())

      tradeInSubscriptions.delete(user, savedSearch.id).futureValue

      verify(subscriptionClient).deleteSubscription(user, subscription.getId, AutoruTradeInSubscriptionsDomain)(traced)
      verify(statEventManager).logEvent(any[SavedSearchEvents.DeleteEvent]())(eq(request))
    }
    "get subscriptions" in {
      implicit val r: Request = newRequest
      val dealer = ModelGenerators.DealerUserRefGen.next
      val personalSavedSearches = Gen.nonEmptyListOf(ModelGenerators.personalSavedSearchGen(OfferSearchesDomain)).next

      val subscriptions = personalSavedSearches.map { p =>
        val request = RequestSource.newBuilder().setHttpQuery(p.queryString)
        val any = toSraasAny(p)
        val builder = ModelGenerators.SubscriptionGen.next.toBuilder
          .setRequest(request)
          .setContext(any)
          .setId(p.id)

        p.deliveries.foreach(builder.setDeliveries)

        builder.build()
      }

      when(subscriptionClient.getUserSubscriptions(?, ?)(?)).thenReturnF(subscriptions)

      val res = tradeInSubscriptions.list(dealer).futureValue

      val savedSearches = res.getSavedSearchesList.asScala

      res.getSavedSearchesCount shouldBe personalSavedSearches.size

      val savedSearchIds = savedSearches.map(_.getId).toSet
      val pagedSearchIds = personalSavedSearches.map(_.id).toSet

      savedSearchIds shouldBe pagedSearchIds

      val serchWithDeliveries = savedSearches.find(_.getId == personalSavedSearches.head.id).get

      serchWithDeliveries.getDeliveries.getPushDelivery.getEnabled shouldBe subscriptions.head.getDeliveries.hasPush
      serchWithDeliveries.getDeliveries.getEmailDelivery.getEnabled shouldBe subscriptions.head.getDeliveries.hasEmail

      verify(subscriptionClient).getUserSubscriptions(dealer, AutoruTradeInSubscriptionsDomain)(traced)
    }
  }
}
