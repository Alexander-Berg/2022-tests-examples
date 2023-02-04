package ru.auto.salesman.service.impl.user

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.OfferStatus.ACTIVE
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.dao.user.TransactionDao
import ru.auto.salesman.dao.user.TransactionDao.Filter.ForTransactionId
import ru.auto.salesman.dao.user.TransactionDao.TransactionAlreadyExists
import ru.auto.salesman.model._
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.user._
import ru.auto.salesman.model.user.product.ProductSource.{AutoApply, AutoProlong}
import ru.auto.salesman.model.user.product.Products
import ru.auto.salesman.service.ProductApplyService.{Request, Response}
import ru.auto.salesman.service.banker.BankerService
import ru.auto.salesman.service.banker.domain._
import ru.auto.salesman.service.impl.user.ProductApplyServiceImpl.transactionId
import ru.auto.salesman.service.impl.user.ProductApplyServiceImplSpec._
import ru.auto.salesman.service.user.UserFeatureService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.{
  BankerApiModelGenerators,
  ServiceModelGenerators,
  UserDaoGenerators
}
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.banker.model.ApiModel
import ru.yandex.vertis.banker.model.ApiModel.ApiError

class ProductApplyServiceImplSpec
    extends BaseSpec
    with OfferModelGenerators
    with BankerApiModelGenerators
    with ServiceModelGenerators
    with UserDaoGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  implicit val rc: RequestContext = AutomatedContext("unit-test")

  val vosClient: VosClient = mock[VosClient]
  val bankerApi: BankerService = mock[BankerService]
  val transactionDao: TransactionDao = mock[TransactionDao]
  val featureService: UserFeatureService = mock[UserFeatureService]

  val service =
    new ProductApplyServiceImpl(vosClient, bankerApi, transactionDao, featureService)

  private def mockFeatureToggleOn() =
    (featureService.useNewRecurrentPaymentWay _)
      .expects()
      .returning(true)

  private def mockFeatureToggleOff() =
    (featureService.useNewRecurrentPaymentWay _)
      .expects()
      .returning(false)

  private def mockVosGetOfferWith(
      offerId: OfferIdentity,
      result: Option[ApiOfferModel.Offer]
  ): Unit =
    (vosClient.getOptOffer _).expects(offerId, Slave).returningZ(result)

  private def mockBankerGetAccount =
    toMockFunction1(bankerApi.getAccount(_: UserId))

  private def mockTransactionService(
      request: Request,
      productPrice: ProductPrice,
      result: TransactionId => Either[TransactionAlreadyExists, Unit]
  ): TransactionId = {
    val daoRequest = ProductApplyServiceImpl
      .transactionRequest(request, productPrice)
      .success
      .value
    (transactionDao.create _)
      .expects(daoRequest)
      .returningZ(result(daoRequest.transactionId))
    daoRequest.transactionId
  }

  private def mockTransactionService(
      request: Request,
      productPrice: ProductPrice
  ): Unit =
    mockTransactionService(request, productPrice, result = _ => Right(()))

  private def mockTransactionServiceAlreadyExists(
      request: Request,
      productPrice: ProductPrice
  ): TransactionId =
    mockTransactionService(
      request,
      productPrice,
      result = transactionId => Left(TransactionAlreadyExists(transactionId))
    )

  private def mockGetTransaction(
      transactionId: TransactionId,
      operateOnMaster: Boolean,
      transaction: TransactionDao.Record
  ): Unit =
    (transactionDao
      .get(_: TransactionDao.Filter, _: Boolean))
      .expects(ForTransactionId(transactionId), operateOnMaster)
      .returningZ(List(transaction))

  "ProductApplyService" should {
    "fail in case of VOS error" in {
      forAll(bindedOfferProductRequestGen()) { request =>
        (vosClient.getOptOffer _)
          .expects(request.optOfferId.value, Slave)
          .throwingZ(new RuntimeException)

        service
          .applyProduct(request)
          .failure
          .exception shouldBe a[RuntimeException]
      }
    }
    "return NotApplied if offer does not exist" in {
      forAll(bindedOfferProductRequestGen()) { request =>
        mockVosGetOfferWith(request.optOfferId.value, None)

        service
          .applyProduct(request)
          .success
          .value shouldBe a[Response.NotApplied]
      }
    }

    "return NotApplies if offer is not active" in {
      forAll(bindedOfferProductRequestGen(), NotActiveOfferGen) { (request, offer) =>
        mockVosGetOfferWith(request.optOfferId.value, Some(offer))

        service
          .applyProduct(request)
          .success
          .value shouldBe a[Response.NotApplied]
      }
    }

    "create autoapply transaction request" in {
      forAll(BindedOfferProductRequestGen, ProductPriceGen) { (request, price) =>
        val autoApplyRequest =
          request.copy(source = AutoApply(scheduleInstanceId = 666))
        val transactionRequest = ProductApplyServiceImpl
          .transactionRequest(autoApplyRequest, price)
          .success
          .value
        transactionRequest.user shouldBe request.user.toString
        transactionRequest.amount shouldBe price.price.effectivePrice
        transactionRequest.fields should contain only "from" -> "auto_apply"
        transactionRequest.payload should have size 1
        val payload = transactionRequest.payload.head
        payload.product shouldBe request.product
        payload.offer shouldBe request.optOfferId
        payload.amount shouldBe price.price.effectivePrice
        payload.context.productPrice shouldBe price
        payload.prolongable shouldBe Prolongable(false)
      }
    }

    "create autoprolong transaction request" in {
      forAll(BindedOfferProductRequestGen, ProductPriceGen) { (request, price) =>
        val autoProlongRequest = request.copy(source = AutoProlong("test-id"))
        val transactionRequest = ProductApplyServiceImpl
          .transactionRequest(autoProlongRequest, price)
          .success
          .value
        transactionRequest.user shouldBe request.user.toString
        transactionRequest.amount shouldBe price.price.effectivePrice
        transactionRequest.payload should have size 1
        transactionRequest.fields should contain only "from" -> "auto_prolong"
        val payload = transactionRequest.payload.head
        payload.product shouldBe request.product
        payload.offer shouldBe request.optOfferId
        payload.amount shouldBe price.price.effectivePrice
        payload.context.productPrice shouldBe price
        payload.prolongable shouldBe Prolongable(true)
      }
    }
  }

  "ProductApplyService" when {

    "user uses Banker for offer product" should {
      def mockVos(request: Request, offer: ApiOfferModel.Offer): Unit =
        mockVosGetOfferWith(request.optOfferId.value, Some(offer))

      def mockVosAccount(
          request: Request,
          offer: ApiOfferModel.Offer,
          account: ApiModel.Account
      ): Unit = {
        mockVos(request, offer)
        mockBankerGetAccount
          .expects(request.user.toString)
          .returningZ(Some(account))
      }

      "fail if can't get user account" in {
        forAll(AutoruUserGen, minSuccessful(10)) { user =>
          forAll(
            bindedOfferProductRequestGen(Gen.const(user)),
            offerGen(statusGen = ACTIVE, userRefGen = user.toString),
            minSuccessful(10)
          ) { (request, offer) =>
            val ex = new RuntimeException
            inSequence {
              mockVos(request, offer)
              mockBankerGetAccount.expects(*).throwingZ(ex)
            }
            service.applyProduct(request).failure.exception shouldBe ex
          }
        }
      }

      "return NotApplied if the user does not have an account and the price is non zero" in {
        forAll(AutoruUserGen, minSuccessful(10)) { user =>
          forAll(
            bindedProductRequestNotZeroPriceGen(
              bindedOfferProductGen(Gen.const(user))
            ),
            offerGen(statusGen = ACTIVE, userRefGen = user.toString),
            minSuccessful(10)
          ) { (request, offer) =>
            inSequence {
              mockVos(request, offer)
              mockBankerGetAccount
                .expects(request.user.toString)
                .returningZ(None)
            }
            service
              .applyProduct(request)
              .success
              .value shouldBe a[Response.NotApplied]
          }
        }
      }

      "create account if the price is zero" in {
        forAll(AutoruUserGen, minSuccessful(10)) { user =>
          forAll(
            bindedProductRequestZeroPriceGen(
              bindedOfferProductGen(Gen.const(user))
            ),
            offerGen(statusGen = ACTIVE, userRefGen = user.toString),
            minSuccessful(10)
          ) { (request, offer) =>
            val ex = new RuntimeException("marker")
            inSequence {
              mockVos(request, offer)
              mockBankerGetAccount
                .expects(request.user.toString)
                .returningZ(None)
              (bankerApi
                .createAccount(_: UserId))
                .expects(request.user.toString)
                .throwingZ(ex)
            }

            service.applyProduct(request).failure.exception shouldBe ex
          }
        }
      }

      "fail if failed to pay by account" in {
        forAll(AutoruUserGen, ProductPriceGen, minSuccessful(10)) {
          (user, productPrice) =>
            forAll(
              bindedOfferProductRequestGen(
                Gen.const(user),
                Gen.const(productPrice)
              ),
              offerGen(statusGen = ACTIVE, userRefGen = user.toString),
              AccountGen,
              minSuccessful(10)
            ) { (request, offer, account) =>
              val ex = new RuntimeException
              inSequence {
                mockVosAccount(request, offer, account)
                mockTransactionService(request, productPrice)
                mockFeatureToggleOff()
                (bankerApi
                  .payByAccount(_: PayByAccountRequest))
                  .expects(*)
                  .throwingZ(ex)
              }
              service.applyProduct(request).failure.exception shouldBe ex
            }
        }
      }

      "return Applied if paid by account" in {
        forAll(ProductPriceGen, AutoruUserGen, minSuccessful(10)) {
          (productPrice, user) =>
            forAll(
              bindedOfferProductRequestGen(
                Gen.const(user),
                Gen.const(productPrice)
              ),
              offerGen(statusGen = ACTIVE, userRefGen = user.toString),
              AccountGen,
              minSuccessful(10)
            ) { (request, offer, account) =>
              inSequence {
                mockVosAccount(request, offer, account)
                mockTransactionService(request, productPrice)
                mockFeatureToggleOff()
                (bankerApi
                  .payByAccount(_: PayByAccountRequest))
                  .expects(where { (r: PayByAccountRequest) =>
                    r.account shouldBe account
                    r.amount shouldBe productPrice.price.effectivePrice
                    true
                  })
                  .returningZ(payByAccountResponse(true))
                (bankerApi
                  .payWithTiedCard(_: PayWithLinkedCardRequest))
                  .expects(*)
                  .never()
              }
              service
                .applyProduct(request)
                .success
                .value shouldBe Response.Applied
            }
        }
      }

      "return Applied if transaction exists but not processed and then paid by account" in {
        forAll(AutoruUserGen, ProductPriceGen, minSuccessful(10)) {
          (user, productPrice) =>
            forAll(
              bindedOfferProductRequestGen(
                Gen.const(user),
                Gen.const(productPrice)
              ),
              offerGen(statusGen = ACTIVE, userRefGen = user.toString),
              transactionRecordGen(Gen.const(TransactionStatuses.Process)),
              AccountGen,
              minSuccessful(10)
            ) { (request, offer, transaction, account) =>
              inSequence {
                mockVosAccount(request, offer, account)
                val transactionId =
                  mockTransactionServiceAlreadyExists(request, productPrice)
                mockGetTransaction(
                  transactionId,
                  operateOnMaster = true,
                  transaction
                )
                mockFeatureToggleOff()
                (bankerApi
                  .payByAccount(_: PayByAccountRequest))
                  .expects(where { (r: PayByAccountRequest) =>
                    r.account shouldBe account
                    r.amount shouldBe productPrice.price.effectivePrice
                    true
                  })
                  .returningZ(payByAccountResponse(true))
                (bankerApi
                  .payWithTiedCard(_: PayWithLinkedCardRequest))
                  .expects(*)
                  .never()
              }
              service
                .applyProduct(request)
                .success
                .value shouldBe Response.Applied
            }
        }
      }

      "return TransactionAlreadyExists if transaction already exists and processed" in {
        forAll(AutoruUserGen, ProductPriceGen, minSuccessful(10)) {
          (user, productPrice) =>
            forAll(
              bindedOfferProductRequestGen(
                Gen.const(user),
                Gen.const(productPrice)
              ),
              offerGen(statusGen = ACTIVE, userRefGen = user.toString),
              transactionRecordGen(Gen.const(TransactionStatuses.Paid)),
              AccountGen,
              minSuccessful(10)
            ) { (request, offer, transaction, account) =>
              inSequence {
                mockVosAccount(request, offer, account)
                val transactionId =
                  mockTransactionServiceAlreadyExists(request, productPrice)
                mockGetTransaction(
                  transactionId,
                  operateOnMaster = true,
                  transaction
                )
                (bankerApi
                  .payByAccount(_: PayByAccountRequest))
                  .expects(*)
                  .never()
                (bankerApi
                  .payWithTiedCard(_: PayWithLinkedCardRequest))
                  .expects(*)
                  .never()
              }
              service
                .applyProduct(request)
                .success
                .value shouldBe a[Response.TransactionAlreadyExists]
            }
        }
      }

      def requestWithPositivePriceOfferAndAccountGen =
        for {
          user <- AutoruUserGen
          request <- bindedOfferProductPositivePriceRequestGen(Gen.const(user))
          offer <- offerGen(statusGen = ACTIVE, userRefGen = user.toString)
          account <- AccountGen
        } yield (request, offer, account)

      "fail if pay with tied card failed" in {
        forAll(requestWithPositivePriceOfferAndAccountGen) {
          case (request, offer, account) =>
            val ex = new RuntimeException
            inSequence {
              mockVosAccount(request, offer, account)
              mockTransactionService(request, request.productPrice)
              mockFeatureToggleOff()
              (bankerApi
                .payByAccount(_: PayByAccountRequest))
                .expects(*)
                .returningZ(payByAccountResponse(false))
              (bankerApi
                .payWithTiedCard(_: PayWithLinkedCardRequest))
                .expects(*)
                .throwingZ(ex)
            }

            service.applyProduct(request).failure.exception shouldBe ex
        }
      }

      "pay with tied card if can't pay by account" in {
        forAll(requestWithPositivePriceOfferAndAccountGen) {
          case (request, offer, account) =>
            inSequence {
              mockVosAccount(request, offer, account)
              mockTransactionService(request, request.productPrice)
              mockFeatureToggleOff()
              (bankerApi
                .payByAccount(_: PayByAccountRequest))
                .expects(*)
                .returningZ(payByAccountResponse(false))
              (bankerApi
                .payWithTiedCard(_: PayWithLinkedCardRequest))
                .expects(where { (r: PayWithLinkedCardRequest) =>
                  r.account shouldBe account
                  r.amount shouldBe request.productPrice.price.effectivePrice
                  r.payload shouldBe PaymentPayload(transactionId(request))
                  r.target shouldBe ApiModel.Target.PURCHASE
                  val expectedReceiptRow = ReceiptRow(
                    request.product.name,
                    Products.receiptNameOf(request.product),
                    1,
                    request.productPrice.price.effectivePrice
                  )
                  r.receiptRows should (have size 1 and contain(
                    expectedReceiptRow
                  ))
                  true
                })
                .returningZ(PayWithLinkedCardResponse.Paid("formId"))
            }

            service
              .applyProduct(request)
              .success
              .value shouldBe Response.Applied
        }
      }

      "return NotApplied if can't pay by account and with cards" in {
        val payWithTiedCardResponseGen: Gen[PayWithLinkedCardResponse] =
          Gen.oneOf(
            PayWithTiedCardErrorResponse,
            PayWithLinkedCardResponse.NoLinkedCards
          )
        forAll(requestWithPositivePriceOfferAndAccountGen, minSuccessful(10)) {
          case (request, offer, account) =>
            forAll(payWithTiedCardResponseGen, minSuccessful(10)) {
              payWithTiedCardResponse =>
                inSequence {
                  mockVosAccount(request, offer, account)
                  mockTransactionService(request, request.productPrice)
                  mockFeatureToggleOff()
                  (bankerApi
                    .payByAccount(_: PayByAccountRequest))
                    .expects(*)
                    .returningZ(payByAccountResponse(false))
                  (bankerApi
                    .payWithTiedCard(_: PayWithLinkedCardRequest))
                    .expects(*)
                    .returningZ(payWithTiedCardResponse)
                }
                service
                  .applyProduct(request)
                  .success
                  .value shouldBe a[Response.NotApplied]
            }
        }
      }

      "pay recurrent if it is trust payment" in {
        forAll(
          requestWithPositivePriceOfferAndAccountGen,
          transactionRecordGen(Gen.const(TransactionStatuses.Process))
        ) { case ((request, offer, account), transaction) =>
          inSequence {
            mockVosAccount(request, offer, account)
            mockTransactionService(request, request.productPrice)
            mockFeatureToggleOn()
            mockGetTransaction(
              request.parentTransactionId.getOrElse("test"),
              operateOnMaster = false,
              transaction.copy(bankerTransactionId = Some("8#" + readableString.next))
            )
            (bankerApi
              .payRecurrent(_: PayRecurrentRequest))
              .expects(where { (r: PayRecurrentRequest) =>
                r.account shouldBe account
                r.amount shouldBe request.productPrice.price.effectivePrice
                r.payload shouldBe PaymentPayload(transactionId(request))
                r.target shouldBe ApiModel.Target.PURCHASE
                val expectedReceiptRow = ReceiptRow(
                  request.product.name,
                  Products.receiptNameOf(request.product),
                  1,
                  request.productPrice.price.effectivePrice
                )
                r.receiptRows should (have size 1 and contain(
                  expectedReceiptRow
                ))
                true
              })
              .returningZ(PayRecurrentResponse.Accepted("IN PROGRESS"))
          }

          service
            .applyProduct(request)
            .success
            .value shouldBe Response.Applied
        }
      }

      "return NotApplied if can't pay recurrent" in {
        forAll(
          requestWithPositivePriceOfferAndAccountGen,
          transactionRecordGen(Gen.const(TransactionStatuses.Process))
        ) { case ((request, offer, account), transaction) =>
          inSequence {
            mockVosAccount(request, offer, account)
            mockTransactionService(request, request.productPrice)
            mockFeatureToggleOn()
            mockGetTransaction(
              request.parentTransactionId.getOrElse("test"),
              operateOnMaster = false,
              transaction.copy(bankerTransactionId = Some("8#" + readableString.next))
            )
            (bankerApi
              .payRecurrent(_: PayRecurrentRequest))
              .expects(where { (r: PayRecurrentRequest) =>
                r.account shouldBe account
                r.amount shouldBe request.productPrice.price.effectivePrice
                r.payload shouldBe PaymentPayload(transactionId(request))
                r.target shouldBe ApiModel.Target.PURCHASE
                val expectedReceiptRow = ReceiptRow(
                  request.product.name,
                  Products.receiptNameOf(request.product),
                  1,
                  request.productPrice.price.effectivePrice
                )
                r.receiptRows should (have size 1 and contain(
                  expectedReceiptRow
                ))
                true
              })
              .returningZ(PayRecurrentResponse.RecurrentError)
          }

          service
            .applyProduct(request)
            .success
            .value shouldBe Response.RecurrentPaymentError
        }
      }

      "fail if pay recurrent failed" in {
        forAll(
          requestWithPositivePriceOfferAndAccountGen,
          transactionRecordGen(Gen.const(TransactionStatuses.Process))
        ) { case ((request, offer, account), transaction) =>
          val ex = new RuntimeException
          inSequence {
            mockVosAccount(request, offer, account)
            mockTransactionService(request, request.productPrice)
            mockFeatureToggleOn()
            mockGetTransaction(
              request.parentTransactionId.getOrElse("test"),
              operateOnMaster = false,
              transaction.copy(bankerTransactionId = Some("8#" + readableString.next))
            )
            (bankerApi
              .payRecurrent(_: PayRecurrentRequest))
              .expects(*)
              .throwingZ(ex)
          }

          service
            .applyProduct(request)
            .failure
            .exception shouldBe ex
        }
      }
    }

    "fail for user product" in {
      forAll(BindedUserProductRequestGen, ProductPriceGen) {
        (requestGenerated, productPrice) =>
          val request = requestGenerated.copy(productPrice = productPrice)
          service.applyProduct(request).failure.exception shouldBe an[Exception]
      }
    }

  }
}

object ProductApplyServiceImplSpec {

  val PayWithTiedCardErrorResponse =
    PayWithLinkedCardResponse.PaymentError(List(ApiError.getDefaultInstance))

  def payByAccountResponse(success: Boolean): PayByAccountResponse =
    if (success)
      PayByAccountResponse.Paid(ApiModel.Transaction.getDefaultInstance)
    else
      PayByAccountResponse.NotEnoughFunds("client didn't have enough money")

}
