package ru.yandex.auto.vin.decoder.scheduler.stage.notifications

import auto.carfax.common.clients.pushnoy.PushnoyClient
import auto.carfax.common.clients.vos.VosClient
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.ApiOfferModel
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.ReportNotificationState.NotificationInfo
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import ru.yandex.auto.vin.decoder.scheduler.stage.notifications.PurchaseSummaryNotificationStage.NotificationMeta
import auto.carfax.common.utils.misc.DateTimeUtils.RichProtoTimestamp
import ru.yandex.pushnoy.PushRequestModel.{ReportPurchaseSummaryForOfferOwnerTemplateRequest, SendPushTemplateRequest}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.ListHasAsScala

class PurchaseSummaryNotificationStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfterAll
  with PurchaseSummaryNotificationsSupport
  with CompoundStageSupport[VinCode, PurchaseSummaryNotificationStage] {

  implicit val t: Traced = Traced.empty

  private val pushnoyClient = mock[PushnoyClient]
  private val pushFeature = mock[Feature[Boolean]]
  private val vosClient = mock[VosClient]

  private def todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS)
  private def tomorrowStart = todayStart.plus(1, ChronoUnit.DAYS)
  private def yesterdayStart = todayStart.minus(1, ChronoUnit.DAYS)
  private def twoDaysAgoStart = yesterdayStart.minus(1, ChronoUnit.DAYS)

  override def beforeAll(): Unit = {
    when(pushFeature.value).thenReturn(true)
    when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(Some(buildVosOffer.build())))
    ()
  }

  "PurchaseSummaryNotificationStage" should {

    "process state even if purchase history is empty" in {
      val state = CompoundState.newBuilder()
      state.getPurchaseSummaryNotificationStateBuilder
        .addNotifications(buildPurchaseSummaryNotification(yesterdayStart))
      checkShouldProcess(state)
    }

    "process state having only notification scheduled for tomorrow" in {
      val state = CompoundState.newBuilder()
      state.getPurchaseSummaryNotificationStateBuilder
        .addNotifications(buildPurchaseSummaryNotification(tomorrowStart))
      checkShouldProcess(state)
    }

    "not send notifications and return correct delay" in {
      val stage = createProcessingStage()
      val state = CompoundState.newBuilder()
      val scheduledFor = tomorrowStart
      state.getPurchaseSummaryNotificationStateBuilder
        .addNotifications(buildPurchaseSummaryNotification(scheduledFor))

      val now = System.currentTimeMillis()
      // Учтём небольшую погрешность
      val allowedError = 1000 * 60
      val (minExpectedDelay, maxExpectedDelay) =
        (scheduledFor.toEpochMilli - now - allowedError, scheduledFor.toEpochMilli - now + allowedError)
      val update = stage.processWithAsync(vin, state)
      val actualDelay = update.delay.toDuration.toMillis
      val beExpectedDelay = be > minExpectedDelay and be < maxExpectedDelay
      actualDelay should beExpectedDelay
    }

    "send only today's notification and reschedule for the tomorrow's notification" in {
      val stage = createProcessingStage()
      val todayPush = buildPurchaseSummaryNotification(
        todayStart,
        List(
          buildPurchase(yesterdayStart),
          buildPurchase(yesterdayStart.plus(1, ChronoUnit.HOURS)),
          buildPurchase(yesterdayStart.plus(2, ChronoUnit.HOURS)),
          buildPurchase(yesterdayStart.plus(3, ChronoUnit.HOURS))
        )
      )
      val tomorrowPush = buildPurchaseSummaryNotification(
        tomorrowStart,
        List(
          buildPurchase(todayStart),
          buildPurchase(todayStart.plus(1, ChronoUnit.HOURS)),
          buildPurchase(todayStart.plus(2, ChronoUnit.HOURS)),
          buildPurchase(todayStart.plus(3, ChronoUnit.HOURS))
        )
      )
      val state = CompoundState.newBuilder()
      state.getPurchaseSummaryNotificationStateBuilder
        .addNotifications(todayPush)
        .addNotifications(tomorrowPush)
      val expectedPushTemplate = buildExpectedPushTemplate(4)
      when(pushnoyClient.sendPushes(eq(userRef), eq(expectedPushTemplate), eq(None), eq(None), eq(None))(?))
        .thenReturn(Future.successful(0))
      val compoundUpdate = stage.processWithAsync(vin, state)
      val update = compoundUpdate.state.getPurchaseSummaryNotificationState
      // 1 уведомление отправлено и 1 не отправлено
      val notifications = update.getNotificationsList.asScala
      notifications.exists(_.getSentTimestamp > 0) shouldBe true
      notifications.exists(_.getSentTimestamp == 0) shouldBe true
      // задержка до момента отправления неотправленного уведомления
      val nextVisit = tomorrowPush.getScheduledForTimestamp.getMillis
      val now = System.currentTimeMillis()
      val (minDelay, maxDelay) = (nextVisit - now - 10000, nextVisit - now + 10000)
      val beCorrectDelay = be >= minDelay and be <= maxDelay
      compoundUpdate.delay.toDuration.toMillis should beCorrectDelay
    }

    "send 1 day old notification as well as the today's one" in {
      val stage = createProcessingStage()
      val todayPush = buildPurchaseSummaryNotification(todayStart, List(buildPurchase(yesterdayStart)))
      val latePush = buildPurchaseSummaryNotification(yesterdayStart, List(buildPurchase(twoDaysAgoStart)))
      val state = CompoundState.newBuilder()
      state.getPurchaseSummaryNotificationStateBuilder
        .addNotifications(todayPush)
        .addNotifications(latePush)
      val expectedPushTemplate = buildExpectedPushTemplate(1)
      when(pushnoyClient.sendPushes(eq(userRef), eq(expectedPushTemplate), eq(None), eq(None), eq(None))(?))
        .thenReturn(Future.successful(0))
      val compoundUpdate = stage.processWithAsync(vin, state)
      val update = compoundUpdate.state.getPurchaseSummaryNotificationState
      // 1 уведомление отправлено и 1 не отправлено
      update.getNotificationsCount shouldBe 2
      update.getNotificationsList.asScala.forall(_.getSentTimestamp > 0) shouldBe true
    }

    "do not process state if notifications were canceled" in {
      val state = CompoundState.newBuilder()
      state.getPurchaseSummaryNotificationStateBuilder
        .addNotifications(
          buildPurchaseSummaryNotification(yesterdayStart).setCancelTimestamp(System.currentTimeMillis())
        )
      val stage = createProcessingStage()
      stage.shouldProcess(state) shouldBe false
    }

    "cancel push for non-active offer" in {
      val stage = createProcessingStage()
      val todayPush = buildPurchaseSummaryNotification(todayStart, List(buildPurchase(yesterdayStart)))
      val state = CompoundState.newBuilder()
      state.getPurchaseSummaryNotificationStateBuilder
        .addNotifications(todayPush)
      when(vosClient.getOffer(?)(?)).thenReturn(
        Future.successful(
          Some(
            buildVosOffer
              .setStatus(ApiOfferModel.OfferStatus.BANNED)
              .build()
          )
        )
      )
      val compoundUpdate = stage.processWithAsync(vin, state)
      val update = compoundUpdate.state.getPurchaseSummaryNotificationState
      // 1 уведомление отправлено и 1 не отправлено
      val notifications = update.getNotificationsList.asScala
      notifications.nonEmpty shouldBe true
      notifications.exists(_.getSentTimestamp > 0) shouldBe false
      notifications.forall(_.getCancelTimestamp > 0) shouldBe true
    }

    "not send push for user already bought the report" in {
      val stage = createProcessingStage()
      val todayPush = buildPurchaseSummaryNotification(todayStart, List(buildPurchase(yesterdayStart)))
      val state = CompoundState.newBuilder()
      state.getPurchaseSummaryNotificationStateBuilder
        .addNotifications(todayPush)
      state.getReadyReportNotificationStateBuilder
        .addInfo(NotificationInfo.newBuilder().setUserId(userRef))
      when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(Some(buildVosOffer.build())))
      val compoundUpdate = stage.processWithAsync(vin, state)
      val update = compoundUpdate.state.getPurchaseSummaryNotificationState
      // 1 уведомление отправлено и 1 не отправлено
      val notifications = update.getNotificationsList.asScala
      notifications.nonEmpty shouldBe true
      notifications.forall(_.getSentTimestamp == 0) shouldBe true
      notifications.forall(_.getCancelTimestamp > 0) shouldBe true
    }
  }

  override def createProcessingStage(): PurchaseSummaryNotificationStage =
    new PurchaseSummaryNotificationStage(pushnoyClient, vosClient, pushFeature)

  private def buildExpectedPushTemplate(purchaseCount: Int): SendPushTemplateRequest = {
    SendPushTemplateRequest
      .newBuilder()
      .setReportPurchaseSummary(
        ReportPurchaseSummaryForOfferOwnerTemplateRequest
          .newBuilder()
          .setPushName("purchase_summary_for_owner")
          .setDeeplink(s"autoru://app/cars/used/sale/$offerId/history")
          .setText(buildNotificationText(NotificationMeta(mark, model, vin.toString, year), purchaseCount))
          .setTitle("Отчёт о вашем автомобиле")
      )
      .build()
  }

  private[this] def countableWord(count: Int): String = {
    if (List(2, 3, 4).contains(count % 10) && (count < 10 || count > 20)) {
      "раза"
    } else {
      "раз"
    }
  }

  private def buildNotificationText(meta: NotificationMeta, purchaseCount: Int) =
    if (purchaseCount < 2) {
      s"Вчера по вашему ${meta.mark} ${meta.model} ${meta.year} купили отчёт.\nКупите и вы!"
    } else {
      s"Вчера отчёт купили $purchaseCount ${countableWord(purchaseCount)}.\nКупите и вы!"
    }
}
