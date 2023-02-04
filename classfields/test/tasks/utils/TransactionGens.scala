package ru.yandex.vertis.billing.banker.tasks.utils

import org.scalacheck.Gen
import ru.yandex.vertis.billing.banker.model.{AccountTransaction, AccountTransactions}
import ru.yandex.vertis.billing.banker.model.AccountTransaction.Statuses
import ru.yandex.vertis.billing.banker.model.PaymentRequest.Targets
import ru.yandex.vertis.billing.banker.model.gens.{
  accountTransactionGen,
  HashAccountTransactionIdGen,
  PaymentSystemAccountTransactionIdGen,
  Producer
}

object TransactionGens {

  val ZeroAccountTransactionGen: Gen[AccountTransaction] = for {
    trType <- Gen.oneOf(AccountTransactions.values.toSeq.filterNot(_ == AccountTransactions.Refund))
    tr <- accountTransactionGen(trType)
  } yield tr.copy(target = Some(Targets.Purchase))

  val PaymentSystemTransactionGen: Gen[AccountTransaction] = for {
    tr <- ZeroAccountTransactionGen
    id <- PaymentSystemAccountTransactionIdGen
  } yield tr.copy(id = id.copy(`type` = tr.id.`type`))

  val PaymentSystemProcessedTransactionGen: Gen[AccountTransaction] =
    PaymentSystemTransactionGen.map(tr => tr.copy(status = Statuses.Processed))

  val PaymentSystemHashUnprocessedTransactionGen: Gen[AccountTransaction] =
    PaymentSystemTransactionGen.map(tr => tr.copy(status = Statuses.Created))

  val HashTransactionGen: Gen[AccountTransaction] = for {
    tr <- ZeroAccountTransactionGen
    id <- HashAccountTransactionIdGen
  } yield tr.copy(id = id.copy(`type` = tr.id.`type`))

  val HashProcessedTransactionGen: Gen[AccountTransaction] =
    HashTransactionGen.map(tr => tr.copy(status = Statuses.Processed))

  val HashUnprocessedTransactionGen: Gen[AccountTransaction] =
    HashTransactionGen.map(tr => tr.copy(status = Statuses.Created))

}
