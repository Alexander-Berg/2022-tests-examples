package ru.yandex.vertis.billing.banker.payment

import org.scalamock.scalatest.MockFactory
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.TransactionsFilter
import ru.yandex.vertis.billing.banker.model.Account.Info
import ru.yandex.vertis.billing.banker.model.AccountId
import ru.yandex.vertis.billing.banker.service.AccountTransactionService
import ru.yandex.vertis.billing.banker.util.RequestContext

import scala.concurrent.Future

trait TransactionServiceMockProvider extends MockFactory {

  private val transactionsService = mock[AccountTransactionService]

  protected def mockBalance(totalIncome: Long): Unit = {
    (transactionsService
      .info(_: AccountId, _: TransactionsFilter)(_: RequestContext))
      .expects(*, *, *)
      .returns(Future.successful(Info(totalIncome, 0L, 0L, 0L))): Unit
  }

}
