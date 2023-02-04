package ru.auto.salesman.service.tskv.dealer

import org.joda.time.DateTime
import ru.auto.salesman.Task
import ru.auto.salesman.model.Product.ProductPaymentStatus
import ru.auto.salesman.model.{ActiveProductNaturalKey, Product, UniqueProductType}
import ru.auto.salesman.service.BillingEventProcessor
import ru.auto.salesman.service.BillingEventProcessor.BillingEventResponse
import ru.auto.salesman.service.tskv.dealer.format.TskvLogFormat
import ru.auto.salesman.service.tskv.dealer.logger.TskvApplyActionLogger
import ru.auto.salesman.tasks.credit.ProductsBilling
import ru.auto.salesman.tasks.credit.ProductsBilling.ProductBillingRequest
import ru.auto.salesman.test.BaseSpec

import scala.util.{Success, Try}

class TskvLoggedProductsBillingSpec extends BaseSpec {

  val productsBilling: ProductsBilling = mock[ProductsBilling]

  val logger = mock[TskvApplyActionLogger]

  class ProductBillingMock extends ProductsBilling {

    def makeRequest(
        request: ProductBillingRequest
    ): Task[BillingEventResponse] = productsBilling.makeRequest(request)

  }

  val loggedProductBilling =
    new ProductBillingMock with TskvLoggedProductsBilling {
      protected def tskvLogger: TskvApplyActionLogger = logger
    }

  "TskvLoggedProductsBilling" should {
    "successfully make request and log results" in {
      val request = generateProductBillingRequest()
      val response = generateProductBillingResponse()

      (productsBilling
        .makeRequest(_: ProductsBilling.ProductBillingRequest))
        .expects(request)
        .returningZ(response)

      (logger
        .log(_: ProductBillingRequest, _: Try[BillingEventResponse])(
          _: TskvLogFormat[ProductBillingRequest, BillingEventResponse]
        ))
        .expects(request, Success(response), *)
        .returningZ(())

      loggedProductBilling.makeRequest(request).success.value shouldBe response
    }

    "successfully make request even if logging failed" in {
      val request = generateProductBillingRequest()
      val response = generateProductBillingResponse()

      (productsBilling
        .makeRequest(_: ProductsBilling.ProductBillingRequest))
        .expects(request)
        .returningZ(response)

      (logger
        .log(_: ProductBillingRequest, _: Try[BillingEventResponse])(
          _: TskvLogFormat[ProductBillingRequest, BillingEventResponse]
        ))
        .expects(request, Success(response), *)
        .throwingZ(new Exception("Unable to log"))

      loggedProductBilling.makeRequest(request).success.value shouldBe response
    }
  }

  private def generateProductBillingRequest(): ProductsBilling.ProductBillingRequest =
    ProductsBilling.ProductBillingRequest(
      client = null,
      priceRequest = null,
      product = Product(
        id = 3,
        key = new ActiveProductNaturalKey(
          payer = "",
          target = "target",
          uniqueProductType = UniqueProductType.FullHistoryReport
        ),
        status = ProductPaymentStatus.Active,
        createDate = DateTime.now(),
        expireDate = None,
        context = None,
        inactiveReason = None,
        prolongable = false,
        pushed = false,
        tariff = None
      ),
      productId = null
    )

  private def generateProductBillingResponse() =
    new BillingEventProcessor.BillingEventResponse(
      deadline = DateTime.now(),
      price = None,
      actualPrice = None,
      promocodeFeatures = Nil,
      holdId = None,
      clientId = None,
      agencyId = None,
      companyId = None,
      regionId = None
    )
}
