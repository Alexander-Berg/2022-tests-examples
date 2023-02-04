package ru.auto.salesman.service.impl.user

import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.user.PaymentOuterClass.PaymentPage
import ru.auto.salesman.dao.user.TransactionDao.Filter.ForUserStatus
import ru.auto.salesman.model.DeprecatedDomains.AutoRu
import ru.auto.salesman.model.user.product.Products.{OfferProduct, UserProduct}
import ru.auto.salesman.model.user.{ProductDescription, Transaction}
import ru.auto.salesman.model.{AutoruUser, DeprecatedDomain, TransactionStatuses}
import ru.auto.salesman.service.ProductDescriptionService
import ru.auto.salesman.service.user.TransactionService
import ru.auto.salesman.test.model.gens.user.UserDaoGenerators
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}
import ru.auto.salesman.util.{Page, SlicedResult}

class PaymentHistoryServiceImplSpec
    extends BaseSpec
    with UserDaoGenerators
    with IntegrationPropertyCheckConfig {
  import PaymentHistoryServiceImplSpec._

  implicit override def domain: DeprecatedDomain = AutoRu

  private val transactionService = mock[TransactionService]
  private val descriptionService = mock[ProductDescriptionService]

  private val service =
    new PaymentHistoryServiceImpl(transactionService, descriptionService)

  "list" should {
    "return payments from transaction service and ask for product description" in {
      forAll(transactionRecordGen()) { testTransaction =>
        testTransaction.payload.foreach { productRequest =>
          productRequest.product match {
            case offerProduct: OfferProduct =>
              (descriptionService.offerDescription _)
                .expects(offerProduct, Category.CARS)
                .returningZ(
                  Some(
                    ProductDescription(name = Some(s"desc:${offerProduct.name}"))
                  )
                )
            case userProduct: UserProduct =>
              (descriptionService.userDescription _)
                .expects(userProduct)
                .returningZ(
                  Some(
                    ProductDescription(name = Some(s"desc:${userProduct.name}"))
                  )
                )
          }
        }

        val transactions =
          List(TransactionServiceImpl.asTransaction(testTransaction))

        (transactionService.list _)
          .expects(
            ForUserStatus(testUser.toString, anyTransactionStatus),
            testPage
          )
          .returningZ(SlicedResult(transactions, transactions.length, testPage))

        val result = service.list(testUser.toString, testPage).success.value
        checkResult(result, testPage, transactions.size, transactions.size)
      }
    }

    "return empty if no transaction exists" in {
      val transactions = List[Transaction]()
      (transactionService.list _)
        .expects(
          ForUserStatus(testUser.toString, anyTransactionStatus),
          testPage
        )
        .returningZ(SlicedResult(transactions, transactions.length, testPage))

      val result = service.list(testUser.toString, testPage).success.value
      checkResult(result, testPage, expectedTotal = 0, expectedCount = 0)

    }

    def checkResult(
        result: PaymentPage,
        expectedPage: Page,
        expectedTotal: Int,
        expectedCount: Int
    ) = {
      result.getSlice.getPage.getSize shouldBe expectedPage.size
      result.getSlice.getPage.getNum shouldBe expectedPage.number
      result.getTotal shouldBe expectedTotal
      result.getPaymentsCount shouldBe expectedCount
    }
  }
}

object PaymentHistoryServiceImplSpec {
  private val testUser = AutoruUser(123)
  private val testPage = Page(1, 10)

  private val anyTransactionStatus = Set(
    TransactionStatuses.New,
    TransactionStatuses.Process,
    TransactionStatuses.Paid,
    TransactionStatuses.Closed,
    TransactionStatuses.Canceled
  )
}
