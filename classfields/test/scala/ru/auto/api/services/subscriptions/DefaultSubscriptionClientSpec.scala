package ru.auto.api.services.subscriptions

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.SearchesModel.Delivery
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.favorite.OfferSearchesDomain
import ru.auto.api.model.subscriptions.AutoSubscriptionsDomain
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.StringUtils._

class DefaultSubscriptionClientSpec extends HttpClientSpec with MockedHttpClient with ScalaCheckPropertyChecks {

  val subscriptionClient = new DefaultSubscriptionClient(http)

  "SubscriptionClient" should {
    "upsert subscriptions by id" in {
      forAll(PrivateUserRefGen, personalSavedSearchGen(OfferSearchesDomain), SubscriptionGen) { (user, search, sub) =>
        val uid = "autoru:uid:" + user.uid
        val params = search.toCreateSubscriptionParameters

        http.expectUrl(PUT, url"/api/3.x/auto/user/$uid/subscription")
        http.expectProto(params)
        http.respondWithProto(OK, sub)

        val res = subscriptionClient.upsertSubscription(user, params, AutoSubscriptionsDomain).futureValue
        res shouldBe sub
      }
    }

    "delete subscriptions by id" in {
      forAll(PrivateUserRefGen, personalSavedSearchGen(OfferSearchesDomain)) { (user, search) =>
        val uid = "autoru:uid:" + user.uid

        http.expectUrl(DELETE, url"/api/3.x/auto/user/$uid/subscription/${search.id}")
        http.respondWithStatus(OK)

        subscriptionClient.deleteSubscription(user, search.id, AutoSubscriptionsDomain).futureValue
      }
    }

    "delete cars subscriptions" in {
      forAll(PrivateUserRefGen, SubscriptionGen) { (user, sub) =>
        val uid = "autoru:uid:" + user.uid

        http.expectUrl(DELETE, url"/api/3.x/auto/user/$uid/subscription/${sub.getId}")
        http.respondWithStatus(OK)

        subscriptionClient.deleteSubscription(user, sub.getId, AutoSubscriptionsDomain).futureValue
      }
    }

    "move subscriptions from user to user" in {
      forAll(PrivateUserRefGen, AnonymousUserRefGen) { (user, anon) =>
        val uid = "autoru:uid:" + user.uid
        val anonUid = "autoru:sid:" + anon.fullId
        http.expectUrl(POST, url"/api/3.x/auto/user/$uid/subscription/move?dest=$anonUid")
        http.respondWithStatus(OK)

        subscriptionClient.moveSubscriptions(user, anon, AutoSubscriptionsDomain)
      }
    }

    "delete email subscription delivery" in {
      forAll(PrivateUserRefGen, SubscriptionGen) { (user, sub) =>
        val uid = "autoru:uid:" + user.uid
        http.expectUrl(DELETE, url"/api/3.x/auto/user/$uid/subscription/${sub.getId}/delivery?type=email")
        http.respondWithStatus(OK)

        subscriptionClient.deleteDelivery(user, sub.getId, Delivery.EMAIL, AutoSubscriptionsDomain)
      }
    }
  }
}
