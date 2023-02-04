package ru.auto.salesman.tasks.credit

import org.scalatest.Inspectors
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.model.Product.ProductPaymentStatus.NeedPayment
import ru.auto.salesman.model.ProductId.CreditApplication
import ru.auto.salesman.model.UniqueProductType.GibddHistoryReport
import ru.auto.salesman.model.{
  ActiveProductNaturalKey,
  DetailedClient,
  OfferCategories,
  Product,
  UniqueProductType
}
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.tasks.credit.ProductsBilling.ProductBillingRequest
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.util.time.DateTimeUtil

class ProductsBillingSpec extends BaseSpec {
  "ProductsBilling" when {

    "used for credit applications" should {

      "return empty section and category for unexpected domain" in {
        val request = ProductBillingRequest(
          TestClientMock,
          PriceRequestMock,
          getTestCreditProduct(
            TestCreditProductUniqueKey
              .copy(uniqueProductType = GibddHistoryReport)
          ),
          TestProductId
        )

        request.category shouldBe None
        request.section shouldBe None
      }

      "return empty section and category for wrong target field" in {

        Inspectors.forEvery(List("cars", "cars:use", "cars:new:premium")) { targetField =>
          val request = ProductBillingRequest(
            TestClientMock,
            PriceRequestMock,
            getTestCreditProduct(
              TestCreditProductUniqueKey
                .copy(target = targetField)
            ),
            TestProductId
          )

          request.category shouldBe None
          request.section shouldBe None
        }
      }

      "return proper section and category" in {
        val requestCarsNew = ProductBillingRequest(
          TestClientMock,
          PriceRequestMock,
          getTestCreditProduct(TestCreditProductUniqueKey),
          TestProductId
        )

        requestCarsNew.category shouldBe Some(OfferCategories.Cars)
        requestCarsNew.section shouldBe Some(Section.NEW)

        val requestCarsUsed = ProductBillingRequest(
          TestClientMock,
          PriceRequestMock,
          getTestCreditProduct(
            TestCreditProductUniqueKey.copy(target = "cars:used")
          ),
          TestProductId
        )

        requestCarsUsed.category shouldBe Some(OfferCategories.Cars)
        requestCarsUsed.section shouldBe Some(Section.USED)
      }
    }
  }

  val Now = DateTimeUtil.now()
  val TestProductId = CreditApplication
  val TestClientId = 20101L
  val TestClientMock = mock[DetailedClient]
  val PriceRequestMock = mock[PriceRequest]

  def getTestCreditProduct(key: ActiveProductNaturalKey) =
    Product(
      1L,
      key,
      NeedPayment,
      Now,
      None,
      None,
      None,
      false,
      false,
      None
    )

  val TestCreditProductUniqueKey =
    ActiveProductNaturalKey(
      "Test Payer",
      "cars:new",
      UniqueProductType.ApplicationCreditAccess
    )
}
