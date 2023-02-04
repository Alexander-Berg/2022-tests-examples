package ru.auto.api.managers.stats

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.app.redis.RedisCache
import ru.auto.api.auth.Application
import ru.auto.api.experiments.ExperimentsModel.{StatsNotification, StatsNotificationType}
import ru.auto.api.managers.stats.ViewedNotificationsManager.NotificationsResult
import ru.auto.api.model.ModelGenerators.OfferIDGen
import ru.auto.api.model.RequestParams
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

class ViewedNotificationsManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  type CasFunction = Option[NotificationsResult] => (NotificationsResult, Option[NotificationsResult])

  trait mocks {
    val redisCache = mock[RedisCache]
    implicit val trace: Traced = Traced.empty
    implicit val request: RequestImpl = new RequestImpl
    request.setApplication(Application.desktop)
    request.setTrace(trace)
    request.setRequestParams(RequestParams.empty.copy(deviceUid = Some("uid")))
    val manager = new ViewedNotificationsManager(redisCache)
    val offerId = OfferIDGen.next
  }

  "NotificationStatsManager" should {
    "should store record about viewed notification" in new mocks {
      val notifications = List(
        StatsNotification
          .newBuilder()
          .setNotificationType(StatsNotificationType.CURRENT_VIEWS)
          .build()
      )
      when(redisCache.updateAndGetExt[NotificationsResult, Option[NotificationsResult]](?, ?, ?)(?)(?, ?)).thenAnswer {
        args =>
          val function = args.getArguments.apply(3).asInstanceOf[CasFunction]
          val (newValue, prev) = function.apply(None)
          newValue shouldBe NotificationsResult(List(0))
          Future.successful(None)
      }
      val result = manager
        .getNotViewedNotifications(offerId, notifications, ViewedNotificationsManager.NotificationExpiration)
        .await
      result.head.getNotificationType shouldBe StatsNotificationType.CURRENT_VIEWS
    }
  }

}
