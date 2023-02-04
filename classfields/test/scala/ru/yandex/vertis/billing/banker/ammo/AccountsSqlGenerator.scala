package ru.yandex.vertis.billing.banker.ammo

import ru.yandex.vertis.billing.banker.model.PaymentSystemIds
import ru.yandex.vertis.billing.banker.ammo.Constants.AccountPrefix

/**
  * Generates sql scripts to create accounts and delete transactions/payments/requests
  *
  * Generated accounts have name like 'stressTestNumber.AccountNumber'
  *
  * TestNumber - number of ammo generator
  * AccountCount - desired count of test accounts
  *
  * @author alex-kovalenko
  */
object AccountsSqlGenerator extends App {

  object DeleteModes extends Enumeration {
    val All = Value(0)
    val Consumes = Value(1)
  }

  val AccountCount = 10

  val Mode = DeleteModes.All

  val Accounts =
    if (AccountCount > 1) {
      (1 to AccountCount).map { i =>
        s"$AccountPrefix$i"
      }
    } else {
      Iterable(AccountPrefix)
    }

  val values = Accounts.map(a => s"('$a', '$a')").mkString(", ")

  println(s"INSERT INTO account (user, account_id) VALUES $values")

  PaymentSystemIds.values.foreach { psId =>
    Mode match {
      case DeleteModes.All =>
        println(
          s"DELETE FROM ${psId}_transaction WHERE account_id IN " +
            s"(SELECT id FROM account WHERE account_id LIKE '$AccountPrefix%');"
        )
        if (psId != PaymentSystemIds.Overdraft) {
          println(
            s"DELETE FROM ${psId}_payment WHERE account_id IN " +
              s"(SELECT id FROM account WHERE account_id LIKE '$AccountPrefix%');"
          )
          println(
            s"DELETE FROM ${psId}_payment_request WHERE account_id IN " +
              s"(SELECT id FROM account WHERE account_id LIKE '$AccountPrefix%');"
          )
        }
      case DeleteModes.Consumes =>
        println(
          s"DELETE FROM ${psId}_transaction WHERE type > 0 AND account_id IN " +
            s"(SELECT id FROM account WHERE account_id LIKE '$AccountPrefix%');"
        )
    }
  }

  Mode match {
    case DeleteModes.All =>
      println(
        s"DELETE FROM account_transaction WHERE account_id IN " +
          s"(SELECT id FROM account WHERE account_id LIKE '$AccountPrefix%');"
      )
    case DeleteModes.Consumes =>
      println(
        s"DELETE FROM account_transaction WHERE type > 0 AND account_id IN " +
          s"(SELECT id FROM account WHERE account_id LIKE '$AccountPrefix%');"
      )
  }

  println(
    s"DELETE FROM lock_account WHERE account_id IN " +
      s"(SELECT id FROM account WHERE account_id LIKE '$AccountPrefix%');"
  )
  println(s"DELETE FROM account WHERE account_id LIKE '$AccountPrefix%';")
}
