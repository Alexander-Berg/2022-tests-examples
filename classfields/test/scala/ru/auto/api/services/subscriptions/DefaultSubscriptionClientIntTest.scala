package ru.auto.api.services.subscriptions

import java.time.Instant
import org.scalactic.source.Position
import org.scalatest.Tag
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.SubscriptionNotFoundException
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.managers.favorite.SavedSearchesManager
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.favorite.OfferSavedSearch
import ru.auto.api.model._
import ru.auto.api.model.subscriptions.AutoSubscriptionsDomain
import ru.auto.api.search.SearchModel.SearchRequestParameters
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.subscriptions.api.ApiModel.Subscription

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
  * Created by artvl on 30.05.17.
  */
class DefaultSubscriptionClientIntTest extends HttpClientSuite {

  val query = SearcherQuery(
    "customs_state=1&image=true&is_clear=false" +
      "&mark-model-nameplate=HONDA%23CIVIC%23%232306765" +
      "&mark-model-nameplate=HONDA%23CIVIC%23%234569475" +
      "&rid=1&state=NEW&state=USED&transmission_full=MECHANICAL"
  )
  val title = "Автомобили Honda Civic VIII; VIII Рестайлинг в Москве и Московской области"

  override protected def config: HttpClientConfig =
    HttpClientConfig.apply("http", "subscriptions-api-test-int.slb.vertis.yandex.net", 80)

  val subscriptionClient = new DefaultSubscriptionClient(http)

  implicit def request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setApplication(Application.iosApp)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r
  }

  private def newSavedSearch =
    OfferSavedSearch(
      "",
      Instant.now,
      Instant.now,
      Cars,
      title,
      template = None,
      query,
      None,
      Some(SearchRequestParameters.getDefaultInstance),
      view = None,
      unsupportedFields = Seq.empty,
      deliveries = Some(SavedSearchesManager.DefaultDeliveries)
    )

  testWithUser("add subscriptions to user") { user =>
    val subscription = newSavedSearch.toCreateSubscriptionParameters

    val result = subscriptionClient.upsertSubscription(user, subscription, AutoSubscriptionsDomain).futureValue

    getSubscription(user, result.getId).futureValue.getId shouldBe result.getId
  }

  testWithUser("update existing subscriptions to user") { user =>
    val subscription = newSavedSearch.toCreateSubscriptionParameters
    val newTitle = "Автомобили Honda Civic VIII"

    subscriptionClient.upsertSubscription(user, subscription, AutoSubscriptionsDomain).futureValue

    val subList = subscriptionClient.getUserSubscriptions(user, AutoSubscriptionsDomain).futureValue
    (subList should have).length(1)

    val updatedBuilder = subscription.toBuilder
    updatedBuilder.getViewBuilder.setTitle(newTitle)

    val updated = updatedBuilder.build()

    val result = subscriptionClient
      .upsertSubscription(user, updated, AutoSubscriptionsDomain)
      .futureValue

    result.getId shouldBe subList.head.getId
    result.getView.getTitle shouldBe newTitle
  }

  testWithUser("delete subscription") { user =>
    val subscription = newSavedSearch.toCreateSubscriptionParameters
    subscriptionClient.upsertSubscription(user, subscription, AutoSubscriptionsDomain).futureValue

    val subscriptions = subscriptionClient.getUserSubscriptions(user, AutoSubscriptionsDomain).futureValue
    subscriptions should not be empty

    val sub = subscriptions.head

    //No exceptions must be thrown
    subscriptionClient.deleteSubscription(user, sub.getId, AutoSubscriptionsDomain).futureValue

    val result = subscriptionClient.getUserSubscriptions(user, AutoSubscriptionsDomain).futureValue
    result shouldBe empty
  }

  testWithTwoUsers("move subscription") { (anon, login) =>
    val subscription = newSavedSearch.toCreateSubscriptionParameters

    subscriptionClient.upsertSubscription(anon, subscription, AutoSubscriptionsDomain).futureValue
    subscriptionClient.getUserSubscriptions(anon, AutoSubscriptionsDomain).futureValue should not be empty

    subscriptionClient.moveSubscriptions(anon, login, AutoSubscriptionsDomain).futureValue

    subscriptionClient.getUserSubscriptions(anon, AutoSubscriptionsDomain).futureValue shouldBe empty
    val loginSubscriptions = subscriptionClient.getUserSubscriptions(login, AutoSubscriptionsDomain).futureValue

    loginSubscriptions.size shouldEqual 1
    loginSubscriptions.head.getRequest.getHttpQuery shouldBe subscription.getRequest.getHttpQuery

  }

  private def cleanup(user: PersonalUserRef): Unit = {
    subscriptionClient
      .getUserSubscriptions(user, AutoSubscriptionsDomain)
      .map(
        _.foreach(sub => subscriptionClient.deleteSubscription(user, sub.getId, AutoSubscriptionsDomain).futureValue)
      )
  }

  private def wrapUser(user: PersonalUserRef)(action: PersonalUserRef => Any): Any = {
    cleanup(user)

    try {
      info(s"For $user")
      action(user)
    } catch {
      case NonFatal(e) =>
        cleanup(user)

        throw e
    }
  }

  private def withBoothUserTypes(action: PersonalUserRef => Any): Any = {
    wrapUser(ModelGenerators.AnonymousUserRefGen.next)(action)
    wrapUser(ModelGenerators.PrivateUserRefGen.next)(action)
  }

  private def testWithUser(
      testName: String,
      testTags: Tag*
  )(testFun: PersonalUserRef => Any /* Assertion */
  )(implicit pos: Position): Unit = {
    test(testName, testTags: _*) {
      withBoothUserTypes(testFun)
    }
  }

  private def testWithTwoUsers(
      testName: String,
      testTags: Tag*
  )(testFun: (AnonymousUser, AutoruUser) => Any)(implicit pos: Position): Unit = {
    test(testName, testTags: _*) {
      val anonymousUser = ModelGenerators.AnonymousUserRefGen.next
      val autoruUser = ModelGenerators.PrivateUserRefGen.next

      cleanup(anonymousUser)
      cleanup(autoruUser)
      try {
        testFun(anonymousUser, autoruUser)
      } catch {
        case NonFatal(e) =>
          cleanup(anonymousUser)
          cleanup(autoruUser)

          throw e
      }
    }
  }

  private def getSubscription(user: PersonalUserRef, id: String): Future[Subscription] = {
    subscriptionClient.getUserSubscriptions(user, AutoSubscriptionsDomain).map { list =>
      list.find(_.getId == id).getOrElse(throw new SubscriptionNotFoundException)
    }
  }
}
