package ru.auto.api.managers.favorite

import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.ResponseStatus
import ru.auto.api.auth.Application
import ru.auto.api.model.CategorySelector.StrictCategory
import ru.auto.api.model.{CategorySelector, ModelGenerators, RequestParams}
import ru.auto.api.services.favorite.FavoriteClient
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.subscriptions.SubscriptionClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import org.mockito.Mockito._
import ru.auto.api.model.favorite.OfferSearchesDomain

import scala.concurrent.Future

/**
  * Created by artvl on 26.05.17.
  */
class SearchesManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with BeforeAndAfter {
  val defaultFavoriteClient: FavoriteClient = mock[FavoriteClient]
  val defaultSubscriptionClient: SubscriptionClient = mock[SubscriptionClient]
  val defaultPassportClient: PassportClient = mock[PassportClient]

  private def wrongCategory(category: StrictCategory): StrictCategory =
    category match {
      case CategorySelector.Cars => CategorySelector.Trucks
      case CategorySelector.Trucks => CategorySelector.Moto
      case CategorySelector.Moto => CategorySelector.Cars
    }

  val searchesManager: SearchesManager = new SearchesManager(
    defaultFavoriteClient,
    defaultSubscriptionClient,
    defaultPassportClient
  )

  before {
    reset(defaultSubscriptionClient)
    reset(defaultFavoriteClient)
    reset(defaultPassportClient)
  }

  implicit val trace: Traced = Traced.empty

  implicit val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.desktop)
    r
  }

  "SearchesManager.getSavedSearches" should {
    "go to both clients" in {
      val user = ModelGenerators.PrivateUserRefGen.next
      val personalSS = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next
      val subscription = ModelGenerators.SubscriptionGen.next

      when(defaultFavoriteClient.getUserSavedSearches(?, ?)(?))
        .thenReturnF(Seq(personalSS))
      when(defaultSubscriptionClient.getUserSubscriptions(?, ?)(?))
        .thenReturnF(Seq(subscription))

      val result = searchesManager.getSavedSearches(personalSS.category, user).await

      result.getStatus shouldBe ResponseStatus.SUCCESS
      result.getSavedSearchesList.size() shouldBe 1
      verify(defaultFavoriteClient).getUserSavedSearches(eq(user), ?)(?)
      verify(defaultSubscriptionClient).getUserSubscriptions(eq(user), ?)(?)
    }
  }

  "SearchesManager.getSavedSearches" should {
    "filter other categories" in {
      val user = ModelGenerators.PrivateUserRefGen.next
      val personalSS = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next
      val subscription = ModelGenerators.SubscriptionGen.next

      when(defaultFavoriteClient.getUserSavedSearches(?, ?)(?))
        .thenReturnF(Seq(personalSS))
      when(defaultSubscriptionClient.getUserSubscriptions(?, ?)(?))
        .thenReturnF(Seq(subscription))

      val result = searchesManager.getSavedSearches(wrongCategory(personalSS.category), user).futureValue

      result.getStatus shouldBe ResponseStatus.SUCCESS
      result.getSavedSearchesList.size() shouldBe 0
      verify(defaultFavoriteClient).getUserSavedSearches(eq(user), ?)(?)
      verify(defaultSubscriptionClient).getUserSubscriptions(eq(user), ?)(?)
    }

    "throw an exception on personal" in {
      val user = ModelGenerators.PrivateUserRefGen.next
      val subscription = ModelGenerators.SubscriptionGen.next

      when(defaultFavoriteClient.getUserSavedSearches(?, ?)(?))
        .thenReturn(Future.failed(new RuntimeException("")))
      when(defaultSubscriptionClient.getUserSubscriptions(?, ?)(?))
        .thenReturnF(Seq(subscription))

      intercept[RuntimeException] {
        searchesManager.getSavedSearches(CategorySelector.Cars, user).futureValue
      }
      verify(defaultSubscriptionClient, never()).getUserSubscriptions(?, ?)(?)
    }

    "throw an exception on subscription" in {
      val user = ModelGenerators.PrivateUserRefGen.next
      val personalSS = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next

      when(defaultFavoriteClient.getUserSavedSearches(?, ?)(?))
        .thenReturnF(Seq(personalSS))

      when(defaultSubscriptionClient.getUserSubscriptions(?, ?)(?))
        .thenReturn(Future.failed(new RuntimeException("")))

      intercept[RuntimeException] {
        searchesManager.getSavedSearches(personalSS.category, user).futureValue
      }
      verify(defaultFavoriteClient).getUserSavedSearches(eq(user), ?)(?)
    }
  }
}
