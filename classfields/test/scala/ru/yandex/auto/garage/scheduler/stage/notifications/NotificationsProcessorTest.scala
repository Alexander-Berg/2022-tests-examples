package ru.yandex.auto.garage.scheduler.stage.notifications

import auto.carfax.common.clients.chat.ChatClient
import auto.carfax.common.clients.passport.PassportClient
import auto.carfax.common.clients.sender.SenderClient
import auto.carfax.common.clients.spamalot.send.ScalaPBSpamalotClient
import auto.carfax.common.utils.tracing.Traced
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import org.mockito.Mockito.{atLeastOnce, never, reset, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.auto.api.vin.Common.IdentifierType
import ru.yandex.auto.garage.scheduler.stage.notifications.NotificationsTestUtils.buildInsurancePolicy
import ru.yandex.auto.vin.decoder.extdata.catalog.YoctoCarsCatalog
import ru.yandex.auto.vin.decoder.extdata.moderation.ProvenOwnerValidationDictionary
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Notification.NotificationType
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.ProvenOwnerState.{DocumentsPhotos, Verdict}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.{
  GarageCard,
  InsuranceInfo,
  Notification,
  ProvenOwnerState,
  Source,
  SourceInfo
}
import ru.yandex.auto.vin.decoder.model.UserRef
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.spamalot.SendResponse
import ru.yandex.vertis.util.time.DateTimeUtil

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationsProcessorTest extends AnyWordSpecLike with MockitoSupport with Matchers with BeforeAndAfter {

  implicit val t: Traced = Traced.empty

  private val chatClient = mock[ChatClient]
  private val spamalotClient = mock[ScalaPBSpamalotClient]
  private val passportClient = mock[PassportClient]
  private val senderClient = mock[SenderClient]
  private val carsCatalog = mock[YoctoCarsCatalog]

  private val TestUser = UserRef.user(123L)

  when(carsCatalog.getMarkByCode(?)).thenReturn(None)
  when(carsCatalog.getModelByCode(?, ?)).thenReturn(None)

  before {
    reset(chatClient)
    reset(spamalotClient)
  }

  private def processor(dict: ProvenOwnerValidationDictionary) = new NotificationProcessor(
    chatClient,
    dict,
    spamalotClient,
    passportClient,
    senderClient,
    carsCatalog
  )

  "process" should {
    "retry notification" when {
      "any exceptions was thrown in processing" in {
        when(chatClient.sendServiceNotification(?, ?, ?)(?)).thenReturn(Future.failed(new RuntimeException))
        val notification = notificationTemplate(NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION)

        val res = processor(dict)
          .process(
            0,
            GarageCard
              .newBuilder()
              .setProvenOwnerState(ProvenOwnerState.newBuilder().setVerdict(Verdict.PROVEN_OWNER_OK))
              .setSource(SourceInfo.newBuilder().setUserId(TestUser.toPlain))
              .build(),
            notification
          )
          .await
        res.hasTimestampCancel shouldBe false
        res.hasTimestampSent shouldBe false
        res.getNumTries shouldBe notification.getNumTries + 1

        verify(chatClient, atLeastOnce()).sendServiceNotification(?, ?, ?)(?)
      }
    }
  }

  "insurance renewal processing" should {
    "send push" when {
      "insurance is 7 days to expire" in {
        when(spamalotClient.send(?)).thenReturn(Future.successful(SendResponse.newBuilder().build()))
        val notification = notificationTemplate(NotificationType.INSURANCE_RENEWAL_NOTIFICATION)
        val res =
          processor(dict)
            .process(
              0,
              GarageCard
                .newBuilder()
                .setInsuranceInfo(
                  InsuranceInfo
                    .newBuilder()
                    .addInsurances(buildInsurancePolicy(policyTo = DateTimeUtil.now().plusDays(7)))
                )
                .setSource(SourceInfo.newBuilder().setUserId(TestUser.toPlain))
                .build(),
              notification
            )
            .await
        res.hasTimestampCancel shouldBe false
        res.hasTimestampSent shouldBe true

        verify(spamalotClient, atLeastOnce()).send(?)
      }
    }
    "fail" when {
      "insurance is not expiring soon" in {
        val notification = notificationTemplate(NotificationType.INSURANCE_RENEWAL_NOTIFICATION)
        val res =
          processor(dict)
            .process(
              0,
              GarageCard
                .newBuilder()
                .setInsuranceInfo(
                  InsuranceInfo
                    .newBuilder()
                    .addInsurances(buildInsurancePolicy(policyTo = DateTimeUtil.now().plusDays(200)))
                )
                .setSource(SourceInfo.newBuilder().setUserId(TestUser.toPlain))
                .build(),
              notification
            )
            .await
        res.hasTimestampCancel shouldBe false
        res.hasTimestampSent shouldBe false
        res.getNumTries shouldBe notification.getNumTries + 1
        verify(spamalotClient, never()).send(?)
      }

      "insurance has been expired" in {
        val notification = notificationTemplate(NotificationType.INSURANCE_RENEWAL_NOTIFICATION)
        val res =
          processor(dict)
            .process(
              0,
              GarageCard
                .newBuilder()
                .setInsuranceInfo(
                  InsuranceInfo
                    .newBuilder()
                    .addInsurances(buildInsurancePolicy(policyTo = DateTimeUtil.now().minusDays(1)))
                )
                .setSource(SourceInfo.newBuilder().setUserId(TestUser.toPlain))
                .build(),
              notification
            )
            .await
        res.hasTimestampCancel shouldBe false
        res.hasTimestampSent shouldBe false
        res.getNumTries shouldBe notification.getNumTries + 1
        verify(spamalotClient, never()).send(?)
      }
    }
  }

  "proven owner processing" should {
    "set timestampSent" when {
      "all fine" in {
        when(chatClient.sendServiceNotification(?, ?, ?)(?)).thenReturn(Future.unit)
        val notification = notificationTemplate(NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION)
        val res =
          processor(dict).processProvenOwner(0, TestUser, "BMW", "X3", Verdict.PROVEN_OWNER_OK, notification).await
        res.hasTimestampCancel shouldBe false
        res.hasTimestampSent shouldBe true

        verify(chatClient, atLeastOnce()).sendServiceNotification(?, ?, ?)(?)
      }
    }
    "throw exception" when {
      "chat client returned error" in {
        when(chatClient.sendServiceNotification(?, ?, ?)(?)).thenReturn(Future.failed(new RuntimeException))
        val notification = notificationTemplate(NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION)

        intercept[RuntimeException] {
          processor(dict).processProvenOwner(0, TestUser, "BMW", "X3", Verdict.PROVEN_OWNER_OK, notification).await
        }

        verify(chatClient, atLeastOnce()).sendServiceNotification(?, ?, ?)(?)
      }

      "no suitable data in dict" in {
        val notification = notificationTemplate(NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION)
        val res = processor(ProvenOwnerValidationDictionary())
          .processProvenOwner(0, TestUser, "BMW", "X3", Verdict.PROVEN_OWNER_OK, notification)
          .await
        res.hasTimestampCancel shouldBe false
        res.hasTimestampSent shouldBe false
        res.getNumTries shouldBe notification.getNumTries
      }
    }
  }

  private def notificationTemplate(
      notificationType: NotificationType,
      deadline: Timestamp = Timestamps.fromSeconds(LocalDateTime.now().plusDays(1).toEpochSecond(ZoneOffset.UTC)),
      numTries: Int = 3,
      sent: Option[Timestamp] = None,
      notSent: Option[Timestamp] = None): Notification = {

    val builder = Notification
      .newBuilder()
      .setNotificationType(notificationType)
      .setNumTries(numTries)
      .setDeliveryDeadline(deadline)
      .setMaxTries(5)

    sent.foreach(builder.setTimestampSent)
    notSent.foreach(builder.setTimestampCancel)

    builder.build()
  }

  def cardTemplate(
      deadline: Timestamp = Timestamps.fromSeconds(LocalDateTime.now().plusDays(1).toEpochSecond(ZoneOffset.UTC)),
      numTries: Int = 3,
      sent: Option[Timestamp] = None,
      notSent: Option[Timestamp] = None): GarageCard = {
    val builder = GarageCard.newBuilder()
    val now = System.currentTimeMillis()
    builder.getSourceBuilder
      .setUserId("user:123")
      .setSource(Source.AUTORU)
      .setManuallyAdded(false)
      .setAddedByIdentifier(IdentifierType.VIN)

    builder.getMetaBuilder
      .setCreated(Timestamps.fromMillis(System.currentTimeMillis()))
      .setStatus(GarageCard.Status.ACTIVE)

    builder.getProvenOwnerStateBuilder
      .setDocumentsPhotos(
        DocumentsPhotos
          .newBuilder()
          .setStsBack(
            PhotoInfo
              .newBuilder()
              .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name").setNamespace("namespace").setGroupId(1))
          )
          .setDrivingLicense(PhotoInfo.newBuilder().setMdsPhotoInfo(MdsPhotoInfo.newBuilder()))
          .setStsFront(PhotoInfo.newBuilder().setMdsPhotoInfo(MdsPhotoInfo.newBuilder()))
          .setUploadedAt(Timestamps.fromMillis(now))
      )
      .setVerdict(Verdict.PROVEN_OWNER_OK)
      .setAssignmentDate(Timestamps.fromMillis(now))

    val notificationBuilder = Notification
      .newBuilder()
      .setNumTries(numTries)
      .setDeliveryDeadline(deadline)
      .setNotificationType(Notification.NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION)
      .setMaxTries(5)

    sent.foreach(notificationBuilder.setTimestampSent)
    notSent.foreach(notificationBuilder.setTimestampCancel)
    builder
      .setWatchingState(GarageCard.WatchingState.newBuilder().setLastVisited(Timestamps.fromMillis(now)))
      .addNotifications(notificationBuilder)

    builder.build()
  }

  private def dict = ProvenOwnerValidationDictionary(
    textCarfaxProvenOwnerOk = Some("ok"),
    textCarfaxProvenOwnerFailed = Some("failed"),
    textCarfaxProvenOwnerNotEnoughPhotos = Some("not enough photos"),
    textCarfaxProvenOwnerBadPhotos = Some("bad photos")
  )

}
