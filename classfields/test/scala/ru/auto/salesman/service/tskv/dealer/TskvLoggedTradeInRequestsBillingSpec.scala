package ru.auto.salesman.service.tskv.dealer

import org.joda.time.DateTime
import ru.auto.salesman.model.PriceModifierFeature
import ru.auto.salesman.model.ProductId.TradeInRequestCarsNew
import ru.auto.salesman.service.BillingEventProcessor
import ru.auto.salesman.service.BillingEventProcessor.BillingEventResponse
import ru.auto.salesman.service.tskv.dealer.format.TskvLogFormat
import ru.auto.salesman.service.tskv.dealer.logger.TskvApplyActionLogger
import ru.auto.salesman.tasks.trade_in.TradeInRequestsBilling
import ru.auto.salesman.tasks.trade_in.TradeInRequestsBilling.Request
import ru.auto.salesman.test.BaseSpec

import scala.util.{Success, Try}

class TskvLoggedTradeInRequestsBillingSpec extends BaseSpec {

  val tradeInRequestsBillingMock = mock[TradeInRequestsBilling]

  val tskvLogger = mock[TskvApplyActionLogger]

  class TradeInRequestsBillingMock extends TradeInRequestsBilling {

    def buyRequest(request: Request): Try[BillingEventResponse] =
      tradeInRequestsBillingMock.buyRequest(request)

  }

  val desider =
    new TradeInRequestsBillingMock with TskvLoggedTradeInRequestsBilling {
      protected def logger: TskvApplyActionLogger = tskvLogger
    }

  "TskvLoggedTradeInDecider" should {
    "successfully buy request and log results" in {
      val request = generateTradeInRequest()
      val response = generateTradeInResponse()

      (tradeInRequestsBillingMock
        .buyRequest(_: Request))
        .expects(request)
        .returningT(response)

      (tskvLogger
        .log(
          _: Request,
          _: Try[BillingEventProcessor.BillingEventResponse]
        )(_: TskvLogFormat[Request, BillingEventResponse]))
        .expects(request, Success(response), *)
        .returningZ(())

      desider.buyRequest(request).success.value shouldBe response
    }

    "successfully buy request even if logging failed" in {
      val request = generateTradeInRequest()
      val response = generateTradeInResponse()

      (tradeInRequestsBillingMock
        .buyRequest(_: Request))
        .expects(request)
        .returningT(response)

      (tskvLogger
        .log(
          _: Request,
          _: Try[BillingEventProcessor.BillingEventResponse]
        )(_: TskvLogFormat[Request, BillingEventResponse]))
        .expects(request, Success(response), *)
        .throwingZ(new Exception("Uanble to log"))

      desider.buyRequest(request).success.value shouldBe response
    }
  }

  private def generateTradeInRequest(): Request =
    Request(
      product = TradeInRequestCarsNew,
      requestId = 222,
      priceRequest = null,
      client = null
    )

  private def generateTradeInResponse(): BillingEventProcessor.BillingEventResponse =
    new BillingEventProcessor.BillingEventResponse(
      deadline = DateTime.now(),
      price = None,
      actualPrice = None,
      promocodeFeatures = List[PriceModifierFeature](),
      holdId = None,
      clientId = None,
      agencyId = None,
      companyId = None,
      regionId = None
    )
}
