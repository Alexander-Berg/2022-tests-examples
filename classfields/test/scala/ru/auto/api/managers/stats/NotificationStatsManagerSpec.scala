package ru.auto.api.managers.stats

import java.time.{LocalDate, ZoneOffset}

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.DailyCountersResponse
import ru.auto.api.auth.Application
import ru.auto.api.experiments.ExperimentsModel.{StatsNotification, StatsNotificationType}
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.managers.offers.OfferStatManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.{CategorySelector, RequestParams}
import ru.auto.api.services.settings.RemoteConfigManager
import ru.auto.api.services.settings.RemoteConfigManager.RemoteConfig
import ru.auto.api.services.telepony.TeleponyClient.CallsStats
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.TimeUtils.TimeProvider
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

class NotificationStatsManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  import NotificationStatsManager._

  trait mocks {
    val offerStatManager = mock[OfferStatManager]
    val viewedNotificationsManager = mock[ViewedNotificationsManager]
    val timeProvider = mock[TimeProvider]
    val remoteConfigManager = mock[RemoteConfigManager]
    val statEventsManager = mock[StatEventsManager]

    val manager = new NotificationStatsManager(
      timeProvider,
      offerStatManager,
      viewedNotificationsManager,
      statEventsManager
    )
    val date = LocalDate.parse("2018-07-07")
    val offsetTime = date.atStartOfDay().atOffset(ZoneOffset.UTC)
    when(timeProvider.currentLocalDate()).thenReturn(date)
    when(timeProvider.currentOffsetDateTime()).thenReturn(offsetTime)
    when(statEventsManager.logOfferNotificationEvent(?, ?, ?)(?)).thenReturnF(())
    implicit val trace: Traced = Traced.empty
    implicit val request: RequestImpl = new RequestImpl
    request.setApplication(Application.desktop)
    request.setTrace(trace)
    request.setRequestParams(RequestParams.empty)
    val category = CategorySelector.Cars
    val offerId = OfferIDGen.next

    def configValue(experiment: String): RemoteConfig = {
      RemoteConfig(Map(NotificationExperimentName -> experiment))
    }

  }

  "NotificationStatsManager" should {
    "return current views notification" in new mocks {
      val dailyCountersResponse = DailyCountersResponse.newBuilder().build()
      when(viewedNotificationsManager.getNotViewedNotifications(?, ?, ?)(?)).thenAnswer { x =>
        Future.successful(x.getArguments.apply(1))
      }
      when(remoteConfigManager.getRemoteConfig(?)).thenReturnF(configValue(ViewsNow.name))
      when(offerStatManager.getCurrentViews(?, ?)(?)).thenReturnF(200)
      when(offerStatManager.getLastCallsStats(?, ?)(?)).thenReturnF(CallsStats(0, None))
      when(offerStatManager.getOfferStat(?, ?)(?)).thenReturnF(dailyCountersResponse)
      val result = manager.getStatNotification(category, offerId).await
      result.getResult.getNotificationType shouldBe StatsNotificationType.CURRENT_VIEWS
    }

    "not return current views notification if not enough views" in new mocks {
      val dailyCountersResponse = DailyCountersResponse.newBuilder().build()
      when(remoteConfigManager.getRemoteConfig(?)).thenReturnF(configValue(ViewsNow.name))
      when(viewedNotificationsManager.getNotViewedNotifications(?, ?, ?)(?)).thenAnswer { x =>
        Future.successful(x.getArguments.apply(1))
      }
      when(offerStatManager.getCurrentViews(?, ?)(?)).thenReturnF(4)
      when(offerStatManager.getLastCallsStats(?, ?)(?)).thenReturnF(CallsStats(0, None))
      when(offerStatManager.getOfferStat(?, ?)(?)).thenReturnF(dailyCountersResponse)
      val result = manager.getStatNotification(category, offerId).await
      result.hasResult shouldBe false
    }

    "return daily views notification" in new mocks {
      val dailyCountersResponseBuilder = DailyCountersResponse
        .newBuilder()
      dailyCountersResponseBuilder
        .addItemsBuilder()
        .setOfferId(offerId.toPlain)
        .addCountersBuilder()
        .setDate(date.minusDays(1).toString)
        .setViews(400)
      val dailyCountersResponse = dailyCountersResponseBuilder.build()
      when(remoteConfigManager.getRemoteConfig(?)).thenReturnF(configValue(DailyViews.name))
      when(viewedNotificationsManager.getNotViewedNotifications(?, ?, ?)(?)).thenAnswer { x =>
        Future.successful(x.getArguments.apply(1))
      }
      when(offerStatManager.getCurrentViews(?, ?)(?)).thenReturnF(0)
      when(offerStatManager.getLastCallsStats(?, ?)(?)).thenReturnF(CallsStats(0, None))
      when(offerStatManager.getOfferStat(?, ?)(?)).thenReturnF(dailyCountersResponse)
      val result = manager.getStatNotification(category, offerId).await
      result.getResult.getNotificationType shouldBe StatsNotificationType.DAILY_VIEWS
    }

    "not return daily views notification if not enough views" in new mocks {
      val dailyCountersResponseBuilder = DailyCountersResponse
        .newBuilder()
      dailyCountersResponseBuilder
        .addItemsBuilder()
        .setOfferId(offerId.toPlain)
        .addCountersBuilder()
        .setDate(date.minusDays(1).toString)
        .setViews(10)
      val dailyCountersResponse = dailyCountersResponseBuilder.build()
      when(remoteConfigManager.getRemoteConfig(?)).thenReturnF(configValue(DailyViews.name))
      when(viewedNotificationsManager.getNotViewedNotifications(?, ?, ?)(?)).thenAnswer { x =>
        Future.successful(x.getArguments.apply(1))
      }
      when(offerStatManager.getCurrentViews(?, ?)(?)).thenReturnF(0)
      when(offerStatManager.getLastCallsStats(?, ?)(?)).thenReturnF(CallsStats(0, None))
      when(offerStatManager.getOfferStat(?, ?)(?)).thenReturnF(dailyCountersResponse)
      val result = manager.getStatNotification(category, offerId).await
      result.hasResult shouldBe false
    }

    "return last call notification" in new mocks {
      val dailyCountersResponse = DailyCountersResponse.newBuilder().build()
      val lastCallTime = offsetTime.minusDays(1).plusHours(3)
      when(viewedNotificationsManager.getNotViewedNotifications(?, ?, ?)(?)).thenAnswer { x =>
        Future.successful(x.getArguments.apply(1))
      }
      when(remoteConfigManager.getRemoteConfig(?)).thenReturnF(configValue(LastCall.name))
      when(offerStatManager.getCurrentViews(?, ?)(?)).thenReturnF(1)
      when(offerStatManager.getLastCallsStats(?, ?)(?)).thenReturnF(CallsStats(0, Some(lastCallTime)))
      when(offerStatManager.getOfferStat(?, ?)(?)).thenReturnF(dailyCountersResponse)
      val result = manager.getStatNotification(category, offerId).await
      result.getResult.getNotificationType shouldBe StatsNotificationType.LAST_CALL
    }

    "not return last call notification" in new mocks {
      val dailyCountersResponse = DailyCountersResponse.newBuilder().build()
      when(viewedNotificationsManager.getNotViewedNotifications(?, ?, ?)(?)).thenAnswer { x =>
        Future.successful(x.getArguments.apply(1))
      }
      when(remoteConfigManager.getRemoteConfig(?)).thenReturnF(configValue(LastCall.name))
      when(offerStatManager.getCurrentViews(?, ?)(?)).thenReturnF(1)
      when(offerStatManager.getLastCallsStats(?, ?)(?)).thenReturnF(CallsStats(0, Some(offsetTime.minusDays(2))))
      when(offerStatManager.getOfferStat(?, ?)(?)).thenReturnF(dailyCountersResponse)
      val result = manager.getStatNotification(category, offerId).await
      result.hasResult shouldBe false
    }

    "not return already viewed notification" in new mocks {
      val dailyCountersResponse = DailyCountersResponse.newBuilder().build()
      when(viewedNotificationsManager.getNotViewedNotifications(?, ?, ?)(?)).thenAnswer { x =>
        val notifications = x.getArguments.apply(1)
        val result = notifications.asInstanceOf[List[StatsNotification]].filterNot {
          _.getNotificationType == StatsNotificationType.CURRENT_VIEWS
        }
        Future.successful(result)
      }
      when(remoteConfigManager.getRemoteConfig(?)).thenReturnF(configValue(Rotation.name))
      when(offerStatManager.getCurrentViews(?, ?)(?)).thenReturnF(200)
      when(offerStatManager.getLastCallsStats(?, ?)(?)).thenReturnF(CallsStats(0, None))
      when(offerStatManager.getOfferStat(?, ?)(?)).thenReturnF(dailyCountersResponse)
      val result = manager.getStatNotification(category, offerId).await
      result.hasResult shouldBe false
    }

  }

}
