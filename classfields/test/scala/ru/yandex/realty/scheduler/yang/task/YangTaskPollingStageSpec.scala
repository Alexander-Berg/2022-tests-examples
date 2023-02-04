package ru.yandex.realty.scheduler.yang.task

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.palma.rent_common.PersonFullName
import realty.palma.yang_task_result.YangTaskResultContent.DocumentTypeNamespace.DocumentType
import realty.palma.yang_task_result.YangTaskResultContent.{PassportMainPageMarkup, PassportRegistrationPageMarkup}
import realty.palma.yang_task_result.YangTaskResultContent.SexNamespace.Sex
import realty.palma.yang_task_result.{
  YangTaskResult => PalmaYangTaskResult,
  YangTaskResultContent => PalmaTaskResultContent
}
import ru.yandex.realty.application.ng.palma.client.PalmaClient
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching2.Stage.{ProcessingResult, Untouched, Updated}
import ru.yandex.realty.yankee.clients.yang.tools.YangToolsClient
import ru.yandex.realty.yankee.clients.yang.tools.model.{
  GetOperationMetaResponse,
  GetOperationResultResponseItem,
  OperationResult,
  OperationStatus,
  PassportData,
  PassportDocumentType,
  PassportGender,
  PassportOperationResult,
  RegistrationAddressData,
  RegistrationAddressResult
}
import ru.yandex.realty.yankee.clients.yang.tools.model.OperationStatus.OperationStatus
import ru.yandex.realty.yankee.model.YangTaskStatus.YangTaskStatus
import ru.yandex.realty.yankee.model.{YangTask, YangTaskDeliveryStatus, YangTaskStatus, YangTaskType}
import ru.yandex.realty.yankee.model.YangTaskType.YangTaskType
import ru.yandex.realty.yankee.proto.model.payload.payload.YangTaskPayload
import ru.yandex.realty.yankee.proto.model.result.result.YangTaskResolutionNamespace.YangTaskResolution
import ru.yandex.realty.yankee.proto.model.result.result.{
  SelfieReconciliationResultContent,
  YangTaskFailureDetails,
  YangTaskResult,
  YangTaskResultContent
}
import ru.yandex.realty.yankee.scheduler.yang.task.YangTaskPollingStage
import ru.yandex.realty.AsyncSpecBase

import java.time.Instant
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class YangTaskPollingStageSpec extends AsyncSpecBase {

  implicit private val trace: Traced = Traced.empty

  "YangTaskPollingStage" should {
    "only reschedule when operation has just been created" in new Wiring with Data {
      val yangTask: YangTask = buildTask(YangTaskType.PassportMainPageMarkup).copy(
        yangOperationCreateTime = Some(Instant.now())
      )

      val processingResult: ProcessingResult[YangTask] = stage.process(yangTask).futureValue

      assertResultUntouched(processingResult)
    }

    "reschedule when operation is running" in new Wiring with Data {
      mockOperationMeta(OperationStatus.Running)
      val yangTask: YangTask = buildTask(YangTaskType.PassportMainPageMarkup)

      val processingResult: ProcessingResult[YangTask] = stage.process(yangTask).futureValue

      assertResultUntouched(processingResult)
    }

    "store failed status" in new Wiring with Data {
      mockOperationMeta(OperationStatus.Failed)
      val yangTask: YangTask = buildTask(YangTaskType.PassportMainPageMarkup)

      val processingResult: ProcessingResult[YangTask] = stage.process(yangTask).futureValue

      assertResultUpdated(processingResult, YangTaskStatus.Failed)
    }

    "store ok passport markup result" in new Wiring with Data {
      mockOperationMeta(OperationStatus.Completed)
      mockOperationResult(buildPassportResponse(TestPassportSuccessResult))
      mockPalmaStoring(buildPalmaResult(TestPalmaPassportContent))
      val yangTask: YangTask = buildTask(YangTaskType.PassportMainPageMarkup)

      val processingResult: ProcessingResult[YangTask] = stage.process(yangTask).futureValue

      assertResultUpdated(processingResult, YangTaskStatus.Success)
    }

    "store bad passport markup result" in new Wiring with Data {
      mockOperationMeta(OperationStatus.Completed)
      mockOperationResult(buildPassportResponse(TestPassportFailureResult))
      val yangTask: YangTask = buildTask(YangTaskType.PassportMainPageMarkup)

      val processingResult: ProcessingResult[YangTask] = stage.process(yangTask).futureValue

      val taskResult: YangTaskResult = buildTaskResult(None, Some(buildFailureDetails()))
      assertResultUpdated(processingResult, YangTaskStatus.Failed, Some(taskResult))
    }

    "store ok registration markup result" in new Wiring with Data {
      mockOperationMeta(OperationStatus.Completed)
      mockOperationResult(buildRegistrationResponse(TestRegistrationSuccessResult))
      mockPalmaStoring(buildPalmaResult(TestPalmaRegistrationContent))
      val yangTask: YangTask = buildTask(YangTaskType.PassportRegistrationPageMarkup)

      val processingResult: ProcessingResult[YangTask] = stage.process(yangTask).futureValue

      assertResultUpdated(processingResult, YangTaskStatus.Success)
    }

    "store bad registration markup result" in new Wiring with Data {
      mockOperationMeta(OperationStatus.Completed)
      mockOperationResult(buildRegistrationResponse(TestRegistrationFailureResult))
      val yangTask: YangTask = buildTask(YangTaskType.PassportRegistrationPageMarkup)

      val processingResult: ProcessingResult[YangTask] = stage.process(yangTask).futureValue

      val taskResult: YangTaskResult = buildTaskResult(None, Some(buildFailureDetails()))
      assertResultUpdated(processingResult, YangTaskStatus.Failed, Some(taskResult))
    }

    "store ok selfie reconciliation result" in new Wiring with Data {
      mockOperationMeta(OperationStatus.Completed)
      mockOperationResult(TestSelfieSuccessResponse)
      val yangTask: YangTask = buildTask(YangTaskType.SelfieReconciliation)

      val processingResult: ProcessingResult[YangTask] = stage.process(yangTask).futureValue

      val taskResult: YangTaskResult = buildTaskResult(Some(TestSelfieSuccessResultContent), None)
      assertResultUpdated(processingResult, YangTaskStatus.Success, Some(taskResult))
    }

    "store bad selfie reconciliation result" in new Wiring with Data {
      mockOperationMeta(OperationStatus.Completed)
      mockOperationResult(TestSelfieFailureResponse)
      val yangTask: YangTask = buildTask(YangTaskType.SelfieReconciliation)

      val processingResult: ProcessingResult[YangTask] = stage.process(yangTask).futureValue

      val taskResult: YangTaskResult = buildTaskResult(
        Some(TestSelfieFailureResultContent),
        Some(buildFailureDetails(YangTaskResolution.OK))
      )
      assertResultUpdated(processingResult, YangTaskStatus.Failed, Some(taskResult))
    }

    "no create duplicate result in palma" in new Wiring with Data {
      mockOperationMeta(OperationStatus.Completed)
      mockOperationResult(buildPassportResponse(TestPassportSuccessResult))
      mockPalmaExistingResult()
      val yangTask: YangTask = buildTask(YangTaskType.PassportMainPageMarkup)

      val processingResult: ProcessingResult[YangTask] = stage.process(yangTask).futureValue

      assertResultUpdated(processingResult, YangTaskStatus.Success)
    }
  }

  trait Wiring {
    self: Data =>

    val yangToolsClient: YangToolsClient = mock[YangToolsClient]
    val palmaClient: PalmaClient[PalmaYangTaskResult] = mock[PalmaClient[PalmaYangTaskResult]]

    val stage = new YangTaskPollingStage(yangToolsClient, palmaClient)

    def mockOperationMeta(status: OperationStatus): Unit =
      (yangToolsClient
        .getOperationMeta(_: String)(_: Traced))
        .expects(YangOperationId, *)
        .once()
        .returning(Future.successful(GetOperationMetaResponse(YangOperationId, status)))

    def mockOperationResult(response: GetOperationResultResponseItem): Unit =
      (yangToolsClient
        .getOperationResult(_: String)(_: Traced))
        .expects(YangOperationId, *)
        .once()
        .returning(Future.successful(Seq(response)))

    private def mockGetPalmaResult(exists: Boolean): Unit =
      (palmaClient
        .get(_: String)(_: Traced))
        .expects(TaskId.toString, *)
        .once()
        .returning {
          val result = if (exists) Some(PalmaYangTaskResult()) else None
          Future.successful(result)
        }

    def mockPalmaStoring(result: PalmaYangTaskResult): Unit = {
      mockGetPalmaResult(exists = false)
      (palmaClient
        .create(_: PalmaYangTaskResult)(_: Traced))
        .expects(result, *)
        .once()
        .returning(Future.successful(result))
    }

    def mockPalmaExistingResult(): Unit =
      mockGetPalmaResult(exists = true)
  }

  trait Data {

    val TaskId = 4321L
    val YangOperationId = "1234"

    def buildTask(taskType: YangTaskType): YangTask =
      YangTask(
        id = TaskId,
        taskType = taskType,
        payload = YangTaskPayload(),
        idempotencyKey = "",
        status = YangTaskStatus.Initiated,
        yangOperationId = Some(YangOperationId),
        yangOperationCreateTime = Some(Instant.now().minusSeconds(100)),
        result = None,
        deliveryStatus = YangTaskDeliveryStatus.NotDelivered,
        createTime = Instant.now(),
        updateTime = Instant.now()
      )

    private val EmptyResponse: GetOperationResultResponseItem =
      GetOperationResultResponseItem(
        passport = None,
        registrationAddress = None,
        isSimilar = None,
        similarity = None,
        errors = None
      )

    val TestPassportData: PassportData = PassportData(
      firstName = "first name",
      lastName = "last name",
      middleName = Some("middle name"),
      gender = PassportGender.M,
      birthday = "20-07-1990",
      documentType = PassportDocumentType.RussianPassport,
      passportSeriesNumber = "series number",
      passportNumber = "passport number",
      issuer = "passport issuer",
      issueDate = "03-08-2010",
      issuerSubdivisionCode = "passport issuer code",
      birthplace = "birth place"
    )

    val TestPassportSuccessResult: PassportOperationResult =
      PassportOperationResult(data = Some(TestPassportData), result = OperationResult.Ok, errors = Seq.empty)

    val TestPassportFailureResult: PassportOperationResult =
      PassportOperationResult(data = None, result = OperationResult.Bad, errors = Seq("error message"))

    def buildPassportResponse(passportResult: PassportOperationResult): GetOperationResultResponseItem =
      EmptyResponse.copy(passport = Some(passportResult))

    val TestRegistrationData: RegistrationAddressData = RegistrationAddressData(
      address = "address",
      guid = "12345678",
      kladrCode = Some("kladr"),
      zipCode = "654321",
      building = "124",
      apartment = Some("32"),
      registrationDate = "1992-06-15"
    )

    val TestRegistrationSuccessResult: RegistrationAddressResult =
      RegistrationAddressResult(data = Some(TestRegistrationData), result = OperationResult.Ok, errors = Seq.empty)

    val TestRegistrationFailureResult: RegistrationAddressResult =
      RegistrationAddressResult(data = None, result = OperationResult.Bad, errors = Seq("error message"))

    def buildRegistrationResponse(registrationResult: RegistrationAddressResult): GetOperationResultResponseItem =
      EmptyResponse.copy(registrationAddress = Some(registrationResult))

    val TestSelfieSuccessResponse: GetOperationResultResponseItem =
      EmptyResponse.copy(isSimilar = Some(true), similarity = Some(0.75f))

    val TestSelfieFailureResponse: GetOperationResultResponseItem =
      EmptyResponse.copy(isSimilar = Some(false), similarity = Some(0.25f), errors = Some(Seq("error message")))

    val TestPalmaPassportContent: PalmaTaskResultContent =
      PalmaTaskResultContent()
        .withPassportMainPageMarkup {
          PassportMainPageMarkup()
            .withPerson(PersonFullName("first name", "last name", "middle name"))
            .withSex(Sex.MALE)
            .withBirthday("1990-07-20")
            .withDocumentType(DocumentType.RUSSIAN_PASSPORT)
            .withPassportSeries("series number")
            .withPassportNumber("passport number")
            .withPassportIssuedBy("passport issuer")
            .withPassportIssueDate("2010-08-03")
            .withDepartmentCode("passport issuer code")
            .withBirthPlace("birth place")
        }

    val TestPalmaRegistrationContent: PalmaTaskResultContent =
      PalmaTaskResultContent()
        .withPassportRegistrationPageMarkup {
          PassportRegistrationPageMarkup()
            .withAddress("address")
            .withGuid("12345678")
            .withKladrCode("kladr")
            .withZipCode("654321")
            .withBuilding("124")
            .withApartment("32")
            .withRegistrationDate("1992-06-15")
        }

    def buildPalmaResult(content: PalmaTaskResultContent): PalmaYangTaskResult =
      PalmaYangTaskResult(TaskId.toString, Some(content))

    val TestSelfieSuccessResultContent: SelfieReconciliationResultContent =
      SelfieReconciliationResultContent(isSimilar = true, similarity = 0.75f)

    val TestSelfieFailureResultContent: SelfieReconciliationResultContent =
      SelfieReconciliationResultContent(similarity = 0.25f)

    def buildFailureDetails(resolution: YangTaskResolution = YangTaskResolution.BAD): YangTaskFailureDetails =
      YangTaskFailureDetails(resolution, Seq("error message"))

    def buildTaskResult(
      selfieContent: Option[SelfieReconciliationResultContent],
      failureDetails: Option[YangTaskFailureDetails]
    ): YangTaskResult =
      YangTaskResult(
        content = selfieContent.map { sc =>
          YangTaskResultContent().withSelfieReconciliation(sc)
        },
        failureDetails = failureDetails
      )

    def assertResultUntouched(processingResult: ProcessingResult[YangTask]): Unit = {
      processingResult.isInstanceOf[Untouched] shouldEqual true
      processingResult.asInstanceOf[Untouched].visitTime.isDefined shouldEqual true
      processingResult.asInstanceOf[Untouched].visitTime.get.isAfter(Instant.now()) shouldEqual true
    }

    def assertResultUpdated(
      processingResult: ProcessingResult[YangTask],
      taskStatus: YangTaskStatus,
      taskResult: Option[YangTaskResult] = None
    ): Unit = {
      processingResult.isInstanceOf[Updated[YangTask]] shouldEqual true
      processingResult.asInstanceOf[Updated[YangTask]].entity.status shouldEqual taskStatus
      processingResult.asInstanceOf[Updated[YangTask]].entity.result shouldEqual taskResult
      processingResult.asInstanceOf[Updated[YangTask]].visitTime.isEmpty shouldEqual true
    }
  }
}
