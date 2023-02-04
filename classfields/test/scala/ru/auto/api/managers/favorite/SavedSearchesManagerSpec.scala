package ru.auto.api.managers.favorite

import akka.http.scaladsl.model.StatusCodes
import com.google.protobuf.Duration
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.OptionValues
import ru.auto.api.SearchesModel.{Delivery, EmailDelivery, PushDelivery, SavedSearchCreateParams}
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.managers.enrich.SearchItemManager
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.model.CategorySelector.{Cars, StrictCategory}
import ru.auto.api.model.events.SavedSearchEvents
import ru.auto.api.model.favorite._
import ru.auto.api.model.subscriptions.AutoSubscriptionsDomain
import ru.auto.api.model.{ModelGenerators, Paging, RequestParams}
import ru.auto.api.search.SearchModel.CatalogFilter
import ru.auto.api.services.favorite.FavoriteClient
import ru.auto.api.services.pushnoy.PushnoyClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.subscriptions.SubscriptionClient
import ru.auto.api.util.search.SearchMappings
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.auto.catalog.model.api.ApiModel.RawCatalog
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.subscriptions.Model.RequestSource
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class SavedSearchesManagerSpec extends BaseSpec with MockitoSupport with OptionValues with AsyncTasksSupport {
  private val favoriteClient: FavoriteClient = mock[FavoriteClient]
  private val subscriptionClient: SubscriptionClient = mock[SubscriptionClient]
  private val searcherClient: SearcherClient = mock[SearcherClient]
  private val pushnoyClient: PushnoyClient = mock[PushnoyClient]
  private val statEventManager: StatEventsManager = mock[StatEventsManager]
  private val searchItemManager: SearchItemManager = mock[SearchItemManager]
  private val catalogManager: CatalogManager = mock[CatalogManager]

  val featureManager: FeatureManager = mock[FeatureManager]
  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  when(featureManager.oldOptionsSearchMapping).thenReturn(feature)
  when(featureManager.allowSearcherRequestEnrichmentWithExpFlags).thenReturn(Feature("", _ => true))
  when(featureManager.allowSearcherRequestEnrichmentWithGlobalFlags).thenReturn(Feature("", _ => true))
  when(featureManager.dealerBoostCoefficient).thenReturn(Feature("", _ => 1.1f))

  val defaultsMapper = new DefaultsMapper(featureManager)
  val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)
  val savedSearchFactoryProvider = new SavedSearchFactoryProvider(searchMappings)

  private val savedSearchesManager = new SavedSearchesManager(
    statEventManager,
    searchItemManager,
    favoriteClient,
    subscriptionClient,
    searcherClient,
    pushnoyClient,
    savedSearchFactoryProvider,
    catalogManager
  )

  implicit val trace: Traced = Traced.empty
  implicit val request: Request = newRequest()

  def newRequest(): RequestImpl = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r
  }

  before {
    reset(favoriteClient, subscriptionClient, statEventManager)
  }

  "SavedSearchesManager" should {
    "get saved searches listing" in {
      val user = ModelGenerators.PersonalUserRefGen.next
      val savedSearches = Gen.nonEmptyListOf(ModelGenerators.personalSavedSearchGen(OfferSearchesDomain)).next
      val requestSource = RequestSource.newBuilder().setHttpQuery(savedSearches.head.queryString)
      val subscriptions = Seq(ModelGenerators.SubscriptionGen.next.toBuilder.setRequest(requestSource).build())

      when(favoriteClient.getUserSavedSearches(?, ?)(?)).thenReturnF(savedSearches)
      when(subscriptionClient.getUserSubscriptions(?, ?)(?)).thenReturnF(subscriptions)
      when(searchItemManager.enrichSearches[PersonalSavedSearch](?, ?, ?)(?)).thenReturnF(savedSearches)
      val res = savedSearchesManager.list(OfferSearchesDomain, user, Paging.Default).futureValue

      res.getSavedSearchesCount shouldBe savedSearches.size
      res.getSavedSearchesList.asScala.map(_.getId).toSet shouldEqual savedSearches.map(_.id).toSet
      val searchWithDeliveries = res.getSavedSearchesList.asScala.find(_.getId == savedSearches.head.id).get
      searchWithDeliveries.getDeliveries.getPushDelivery.getEnabled shouldBe subscriptions.head.getDeliveries.hasPush
      searchWithDeliveries.getDeliveries.getEmailDelivery.getEnabled shouldBe subscriptions.head.getDeliveries.hasEmail

      verify(favoriteClient).getUserSavedSearches(user, OfferSearchesDomain)(trace)
      verify(subscriptionClient).getUserSubscriptions(user, AutoSubscriptionsDomain)(trace)
    }

    "get saved search by id" in {
      val user = ModelGenerators.PersonalUserRefGen.next
      val savedSearch = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next
      val requestSource = RequestSource.newBuilder().setHttpQuery(savedSearch.queryString)
      val subscription = {
        val b = ModelGenerators.SubscriptionGen.next.toBuilder
        savedSearch.qualifier.foreach(b.setQualifier)
        b.setRequest(requestSource).build()
      }
      val subList = subscription :: Gen.listOfN(4, ModelGenerators.SubscriptionGen).next

      when(favoriteClient.getSavedSearch(?, ?, ?)(?)).thenReturnF(Some(savedSearch))
      when(subscriptionClient.getUserSubscriptions(?, ?)(?)).thenReturnF(subList)
      when(searchItemManager.enrichSearch[PersonalSavedSearch](?, ?, ?)(?)).thenReturnF(savedSearch)
      val res = savedSearchesManager.get(OfferSearchesDomain, user, savedSearch.id).futureValue

      res.getSearch.getId shouldBe savedSearch.id
      res.getSearch.getDeliveries.getEmailDelivery.getEnabled shouldBe subscription.getDeliveries.hasEmail
      res.getSearch.getDeliveries.getPushDelivery.getEnabled shouldBe subscription.getDeliveries.hasPush

      verify(favoriteClient).getSavedSearch(OfferSearchesDomain, user, savedSearch.id)(trace)
      verify(subscriptionClient).getUserSubscriptions(user, AutoSubscriptionsDomain)(trace)
    }

    "get saved search by id without subscription" in {
      val user = ModelGenerators.PersonalUserRefGen.next
      val savedSearch = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next
      val subList = Gen.listOfN(4, ModelGenerators.SubscriptionGen).next

      when(favoriteClient.getSavedSearch(?, ?, ?)(?)).thenReturnF(Some(savedSearch))
      when(subscriptionClient.getUserSubscriptions(?, ?)(?)).thenReturnF(subList)
      when(searchItemManager.enrichSearch[PersonalSavedSearch](?, ?, ?)(?)).thenReturnF(savedSearch)
      val res = savedSearchesManager.get(OfferSearchesDomain, user, savedSearch.id).futureValue

      res.getSearch.getId shouldBe savedSearch.id
      res.getSearch.getDeliveries.getEmailDelivery.getEnabled shouldBe false
      res.getSearch.getDeliveries.getPushDelivery.getEnabled shouldBe false

      verify(favoriteClient).getSavedSearch(OfferSearchesDomain, user, savedSearch.id)(trace)
      verify(subscriptionClient).getUserSubscriptions(user, AutoSubscriptionsDomain)(trace)
    }

    "delete saved search by id" in {
      implicit val req = newRequest()
      val user = ModelGenerators.PersonalUserRefGen.next
      val savedSearch = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next
      val requestSource = RequestSource.newBuilder().setHttpQuery(savedSearch.queryString)
      val subscription = {
        val b = ModelGenerators.SubscriptionGen.next.toBuilder
        savedSearch.qualifier.foreach(b.setQualifier)
        b.setRequest(requestSource).build()
      }
      val subList = subscription :: Gen.listOfN(4, ModelGenerators.SubscriptionGen).next

      when(favoriteClient.getSavedSearch(?, ?, ?)(?)).thenReturnF(Some(savedSearch))
      when(favoriteClient.deleteSavedSearch(?, ?, ?)(?)).thenReturnF(())
      when(subscriptionClient.getUserSubscriptions(?, ?)(?)).thenReturnF(subList)
      when(subscriptionClient.deleteSubscription(?, ?, ?)(?)).thenReturnF(())
      when(statEventManager.logEvent(?)(?)).thenReturnF(())

      savedSearchesManager.delete(OfferSearchesDomain, user, savedSearch.id).futureValue
      req.tasks.start(StatusCodes.OK).foreach(_.await)

      verify(favoriteClient).getSavedSearch(OfferSearchesDomain, user, savedSearch.id)(trace)
      verify(favoriteClient).deleteSavedSearch(OfferSearchesDomain, user, savedSearch.id)(trace)
      verify(subscriptionClient).getUserSubscriptions(user, AutoSubscriptionsDomain)(trace)
      verify(subscriptionClient).deleteSubscription(user, subscription.getId, AutoSubscriptionsDomain)(trace)
      verify(statEventManager).logEvent(any[SavedSearchEvents.DeleteEvent]())(eq(req))
    }

    "create saved search" in {
      implicit val req = newRequest()
      val user = ModelGenerators.PersonalUserRefGen.next
      val savedSearch = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain, optCategory = Some(Cars)).next
      val createParams = SavedSearchCreateParams
        .newBuilder()
        .setHttpQuery(savedSearch.queryString)
        .setTitle(savedSearch.title)
        .build()
      val subscription = ModelGenerators.SubscriptionGen.next

      when(favoriteClient.addSavedSearch(?, ?)(?)).thenReturnF(())
      when(favoriteClient.getUserSavedSearches(?, ?)(?)).thenReturnF(Seq.empty)
      when(subscriptionClient.upsertSubscription(?, ?, ?)(?)).thenReturnF(subscription)
      when(statEventManager.logEvent(?)(?)).thenReturnF(())
      when(
        catalogManager
          .exactByCatalogFilter(
            any[StrictCategory](),
            any[Option[String]](),
            any[Seq[CatalogFilter]](),
            any[Boolean](),
            any[Boolean]()
          )(?)
      ).thenReturnF(RawCatalog.getDefaultInstance)
      stub(
        searchItemManager
          .enrichSearch[PersonalSavedSearch](_: PersonalSavedSearch, _: Boolean, _: Boolean)(_: RequestImpl)
      ) {
        case (s, _, _, _) =>
          Future.successful(s)
      }
      when(searchItemManager.getSearchTitleSafe(?)(?)).thenReturnF("Все марки автомобилей")

      val res = savedSearchesManager.add(user, OfferSearchesDomain, savedSearch.category, createParams).futureValue
      req.tasks.start(StatusCodes.OK).foreach(_.await)

      res.getSearch.getDeliveries.getEmailDelivery.getEnabled shouldBe true
      res.getSearch.getDeliveries.getPushDelivery.getEnabled shouldBe true
      res.getSearch.getTitle shouldBe "Все марки автомобилей"

      verify(favoriteClient).addSavedSearch(eq(user), ?)(eq(trace))
      verify(subscriptionClient).upsertSubscription(eq(user), ?, ?)(eq(trace))
      verify(statEventManager).logEvent(any[SavedSearchEvents.AddEvent]())(eq(req))
    }

    "delete push delivery" in {
      implicit val req = newRequest()
      val user = ModelGenerators.PersonalUserRefGen.next
      val savedSearch = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next
      val delivery = PushDelivery.newBuilder().setEnabled(false).build()
      val requestSource = RequestSource.newBuilder().setHttpQuery(savedSearch.queryString)
      val subscription = {
        val b = ModelGenerators.SubscriptionGen.next.toBuilder
        savedSearch.qualifier.foreach(b.setQualifier)
        b.setRequest(requestSource).build()
      }
      val subList = subscription :: Gen.listOfN(4, ModelGenerators.SubscriptionGen).next

      when(favoriteClient.getSavedSearch(?, ?, ?)(?)).thenReturnF(Some(savedSearch))
      when(favoriteClient.upsertSavedSearch(?, ?, ?)(?)).thenReturnF(())
      when(subscriptionClient.getUserSubscriptions(?, ?)(?)).thenReturnF(subList)
      when(subscriptionClient.deleteDelivery(?, ?, ?, ?)(?)).thenReturnF(())
      when(statEventManager.logEvent(?)(?)).thenReturnF(())

      savedSearchesManager.updatePushDelivery(OfferSearchesDomain, user, savedSearch.id, delivery).futureValue
      req.tasks.start(StatusCodes.OK).foreach(_.await)

      verify(favoriteClient).getSavedSearch(OfferSearchesDomain, user, savedSearch.id)(trace)

      verify(subscriptionClient).getUserSubscriptions(user, AutoSubscriptionsDomain)(trace)
      verify(subscriptionClient).deleteDelivery(user, subscription.getId, Delivery.PUSH, AutoSubscriptionsDomain)(trace)
      verify(statEventManager).logEvent(any[SavedSearchEvents.UpdatePushEvent]())(eq(req))
    }

    "update email delivery" in {
      implicit val req = newRequest()
      val user = ModelGenerators.PersonalUserRefGen.next
      val savedSearch = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next
      val delivery = EmailDelivery
        .newBuilder()
        .setEnabled(true)
        .setPeriod(
          Duration
            .newBuilder()
            .setSeconds(3600L)
        )
        .build()
      val requestSource = RequestSource.newBuilder().setHttpQuery(savedSearch.queryString)
      val subscription = {
        val b = ModelGenerators.SubscriptionGen.next.toBuilder
        savedSearch.qualifier.foreach(b.setQualifier)
        b.setRequest(requestSource).build()
      }
      val subList = subscription :: Gen.listOfN(4, ModelGenerators.SubscriptionGen).next

      when(favoriteClient.getSavedSearch(?, ?, ?)(?)).thenReturnF(Some(savedSearch))
      when(favoriteClient.upsertSavedSearch(?, ?, ?)(?)).thenReturnF(())
      when(subscriptionClient.getUserSubscriptions(?, ?)(?)).thenReturnF(subList)
      when(subscriptionClient.upsertSubscription(?, ?, ?)(?)).thenReturnF(subscription)
      when(statEventManager.logEvent(?)(?)).thenReturnF(())

      savedSearchesManager.updateEmailDelivery(OfferSearchesDomain, user, savedSearch.id, delivery).futureValue
      req.tasks.start(StatusCodes.OK).foreach(_.await)

      verify(favoriteClient).getSavedSearch(OfferSearchesDomain, user, savedSearch.id)(trace)
      verify(subscriptionClient).getUserSubscriptions(user, AutoSubscriptionsDomain)(trace)
      verify(subscriptionClient).upsertSubscription(eq(user), ?, ?)(eq(trace))
      verify(statEventManager).logEvent(any[SavedSearchEvents.UpdateEmailEvent]())(eq(req))
    }
  }
}
