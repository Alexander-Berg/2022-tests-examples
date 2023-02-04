package ru.yandex.vertis.billing.banker.api.v1.service.customer

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.banker.api.v1.view.{
  AccountPatchView,
  AccountTransactionView,
  AccountView,
  ConsumeAccountTransactionRequestView
}
import ru.yandex.vertis.billing.banker.exceptions.Exceptions.NotEnoughFunds
import ru.yandex.vertis.billing.banker.model.gens.{
  accountTransactionGen,
  transactionRqGen,
  AccountTransactionWithoutRefundGen,
  Producer,
  RequestParams
}
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.service.{AccessDenyException, AccountTransactionService}
import ru.yandex.vertis.billing.banker.util.{Page, SlicedResult}

import scala.concurrent.Future

/**
  * Spec on [[AccountRoute]]
  *
  * @author ruslansd
  */
class AccountRouteSpec extends AnyWordSpecLike with RootHandlerSpecBase {

  private lazy val customer = "test_customer"

  import ru.yandex.vertis.billing.banker.api.v1.view.AccountPatchView.jsonFormat
  import ru.yandex.vertis.billing.banker.api.v1.view.AccountView.{
    jsonFormat,
    modelIterableUnmarshaller,
    modelUnmarshaller
  }

  override def basePath: String = s"/api/1.x/service/autoru/customer/$customer"
  private val account = Account("1", customer)
  private val accountPatch = Account.Patch(email = Some("new_email@example.com"), phone = Some("+79991112233"))

  "/account get" should {
    val request = Get(url("/account"))
    "get account" in {
      when(backend.accounts.get(?[User])(?))
        .thenReturn(Future.successful(Iterable(account)))

      request ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Iterable[Account]]
        response.headOption shouldBe Some(account)
      }
    }

    "fail get account without headers" in {
      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "fail AccessDenyException" in {
      when(backend.accounts.get(?[User])(?))
        .thenReturn(Future.failed(new AccessDenyException(account.user, "artificial")))
      request ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.Forbidden
      }
    }
  }

  "/account post" should {
    import spray.json.enrichAny
    val view = AccountView.asView(account).toJson.compactPrint
    val request = Post(url("/account"))
    "post account" in {
      when(backend.accounts.create(?)(?))
        .thenReturn(Future.successful(account))

      request.withEntity(ContentTypes.`application/json`, view) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Account]
          response shouldBe account
        }
    }

    "fail post account without headers" in {
      request.withEntity(ContentTypes.`application/json`, view) ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
        }
    }

    "fail post account without entity" in {
      request ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "fail post with AccessDenyException" in {
      when(backend.accounts.create(?)(?))
        .thenReturn(Future.failed(new AccessDenyException(account.user, "artificial")))
      request.withEntity(ContentTypes.`application/json`, view) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }
  }

  "PUT /account/{id}" should {
    "update email and phone" in {
      import spray.json.enrichAny
      val view = AccountPatchView.asView(accountPatch).toJson.compactPrint
      val request = Put(url(s"/account/${account.id}"))
      val updatedAccount =
        account.copy(properties = Account.Properties(email = accountPatch.email, phone = accountPatch.phone))
      when(backend.accounts.update(eq(account.id), eq(accountPatch))(?))
        .thenReturn(Future.successful(updatedAccount))

      request.withEntity(ContentTypes.`application/json`, view) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Account]
          response shouldBe updatedAccount
        }
    }
  }

  "DELETE /account/{id}/email" should {
    "clear customer email" in {
      val updatedAccount = account.copy(properties = account.properties.copy(email = None))
      when(backend.accounts.update(eq(account.id), eq(Account.Patch(clearEmail = true)))(?))
        .thenReturn(Future.successful(updatedAccount))

      Delete(url(s"/account/${account.id}/email")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Account]
          response shouldBe updatedAccount
        }
    }
  }

  "DELETE /account/{id}/phone" should {
    "clear customer phone" in {
      val updatedAccount = account.copy(properties = account.properties.copy(phone = None))
      when(backend.accounts.update(eq(account.id), eq(Account.Patch(clearPhone = true)))(?))
        .thenReturn(Future.successful(updatedAccount))

      Delete(url(s"/account/${account.id}/phone")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Account]
          response shouldBe updatedAccount
        }
    }
  }

  "/account/{id}/transactions" should {
    val transactions = AccountTransactionWithoutRefundGen
      .next(100)
      .map { tr =>
        tr.copy(status = AccountTransaction.Statuses.Created)
      }
      .toList

    import AccountTransactionView.slicedResultUnmarshaller

    "list transactions" in {
      val result = SlicedResult(transactions, transactions.size, Page(0, transactions.size))
      when(backend.transactions.list(?, ?)(?))
        .thenReturn(Future.successful(result))

      Get(url(s"/account/${account.id}/transactions?from=2017-01-01")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val actual = responseAs[SlicedResult[AccountTransaction]]
          val expectedResult = AccountTransactionService.normalize(result)
          expectedResult should contain theSameElementsAs actual
        }
    }
    "list transactions with DateTimeInterval.to" in {
      val result = SlicedResult(transactions, transactions.size, Page(0, transactions.size))
      when(backend.transactions.list(?, ?)(?))
        .thenReturn(Future.successful(result))

      Get(url(s"/account/${account.id}/transactions?from=2017-01-01&to=2017-01-02")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val actual = responseAs[SlicedResult[AccountTransaction]]
          val expectedResult = AccountTransactionService.normalize(result)
          expectedResult should contain theSameElementsAs actual
        }
    }

    "fail list transactions without headers" in {
      Get(url(s"/account/${account.id}/transactions?from=2017-01-01")) ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
        }
    }

    "fail with AccessDenyException" in {
      when(backend.transactions.list(?, ?)(?))
        .thenReturn(Future.failed(new AccessDenyException(account.user, "artificial")))
      Get(url(s"/account/${account.id}/transactions?from=2017-01-01")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }
  }

  "/account/{id}/consume" should {
    import AccountTransactionView.modelUnmarshaller
    import ru.yandex.vertis.billing.banker.api.v1.view.ConsumeAccountTransactionRequestView.jsonFormat
    import spray.json.enrichAny
    val trReq = transactionRqGen(
      RequestParams(`type` = Some(AccountTransactions.Withdraw), receiptNeeded = Some(true))
    ).next
    val tr = accountTransactionGen(AccountTransactions.Withdraw).next
      .copy(
        withdraw = trReq.amount,
        receiptData = trReq.receiptData,
        status = AccountTransaction.Statuses.Created
      )

    val trr = AccountTransactionResponse(
      trReq,
      tr,
      None
    )
    val entity = ConsumeAccountTransactionRequestView
      .asView(
        trr.request.asInstanceOf[ConsumeAccountTransactionRequest]
      )
      .toJson
      .compactPrint
    val request = Put(url(s"/account/${account.id}/consume"))

    "consume" in {
      when(backend.transactions.execute(?)(?))
        .thenReturn(Future.successful(trr))

      request.withEntity(ContentTypes.`application/json`, entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[AccountTransaction]
          response shouldBe tr
        }
    }
    "not enough funds" in {
      when(backend.transactions.execute(?)(?))
        .thenReturn(Future.failed(NotEnoughFunds(trr.request)))

      request.withEntity(ContentTypes.`application/json`, entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.PaymentRequired
        }
    }

    "fail consume without headers" in {
      request.withEntity(ContentTypes.`application/json`, entity) ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
        }
    }

    "fail consume without entity" in {
      request ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "fail consume by AccessDenyException" in {
      when(backend.transactions.execute(?)(?))
        .thenReturn(Future.failed(new AccessDenyException(account.user, "artificial")))
      request.withEntity(ContentTypes.`application/json`, entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }
    }
  }

}
