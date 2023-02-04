package ru.yandex.vertis.billing.banker.dao.util

import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.api.actionBasedSQLInterpolation
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{
  JdbcAccountDao,
  JdbcAccountTransactionDao,
  JdbcKeyValueDao,
  JdbcPaymentRequestMetaDao,
  JdbcPaymentSystemDao,
  JdbcRecurrentPaymentDao,
  JdbcSagaLogDao,
  JdbcTrustExternalPurchaseDao,
  JdbcTrustPlusBonusDao,
  JdbcTrustRecurrentDao,
  JdbcYandexKassaV3PaymentRequestExternalDao,
  JdbcYandexKassaV3RecurrentDao
}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Provides ability to clean all data from the dao
  *
  * @author alex-kovalenko
  */
trait CleanableDao {
  def clean(): Future[Unit]
}

trait CleanableJdbcAccountDao extends JdbcAccountDao with CleanableDao {

  def clean(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM account")
      .map(_ => ())
}

trait CleanableJdbcKeyValueDao extends JdbcKeyValueDao with CleanableDao {

  def clean(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM key_value")
      .map(_ => ())
}

trait CleanableJdbcAccountTransactionDao extends JdbcAccountTransactionDao with CleanableDao {

  def clean(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM #$table")
      .map(_ => ())

  def cleanLocks(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM lock_account")
      .map(_ => ())
}

trait CleanablePaymentSystemDao extends PaymentSystemDao with CleanableDao {

  implicit protected def ec: ExecutionContext

  def clean(): Future[Unit] =
    for {
      _ <- cleanPayments()
      _ <- cleanRequests()
      _ <- cleanMethods()
    } yield ()

  def cleanPayments(): Future[Unit]
  def cleanRequests(): Future[Unit]
  def cleanMethods(): Future[Unit]
}

trait CleanableJdbcPaymentSystemDao extends JdbcPaymentSystemDao with CleanablePaymentSystemDao {

  def cleanPayments(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM #${prefix}_payment")
      .map(_ => ())

  def cleanRequests(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM #${prefix}_payment_request")
      .map(_ => ())

  def cleanMethods(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM #${prefix}_payment_method")
      .map(_ => ())

}

trait CleanableJdbcPaymentRequestMetaDao extends JdbcPaymentRequestMetaDao with CleanableDao {

  def clean(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM payment_request_meta")
      .map(_ => ())
}

trait CleanableJdbcYandexKassaV3RecurrentDao extends JdbcYandexKassaV3RecurrentDao with CleanableDao {

  def clean(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM yandexkassa_v3_recurrent")
      .map(_ => ())
}

trait CleanableJdbcYandexKassaV3PaymentRequestExternalDao
  extends JdbcYandexKassaV3PaymentRequestExternalDao
  with CleanableDao {

  def clean(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM yandexkassa_v3_payment_external_request")
      .map(_ => ())
}

trait CleanableJdbcTrustExternalPurchaseDao extends JdbcTrustExternalPurchaseDao with CleanableDao {

  def clean(): Future[Unit] =
    for {
      _ <- cleanRefunds()
      _ <- cleanPurchases()
    } yield ()

  def cleanRefunds(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM trust_purchase_refund_external")
      .map(_ => ())

  def cleanPurchases(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM trust_purchase_external")
      .map(_ => ())
}

trait CleanableJdbcTrustRecurrentDao extends JdbcTrustRecurrentDao with CleanableDao {

  def clean(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM trust_recurrent")
      .map(_ => ())
}

trait CleanableJdbcTrustPlusBonusDao extends JdbcTrustPlusBonusDao with CleanableDao {

  def clean(): Future[Unit] =
    for {
      _ <- cleanRefunds()
      _ <- cleanTopups()
    } yield ()

  def cleanRefunds(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM trust_plus_bonus_refund")
      .map(_ => ())

  def cleanTopups(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM trust_plus_bonus_topup")
      .map(_ => ())
}

trait CleanableJdbcSagaLogDao extends JdbcSagaLogDao with CleanableDao {

  def clean(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM saga_log")
      .map(_ => ())
}

trait CleanableJdbcRecurrentPaymentDao extends JdbcRecurrentPaymentDao with CleanableDao {

  def clean(): Future[Unit] =
    for {
      _ <- cleanPayments()
      _ <- cleanRequests()
    } yield ()

  def cleanRequests(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM recurrent_payment_request")
      .map(_ => ())

  def cleanPayments(): Future[Unit] =
    database
      .run(sqlu"DELETE FROM recurrent_payment")
      .map(_ => ())
}
