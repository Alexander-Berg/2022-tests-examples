package ru.yandex.realty.scheduler.yang.task

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.palma.rent_common.RentDocument
import realty.palma.rent_user.RentUser
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.palma.client.PalmaClient
import ru.yandex.realty.application.ng.palma.encrypted.PalmaEncryptedServiceClient
import ru.yandex.realty.clients.mds.AvatarsClient
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching2.Stage.{ProcessingResult, Updated}
import ru.yandex.realty.yankee.backend.manager.ShiftImageManager
import ru.yandex.realty.yankee.clients.yang.tools.YangToolsClient
import ru.yandex.realty.yankee.clients.yang.tools.model.{CreateOperationRequest, CreateOperationResponse}
import ru.yandex.realty.yankee.model.{YangTask, YangTaskDeliveryStatus, YangTaskStatus, YangTaskType}
import ru.yandex.realty.yankee.model.YangTaskType.YangTaskType
import ru.yandex.realty.yankee.proto.model.payload.payload.PassportVerificationPayload.{
  CombinedSelfieAndPassportDocuments,
  PassportMainPageDocuments,
  PassportRegistrationPageDocuments,
  SeparateSelfieAndPassportDocuments
}
import ru.yandex.realty.yankee.proto.model.payload.payload.{PassportVerificationPayload, YangTaskPayload}
import ru.yandex.realty.yankee.scheduler.yang.task.YangTaskInitiationStage
import ru.yandex.vertis.RawMdsIdentity
import ru.yandex.vertis.palma.encrypted.content.Image
import ru.yandex.vertis.palma.services.encrypted_service.Images.GetBytesRequest

import java.time.Instant
import java.util
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

@RunWith(classOf[JUnitRunner])
class YangTaskInitiationStageSpec extends AsyncSpecBase {

  implicit private val trace: Traced = Traced.empty

  "YangTaskInitiationStage" should {
    "initiate passport main page markup operation" in new Wiring with Data {
      val payload: PassportVerificationPayload = PassportVerificationPayload()
        .withPassportMainPageDocuments {
          PassportMainPageDocuments()
            .withPassportMainPageDocumentId(PassportMainPageId)
        }
      val task: YangTask = buildTask(YangTaskType.PassportMainPageMarkup, payload, s"$Uid/$PassportMainPageId")
      mockRentUserClient()
      mockEncryptedServiceClient(PassportMainPageId)
      mockMdsClient(PassportMainPageId)
      mockYangToolsClient(task.idempotencyKey)

      val processingResult: ProcessingResult[YangTask] = invokeStage(task)

      assertProcessingResult(processingResult)
    }

    "initiate passport registration page markup operation" in new Wiring with Data {
      val payload: PassportVerificationPayload = PassportVerificationPayload()
        .withPassportRegistrationPageDocuments {
          PassportRegistrationPageDocuments()
            .withPassportRegistrationPageDocumentId(PassportRegistrationPageId)
        }
      val task: YangTask =
        buildTask(YangTaskType.PassportRegistrationPageMarkup, payload, s"$Uid/$PassportRegistrationPageId")
      mockRentUserClient()
      mockEncryptedServiceClient(PassportRegistrationPageId)
      mockMdsClient(PassportRegistrationPageId)
      mockYangToolsClient(task.idempotencyKey)

      val processingResult: ProcessingResult[YangTask] = invokeStage(task)

      assertProcessingResult(processingResult)
    }

    "initiate combined selfie reconciliation operation" in new Wiring with Data {
      val payload: PassportVerificationPayload = PassportVerificationPayload()
        .withCombinedSelfieAndPassportDocuments {
          CombinedSelfieAndPassportDocuments()
            .withSelfieWithPassportDocumentId(SelfieWithPassportId)
        }
      val task: YangTask =
        buildTask(YangTaskType.SelfieReconciliation, payload, s"$Uid/$SelfieWithPassportId")
      mockRentUserClient()
      mockEncryptedServiceClient(SelfieWithPassportId)
      mockMdsClient(SelfieWithPassportId)
      mockYangToolsClient(task.idempotencyKey)

      val processingResult: ProcessingResult[YangTask] = invokeStage(task)

      assertProcessingResult(processingResult)
    }

    "initiate separate selfie reconciliation operation" in new Wiring with Data {
      val payload: PassportVerificationPayload = PassportVerificationPayload()
        .withSeparateSelfieAndPassportDocuments {
          SeparateSelfieAndPassportDocuments()
            .withPassportMainPageDocumentId(PassportMainPageId)
            .withSelfieDocumentId(SelfieId)
        }
      val task: YangTask =
        buildTask(YangTaskType.SelfieReconciliation, payload, s"$Uid/$SelfieId/$PassportMainPageId")
      mockRentUserClient()
      mockRentUserClient()
      mockEncryptedServiceClient(SelfieId)
      mockEncryptedServiceClient(PassportMainPageId)
      mockMdsClient(SelfieId)
      mockMdsClient(PassportMainPageId)
      mockYangToolsClient(task.idempotencyKey)

      val processingResult: ProcessingResult[YangTask] = invokeStage(task)

      assertProcessingResult(processingResult)
    }
  }

  trait Wiring {
    self: Data =>

    val yangToolsClient: YangToolsClient = mock[YangToolsClient]
    val palmaRentUserClient: PalmaClient[RentUser] = mock[PalmaClient[RentUser]]
    val palmaEncryptedServiceClient: PalmaEncryptedServiceClient = mock[PalmaEncryptedServiceClient]
    val mdsAvatarsClient: AvatarsClient = mock[AvatarsClient]

    def mockRentUserClient(): Unit =
      (palmaRentUserClient
        .get(_: String)(_: Traced))
        .expects(Uid.toString, *)
        .returning(Future.successful(Some(RentUserResponse)))

    def mockEncryptedServiceClient(documentId: String): Unit =
      (palmaEncryptedServiceClient
        .getImageBytes(_: GetBytesRequest)(_: Traced))
        .expects(where {
          case (request: GetBytesRequest, _) =>
            request.key == buildImageKey(documentId)
        })
        .returning(Future.successful(Some(documentId.getBytes)))

    def mockMdsClient(documentId: String): Unit =
      (mdsAvatarsClient
        .uploadData(_: Array[Byte], _: String, _: String, _: Option[Long], _: FiniteDuration)(_: Traced))
        .expects(where {
          case (bytes, _, _, _, _, _) =>
            util.Arrays.equals(bytes, documentId.getBytes)
        })
        .returning(Future.successful(buildMdsIdentity(documentId)))

    def mockYangToolsClient(uniqueKey: String): Unit =
      (yangToolsClient
        .createOperation(_: CreateOperationRequest)(_: Traced))
        .expects(where {
          case (request, _) =>
            request.uniqueKey == uniqueKey
        })
        .returning(Future.successful(CreateOperationResponse(OperationId)))

    def invokeStage(task: YangTask): ProcessingResult[YangTask] = {
      val mdsUrlBuilder: MdsUrlBuilder = new MdsUrlBuilder(MdsBaseUrl)
      val shiftImageManager: ShiftImageManager =
        new ShiftImageManager(palmaRentUserClient, palmaEncryptedServiceClient, mdsAvatarsClient, mdsUrlBuilder)
      val stage: YangTaskInitiationStage =
        new YangTaskInitiationStage(yangToolsClient, shiftImageManager)
      stage.process(task).futureValue
    }
  }

  trait Data {
    val MdsBaseUrl = "//hostname"

    val Uid = 12345678L
    val PassportMainPageId = "passport_main_page_id"
    val PassportRegistrationPageId = "passport_registration_page_id"
    val SelfieWithPassportId = "selfie_with_passport_id"
    val SelfieId = "selfie_id"
    val ImageKeySuffix = "image_key"
    val Namespace = "namespace"
    val GroupId = 12345
    val OperationId = "operation-id"

    val RentUserResponse: RentUser = RentUser()
      .withDocuments {
        Seq(
          buildRentDocument(PassportMainPageId),
          buildRentDocument(PassportRegistrationPageId),
          buildRentDocument(SelfieWithPassportId),
          buildRentDocument(SelfieId)
        )
      }

    private def buildRentDocument(documentId: String): RentDocument =
      RentDocument()
        .withId(documentId)
        .withImage(Image().withKey(buildImageKey(documentId)))

    def buildImageKey(documentId: String): String =
      s"$documentId/$ImageKeySuffix"

    def buildMdsIdentity(documentId: String): RawMdsIdentity =
      RawMdsIdentity
        .newBuilder()
        .setNamespace(Namespace)
        .setGroupId(GroupId)
        .setName(documentId)
        .build()

    def buildTask(taskType: YangTaskType, payload: PassportVerificationPayload, idempotencyKey: String): YangTask =
      YangTask(
        taskType = taskType,
        payload = YangTaskPayload().withPassportVerification(payload.withUid(Uid)),
        idempotencyKey = idempotencyKey,
        status = YangTaskStatus.New,
        yangOperationId = None,
        yangOperationCreateTime = None,
        result = None,
        deliveryStatus = YangTaskDeliveryStatus.NotDelivered,
        createTime = Instant.now(),
        updateTime = Instant.now()
      )

    def assertProcessingResult(processingResult: ProcessingResult[YangTask]): Unit = {
      processingResult.isInstanceOf[Updated[YangTask]] shouldEqual true
      val updatedResult = processingResult.asInstanceOf[Updated[YangTask]]
      updatedResult.entity.status shouldEqual YangTaskStatus.Initiated
      updatedResult.entity.yangOperationId shouldEqual Some(OperationId)
      updatedResult.entity.yangOperationCreateTime.isDefined shouldEqual true
      updatedResult.visitTime shouldEqual None
    }
  }
}
