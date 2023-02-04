package ru.yandex.auto.vin.decoder.scheduler.stage.notifications

import auto.carfax.common.clients.passport.PassportClient
import auto.carfax.common.clients.pushnoy.PushnoyClient
import auto.carfax.common.clients.sender.SenderClient
import auto.carfax.common.clients.vos.VosClient
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.ResponseModel.RawEssentialsReportResponse
import ru.auto.api.vin.VinReportModel.PtsBlock.{IntItem, StringItem}
import ru.auto.api.vin.VinReportModel.{AutoruOffersBlock, OfferRecord, PtsBlock, RawVinEssentialsReport}
import ru.auto.api.{ApiOfferModel, CommonModel}
import ru.yandex.auto.vin.decoder.model.{CommonVinCode, VinCode}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.ReportNotificationState.NotificationInfo
import ru.yandex.auto.vin.decoder.report.converters.formats.proto.builder.additional.UrlBuilder
import ru.yandex.auto.vin.decoder.report.processors.report.ReportManager
import ru.yandex.auto.vin.decoder.scheduler.models.{ExactDelay, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import ru.yandex.auto.vin.decoder.scheduler.stage.StageSupport.createDefaultProcessingState
import ru.yandex.auto.vin.decoder.utils.sources.PartnerProgressManager.PartnersProgress
import ru.yandex.auto.vin.decoder.utils.sources.ProgressCounter.IntCounter
import ru.yandex.auto.vin.decoder.utils.sources.{PartnerProgress, PartnerProgressManager, ProgressCounter}
import ru.yandex.passport.model.api.ApiModel.UserEssentials
import ru.yandex.pushnoy.PushRequestModel.{SendPushTemplateRequest, VinReportIsReadyTemplateRequest}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}

class ReadyReportNotificationStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfterAll
  with Matchers
  with CompoundStageSupport[VinCode, ReadyReportNotificationStage] {

  private val vinCode = CommonVinCode("WAUZZZF52HA005675")

  private val offerId = "123123"

  private val essentialReport: RawEssentialsReportResponse = {
    val pts =
      PtsBlock
        .newBuilder()
        .setMark(StringItem.newBuilder().setValueText("mark"))
        .setModel(StringItem.newBuilder().setValueText("model"))
        .setYear(IntItem.newBuilder().setValue(2020))

    val offers = AutoruOffersBlock
      .newBuilder()
      .addOffers(
        OfferRecord
          .newBuilder()
          .setOfferId(offerId)
      )

    val report =
      RawVinEssentialsReport
        .newBuilder()
        .setPtsInfo(pts)
        .setAutoruOffers(offers)

    RawEssentialsReportResponse.newBuilder().setReport(report).build()
  }

  private def getProgress(counter: ProgressCounter): PartnersProgress = {
    PartnersProgress(Seq(PartnerProgress("", counter, Seq.empty)))
  }

  "ReadyReportNotificationStage" should {

    "send notifications according to report readiness" in {

      val partnerProgressManager: PartnerProgressManager = mock[PartnerProgressManager]
      val passportClient: PassportClient = mock[PassportClient]
      val senderClient: SenderClient = mock[SenderClient]
      val pushnoyClient: PushnoyClient = mock[PushnoyClient]
      val reportManager: ReportManager = mock[ReportManager]
      val urlBuilder: UrlBuilder = mock[UrlBuilder]
      val pushFeature: Feature[Boolean] = mock[Feature[Boolean]]
      val vosClient: VosClient = mock[VosClient]

      val enabledDealer = 1L
      val disabledDealer = 2L
      when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, eq(Some(enabledDealer))))
        .thenReturn(getProgress(1.of(1)))
      when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, eq(Some(disabledDealer))))
        .thenReturn(getProgress(0.of(0)))
      when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, eq(None)))
        .thenReturn(getProgress(1.of(1)))

      when(reportManager.getEssentialsReport(?, ?)(?))
        .thenReturn(Future.successful(essentialReport))

      when(senderClient.sendLetterWithJsonParams(?, ?, ?)(?)).thenReturn(Future.unit)

      when(passportClient.getEssentials(?)(?))
        .thenReturn(Future.successful(UserEssentials.newBuilder().setEmail("email@email").build()))

      when(pushnoyClient.sendPushes(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(0))

      when(pushFeature.value).thenReturn(true)

      when(urlBuilder.forPaidReportPdf(?, ?, ?)).thenReturn("http://url")

      when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(None))

      val push =
        SendPushTemplateRequest
          .newBuilder()
          .setVinReportReady(
            VinReportIsReadyTemplateRequest
              .newBuilder()
              .setMarkName("mark")
              .setModelName("model")
              .setVin(vinCode.toString)
              .setYear(2020)
          )
          .build()

      val info1 =
        NotificationInfo
          .newBuilder()
          .setUserId("test1")
          .setSentTimestamp(System.currentTimeMillis())
          .build()

      val info2 =
        NotificationInfo.newBuilder
          .setUserId("test2")
          .setClientId(enabledDealer)
          .build

      val info3 =
        NotificationInfo.newBuilder
          .setUserId("test3")
          .setClientId(disabledDealer)
          .build

      val stateBuilder = CompoundState.newBuilder()

      stateBuilder.getReadyReportNotificationStateBuilder
        .addInfo(info1)
        .addInfo(info2)
        .addInfo(info3)

      val ps = createDefaultProcessingState(stateBuilder.build())
      val stage = new ReadyReportNotificationStage(
        partnerProgressManager,
        passportClient,
        senderClient,
        pushnoyClient,
        reportManager,
        urlBuilder,
        vosClient,
        pushFeature
      )

      val res: WatchingStateUpdate[CompoundState] = stage.processWithAsync(vinCode, ps)

      verify(senderClient, times(2)).sendLetter(?, ?, ?)(?)
      verify(pushnoyClient).sendPushes(eq("test2"), eq(push), ?, ?, ?)(?)
      verify(partnerProgressManager, times(2)).buildReportProgress(?, ?, ?, ?, ?)
      verify(passportClient, times(2)).getEssentials(?)(?)
      verify(pushFeature, times(2)).value

      val newState = res.state.getReadyReportNotificationState

      newState.getInfoCount shouldBe 3
      newState.getInfoList.asScala.count(_.getSentTimestamp != 0) shouldBe 3

    }

    "dont send notifications if not ready" in {

      val partnerProgressManager: PartnerProgressManager = mock[PartnerProgressManager]
      val passportClient: PassportClient = mock[PassportClient]
      val senderClient: SenderClient = mock[SenderClient]
      val pushnoyClient: PushnoyClient = mock[PushnoyClient]
      val reportManager: ReportManager = mock[ReportManager]
      val urlBuilder: UrlBuilder = mock[UrlBuilder]
      val pushFeature: Feature[Boolean] = mock[Feature[Boolean]]
      val vosClient: VosClient = mock[VosClient]

      when(pushFeature.value).thenReturn(true)

      when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, ?))
        .thenReturn(getProgress(1.of(2)))

      when(reportManager.getEssentialsReport(?, ?)(?))
        .thenReturn(Future.successful(essentialReport))

      when(senderClient.sendLetterWithJsonParams(?, ?, ?)(?)).thenReturn(Future.unit)

      when(passportClient.getEssentials(?)(?))
        .thenReturn(Future.successful(UserEssentials.newBuilder().build()))

      when(pushnoyClient.sendPushes(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(0))

      when(urlBuilder.forPaidReportPdf(?, ?, ?)).thenReturn("http://url")

      val info1 =
        NotificationInfo
          .newBuilder()
          .setUserId("test1")
          .setSentTimestamp(System.currentTimeMillis())
          .build()

      val info2 =
        NotificationInfo
          .newBuilder()
          .setUserId("test2")
          .build()

      val stateBuilder = CompoundState.newBuilder()

      stateBuilder.getReadyReportNotificationStateBuilder
        .addInfo(info1)
        .addInfo(info2)

      val ps = createDefaultProcessingState(stateBuilder.build())
      val stage = new ReadyReportNotificationStage(
        partnerProgressManager,
        passportClient,
        senderClient,
        pushnoyClient,
        reportManager,
        urlBuilder,
        vosClient,
        pushFeature
      )
      val res: WatchingStateUpdate[CompoundState] = stage.processWithAsync(vinCode, ps)

      verifyNoInteractions(senderClient)
      verifyNoInteractions(pushnoyClient)
      verify(partnerProgressManager).buildReportProgress(?, ?, ?, ?, ?)
      verifyNoInteractions(passportClient)
      verifyNoInteractions(reportManager)
      verifyNoInteractions(pushFeature)

      val newState = res.state.getReadyReportNotificationState

      res.delay shouldBe ExactDelay(1.minutes)
      newState.getInfoCount shouldBe 2
      newState.getInfoList.asScala.count(_.getSentTimestamp != 0) shouldBe 1
    }

    "dont send notifications if no unsent notification" in {

      val partnerProgressManager: PartnerProgressManager = mock[PartnerProgressManager]
      val passportClient: PassportClient = mock[PassportClient]
      val senderClient: SenderClient = mock[SenderClient]
      val pushnoyClient: PushnoyClient = mock[PushnoyClient]
      val reportManager: ReportManager = mock[ReportManager]
      val urlBuilder: UrlBuilder = mock[UrlBuilder]
      val pushFeature: Feature[Boolean] = mock[Feature[Boolean]]
      val vosClient: VosClient = mock[VosClient]

      when(pushFeature.value).thenReturn(true)

      when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, ?))
        .thenReturn(getProgress(1.of(1)))

      when(reportManager.getEssentialsReport(?, ?)(?))
        .thenReturn(Future.successful(essentialReport))

      when(senderClient.sendLetterWithJsonParams(?, ?, ?)(?)).thenReturn(Future.unit)

      when(passportClient.getEssentials(?)(?))
        .thenReturn(Future.successful(UserEssentials.newBuilder().build()))

      when(pushnoyClient.sendPushes(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(0))

      when(urlBuilder.forPaidReportPdf(?, ?, ?)).thenReturn("http://url")

      when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(None))

      val info1 =
        NotificationInfo
          .newBuilder()
          .setUserId("test1")
          .setSentTimestamp(System.currentTimeMillis())
          .build()

      val stateBuilder = CompoundState.newBuilder()

      stateBuilder.getReadyReportNotificationStateBuilder
        .addInfo(info1)

      val ps = createDefaultProcessingState(stateBuilder.build())
      val stage = new ReadyReportNotificationStage(
        partnerProgressManager,
        passportClient,
        senderClient,
        pushnoyClient,
        reportManager,
        urlBuilder,
        vosClient,
        pushFeature
      )
      val res: WatchingStateUpdate[CompoundState] = stage.processWithAsync(vinCode, ps)

      verifyNoInteractions(senderClient)
      verifyNoInteractions(pushnoyClient)
      verifyNoInteractions(partnerProgressManager)
      verifyNoInteractions(reportManager)
      verifyNoInteractions(passportClient)
      verifyNoInteractions(pushFeature)

      val newState = res.state.getReadyReportNotificationState

      newState.getInfoCount shouldBe 1
      newState.getInfoList.asScala.count(_.getSentTimestamp != 0) shouldBe 1
    }
  }

  "ReadyReportNotificationStage" should {

    "send image and pdf url in letter notification" in {
      val partnerProgressManager: PartnerProgressManager = mock[PartnerProgressManager]
      val passportClient: PassportClient = mock[PassportClient]
      val senderClient: SenderClient = mock[SenderClient]
      val pushnoyClient: PushnoyClient = mock[PushnoyClient]
      val reportManager: ReportManager = mock[ReportManager]
      val urlBuilder: UrlBuilder = mock[UrlBuilder]
      val pushFeature: Feature[Boolean] = mock[Feature[Boolean]]
      val vosClient: VosClient = mock[VosClient]

      when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, ?))
        .thenReturn(getProgress(1.of(1)))

      when(reportManager.getEssentialsReport(?, ?)(?))
        .thenReturn(Future.successful(essentialReport))

      when(senderClient.sendLetterWithJsonParams(?, ?, ?)(?)).thenReturn(Future.unit)

      when(passportClient.getEssentials(?)(?))
        .thenReturn(
          Future.successful(
            UserEssentials
              .newBuilder()
              .setEmail("email@nemail.ya")
              .build()
          )
        )

      when(pushnoyClient.sendPushes(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(0))

      when(pushFeature.value).thenReturn(false)
      when(urlBuilder.forPaidReportPdf(None, None, vinCode)).thenReturn("http://url")

      val info1 =
        NotificationInfo
          .newBuilder()
          .setUserId("test1")
          .build()

      val stateBuilder = CompoundState.newBuilder()

      stateBuilder.getReadyReportNotificationStateBuilder
        .addInfo(info1)

      val ps = createDefaultProcessingState(stateBuilder.build())
      val stage = new ReadyReportNotificationStage(
        partnerProgressManager,
        passportClient,
        senderClient,
        pushnoyClient,
        reportManager,
        urlBuilder,
        vosClient,
        pushFeature
      )

      when(vosClient.getOffer(?)(?)).thenReturn(
        Future.successful(
          Some(
            ApiOfferModel.Offer
              .newBuilder()
              .setState(
                ApiOfferModel.State
                  .newBuilder()
                  .addAllImageUrls(
                    Seq(
                      CommonModel.Photo
                        .newBuilder()
                        .putSizes("832x624", "//photo_url")
                        .build()
                    ).asJava
                  )
              )
              .build()
          )
        )
      )

      stage.processWithAsync(vinCode, ps)

      verify(senderClient).sendLetter(
        eq("vin_report_done"),
        eq("email@nemail.ya"),
        eq(
          Map(
            "vin" -> vinCode.toString,
            "year" -> "2020",
            "mark_model" -> "mark model",
            "url_download" -> "http://url",
            "image" -> "https://photo_url"
          )
        )
      )(?)
    }

    "send pdf url and not image url in letter notification" in {
      val partnerProgressManager: PartnerProgressManager = mock[PartnerProgressManager]
      val passportClient: PassportClient = mock[PassportClient]
      val senderClient: SenderClient = mock[SenderClient]
      val pushnoyClient: PushnoyClient = mock[PushnoyClient]
      val reportManager: ReportManager = mock[ReportManager]
      val urlBuilder: UrlBuilder = mock[UrlBuilder]
      val pushFeature: Feature[Boolean] = mock[Feature[Boolean]]
      val vosClient: VosClient = mock[VosClient]

      when(partnerProgressManager.buildReportProgress(?, ?, ?, ?, ?))
        .thenReturn(getProgress(1.of(1)))

      when(reportManager.getEssentialsReport(?, ?)(?))
        .thenReturn(Future.successful(essentialReport))

      when(senderClient.sendLetterWithJsonParams(?, ?, ?)(?)).thenReturn(Future.unit)

      when(passportClient.getEssentials(?)(?))
        .thenReturn(
          Future.successful(
            UserEssentials
              .newBuilder()
              .setEmail("email@nemail.ya")
              .build()
          )
        )

      when(pushnoyClient.sendPushes(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(0))

      when(pushFeature.value).thenReturn(false)
      when(urlBuilder.forPaidReportPdf(None, None, vinCode)).thenReturn("http://url")

      val info1 =
        NotificationInfo
          .newBuilder()
          .setUserId("test1")
          .build()

      val stateBuilder = CompoundState.newBuilder()

      stateBuilder.getReadyReportNotificationStateBuilder
        .addInfo(info1)

      val ps = createDefaultProcessingState(stateBuilder.build())
      val stage = new ReadyReportNotificationStage(
        partnerProgressManager,
        passportClient,
        senderClient,
        pushnoyClient,
        reportManager,
        urlBuilder,
        vosClient,
        pushFeature
      )

      when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(None))

      stage.processWithAsync(vinCode, ps)

      verify(senderClient).sendLetter(
        eq("vin_report_done"),
        eq("email@nemail.ya"),
        eq(
          Map(
            "vin" -> vinCode.toString,
            "year" -> "2020",
            "mark_model" -> "mark model",
            "url_download" -> "http://url"
          )
        )
      )(?)
    }
  }

  override def createProcessingStage(): ReadyReportNotificationStage = ???
}
