package ru.auto.salesman.service.impl.user

import com.github.nscala_time.time.Imports.DateTime
import com.google.protobuf.timestamp.Timestamp
import org.scalacheck.Gen
import ru.auto.salesman.Task
import ru.auto.salesman.dao.user.TransactionDao
import ru.auto.salesman.dao.user.TransactionDao.{Filter, Patch}
import ru.auto.salesman.environment.RichDateTime
import ru.auto.salesman.exceptions.CompositeException
import ru.auto.salesman.model.TransactionStatuses.Paid
import ru.auto.salesman.model._
import ru.auto.salesman.model.user.PaidTransaction.UnexpectedUnpaidTransaction
import ru.auto.salesman.model.user.{
  PaidProduct,
  PaidTransaction,
  PaymentRequest,
  Transaction
}
import ru.auto.salesman.service.impl.user.prolongation.FailedProlongationProcessor
import ru.auto.salesman.service.user.exceptions.{
  ConflictModificationException,
  NoActivePaidProductForTransactionException
}
import ru.auto.salesman.service.user.{TransactionService, UserProductService}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.banker.model.events_model.{
  PaymentFailureNotification,
  PaymentFailureNotificationsList
}

import scala.util.{Failure, Success, Try}

class PaymentServiceImplSpec extends BaseSpec with UserModelGenerators {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSize = 100)

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  implicit val rc: RequestContext = AutomatedContext("PaymentServiceImplSpec")

  val transactionService: TransactionService = mock[TransactionService]
  val productService = mock[UserProductService]
  val failedProlongationProcessor = mock[FailedProlongationProcessor]

  val service = new PaymentServiceImpl(
    transactionService,
    productService,
    failedProlongationProcessor
  )

  private def whenGetTransaction =
    toMockFunction1(transactionService.getTransaction)

  private def whenGetTransaction(
      request: PaymentRequest,
      result: Try[Transaction]
  ): Unit =
    whenGetTransaction
      .expects(
        TransactionDao.Filter.ForTransactionId(request.payload.transactionId)
      )
      .returning(Task.fromTry(result))

  private def whenUpdateTransaction =
    toMockFunction2(transactionService.update)

  private def whenUpdateTransaction(
      request: PaymentRequest,
      transaction: Transaction,
      result: Try[Unit]
  ): Unit =
    whenUpdateTransaction
      .expects(
        transaction.transactionId,
        Patch.ReceiveTransaction(
          TransactionStatuses.Paid,
          request.bankerTransactionId,
          request.time
        )
      )
      .returning(Task.fromTry(result))

  private def whenDeactivateTransaction =
    toMockFunction1(transactionService.deactivate(_: PaidTransaction))

  "PaymentServiceImpl" when {
    "receiveRaw" should {
      "filter out unparsed requests" in {
        forAll(paymentRequestRawGen, paymentRequestRawGen) {
          case (randomReq, rawReqToSkip) =>
            val rawAutoruReq =
              randomReq
                .copy(payload = randomReq.payload.copy(domain = "autoru"))
                .copy(action = PaymentActions.Deactivate)

            val transaction = transactionGen().next
              .copy(
                bankerTransactionId = None,
                status = TransactionStatuses.Canceled
              )

            (transactionService
              .getTransaction(_: Filter))
              .expects(*)
              .returningZ(transaction)

            val _ = service
              .receiveRaw(Iterable(rawAutoruReq, rawReqToSkip))
              .success
              .value
        }
      }
    }

    "got any request" should {
      "accept empty" in {
        service.receive(Iterable.empty).success.value shouldBe (())
      }

      "fail if there is no transaction" in {
        forAll(paymentRequestGen()) { request =>
          whenGetTransaction(request, Failure(new NoSuchElementException))
          service
            .receive(Iterable(request))
            .failure
            .exception shouldBe a[NoSuchElementException]
        }
      }

      "fail if the transaction has another banker transaction" in {
        forAll(
          paymentRequestGen(),
          TransactionGen.suchThat(_.bankerTransactionId.isDefined)
        ) { (request, transaction) =>
          whenever(
            !transaction.bankerTransactionId.contains(
              request.bankerTransactionId
            )
          ) {
            whenGetTransaction(request, Success(transaction))
            service
              .receive(Iterable(request))
              .failure
              .exception shouldBe a[ConflictModificationException]
          }
        }
      }
    }

    "got Activate request" should {
      val activateRequestGen: Gen[PaymentRequest] =
        paymentRequestGen(action = Some(PaymentActions.Activate))
      val notPaidTransactionGen: Gen[Transaction] =
        TransactionGen.map(_.copy(bankerTransactionId = None))

      "do nothing if the transaction has the same bankerTransactionId" in {
        forAll(activateRequestGen, TransactionGen) { (request, transactionSource) =>
          val transaction = transactionSource.copy(
            bankerTransactionId = Some(request.bankerTransactionId)
          )
          whenGetTransaction(request, Success(transaction))

          service.receive(Iterable(request)).success.value shouldBe (())
        }
      }

      "fail if can't update transaction status" in {
        forAll(activateRequestGen, notPaidTransactionGen) { (request, transaction) =>
          val ex = new RuntimeException
          whenGetTransaction(request, Success(transaction))
          whenUpdateTransaction(request, transaction, Failure(ex))
          service.receive(Iterable(request)).failure.exception shouldBe ex
        }
      }

      "activate if the transaction does not have bankerTransactionId" in {
        forAll(activateRequestGen, notPaidTransactionGen) { (request, transaction) =>
          whenGetTransaction(request, Success(transaction))
          whenUpdateTransaction(request, transaction, Success(()))
          (transactionService.operate _)
            .expects(
              PaidTransaction(
                transaction,
                request.bankerTransactionId,
                request.time
              )
            )
            .returningZ(())

          service.receive(Iterable(request)).success.value shouldBe (())
        }
      }

      "not fail if can't operate transaction" in {
        forAll(activateRequestGen, notPaidTransactionGen) { (request, transaction) =>
          whenGetTransaction(request, Success(transaction))
          whenUpdateTransaction(request, transaction, Success(()))
          (transactionService.operate _)
            .expects(
              PaidTransaction(
                transaction,
                request.bankerTransactionId,
                request.time
              )
            )
            .throwingZ(new RuntimeException)

          service.receive(Iterable(request)).success.value shouldBe (())
        }
      }
    }

    "got Deactivate request" should {
      val deactivateRequestGen: Gen[PaymentRequest] =
        paymentRequestGen(action = Some(PaymentActions.Deactivate))
      val requestWithTransactionGen: Gen[(PaymentRequest, Transaction)] = for {
        request <- deactivateRequestGen
        transaction <- transactionGen(
          bankerTransactionId = Some(request.bankerTransactionId),
          paidAt = createdAt => Gen.some(paidAtGen(createdAt)),
          statusGen = Gen.oneOf(
            TransactionStatuses.values.filter(_ != TransactionStatuses.Canceled)
          )
        )
      } yield
        (
          request,
          transaction.copy(
            bankerTransactionId = Some(request.bankerTransactionId)
          )
        )

      "fail if non-canceled transaction does not have bankerTransactionId" in {
        forAll(
          deactivateRequestGen,
          TransactionGen
            .map(_.copy(bankerTransactionId = None, status = Paid))
        ) { (request, transaction) =>
          whenGetTransaction(request, Success(transaction))
          service
            .receive(Iterable(request))
            .failure
            .exception shouldBe an[UnexpectedUnpaidTransaction]
        }
      }

      "do nothing if the transaction is already Cancelled" in {
        forAll(requestWithTransactionGen) { case (request, transaction) =>
          whenGetTransaction(
            request,
            Success(transaction.copy(status = TransactionStatuses.Canceled))
          )
          service.receive(Iterable(request)).success.value shouldBe (())
        }
      }

      "fail if can't deactivate transaction" in {
        forAll(requestWithTransactionGen) { case (request, transaction) =>
          val ex = new RuntimeException
          whenGetTransaction(request, Success(transaction))
          whenDeactivateTransaction
            .expects(
              PaidTransaction(
                transaction,
                request.bankerTransactionId,
                transaction.paidAt.value
              )
            )
            .throwingZ(ex)

          service.receive(Iterable(request)).failure.exception shouldBe ex
        }
      }

      "deactivate transaction" in {
        forAll(requestWithTransactionGen) { case (request, transaction) =>
          whenGetTransaction(request, Success(transaction))
          whenDeactivateTransaction
            .expects(PaidTransaction.fromPaid(transaction).right.value)
            .returningZ(())

          service.receive(Iterable(request)).success.value shouldBe (())
        }
      }
    }

    "receiveFailureRaw" should {
      "handle transaction payment failure and mark as non-prolongable its prolongable products" in {
        forAll(
          Gen.listOfN(2, TransactionGen),
          PaidOfferProductGen.map(Some(_)),
          Gen.listOfN(2, productRequestGen(product = AutoProlongableOfferProductGen))
        ) { (transactions, paidProductSource, prolongableProducts) =>
          val transaction1 =
            transactions.head.copy(
              status = TransactionStatuses.Process,
              payload = Seq(prolongableProducts.head)
            )
          val transaction2 = transactions.last.copy(
            status = TransactionStatuses.New,
            payload = Seq(prolongableProducts.last)
          )
          val request = PaymentFailureNotificationsList(
            Seq(
              paymentFailureByTransaction(transaction1),
              paymentFailureByTransaction(transaction2)
            )
          )
          (transactionService.getTransaction _)
            .expects(
              TransactionDao.Filter.ForTransactionId(transaction1.transactionId)
            )
            .returningZ(transaction1)

          (transactionService.getTransaction _)
            .expects(
              TransactionDao.Filter.ForTransactionId(transaction2.transactionId)
            )
            .returningZ(transaction2)

          (productService.getActivePaidProduct _)
            .expects(
              UserProductService.Request(
                transaction1.payload.head.product,
                transaction1.user,
                transaction1.payload.head.offer
              )
            )
            .returningZ(paidProductSource)

          (productService.getActivePaidProduct _)
            .expects(
              UserProductService.Request(
                transaction2.payload.head.product,
                transaction2.user,
                transaction2.payload.head.offer
              )
            )
            .returningZ(paidProductSource)

          (failedProlongationProcessor
            .turnProlongationOffAndNotify(_: PaidProduct)(_: RequestContext))
            .expects(paidProductSource.get, *)
            .returningT(())
            .twice()

          service.receiveFailureRaw(request).success.value shouldBe (())
        }
      }

      "fail if transaction status is not NEW or PROCESS" in {
        forAll(Gen.listOfN(3, TransactionGen)) { transactions =>
          val transaction1 = transactions.head.copy(status = TransactionStatuses.Closed)
          val transaction2 =
            transactions.drop(1).head.copy(status = TransactionStatuses.Paid)
          val transaction3 = transactions.last.copy(status = TransactionStatuses.Canceled)
          val request = PaymentFailureNotificationsList(
            Seq(
              paymentFailureByTransaction(transaction1),
              paymentFailureByTransaction(transaction2),
              paymentFailureByTransaction(transaction3)
            )
          )
          (transactionService.getTransaction _)
            .expects(
              TransactionDao.Filter.ForTransactionId(transaction1.transactionId)
            )
            .returningZ(transaction1)

          (transactionService.getTransaction _)
            .expects(
              TransactionDao.Filter.ForTransactionId(transaction2.transactionId)
            )
            .returningZ(transaction2)
          (transactionService.getTransaction _)
            .expects(
              TransactionDao.Filter.ForTransactionId(transaction3.transactionId)
            )
            .returningZ(transaction3)

          val res = service
            .receiveFailureRaw(request)
            .failure
            .exception

          res shouldBe an[ConflictModificationException]
        }
      }

      "fail if no active paid product found for transaction`s prolongable product" in {
        forAll(
          TransactionGen,
          productRequestGen(product = AutoProlongableOfferProductGen),
          productRequestGen(product = NonAutoProlongableOfferProductGen)
        ) { (transactionSource, prolongableProduct, notProlongableProduct) =>
          val transaction = transactionSource
            .copy(
              status = TransactionStatuses.New,
              payload = Seq(
                prolongableProduct,
                notProlongableProduct
              )
            )
          val request = PaymentFailureNotificationsList(
            Seq(
              paymentFailureByTransaction(transaction)
            )
          )
          (transactionService.getTransaction _)
            .expects(
              TransactionDao.Filter.ForTransactionId(transaction.transactionId)
            )
            .returningZ(transaction)

          (productService.getActivePaidProduct _)
            .expects(
              UserProductService.Request(
                prolongableProduct.product,
                transaction.user,
                prolongableProduct.offer
              )
            )
            .returningZ(None)

          val res = service
            .receiveFailureRaw(request)
            .failure
            .exception

          res shouldBe an[CompositeException]
          res match {
            case e: CompositeException =>
              e.errors.head shouldBe an[NoActivePaidProductForTransactionException]
            case _ => throw new Exception("should not come here")
          }

        }
      }
    }
  }

  private def paymentFailureByTransaction(transaction: Transaction) =
    PaymentFailureNotification(
      Some(
        Timestamp(
          DateTime
            .parse("2020-05-27T12:13:14.123456789+04:30:15")
            .asTimestamp
            .getSeconds
        )
      ),
      Some(
        PaymentFailureNotification.PaymentFailurePayload(
          transaction.transactionId,
          "NO_MONEY"
        )
      )
    )
}
