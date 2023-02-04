package ru.yandex.vertis.billing.banker.service.impl

import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.YandexKassaRecurrentDao
import ru.yandex.vertis.billing.banker.dao.gens.YandexKassaRecurrentRecordGen
import ru.yandex.vertis.billing.banker.model.AccountTransaction.Activities
import ru.yandex.vertis.billing.banker.model.PaymentRequest.Targets
import ru.yandex.vertis.billing.banker.model._
import ru.yandex.vertis.billing.banker.model.gens.{
  AccountGen,
  AccountTransactionGen,
  AccountTransactionWithoutRefundGen,
  Producer
}
import ru.yandex.vertis.billing.banker.service.AccountBootstrapService.AttachedCard
import ru.yandex.vertis.billing.banker.service.impl.AccountBootstrapServiceImplSpec.{Mocks, Setup}
import ru.yandex.vertis.billing.banker.service.{AccountBootstrapService, AccountService, AccountTransactionService}
import ru.yandex.vertis.billing.banker.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

/**
  * Runnable spec on [[AccountTransactionServiceImpl]]
  *
  * @author alex-kovalenko
  */
class AccountBootstrapServiceImplSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  implicit val rc = AutomatedContext("AccountBootstrapServiceImplSpec")

  "AccountBootstrapServiceImpl" should {
    "fail" when {
      "can't create account" in new Mocks {
        when(accounts.create(?)(?)).thenReturn(Future.failed(new Exception("artificial")))
        val source = AccountBootstrapService.Source(AccountGen.next, 0L, Iterable.empty)
        bootstrap.create(source).toTry should matchPattern {
          case Failure(e) if e.getMessage == "artificial" =>
        }
      }
      "can't execute transaction" in new Mocks {
        when(accounts.create(?)(?)).thenReturn(Future.successful(AccountGen.next))
        when(transactions.get(?)(?)).thenReturn(Future.successful(Iterable.empty))
        when(transactions.execute(?)(?)).thenReturn(Future.failed(new Exception("artificial")))
        val source = AccountBootstrapService.Source(AccountGen.next, 100L, Iterable.empty)
        bootstrap.create(source).toTry should matchPattern {
          case Failure(e) if e.getMessage == "artificial" =>
        }
      }
    }

    "do not execute transaction if zero and no transaction exist" in new Setup {
      val source = AccountBootstrapService.Source(AccountGen.next, 0L, Iterable.empty)
      bootstrap.create(source).futureValue
      Mockito.verify(accounts).create(source.account)
      Mockito.verify(transactions).get(?)(?)
      Mockito.verifyNoMoreInteractions(transactions)
    }

    "do not execute transaction if zero and existent transaction is zero" in new Setup {
      val source = AccountBootstrapService.Source(AccountGen.next, 0L, Iterable.empty)
      val tr = AccountTransactionWithoutRefundGen.next.copy(
        income = 0L,
        withdraw = 0L
      )
      when(transactions.get(?)(?))
        .thenReturn(Future.successful(Iterable(tr)))
      bootstrap.create(source).futureValue
      Mockito.verify(accounts).create(source.account)
      Mockito.verify(transactions).get(?)(?)
      Mockito.verifyNoMoreInteractions(transactions)
    }

    "reset income if zero and transaction exists" in new Setup {
      val source = AccountBootstrapService.Source(AccountGen.next, 0L, Iterable.empty)
      when(transactions.get(?)(?))
        .thenReturn(Future.successful(AccountTransactionGen.next(1).toList))
    }

    "upsert account and income" in new Setup {
      val accountCaptor: ArgumentCaptor[Account] =
        ArgumentCaptor.forClass(classOf[Account])
      val requestCaptor: ArgumentCaptor[AccountTransactionRequest] =
        ArgumentCaptor.forClass(classOf[AccountTransactionRequest])
      val recurrentsCaptor: ArgumentCaptor[Iterable[YandexKassaRecurrentDao.Record]] =
        ArgumentCaptor.forClass(classOf[Iterable[YandexKassaRecurrentDao.Record]])

      val account = AccountGen.next
      val income = 1000L
      val cards = YandexKassaRecurrentRecordGen
        .next(3)
        .map(r => AttachedCard(r.cddPanMask, r.baseInvoiceId.toInt, r.isPreferred))
        .toList
      val source = AccountBootstrapService.Source(account, income, cards)

      val created = bootstrap.create(source).futureValue
      created shouldBe account

      Mockito.verify(accounts).create(accountCaptor.capture())(?)
      accountCaptor.getValue shouldBe account

      Mockito.verify(transactions).execute(requestCaptor.capture())(?)
      val atr = requestCaptor.getValue
      atr.account shouldBe account.id
      atr.amount shouldBe income
      atr.payload shouldBe Payload.Empty
      atr.id should matchPattern {
        case id: HashAccountTransactionId
            if id.`type` == AccountTransactions.Incoming &&
              id.id == s"${account.id}_balance" =>
      }
      atr.id.`type` shouldBe AccountTransactions.Incoming
      atr.activity shouldBe Activities.Active
    }
  }
}

object AccountBootstrapServiceImplSpec {

  trait Mocks extends MockitoSupport {
    implicit val ec = ExecutionContext.global
    val accounts = mock[AccountService]
    val transactions = mock[AccountTransactionService]
    val bootstrap = new AccountBootstrapServiceImpl(accounts, transactions)
  }

  trait Setup extends Mocks {

    stub(accounts.create(_: Account)(_: RequestContext)) { case (a, _) =>
      Future.successful(a)
    }
    when(transactions.get(?)(?)).thenReturn(Future.successful(Iterable.empty))

    stub(transactions.execute(_: AccountTransactionRequest)(_: RequestContext)) { case (atr, _) =>
      Future.successful(
        AccountTransactionResponse(
          atr,
          AccountTransaction(
            id = atr.id,
            account = atr.account,
            user = "",
            timestamp = atr.timestamp,
            income = atr.amount,
            withdraw = 0L,
            overdraft = 0L,
            refund = 0L,
            payload = atr.payload,
            payGatePayload = None,
            activity = atr.activity,
            receiptData = None,
            target = Some(Targets.ExternalTransfer),
            refundFor = atr.refundFor
          ),
          None
        )
      )
    }
  }
}
