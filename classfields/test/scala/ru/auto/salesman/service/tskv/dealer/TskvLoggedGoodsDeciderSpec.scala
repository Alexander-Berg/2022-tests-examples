package ru.auto.salesman.service.tskv.dealer

import org.joda.time.DateTime
import ru.auto.salesman.model.ActivateDate
import ru.auto.salesman.service.GoodsDecider
import ru.auto.salesman.service.GoodsDecider.{Action, Request, Response}
import ru.auto.salesman.service.impl.DeciderUtilsSpec
import ru.auto.salesman.service.tskv.dealer.format.TskvLogFormat
import ru.auto.salesman.service.tskv.dealer.logger.TskvApplyActionLogger
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.billing.Model.OfferBilling

import scala.util.{Success, Try}

class TskvLoggedGoodsDeciderSpec extends BaseSpec {

  val goodsDecider = mock[GoodsDecider]
  val tskvLogger = mock[TskvApplyActionLogger]

  class GoodsDeciderMock extends GoodsDecider {

    def apply(request: GoodsDecider.Request)(
        implicit rc: RequestContext
    ): Try[GoodsDecider.Response] =
      goodsDecider.apply(request)

  }

  val loggedGoodsDecider =
    new GoodsDeciderMock with TskvLoggedGoodsDecider {
      protected def logger: TskvApplyActionLogger = tskvLogger
    }

  implicit val rc: RequestContext = AutomatedContext("unit-test")
  val request = DeciderUtilsSpec.testRequest

  val response = Response(
    Action.Activate(
      activateDate = ActivateDate(DateTime.now),
      offerBilling = OfferBilling.newBuilder().setVersion(1).build(),
      features = List.empty
    )
  )

  "TskvLoggedQuotaDecider" should {
    "successfully apply goods and log results" in {
      (goodsDecider
        .apply(_: Request)(_: RequestContext))
        .expects(request, *)
        .returningT(response)

      (tskvLogger
        .log(_: Request, _: Try[Response])(
          _: TskvLogFormat[Request, Response]
        ))
        .expects(request, Success(response), *)
        .returningZ(())

      loggedGoodsDecider
        .apply(request)
        .success
        .value shouldBe response
    }

    "fail application if unable to log results" in {
      (goodsDecider
        .apply(_: Request)(_: RequestContext))
        .expects(request, *)
        .returningT(response)

      (tskvLogger
        .log(_: Request, _: Try[Response])(
          _: TskvLogFormat[Request, Response]
        ))
        .expects(request, Success(response), *)
        .returningZ(())

      loggedGoodsDecider
        .apply(request)
        .success
        .value shouldBe response
    }
  }

}
