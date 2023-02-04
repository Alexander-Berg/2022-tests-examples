package ru.auto.salesman.api.v1.service.transaction

import akka.http.scaladsl.model.HttpMethods.{DELETE, PUT}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes._
import org.scalacheck.Gen
import org.scalatest.Inspectors
import ru.auto.salesman.api.v1.SalesmanApiUtils.SalesmanHttpRequest
import ru.auto.salesman.api.v1.{HandlerBaseSpec, JdbcProductServices}
import ru.auto.salesman.model.{TransactionId, TransactionStatuses}
import ru.auto.salesman.model.TransactionStatuses.{New, Process}
import ru.auto.salesman.model.user.product.ProductProvider.AutoProlongable
import ru.auto.salesman.model.user.{Prolongable, TransactionRequest}

trait HandlerSpec extends HandlerBaseSpec with JdbcProductServices {

  import HandlerSpec._

  "Prolongable handler" should {

    "put prolongable flag for new transaction products" in {
      forAll(TransactionRequestGen) { transactionRequest =>
        val transaction = createTransaction(
          transactionRequest.withProlongable(Prolongable(false))
        )
        checkProlongableTrue(transaction.transactionId)
      }
    }

    "put prolongable flag for processing transaction products" in {
      forAll(TransactionRequestGen) { transactionRequest =>
        val transaction = createTransaction(
          transactionRequest.withProlongable(Prolongable(false))
        )
        import transaction.transactionId
        updateTransactionStatus(transactionId, Process)
        checkProlongableTrue(transactionId)
      }
    }

    "return not found on transaction with statuses other than new and process" in {
      val statusGen =
        Gen.oneOf(TransactionStatuses.values.diff(Set(New, Process)).toSeq)
      forAll(TransactionRequestGen, statusGen) {
        (transactionRequest, transactionStatus) =>
          val transaction = createTransaction(
            transactionRequest.withProlongable(Prolongable(false))
          )
          import transaction.transactionId
          updateTransactionStatus(transactionId, transactionStatus)
          checkNotFoundOnPut(transactionId)
      }
    }

    "return not found on unexisting transaction" in {
      forAll(readableString)(checkNotFoundOnPut)
    }

    "delete prolongable flag for new transaction products" in {
      forAll(TransactionRequestGen) { transactionRequest =>
        val transaction = createTransaction(
          transactionRequest.withProlongable(Prolongable(true))
        )
        checkProlongableFalse(transaction.transactionId)
      }
    }

    "delete prolongable flag for processing transaction products" in {
      forAll(TransactionRequestGen) { transactionRequest =>
        val transaction = createTransaction(
          transactionRequest.withProlongable(Prolongable(true))
        )
        import transaction.transactionId
        updateTransactionStatus(transactionId, Process)
        checkProlongableFalse(transactionId)
      }
    }

    "DELETE return not found on transaction with statuses other than new and process" in {
      val statusGen =
        Gen.oneOf(TransactionStatuses.values.diff(Set(New, Process)).toSeq)
      forAll(TransactionRequestGen, statusGen) {
        (transactionRequest, transactionStatus) =>
          val transaction = createTransaction(
            transactionRequest.withProlongable(Prolongable(true))
          )
          import transaction.transactionId
          updateTransactionStatus(transactionId, transactionStatus)
          checkNotFoundOnDelete(transactionId)
      }
    }

    "DELETE return not found on unexisting transaction" in {
      forAll(readableString)(checkNotFoundOnDelete)
    }
  }

  private def checkProlongableTrue(id: TransactionId) =
    putProlongableRequest(id) ~> route ~> check {
      status shouldBe OK
      Inspectors.forEvery(getTransaction(id).payload) { productRequest =>
        productRequest.product match {
          case _: AutoProlongable =>
            productRequest.prolongable shouldBe Prolongable(true)
          case _ =>
            productRequest.prolongable shouldBe Prolongable(false)
        }
      }
    }

  private def checkProlongableFalse(id: TransactionId) =
    deleteProlongableRequest(id) ~> route ~> check {
      status shouldBe OK
      Inspectors.forEvery(getTransaction(id).payload) { productRequest =>
        productRequest.prolongable shouldBe Prolongable(false)
      }
    }

  private def checkNotFoundOnPut(id: TransactionId) =
    putProlongableRequest(id) ~> route ~> check(status shouldBe NotFound)

  private def checkNotFoundOnDelete(id: TransactionId) =
    deleteProlongableRequest(id) ~> route ~> check(status shouldBe NotFound)

  private def putProlongableRequest(id: TransactionId) =
    HttpRequest(PUT, s"/api/1.x/service/autoru/transaction/$id/prolongable")
      .withSalesmanTestHeader()

  private def deleteProlongableRequest(id: TransactionId) =
    HttpRequest(DELETE, s"/api/1.x/service/autoru/transaction/$id/prolongable")
      .withSalesmanTestHeader()
}

object HandlerSpec {

  implicit class RichTransactionRequest(
      val transactionRequest: TransactionRequest
  ) extends AnyVal {

    def withProlongable(prolongable: Prolongable): TransactionRequest =
      transactionRequest.copy(
        payload = transactionRequest.payload.map(_.copy(prolongable = prolongable))
      )
  }
}
