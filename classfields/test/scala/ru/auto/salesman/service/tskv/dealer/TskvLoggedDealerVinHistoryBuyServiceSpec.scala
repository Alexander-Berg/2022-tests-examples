package ru.auto.salesman.service.tskv.dealer

import org.joda.time.DateTime
import ru.auto.api.ResponseModel.VinHistoryApplyResponse.PaymentStatus
import ru.auto.salesman.service.DealerVinHistoryBuyService
import ru.auto.salesman.service.VinHistoryService.{Request, Response}
import ru.auto.salesman.service.tskv.dealer.format.TskvLogFormat
import ru.auto.salesman.service.tskv.dealer.logger.TskvApplyActionLogger
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.{AutomatedContext, RequestContext}

import scala.util.{Success, Try}

class TskvLoggedDealerVinHistoryBuyServiceSpec extends BaseSpec {

  val dealerVinHistoryBuyService: DealerVinHistoryBuyService =
    mock[DealerVinHistoryBuyService]

  val logger: TskvApplyActionLogger = mock[TskvApplyActionLogger]

  val tskvLoggedVinHistoryGoodsDesider =
    new TskvLoggedDealerVinHistoryBuyService(
      dealerVinHistoryBuyService,
      logger
    )

  val request = Request(
    clientId = 123,
    vin = "vin test "
  )

  val response = Response(
    paymentStatus = PaymentStatus.OK,
    deadline = DateTime.now
  )

  implicit val rc: RequestContext = AutomatedContext("unit-test")

  "TskvLoggedVinHisoryDecider" should {

    "successfully buy view and log result" in {
      (dealerVinHistoryBuyService
        .buyView(_: Request)(_: RequestContext))
        .expects(request, *)
        .returning(Success(response))

      (logger
        .log(_: Request, _: Try[Response])(
          _: TskvLogFormat[Request, Response]
        ))
        .expects(request, Success(response), *)
        .returningZ(())

      tskvLoggedVinHistoryGoodsDesider
        .buyView(request)
        .success
        .value shouldBe response
    }

    "successfully buy view even if logging failed" in {
      (dealerVinHistoryBuyService
        .buyView(_: Request)(_: RequestContext))
        .expects(request, *)
        .returning(Success(response))

      (logger
        .log(_: Request, _: Try[Response])(
          _: TskvLogFormat[Request, Response]
        ))
        .expects(request, Success(response), *)
        .throwingZ(new Exception("Unable to log"))

      tskvLoggedVinHistoryGoodsDesider
        .buyView(request)
        .success
        .value shouldBe response
    }
  }
}
