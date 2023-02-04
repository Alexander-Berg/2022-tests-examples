package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{Suite, Suites}
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.util.{CleanableDao, CleanableJdbcAccountTransactionDao}
import ru.yandex.vertis.billing.banker.dao.{AccountDao, AccountTransactionDao, AccountTransactionDaoBehavior}
import ru.yandex.vertis.billing.banker.model.{Account, AccountId, PaymentSystemId, PaymentSystemIds}
import slick.jdbc.JdbcBackend

/**
  * Runnable specs on [[AccountTransactionDao]]
  *
  * @author alex-kovalenko
  */
class JdbcAccountTransactionDaoSpec extends Suites(JdbcAccountTransactionDaoSpec.Suites: _*)

class JdbcAccountTransactionDaoSuite(
    prefix: String,
    dao: JdbcBackend.Database => AccountTransactionDao with CleanableDao)
  extends AccountTransactionDaoBehavior
  with Matchers
  with AnyWordSpecLike
  with JdbcSpecTemplate {

  override val accounts: AccountDao = new JdbcAccountDao(database)

  override val account: Account = accounts.upsert(Account("JdbcAccountTransactionDaoSpec", "u1")).futureValue

  override val accountId: AccountId = account.id

  override val transactions: AccountTransactionDao with CleanableDao = dao(database)

  s"AccountTransactionDao.$prefix" should {
    behave.like(emptyDao())
    behave.like(updatableDao())
    behave.like(readableDao())
    behave.like(readyForPurchases())
    behave.like(readyForIncomes())
    behave.like(readyForWithdraws())
    behave.like(readyForRefunds())
    behave.like(searchByPayload())
    behave.like(getStatistics())
    behave.like(scanByPaidSince())
  }
}

object JdbcAccountTransactionDaoSpec extends AsyncSpecBase {

  class JdbcPaymentSystemAccountTransactionDaoSuite(psId: PaymentSystemId)
    extends JdbcAccountTransactionDaoSuite(psId.toString, paymentSystemATD(psId))

  class JdbcGlobalAccountTransactionDaoSuite extends JdbcAccountTransactionDaoSuite("account", globalSystemATD)

  private def paymentSystemATD(psId: PaymentSystemId) =
    (database: JdbcBackend.Database) =>
      new PaymentSystemJdbcAccountTransactionDao(database, psId) with CleanableJdbcAccountTransactionDao

  private def globalSystemATD =
    (database: JdbcBackend.Database) =>
      new GlobalJdbcAccountTransactionDao(database) with CleanableJdbcAccountTransactionDao

  private lazy val Suites: Seq[Suite] = PaymentSystemIds.values.toSeq.map(id =>
    new JdbcPaymentSystemAccountTransactionDaoSuite(id)
  ) :+ new JdbcGlobalAccountTransactionDaoSuite

}
