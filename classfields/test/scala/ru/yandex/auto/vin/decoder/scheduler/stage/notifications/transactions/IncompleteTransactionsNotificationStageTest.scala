package ru.yandex.auto.vin.decoder.scheduler.stage.notifications.transactions

import akka.actor.ActorSystem
import akka.testkit.TestKit
import auto.carfax.common.clients.passport.PassportClient
import auto.carfax.common.clients.sender.SenderClient
import auto.carfax.common.clients.spamalot.send.ScalaPBSpamalotClient
import auto.carfax.common.clients.vos.VosClient
import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.api.vin.ResponseModel.RawEssentialsReportResponse
import ru.auto.salesman.model.user.ApiModel.{VinHistoryBoughtReport, VinHistoryBoughtReports}
import ru.yandex.auto.vin.decoder.api.exceptions.{
  IncorrectVinException,
  InvalidLicensePlateException,
  UnknownLicensePlateException,
  UnknownVinException
}
import ru.yandex.auto.vin.decoder.manager.RelationshipManager
import ru.yandex.auto.vin.decoder.model.{AutoruOfferId, LicensePlate, UserRef, VinCode}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.ReportNotificationState.{NotificationContext, NotificationInfo}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, ReportNotificationState}
import ru.yandex.auto.vin.decoder.report.processors.report.ReportManager
import ru.yandex.auto.vin.decoder.salesman.SalesmanClient
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import ru.yandex.auto.vin.decoder.scheduler.stage.StageSupport.createDefaultProcessingState
import ru.yandex.auto.vin.decoder.scheduler.stage.notifications.PurchaseSummaryNotificationsSupport
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.passport.model.api.ApiModel.UserEssentials
import ru.yandex.vertis.commons.http.client.WrongStatusCodeException
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.spamalot.SendResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.IterableHasAsJava

class IncompleteTransactionsNotificationStageTest
  extends TestKit(ActorSystem("test-system"))
  with AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter
  with PurchaseSummaryNotificationsSupport
  with CompoundStageSupport[VinCode, IncompleteTransactionsNotificationStage[VinCode]] {

  private val relationshipManager = mock[RelationshipManager]
  private val salesmanClient = mock[SalesmanClient]
  private val vosClient = mock[VosClient]
  private val reportManager = mock[ReportManager]
  private val spamalotClient = mock[ScalaPBSpamalotClient]
  private val senderClient = mock[SenderClient]
  private val passportClient = mock[PassportClient]

  before {
    reset(relationshipManager)
    reset(salesmanClient)
    reset(vosClient)
    reset(reportManager)
    reset(spamalotClient)
    reset(senderClient)
    reset(passportClient)
  }

  implicit private val t: Traced = Traced.empty

  private val TestVin = VinCode("WDD2120341A813160")
  private val TestLp = LicensePlate("A123AA77")
  private val TestUser = UserRef.user(123L)
  private val TestOfferId = AutoruOfferId(123L, "abc")
  private val stage = createProcessingStage()
  private val passportUserWithEmail = Future.successful(UserEssentials.newBuilder().setEmail("email@ya.ru").build())
  private val passportUserWithEmptyEmail = Future.successful(UserEssentials.newBuilder().setEmail("").build())

  "shouldProcess" should {
    "return true" when {
      "exists not processed notifications" in {
        val processed = buildNotification(tsSent = Some(System.currentTimeMillis()))
        val notProcessed = buildNotification()
        val state = CompoundState
          .newBuilder()
          .setIncompleteTransactionsNotificationState(
            ReportNotificationState.newBuilder().addAllInfo(List(processed, notProcessed).asJava)
          )

        checkShouldProcess(state)
      }
    }
    "return false" when {
      "all notifications already processed" in {
        val processed1 = buildNotification(tsSent = Some(System.currentTimeMillis()))
        val processed2 = buildNotification(tsCancel = Some(System.currentTimeMillis()))
        val state = CompoundState
          .newBuilder()
          .setIncompleteTransactionsNotificationState(
            ReportNotificationState.newBuilder().addAllInfo(List(processed1, processed2).asJava)
          )

        checkIgnored(state)
      }
    }
  }

  "process" should {
    "reschedule" when {
      "not all notifications processed" in {
        val notification1 = buildNotification(vin = Some(TestVin.toString))
        val state = CompoundState
          .newBuilder()
          .setIncompleteTransactionsNotificationState(
            ReportNotificationState.newBuilder().addAllInfo(List(notification1).asJava)
          )

        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(passportClient.getEssentials(?)(?)).thenReturn(passportUserWithEmail)
        when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(
          Future.successful(RawEssentialsReportResponse.newBuilder().build())
        )
        when(spamalotClient.send(?)).thenReturn(Future.failed(new RuntimeException("")))

        val ps = createDefaultProcessingState(state.build())
        val update = stage.processWithAsync(TestVin, ps)
        val updatedState = update.state.getIncompleteTransactionsNotificationState.getInfo(0)

        updatedState.getSentTimestamp shouldBe 0
        updatedState.getCancelTimestamp shouldBe 0
        update.delay.toDuration shouldBe 5.minutes
        verify(spamalotClient, times(1)).send(?)
      }
    }
    "finish" when {
      "all notifications processed" in {
        val notification1 = buildNotification(vin = Some(TestVin.toString))
        val state = CompoundState
          .newBuilder()
          .setIncompleteTransactionsNotificationState(
            ReportNotificationState.newBuilder().addAllInfo(List(notification1).asJava)
          )

        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(passportClient.getEssentials(?)(?)).thenReturn(passportUserWithEmail)
        when(spamalotClient.send(?)).thenReturn(Future.successful(SendResponse.newBuilder().build()))
        when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(
          Future.successful(RawEssentialsReportResponse.newBuilder().build())
        )

        val ps = createDefaultProcessingState(state.build())
        val update = stage.processWithAsync(TestVin, ps)
        val updatedState = update.state.getIncompleteTransactionsNotificationState.getInfo(0)

        updatedState.getSentTimestamp > 0 shouldBe true
        updatedState.getCancelTimestamp shouldBe 0
        verify(spamalotClient, times(1)).send(?)
      }
    }
  }

  "processNotification" should {
    "cancel notification" when {
      "don't need send" in {
        val notification = buildNotification(vin = Some(TestVin.toString))
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(NonEmptySalesmanResponse))

        val res = stage.processNotification(TestVin, notification).await
        res.getCancelTimestamp > 0 shouldBe true
        res.getSentTimestamp shouldBe 0
        verify(spamalotClient, never()).send(?)
      }
      "can't resolve license plate (invalid lp)" in {
        val notification = buildNotification(lp = Some(TestLp.toString))
        when(relationshipManager.resolveVin(?, ?)(?, ?, ?))
          .thenReturn(Future.failed(InvalidLicensePlateException(TestLp)))

        val res = stage.processNotification(TestVin, notification).await
        res.getCancelTimestamp > 0 shouldBe true
        res.getSentTimestamp shouldBe 0
        verify(spamalotClient, never()).send(?)
      }
      "can't resolve license plate (unknown lp)" in {
        val notification = buildNotification(lp = Some(TestLp.toString))
        when(relationshipManager.resolveVin(?, ?)(?, ?, ?))
          .thenReturn(Future.failed(UnknownLicensePlateException(TestLp)))

        val res = stage.processNotification(TestVin, notification).await
        res.getCancelTimestamp > 0 shouldBe true
        res.getSentTimestamp shouldBe 0
        verify(spamalotClient, never()).send(?)
      }
      "invalid vin" in {
        val notification = buildNotification(vin = Some(TestVin.toString))
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(
          Future.failed(IncorrectVinException(""))
        )

        val res = stage.processNotification(TestVin, notification).await
        res.getCancelTimestamp > 0 shouldBe true
        res.getSentTimestamp shouldBe 0
        verify(spamalotClient, never()).send(?)
      }
      "unknown vin" in {
        val notification = buildNotification(vin = Some(TestVin.toString))
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(
          Future.failed(new UnknownVinException(""))
        )

        val res = stage.processNotification(TestVin, notification).await
        res.getCancelTimestamp > 0 shouldBe true
        res.getSentTimestamp shouldBe 0
        verify(spamalotClient, never()).send(?)
      }
    }
    "sent notification" when {
      "need send" in {
        val notification = buildNotification(vin = Some(TestVin.toString))
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(passportClient.getEssentials(?)(?)).thenReturn(passportUserWithEmail)
        when(senderClient.sendLetter(?, ?, ?)(?)).thenReturn(Future.unit)
        when(spamalotClient.send(?)).thenReturn(Future.successful(SendResponse.newBuilder().build()))
        when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(
          Future.successful(RawEssentialsReportResponse.newBuilder().build())
        )

        val res = stage.processNotification(TestVin, notification).await
        res.getCancelTimestamp shouldBe 0
        res.getSentTimestamp > 0 shouldBe true
        verify(spamalotClient, times(1)).send(?)
      }

      "email notification fails with error" in {
        val notification = buildNotification(vin = Some(TestVin.toString))
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(passportClient.getEssentials(?)(?)).thenReturn(passportUserWithEmail)
        when(senderClient.sendLetter(?, ?, ?)(?)).thenReturn(Future.failed(new RuntimeException()))
        when(spamalotClient.send(?)).thenReturn(Future.successful(SendResponse.newBuilder().build()))
        when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(
          Future.successful(RawEssentialsReportResponse.newBuilder().build())
        )

        val res = stage.processNotification(TestVin, notification).await
        res.getCancelTimestamp shouldBe 0
        res.getSentTimestamp > 0 shouldBe true
        verify(spamalotClient, times(1)).send(?)
      }
    }

    "send email notification" when {
      "it receives a user with an email" in {
        val notification = buildNotification(vin = Some(TestVin.toString))
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(passportClient.getEssentials(?)(?)).thenReturn(passportUserWithEmail)
        when(senderClient.sendLetter(?, ?, ?)(?)).thenReturn(Future.unit)
        when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(
          Future.successful(RawEssentialsReportResponse.newBuilder().build())
        )
        when(spamalotClient.send(?)).thenReturn(Future.successful(SendResponse.newBuilder().build()))

        stage.processNotification(TestVin, notification).await
        verify(senderClient, times(1)).sendLetter(?, ?, ?)(?)

      }
    }

    "not to send email notification" when {
      "user email is empty" in {
        val notification = buildNotification(vin = Some(TestVin.toString))
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(passportClient.getEssentials(?)(?)).thenReturn(passportUserWithEmptyEmail)
        when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(
          Future.successful(RawEssentialsReportResponse.newBuilder().build())
        )
        when(spamalotClient.send(?)).thenReturn(Future.successful(SendResponse.newBuilder().build()))

        stage.processNotification(TestVin, notification).await
        verify(senderClient, never()).sendLetter(?, ?, ?)(?)
      }

      "user wasn't found" in {
        val notification = buildNotification(vin = Some(TestVin.toString))
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(passportClient.getEssentials(?)(?)).thenReturn(Future.failed(WrongStatusCodeException(404, null, "")))
        when(reportManager.getEssentialsReport(?, ?)(?)).thenReturn(
          Future.successful(RawEssentialsReportResponse.newBuilder().build())
        )
        when(spamalotClient.send(?)).thenReturn(Future.successful(SendResponse.newBuilder().build()))

        stage.processNotification(TestVin, notification).await
        verify(senderClient, never()).sendLetter(?, ?, ?)(?)
      }
    }
  }

  "checkNeedSendNotification" should {
    "return true" when {
      "report not bought (standalone transaction)" in {
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        val res = stage.checkNeedSendNotification(TestVin, TestUser, None).await
        res shouldBe true
      }
      "report not bought and offer active (offer transaction)" in {
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(Some(buildOffer(OfferStatus.ACTIVE))))
        val res = stage.checkNeedSendNotification(TestVin, TestUser, Some(TestOfferId)).await
        res shouldBe true
      }
    }
    "return false" when {
      "report already bought (standalone transaction)" in {
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(NonEmptySalesmanResponse))
        val res = stage.checkNeedSendNotification(TestVin, TestUser, None).await
        res shouldBe false
      }
      "report already bought and offer active (offer transaction)" in {
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(NonEmptySalesmanResponse))
        when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(Some(buildOffer(OfferStatus.ACTIVE))))
        val res = stage.checkNeedSendNotification(TestVin, TestUser, Some(TestOfferId)).await
        res shouldBe false
      }
      "report not bought and offer inactive (offer transaction)" in {
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(Some(buildOffer(OfferStatus.BANNED))))
        val res = stage.checkNeedSendNotification(TestVin, TestUser, Some(TestOfferId)).await
        res shouldBe false
      }
      "report not bought and offer not found" in {
        when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
          .thenReturn(Future.successful(EmptySalesmanResponse))
        when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(None))
        val res = stage.checkNeedSendNotification(TestVin, TestUser, Some(TestOfferId)).await
        res shouldBe false
      }
    }
  }

  "prepare vin" should {
    "return vin" when {
      "notification contains valid vin" in {
        val notification = buildNotification(Some(TestVin.toString), None, None)
        val res = stage.prepareVin(notification).await
        res shouldBe TestVin
      }
      "notification contains valid known lp" in {
        val notification = buildNotification(None, Some(TestLp.toString), None)
        when(relationshipManager.resolveVin(?, ?)(?, ?, ?)).thenReturn(Future.successful(TestVin))
        val res = stage.prepareVin(notification).await
        res shouldBe TestVin
      }
    }
  }

  private def buildNotification(
      vin: Option[String] = None,
      lp: Option[String] = None,
      offerId: Option[String] = None,
      tsSent: Option[Long] = None,
      tsCancel: Option[Long] = None): NotificationInfo = {
    val contextBuilder = NotificationContext.newBuilder()
    vin.foreach(contextBuilder.setVin)
    lp.foreach(contextBuilder.setLicensePlate)
    offerId.foreach(contextBuilder.setOfferId)

    NotificationInfo
      .newBuilder()
      .setContext(contextBuilder)
      .setUserId(TestUser.toPlain)
      .setSentTimestamp(tsSent.getOrElse(0))
      .setCancelTimestamp(tsCancel.getOrElse(0))
      .build()
  }

  private lazy val EmptySalesmanResponse: VinHistoryBoughtReports = {
    VinHistoryBoughtReports.newBuilder().build()
  }

  private lazy val NonEmptySalesmanResponse: VinHistoryBoughtReports = {
    VinHistoryBoughtReports
      .newBuilder()
      .addReports(
        VinHistoryBoughtReport.newBuilder().build()
      )
      .build()
  }

  private def buildOffer(status: OfferStatus): ApiOfferModel.Offer = {
    ApiOfferModel.Offer.newBuilder().setStatus(status).build()
  }

  override def createProcessingStage(): IncompleteTransactionsNotificationStage[VinCode] =
    new IncompleteTransactionsNotificationStage[VinCode](
      relationshipManager,
      salesmanClient,
      vosClient,
      reportManager,
      spamalotClient,
      senderClient,
      passportClient
    )
}
