package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.notifications

import org.mockito.Mockito.{doNothing, doReturn, spy}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.YdbWorkerTestImpl
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType._
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.letters.renderers._
import ru.yandex.vos2.autoru.model.AutoruModelUtils.AutoruRichOfferBuilder
import ru.yandex.vos2.autoru.notifications.AutoruNotifyManager
import ru.yandex.vos2.autoru.services.autoru_api.AutoruApiClient
import ru.yandex.vos2.autoru.services.chat.ChatClient
import ru.yandex.vos2.notifications.RendererByUser
import ru.yandex.vos2.services.phone.DefaultSmsSenderClient
import ru.yandex.vos2.services.pushnoy.DefaultPushnoyClient
import ru.yandex.vos2.services.sender.HttpSenderClient
import ru.yandex.vos2.services.spamalot.SpamalotClient

import java.time.{ZoneId, ZonedDateTime}
import scala.util.Success

class NotificationProcessorWorkerYdbTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with InitTestDbs
  with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    def getWorker(currentTime: ZonedDateTime, notifyManager: AutoruNotifyManager) = {
      val mockStage = spy(
        new NotificationProcessorWorkerYdb(
          notifyManager,
          components.regionTree
        ) with YdbWorkerTestImpl
      )

      doReturn(currentTime, Nil: _*).when(mockStage).getCurrentTime(any[ZoneId]())
      mockStage
    }
  }

  ("Send no critical notification at day") in new Fixture {
    val offer = getOfferBuilder(false)
    val notifyManager = getNotifyManager(false)
    val worker = getWorker(ZonedDateTime.now().withHour(14), notifyManager)
    val result = worker.process((offer.build()), None).updateOfferFunc.get(offer.build())
    assert(result.getOfferAutoru.getNotificationsCount == 1)
    assert(result.getOfferAutoru.getNotifications(0).hasTimestampSent)
  }

  ("Not send no critical notification at night") in new Fixture {
    val offer = getOfferBuilder(false)
    val notifyManager = getNotifyManager(false)
    val worker = getWorker(ZonedDateTime.now().withHour(23), notifyManager)
    val result = worker.process((offer.build()), None)

    assert(result.updateOfferFunc.isEmpty)
    assert(offer.getOfferAutoru.getNotificationsCount == 1)
    assert(!offer.getOfferAutoru.getNotifications(0).hasTimestampSent)
  }

  ("Send critical notification at day") in new Fixture {
    val offer = getOfferBuilder(true)
    val notifyManager = getNotifyManager(false)
    val worker = getWorker(ZonedDateTime.now().withHour(14), notifyManager)
    val result = worker.process((offer.build()), None).updateOfferFunc.get(offer.build())

    assert(result.getOfferAutoru.getNotificationsCount == 1)
    assert(result.getOfferAutoru.getNotifications(0).hasTimestampSent)
  }

  ("Send critical notification at night") in new Fixture {
    val offer = getOfferBuilder(true)
    val notifyManager = getNotifyManager(false)
    val worker = getWorker(ZonedDateTime.now().withHour(23), notifyManager)
    val result = worker.process((offer.build()), None).updateOfferFunc.get(offer.build())

    assert(result.getOfferAutoru.getNotificationsCount == 1)
    assert(result.getOfferAutoru.getNotifications(0).hasTimestampSent)
  }

  ("Send is failed") in new Fixture {
    val offer = getOfferBuilder(false)
    val notifyManager = getNotifyManager(true)
    val worker = getWorker(ZonedDateTime.now().withHour(14), notifyManager)
    val result = worker.process((offer.build()), None)
    val resultOffer = result.updateOfferFunc.get(offer.build())

    assert(resultOffer.getOfferAutoru.getNotifications(0).getNumTries == 1)
    assert(!resultOffer.getOfferAutoru.getNotifications(0).hasTimestampSent)
    assert(result.nextCheck.nonEmpty)
  }

  ("Nothing to send") in new Fixture {
    val offer = getOfferBuilder(false)
    offer.getOfferAutoruBuilder.clearNotifications()

    val notifyManager = getNotifyManager(false)
    val worker = getWorker(ZonedDateTime.now(), notifyManager)
    val result = worker.process((offer.build()), None)

    assert(offer.getOfferAutoru.getNotificationsCount == 0)
    assert(result.nextCheck.isEmpty)
  }

  ("Tries to send") in new Fixture {
    val offer = getOfferBuilder(false)
    offer.getOfferAutoruBuilder.getNotificationsBuilder(0).setMaxTries(2)

    val notifyManager = getNotifyManager(true)
    val worker = getWorker(ZonedDateTime.now().withHour(14), notifyManager)
    val result = worker.process((offer.build()), None)
    val resultOffer = result.updateOfferFunc.get(offer.build())

    assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
    assert(!resultOffer.getOfferAutoru.getNotifications(0).hasTimestampSent)
    assert(resultOffer.getOfferAutoru.getNotifications(0).getNumTries == 1)
    assert(result.nextCheck.nonEmpty)

    // И еще разок
    val result2 = worker.process(resultOffer, None)
    val resultOffer2 = result2.updateOfferFunc.get(resultOffer)

    assert(resultOffer2.getOfferAutoru.getNotificationsCount == 1)
    assert(!resultOffer2.getOfferAutoru.getNotifications(0).hasTimestampSent)
    assert(resultOffer2.getOfferAutoru.getNotifications(0).getNumTries == 2)
    assert(result2.nextCheck.nonEmpty)
  }

  private def getOfferBuilder(isCritical: Boolean): Offer.Builder = {
    val notificationType = {
      if (isCritical) NotificationType.MODERATION_BAN
      else NotificationType.OFFER_CREATION
    }

    val offer = getOfferById(1043026846L).toBuilder
      .putNotificationByType(notificationType, isCritical = isCritical)

    offer.getUserBuilder.getUserContactsBuilder
      .setEmail("amisyura@yandex-team.ru")

    if (isCritical) offer.addReasonsBan("WRONG_MODEL")

    offer
  }

  private def getMockSenderClient(thrownResult: Boolean = false) = {
    val client = mockStrict[HttpSenderClient]

    if (thrownResult) {
      when(client.sendLetter(?, ?)).thenThrow(classOf[RuntimeException])
    } else {
      when(client.sendLetter(?, ?)).thenReturn(Success(()))
    }

    client
  }

  private def getMockSmsClient = {
    val client = mockStrict[DefaultSmsSenderClient]
    when(client.send(?, ?)(?)).thenReturn(Success(""))
    client
  }

  private def getMockPushSenderClient = {
    val client = mockStrict[DefaultPushnoyClient]
    when(client.pushToUser(?, ?)(?)).thenReturn(Success(1))
    client
  }

  private def getMockSpamalotClient = {
    val client = mockStrict[SpamalotClient]
    when(client.send(?)(?)).thenReturn(Success(()))
    client
  }

  private def getMockChatClient = {
    val client = mockStrict[ChatClient]
    doNothing().when(client).serviceNotification(?, ?, ?)(?)
    client
  }

  private def getMockAutoruApiClient = {
    val client = mockStrict[AutoruApiClient]
    doNothing().when(client).sendToOfferChat(?, ?, ?, ?, ?, ?, ?, ?)(?)
    client
  }

  private def getRenderers = Map(
    OFFER_EXPIRATION -> OfferExpiration,
    OFFER_NOT_ACTUAL -> OfferNotActual,
    OFFER_CREATION -> new OfferCreationRenderer(
      components.carsCatalog,
      components.regionTree,
      components.mdsPhotoUtils,
      components.passportClient,
      components.featuresManager.TokenLoginSmsAfterCallCenter
    ),
    MODERATION_BAN -> new ModerationBanRenderer(
      components.carsCatalog,
      components.trucksCatalog,
      components.motoCatalog,
      components.banReasons,
      components.featuresManager.DuplicateBanNotification,
      components.featuresManager.NotificationsOverSpamalot
    ),
    LOW_RATING -> new LowRatingRenderer(components.featuresManager.NotificationsOverSpamalot),
    OFFER_HIDE_NO_PHONES -> new OfferHideNoPhones(
      components.carsCatalog,
      components.trucksCatalog,
      components.motoCatalog
    ),
    RECHECK_REACTIVATED_OFFER -> new RecheckReactivatedOfferRenderer(components.offerReactivationDictionary)
  )

  private def getUserRenderers: Map[NotificationType, RendererByUser] = Map()

  private def getNotifyManager(thrownSmsSenderResult: Boolean): AutoruNotifyManager = {
    new AutoruNotifyManager(
      emailSenderClient = getMockSenderClient(thrownSmsSenderResult),
      smsSenderClient = getMockSmsClient,
      chatClient = getMockChatClient,
      publicApiClient = getMockAutoruApiClient,
      pushClient = getMockPushSenderClient,
      renderers = getRenderers,
      userRenderers = getUserRenderers,
      features = components.featuresManager,
      spamalotClient = getMockSpamalotClient
    )
  }

}
