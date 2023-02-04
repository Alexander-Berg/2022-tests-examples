package ru.yandex.realty.managers.rent

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.api.ProtoResponse
import ru.yandex.realty.api.ProtoResponse.ApiUpdateContractStatusResponse
import ru.yandex.realty.clients.amohub.AmohubLeadServiceClient
import ru.yandex.realty.clients.geohub.GeohubClient
import ru.yandex.realty.clients.notifier.NotifierClient
import ru.yandex.realty.clients.rent.{
  RentClient,
  RentContractServiceClient,
  RentFlatKeyServiceClient,
  RentFlatQuestionnaireServiceClient,
  RentFlatServiceClient,
  RentFlatShowingServiceClient,
  RentInsuranceServiceClient,
  RentKeysHandoverServiceClient,
  RentModerationClient,
  RentOwnerRequestServiceClient,
  RentScoreServiceClient,
  RentStatisticsServiceClient,
  RentUserDocumentServiceClient,
  RentUserServiceClient,
  RentUtilServiceClient
}
import ru.yandex.realty.errors.{RentPaymentErrorException, RentUpdateContractStatusException}
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.rent.proto.api.moderation.ContractStatusErrorNamespace.ContractStatusErrorCode
import ru.yandex.realty.rent.proto.api.moderation.{
  UpdateContractStatusError,
  UpdateContractStatusRequest,
  UpdateContractStatusResponse,
  UpdateContractStatusSuccessResponse
}
import ru.yandex.realty.rent.proto.api.payment.RentPaymentErrorNamespace.RentPaymentErrorCode
import ru.yandex.realty.rent.proto.api.payment.{
  RentPaymentError,
  RentPaymentInitRequest,
  RentPaymentInitResponse,
  RentPaymentInitSuccessResponse
}
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat

import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class DefaultRentManagerSpec extends AsyncSpecBase {

  "RentManager" should {
    "successfully init payment" in new Wiring with Data {
      val request = RentPaymentInitRequest
        .newBuilder()
        .setSuccessUrl(successUrl)
        .setFailUrl(failUrl)
        .setRedirectDueDate(DateTimeFormat.write(redirectDueDate))
        .build()
      val response = RentPaymentInitResponse
        .newBuilder()
        .setSuccess(
          RentPaymentInitSuccessResponse
            .newBuilder()
            .setPaymentUrl(paymentUrl)
        )
        .build()

      (rentClient
        .initPayment(_: PassportUser, _: String, _: String, _: RentPaymentInitRequest)(_: Traced))
        .expects(user, flatId, paymentId, request, trace)
        .returning(Future.successful(response))

      val result = rentManager
        .initPayment(
          user,
          flatId,
          paymentId,
          request,
          None
        )(trace)
        .futureValue
      result.hasResponse shouldBe true
      result.getResponse shouldEqual (response.getSuccess)
    }

    "throw RentPaymentErrorException when init payment returns error" in new Wiring with Data {
      val request = RentPaymentInitRequest.getDefaultInstance
      val rentPaymentError = RentPaymentError
        .newBuilder()
        .setErrorCode(RentPaymentErrorCode.INTERNAL_ERROR)
        .setShortMessage("Неверные параметры.")
        .setDetails("Неверный токен. Проверьте пару TerminalKey/SecretKey.")
        .build()
      val response = RentPaymentInitResponse
        .newBuilder()
        .setError(rentPaymentError)
        .build()

      (rentClient
        .initPayment(_: PassportUser, _: String, _: String, _: RentPaymentInitRequest)(_: Traced))
        .expects(user, flatId, paymentId, request, trace)
        .returning(Future.successful(response))
      val rentPaymentErrorException = interceptCause[RentPaymentErrorException] {
        rentManager
          .initPayment(
            user,
            flatId,
            paymentId,
            request,
            None
          )(trace)
          .futureValue
      }
      rentPaymentErrorException.paymentError shouldBe (rentPaymentError)
    }
  }

  "RentManager.updateContractStatus" should {
    // failed scenarios
    "throws Exception when rent client fails" in new Wiring with Data {
      (rentModerationClient
        .updateContractStatus(_: String, _: String, _: UpdateContractStatusRequest)(_: Traced))
        .expects(sampleFlatId, sampleContractId, sampleRequest, *)
        .returning(Future.failed(sampleException))

      val err: RuntimeException = interceptCause[RuntimeException] {
        rentManager.updateContractStatus(sampleFlatId, sampleContractId, sampleRequest)(trace).futureValue
      }

      err shouldEqual sampleException
    }

    "throws IllegalStateException when rent client returns empty result set" in new Wiring with Data {
      (rentModerationClient
        .updateContractStatus(_: String, _: String, _: UpdateContractStatusRequest)(_: Traced))
        .expects(sampleFlatId, sampleContractId, sampleRequest, *)
        .returning(Future.successful(sampleEmptyResultSetResponse))

      val err: IllegalStateException = interceptCause[IllegalStateException] {
        rentManager.updateContractStatus(sampleFlatId, sampleContractId, sampleRequest)(trace).futureValue
      }

      err.getMessage shouldEqual "UpdateContractStatusResponse result is not set"
    }

    "throws RentUpdateContractStatusException when rent client returns business error code" in new Wiring with Data {
      (rentModerationClient
        .updateContractStatus(_: String, _: String, _: UpdateContractStatusRequest)(_: Traced))
        .expects(sampleFlatId, sampleContractId, sampleRequest, *)
        .returning(Future.successful(sampleBusinessErrorResponse))

      val err: RentUpdateContractStatusException = interceptCause[RentUpdateContractStatusException] {
        rentManager.updateContractStatus(sampleFlatId, sampleContractId, sampleRequest)(trace).futureValue
      }

      err.getMessage shouldEqual s"Update contract status error: $expectedContractStatusErrorCode"
      err.error shouldEqual expectedUpdateContractStatusError
    }

    // succeed scenarios

    "returns succeed api response if rent client returns suceed response" in new Wiring with Data {
      (rentModerationClient
        .updateContractStatus(_: String, _: String, _: UpdateContractStatusRequest)(_: Traced))
        .expects(sampleFlatId, sampleContractId, sampleRequest, *)
        .returning(Future.successful(sampleSucceedResponse))

      val result: ProtoResponse.ApiUpdateContractStatusResponse =
        rentManager.updateContractStatus(sampleFlatId, sampleContractId, sampleRequest)(trace).futureValue

      result shouldEqual expectedApiUpdateContractStatusResponse
    }
  }

  trait Wiring {
    val rentClient: RentClient = mock[RentClient]
    val rentModerationClient: RentModerationClient = mock[RentModerationClient]
    val geoHubClient: GeohubClient = mock[GeohubClient]
    val rentFlatGrpcClient: RentFlatServiceClient = mock[RentFlatServiceClient]
    val rentKeysHandoverClient: RentKeysHandoverServiceClient = mock[RentKeysHandoverServiceClient]
    val rentScoreClient: RentScoreServiceClient = mock[RentScoreServiceClient]
    val notifierClient: NotifierClient = mock[NotifierClient]
    val mockInsuranceClient: RentInsuranceServiceClient = mock[RentInsuranceServiceClient]
    val mockRentUserDocumentClient: RentUserDocumentServiceClient = mock[RentUserDocumentServiceClient]
    val rentContractClient: RentContractServiceClient = mock[RentContractServiceClient]
    val rentFlatKeyClient: RentFlatKeyServiceClient = mock[RentFlatKeyServiceClient]
    val rentOwnerRequestClient: RentOwnerRequestServiceClient = mock[RentOwnerRequestServiceClient]
    val rentUserClient: RentUserServiceClient = mock[RentUserServiceClient]
    val rentFlatQuestionnaireClient = mock[RentFlatQuestionnaireServiceClient]
    val rentFlatShowingClient = mock[RentFlatShowingServiceClient]
    val amohubLeadClient: AmohubLeadServiceClient = mock[AmohubLeadServiceClient]
    val rentFlatShowingsClient: RentFlatShowingServiceClient = mock[RentFlatShowingServiceClient]
    val rentUtilServiceClient: RentUtilServiceClient = mock[RentUtilServiceClient]
    val rentStatisticsServiceClient: RentStatisticsServiceClient = mock[RentStatisticsServiceClient]

    val rentManager: RentManager =
      new DefaultRentManager(
        rentClient,
        rentOwnerRequestClient,
        rentModerationClient,
        geoHubClient,
        rentFlatGrpcClient,
        rentKeysHandoverClient,
        rentScoreClient,
        notifierClient,
        mockInsuranceClient,
        mockRentUserDocumentClient,
        rentContractClient,
        rentFlatKeyClient,
        rentUserClient,
        rentFlatShowingsClient,
        rentFlatQuestionnaireClient,
        rentStatisticsServiceClient,
        amohubLeadClient
      )
    val trace: Traced = Traced.empty
  }

  trait Data {
    val user: PassportUser = PassportUser(12345L)
    val successUrl = "http://success"
    val failUrl = "http://fail"
    val redirectDueDate: DateTime = DateTime.parse("2021-01-01")
    val flatId = "f1"
    val contractId = "c1"
    val paymentId = "p1"

    val paymentUrl = "http://payment-url"

    val sampleFlatId = "flat-id-1"
    val sampleContractId = "contract-id-d"
    val sampleException: RuntimeException = new RuntimeException("sample error")

    val sampleRequest: UpdateContractStatusRequest = UpdateContractStatusRequest.newBuilder().build()
    val sampleEmptyResultSetResponse: UpdateContractStatusResponse = UpdateContractStatusResponse.newBuilder().build()

    val expectedContractStatusErrorCode = ContractStatusErrorCode.SET_ACTIVE_STATUS_ERROR

    val expectedUpdateContractStatusError: UpdateContractStatusError = UpdateContractStatusError
      .newBuilder()
      .setErrorCode(expectedContractStatusErrorCode)
      .build()

    val sampleBusinessErrorResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setError(
        expectedUpdateContractStatusError
      )
      .build()

    val sampleSucceedResponse: UpdateContractStatusResponse = UpdateContractStatusResponse
      .newBuilder()
      .setSuccess(UpdateContractStatusSuccessResponse.newBuilder().build())
      .build()

    val expectedApiUpdateContractStatusResponse: ApiUpdateContractStatusResponse = ApiUpdateContractStatusResponse
      .newBuilder()
      .setResponse(sampleSucceedResponse.getSuccess)
      .build()
  }
}
