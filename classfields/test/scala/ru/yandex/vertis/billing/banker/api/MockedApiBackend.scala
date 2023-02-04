package ru.yandex.vertis.billing.banker.api

import ru.yandex.vertis.billing.banker.Domains
import ru.yandex.vertis.billing.banker.backend_api.{ApiBackend, ApiBackendRegistry}
import ru.yandex.vertis.billing.banker.dao.{RecurrentPaymentDao, YandexKassaV3AntiFraudDao}
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds
import ru.yandex.vertis.billing.banker.payment.impl.PaymentServiceImpl
import ru.yandex.vertis.billing.banker.payment.recurrent.RecurrentProcessor
import ru.yandex.vertis.billing.banker.service._
import ru.yandex.vertis.billing.banker.service.impl.{AntiFraudServiceImpl, PaymentSystemTransactionService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

/**
  * Mocked Api Backend for testing api routing, marshalling and etc
  *
  * @author ruslansd
  */
trait MockedApiBackend extends MockitoSupport {

  private val accounts: AccountService = mock[AccountService]

  protected val allSetups = PaymentSystemIds.values.toSeq.map { id =>
    val support = mock[PaymentSystemSupport]
    val transactions = mock[PaymentSystemTransactionService]
    val recurrentProcessor = mock[RecurrentProcessor]
    when(support.psId).thenReturn(id)
    PaymentSetup(support, transactions, recurrentProcessor)
  }

  private val paymentSetupsRegistry: PaymentSetupsRegistry = {
    val m = mock[PaymentSetupsRegistry]
    when(m.all()).thenReturn(allSetups)
    stub(m.get _) { case id =>
      Future.successful(allSetups.filter(_.support.psId == id).head)
    }
    m
  }

  private val accountTransactionService: AccountTransactionService = mock[AccountTransactionService]
  private val applePayService: ApplePayService = mock[ApplePayService]
  private val receiptService: ReceiptService = mock[ReceiptService]

  private val recurrentPaymentDao: RecurrentPaymentDao = mock[RecurrentPaymentDao]

  private val paymentService =
    new PaymentServiceImpl(
      accountTransactionService,
      paymentSetupsRegistry,
      recurrentPaymentDao
    )

  private val antiFraudService =
    new AntiFraudServiceImpl(
      new YandexKassaV3AntiFraudDao.Fake
    )

  val backend =
    ApiBackend(
      accounts,
      paymentSetupsRegistry,
      accountTransactionService,
      paymentService,
      Some(applePayService),
      receiptService,
      antiFraudService
    )

  val registry = {
    val r = new ApiBackendRegistry
    r.register(Domains.AutoRu, backend)
    r
  }

}
