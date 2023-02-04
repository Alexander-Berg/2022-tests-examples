package ru.auto.api.services.subscriptions

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.managers.favorite.WatchManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.StringUtils._
import ru.yandex.vertis.subscriptions.api.ApiModel.CreateWatchParameters

class DefaultWatchClientSpec extends HttpClientSpec with MockedHttpClient with ScalaCheckPropertyChecks {

  val watchClient = new DefaultWatchClient(http)

  "DefaultWatchClient" should {

    "add watch for user" in {
      forAll(PrivateUserRefGen, OfferIDGen) { (user, id) =>
        val patch = WatchManager.watchPatch(add = Seq(id))

        http.expectUrl(PUT, url"/api/3.x/auto/user/$user/watch")
        http.expectProto(
          CreateWatchParameters
            .newBuilder()
            .setPatch(patch)
            .setDeliveries(WatchManager.PrivateUserDefaultDeliveries)
            .build()
        )
        http.respondWithStatus(OK)

        watchClient.patchWatch(user, patch, WatchManager.PrivateUserDefaultDeliveries)
      }
    }

    "move watch from user to user" in {
      forAll(PrivateUserRefGen, AnonymousUserRefGen) { (user, anon) =>
        http.expectUrl(POST, url"/api/3.x/auto/user/$anon/watch/move?dest=user%3A${user.uid}")
        http.expectProto(WatchManager.PrivateUserDefaultDeliveries)
        http.respondWithStatus(OK)

        watchClient.moveWatch(anon, user, WatchManager.PrivateUserDefaultDeliveries)
      }
    }
  }
}
