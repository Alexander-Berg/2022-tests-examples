package ru.yandex.vertis.billing.banker.payment

import org.scalatest.Suite
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.actor.{PaymentActor, PaymentSystemTransactionActor}
import ru.yandex.vertis.billing.banker.config.TrustSettings
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{
  GlobalJdbcAccountTransactionDao,
  JdbcAccountDao,
  JdbcDowntimeMethodDao,
  JdbcKeyValueDao,
  JdbcPaymentSystemDao,
  JdbcSagaLogDao,
  JdbcTrustExternalPurchaseDao,
  JdbcTrustPlusBonusDao,
  JdbcTrustRecurrentDao,
  PaymentSystemJdbcAccountTransactionDao
}
import ru.yandex.vertis.billing.banker.dao.util._
import ru.yandex.vertis.billing.banker.model.{Account, AccountTransaction, PaymentSystemIds, State}
import ru.yandex.vertis.billing.banker.payment.TrustEnvironmentProvider._
import ru.yandex.vertis.billing.banker.payment.impl.TrustPaymentSupport.{CardPaymentMethod, YandexAccountPaymentMethod}
import ru.yandex.vertis.billing.banker.payment.impl.{
  PlusBonusService,
  TrustPaymentHelper,
  TrustPaymentHelperImpl,
  TrustPaymentSupport
}
import ru.yandex.vertis.billing.banker.payment.payload.TrustPayloadExtractor
import ru.yandex.vertis.billing.banker.payment.util.TrustMockProvider
import ru.yandex.vertis.billing.banker.service.impl._
import ru.yandex.vertis.billing.banker.service.refund.processor.TrustRefundProcessor
import ru.yandex.vertis.billing.banker.service.{
  EffectAccountTransactionService,
  EffectPaymentSystemService,
  TransparentValidator
}
import ru.yandex.vertis.billing.banker.util.RequestContext

import scala.concurrent.Future
import scala.concurrent.duration._

trait TrustEnvironmentProvider extends JdbcSpecTemplate with TrustMockProvider { this: Suite =>

  implicit protected def rc: RequestContext

  protected val keyValueDao = new JdbcKeyValueDao(database) with CleanableJdbcKeyValueDao

  protected val epochService = new EpochServiceImpl(keyValueDao)

  protected val paymentSystemDao =
    new JdbcPaymentSystemDao(database, PaymentSystemIds.Trust) with CleanableJdbcPaymentSystemDao

  protected val trustPlusBonusDao = new JdbcTrustPlusBonusDao(database) with CleanableJdbcTrustPlusBonusDao

  protected val accountDao = new JdbcAccountDao(database) with CleanableJdbcAccountDao

  protected val accountService = new AccountServiceImpl(accountDao)

  protected val accountTransactionsDao =
    new GlobalJdbcAccountTransactionDao(database) with CleanableJdbcAccountTransactionDao

  protected val plusBonusService = new PlusBonusService(trustPlusBonusDao, trustMock)

  protected val sagaLogDao = new JdbcSagaLogDao(database) with CleanableJdbcSagaLogDao

  protected val sagaCoordinator = new SagaCoordinator(sagaLogDao)

  protected val accountRequestWithTopupSaga =
    new AccountRequestWithTopupSaga(sagaCoordinator, accountTransactionsDao, plusBonusService)

  protected val accountTransactionService =
    new GlobalAccountTransactionService(accountTransactionsDao, Some(accountRequestWithTopupSaga))

  protected val psTransactionsDao =
    new PaymentSystemJdbcAccountTransactionDao(database, PaymentSystemIds.Trust) with CleanableJdbcAccountTransactionDao

  protected val psTransactionsService =
    new PaymentSystemTransactionService(psTransactionsDao, TransparentValidator) with EffectAccountTransactionService {

      override def effect(tr: AccountTransaction): Future[Unit] =
        PaymentSystemTransactionActor
          .asRequestOpt(tr)
          .map(accountTransactionService.execute)
          .getOrElse(Future.successful(()))
          .flatMap(_ => this.processed(tr))

    }

  protected val downtimePaymentSystemDao = new JdbcDowntimeMethodDao(database, PaymentSystemIds.Trust)

  protected val downtimePaymentSystemService =
    new DowntimePaymentServiceImpl(PaymentSystemIds.Trust, downtimePaymentSystemDao)

  protected val purchaseDao =
    new JdbcTrustExternalPurchaseDao(database) with CleanableJdbcTrustExternalPurchaseDao

  protected val recurrentDao = new JdbcTrustRecurrentDao(database)

  protected val trustRefundProcessor = new TrustRefundProcessor(trustMock, purchaseDao)

  protected val refundHelperService = new RefundHelperServiceImpl(
    database,
    accountTransactionsDao,
    trustRefundProcessor,
    psTransactionsDao,
    paymentSystemDao
  )

  protected val psService =
    new PaymentSystemServiceImpl(
      database,
      paymentSystemDao,
      refundHelperService,
      new TrustPayloadExtractor(purchaseDao)
    ) with EffectPaymentSystemService {

      override def effect(p: State.EnrichedPayment): Future[Unit] = {
        PaymentActor
          .asRequest(paymentSystemDao.psId, p)
          .map(psTransactionsService.execute)
          .getOrElse(Future.successful(()))
          .flatMap(_ => this.processed(p.payment))
      }

    }

  protected val trustSettings = mock[TrustSettings]
  when(trustSettings.notifyUrl).thenReturn(NotifyUrl)
  when(trustSettings.paymentTimeout).thenReturn(PaymentTimeout)

  protected val paymentHelper: TrustPaymentHelper =
    new TrustPaymentHelperImpl(psService, trustMock, purchaseDao, trustSettings)

  protected val paymentSupport =
    new TrustPaymentSupport(
      psService,
      trustMock,
      purchaseDao,
      recurrentDao,
      paymentHelper
    )

  override def beforeEach(): Unit = {
    downtimePaymentSystemService.enable(CardPaymentMethod).futureValue
    downtimePaymentSystemService.enable(YandexAccountPaymentMethod).futureValue
    purchaseDao.clean().futureValue
    paymentSystemDao.cleanPayments().futureValue
    paymentSystemDao.cleanRequests().futureValue
    accountTransactionsDao.cleanLocks().futureValue
    accountTransactionsDao.clean().futureValue
    psTransactionsDao.cleanLocks().futureValue
    psTransactionsDao.clean().futureValue
    accountDao.clean().futureValue
    keyValueDao.clean().futureValue
  }

  protected def createAccount(accountId: String) = accountService.create(Account(accountId, accountId)).futureValue

}

object TrustEnvironmentProvider {

  val NotifyUrl = "http://localhost:5002/api/1.x/service/autoru/trust"
  val PaymentTimeout = 30.minutes
}
