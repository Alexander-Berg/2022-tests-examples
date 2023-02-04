package ru.auto.salesman.service.tskv.dealer

import org.joda.time.DateTime
import ru.auto.salesman.controller.QuotaController
import ru.auto.salesman.model.{Quota, QuotaRequest, QuotaTypes}
import ru.auto.salesman.service.tskv.dealer.format.TskvLogFormat
import ru.auto.salesman.service.tskv.dealer.logger.TskvApplyActionLogger
import ru.auto.salesman.test.BaseSpec

import scala.util.Try

class TskvLoggedQuotaControllerSpec extends BaseSpec {

  val quotaController: QuotaController = mock[QuotaController]

  val logger: TskvApplyActionLogger = mock[TskvApplyActionLogger]

  val tskvLoggedQuotaDecider = new TskvLoggedQuotaController(
    quotaController = quotaController,
    logger = logger
  )

  "TskvLoggedQuotaDecider" should {
    "successfully activate quota and log results" in {
      val request = generateQuotaRequest()
      val response = generateQuotaResponse()

      (quotaController
        .activate(_: QuotaRequest, _: Option[Quota]))
        .expects(request, None)
        .returningT(response)

      (logger
        .log(_: QuotaRequest, _: Try[QuotaController.Response])(
          _: TskvLogFormat[QuotaRequest, QuotaController.Response]
        ))
        .expects(request, *, *)
        .returningZ(())

      tskvLoggedQuotaDecider
        .activate(request, lastActivation = None)
        .success
        .get shouldBe response
    }

    "fail activation if unable to log results" in {
      val request = generateQuotaRequest()
      val response = generateQuotaResponse()

      (quotaController
        .activate(_: QuotaRequest, _: Option[Quota]))
        .expects(request, None)
        .returningT(response)

      (logger
        .log(_: QuotaRequest, _: Try[QuotaController.Response])(
          _: TskvLogFormat[QuotaRequest, QuotaController.Response]
        ))
        .expects(request, *, *)
        .throwingZ(new Exception("Unable to log"))

      tskvLoggedQuotaDecider
        .activate(request, lastActivation = None)
        .failed
        .get
        .getMessage shouldBe "Unable to log"
    }
  }

  private def generateQuotaRequest(): QuotaRequest = {
    val quotaSettings = QuotaRequest.Settings(
      size = 5,
      days = 4,
      price = Some(2)
    )
    QuotaRequest(
      clientId = 1,
      quotaType = QuotaTypes.withName("quota:placement:cars:new"),
      settings = quotaSettings,
      from = DateTime.now().minusDays(2)
    )
  }

  private def generateQuotaResponse(): QuotaController.Response =
    QuotaController.Response(
      action = QuotaController.NoAction()
    )

}
