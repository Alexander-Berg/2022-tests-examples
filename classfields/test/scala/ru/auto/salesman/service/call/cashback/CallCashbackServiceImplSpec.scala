package ru.auto.salesman.service.call.cashback

import billing.cashback.payment.CashbackPaymentServiceGrpc.CashbackPaymentServiceStub
import billing.cashback.payment.{
  GetPaymentByIdRequest,
  Payment,
  PaymentCancelRequest,
  PaymentStatus,
  PaymentWithdrawRequest
}
import io.grpc.{Status, StatusRuntimeException}
import io.grpc.Status.Code._
import org.scalamock.matchers.ArgCapture.CaptureOne
import ru.auto.salesman.model.{AutoruDealer, CityId, Client, ClientStatuses, RegionId}
import ru.auto.salesman.service.call.cashback.CallCashbackService.CallCashbackServiceError
import ru.auto.salesman.service.call.cashback.domain.{CallCashbackRule, CallId}
import ru.auto.salesman.service.call.cashback.impl.CallCashbackServiceImpl.CallCarsUsedCashbackWorkspace
import ru.auto.salesman.service.call.cashback.impl.CallCashbackServiceImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.grpc.GrpcClient
import sourcecode.{File, Name}

import scala.concurrent.Future

class CallCashbackServiceImplSpec extends BaseSpec {

  val callCashbackRulesService = mock[CallCashbackRulesService]
  val grpc = mock[GrpcClient[CashbackPaymentServiceStub]]

  val service = new CallCashbackServiceImpl(callCashbackRulesService, grpc)

  val client =
    Client(
      clientId = 23965,
      agencyId = None,
      categorizedClientId = None,
      companyId = None,
      RegionId(1L),
      CityId(2L),
      ClientStatuses.Active,
      singlePayment = Set(),
      firstModerated = false,
      paidCallsAvailable = false,
      priorityPlacement = true
    )
  val callId = "xaQhSdfJwSU"
  val price = 10000

  "CallCashbackServiceImpl" should {

    "withdrawn cashback according to proper rule" in {
      val request = PaymentWithdrawRequest(
        workspace = CallCarsUsedCashbackWorkspace,
        clientId = AutoruDealer(client.clientId).toString,
        amountKopecks = 5000,
        paymentId = callId
      )

      val arg = CaptureOne[CashbackPaymentServiceStub => Future[Payment]]()

      (grpc
        .call(_: CashbackPaymentServiceStub => Future[Payment])(_: File, _: Name))
        .expects(capture(arg), *, *)
        .returningZ(Payment.defaultInstance)

      (callCashbackRulesService.getCashbackRule _)
        .expects(client)
        .returningZ(Some(CallCashbackRule(50)))

      service.withdrawCashback(client, CallId(callId), price).success.value
      getGrpcRequestArg(arg).shouldEqual(request)
    }

    "ignore already exists" in {
      (grpc
        .call(_: CashbackPaymentServiceStub => Future[Payment])(_: File, _: Name))
        .expects(*, *, *)
        .throwingZ(new StatusRuntimeException(Status.fromCode(ALREADY_EXISTS)))

      (callCashbackRulesService.getCashbackRule _)
        .expects(client)
        .returningZ(Some(CallCashbackRule(50)))

      service.withdrawCashback(client, CallId(callId), price).success.value
    }

    "map FAILED_PRECONDITION to conflict" in {
      (grpc
        .call(_: CashbackPaymentServiceStub => Future[Payment])(_: File, _: Name))
        .expects(*, *, *)
        .throwingZ(new StatusRuntimeException(Status.fromCode(FAILED_PRECONDITION)))

      (callCashbackRulesService.getCashbackRule _)
        .expects(client)
        .returningZ(Some(CallCashbackRule(50)))

      service
        .withdrawCashback(client, CallId(callId), price)
        .failure
        .exception shouldBe an[CallCashbackServiceError.ConflictError]
    }

    "not withdraw cashback if there is no rule for client" in {
      (grpc
        .call(_: CashbackPaymentServiceStub => Future[Payment])(_: File, _: Name))
        .expects(*, *, *)
        .never()

      (callCashbackRulesService.getCashbackRule _)
        .expects(client)
        .returningZ(None)

      service.withdrawCashback(client, CallId(callId), price).success.value
    }

    "cancel cashback withdrawal" in {
      val request = PaymentCancelRequest(
        workspace = CallCarsUsedCashbackWorkspace,
        clientId = AutoruDealer(client.clientId).toString,
        paymentId = callId
      )

      val arg = CaptureOne[CashbackPaymentServiceStub => Future[Payment]]()

      (grpc
        .call(_: CashbackPaymentServiceStub => Future[Payment])(_: File, _: Name))
        .expects(capture(arg), *, *)
        .returningZ(Payment.defaultInstance)

      service.cancelCashbackWithdrawal(client, CallId(callId)).success.value
      getGrpcRequestArg(arg).shouldEqual(request)
    }

    "get withdrawn cashback amount" in {
      val request = GetPaymentByIdRequest(
        workspace = CallCarsUsedCashbackWorkspace,
        clientId = AutoruDealer(client.clientId).toString,
        paymentId = callId
      )

      val arg = CaptureOne[CashbackPaymentServiceStub => Future[Payment]]()

      (grpc
        .call(_: CashbackPaymentServiceStub => Future[Payment])(_: File, _: Name))
        .expects(capture(arg), *, *)
        .returningZ(
          Payment.defaultInstance
            .withAmountKopecks(50)
            .withPaymentStatus(PaymentStatus.ACTIVE)
        )

      service
        .getWithdrawnCashbackAmount(client, CallId(callId))
        .success
        .value
        .shouldEqual(Some(50))
      getGrpcRequestArg(arg).shouldEqual(request)
    }

  }

  private def getGrpcRequestArg[T](capture: CaptureOne[T]) = {
    val requestLambdaField = capture.value.getClass.getDeclaredFields.head
    requestLambdaField.setAccessible(true)
    requestLambdaField.get(capture.value)
  }

}
