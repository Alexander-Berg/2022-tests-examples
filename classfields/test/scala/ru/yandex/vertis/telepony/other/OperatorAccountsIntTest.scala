package ru.yandex.vertis.telepony.other

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.jdbc.SetParameters._
import ru.yandex.vertis.telepony.dao.jdbc.api._
import ru.yandex.vertis.telepony.model.{Operator, OperatorAccount, OperatorAccounts}
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class OperatorAccountsIntTest extends SpecBase with JdbcSpecTemplate {

  "OperatorAccounts" must {
    "contain the same elements as DB table" in {
      val dbAccounts = readAccountsFromDb().futureValue
      val codeAccounts = OperatorAccounts.all.flatMap(OperatorAccount.unapply)
      codeAccounts should contain theSameElementsAs dbAccounts
    }
  }

  def readAccountsFromDb(): Future[Seq[(Int, Operator, String)]] =
    dualDb.slave.underlyingDb.run {
      sql"select id, operator_id, name from operator_account".as[(Int, Operator, String)]
    }

}
