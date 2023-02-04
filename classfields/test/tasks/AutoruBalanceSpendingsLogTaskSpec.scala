package ru.yandex.vertis.billing.banker.tasks

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.ISODateTimeFormat
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.banker.model.BalanceSpendingsModel
import ru.yandex.vertis.banker.model.CommonModel.OpaquePayload
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.{GlobalJdbcAccountTransactionDao, JdbcAccountDao, JdbcKeyValueDao}
import ru.yandex.vertis.billing.banker.dao.util.{
  CleanableJdbcAccountDao,
  CleanableJdbcAccountTransactionDao,
  CleanableJdbcKeyValueDao
}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{ReceiptData, ReceiptGood, Target, Targets}
import ru.yandex.vertis.billing.banker.model.{
  Account,
  AccountTransactionRequest,
  AccountTransactions,
  Payload,
  PaymentSystemAccountTransactionId,
  PaymentSystemIds
}
import ru.yandex.vertis.billing.banker.service.impl.{EpochServiceImpl, GlobalAccountTransactionService}
import ru.yandex.vertis.billing.banker.service.log.BatchAsyncProtoLoggerMock
import ru.yandex.vertis.billing.banker.util.{AutomatedContext, Logging, RequestContext}
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDao.{ForId, Patch}
import ru.yandex.vertis.billing.banker.model.AccountTransaction.{Activities, Statuses}
import ru.yandex.vertis.billing.banker.payment.TrustEnvironmentProvider

class AutoruBalanceSpendingsLogTaskSpec
  extends EffectAsyncTaskSpecBase("AutoruBalanceSpendingsLogTaskSpec")
  with JdbcSpecTemplate
  with Logging
  with TrustEnvironmentProvider {

  implicit protected val rc: RequestContext = AutomatedContext("AutoruBalanceSpendingsLogTaskSpec")

  private val AccountId = "test_account"

  private val User = "test_user"

  implicit val domain = ru.yandex.vertis.billing.banker.Domains.AutoRu

  private val dateTime = new DateTime(DateTimeZone.forID("Europe/Moscow")).plusMinutes(2)
  private val epoch = dateTime.getMillis
  private val stringDate = dateTime.toString(ISODateTimeFormat.date())

  override def beforeEach(): Unit = {
    super.beforeEach()
    prepareUser()
    ()
  }

  "AutoruBalanceSpendingsLogTask" should {
    "write direct card payment" in {
      val (task, brokerClientMock) = initTaskAndBrokerMock()

      val transactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "payment_id", AccountTransactions.Incoming)
      paymentUsingCard(transactionId, Targets.Purchase)

      task.execute().futureValue
      brokerClientMock.sentMessages should have size 1
      val brokerMessage = brokerClientMock.sentMessages.head

      purchaseAssertion(brokerMessage)
    }
    "write direct payment refund" in {
      val (task, brokerClientMock) = initTaskAndBrokerMock()

      val transactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "payment_id", AccountTransactions.Incoming)
      paymentUsingCard(transactionId, Targets.Purchase)
      refundForPurchaseUsingCard(refundForTransactionId = transactionId)
      task.execute().futureValue

      brokerClientMock.sentMessages should have size 2
      val purchase = brokerClientMock.sentMessages.find(_.getAmount.toDouble > 0)
      purchase should not be empty
      purchaseAssertion(purchase.get)
      val refund = brokerClientMock.sentMessages.find(_.getAmount.toDouble < 0)
      refund should not be empty
      refundAssertion(refund.get)
    }
    "don't write wallet income" in {
      val (task, brokerClientMock) = initTaskAndBrokerMock()

      val transactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "payment_id", AccountTransactions.Incoming)
      walletAddFunds(transactionId)
      task.execute().futureValue

      brokerClientMock.sentMessages should have size 0
    }
    "don't write wallet income refund" in {
      val (task, brokerClientMock) = initTaskAndBrokerMock()

      val transactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "payment_id", AccountTransactions.Incoming)
      walletAddFunds(transactionId)
      refundForWalletAddFunds(refundForTransactionId = transactionId)

      task.execute().futureValue
      brokerClientMock.sentMessages shouldBe empty
    }
    "write purchase from wallet" in {
      val (task, brokerClientMock) = initTaskAndBrokerMock()

      val transactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "payment_id", AccountTransactions.Incoming)
      val withdrawId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "withdraw_id", AccountTransactions.Withdraw)
      walletAddFunds(transactionId)
      purchaseUsingWallet(withdrawId)
      task.execute().futureValue

      brokerClientMock.sentMessages should have size 1
      purchaseUsingWalletAssertion(brokerClientMock.sentMessages.head)
    }
    "write refund of purchase from wallet" in {
      val (task, brokerClientMock) = initTaskAndBrokerMock()
      val transactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "payment_id", AccountTransactions.Incoming)
      val withdrawId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "withdraw_id", AccountTransactions.Withdraw)
      walletAddFunds(transactionId)
      purchaseUsingWallet(withdrawId)
      refundForPurchaseUsingWallet(refundForWithdrawId = withdrawId)
      task.execute().futureValue

      brokerClientMock.sentMessages should have size 2
      val purchase = brokerClientMock.sentMessages.find(_.getAmount.toDouble > 0)
      purchase should not be empty
      purchaseUsingWalletAssertion(purchase.get)
      val refund = brokerClientMock.sentMessages.find(_.getAmount.toDouble < 0)
      refund should not be empty
      refundUsingWalletAssertion(refund.get)
    }
    "check that epoch moved" in {
      val (task, _) = initTaskAndBrokerMock()
      val transactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "payment_id", AccountTransactions.Incoming)
      paymentUsingCard(transactionId, Targets.Purchase)
      task.execute().futureValue

      val epoch = keyValueDao.get("epoch_AutoruBalanceSpendings").futureValue
      epoch should not be empty
    }
    "don't write transactions for security deposit/binding/external transfer" in {
      val (task, brokerClientMock) = initTaskAndBrokerMock()
      val securityDepositTransactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "security_deposit_id", AccountTransactions.Incoming)
      paymentUsingCard(securityDepositTransactionId, Targets.SecurityDeposit)
      val bindingTransactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "binding_id", AccountTransactions.Incoming)
      paymentUsingCard(bindingTransactionId, Targets.Binding)
      val externalPaymentTransactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "binding_id", AccountTransactions.Incoming)
      paymentUsingCard(externalPaymentTransactionId, Targets.ExternalTransfer)
      task.execute().futureValue

      brokerClientMock.sentMessages shouldBe empty
    }
    "check sum after deduplication" in {
      val (task, brokerClientMock) = initTaskAndBrokerMock()
      val transactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "payment_id", AccountTransactions.Incoming)
      walletAddFunds(transactionId)
      val withdrawId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "withdraw_id", AccountTransactions.Withdraw)
      purchaseUsingWallet(withdrawId)
      refundForPurchaseUsingWallet(refundForWithdrawId = withdrawId)
      val directPaymentTransactionId =
        PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "direcit_payment", AccountTransactions.Incoming)
      paymentUsingCard(directPaymentTransactionId, Targets.Purchase)
      refundForPurchaseUsingCard(refundForTransactionId = directPaymentTransactionId)
      task.execute().futureValue

      val sum = brokerClientMock.sentMessages
        .map { spending =>
          spending.getDeduplicationId -> spending
        }
        .toMap
        .values
        .map(_.getAmount.toDouble)
        .sum
      sum shouldBe 0
    }
  }

  private def prepareUser(): Account = {
    accountDao.upsert(Account(AccountId, User)).futureValue
  }

  private def walletAddFunds(transactionId: PaymentSystemAccountTransactionId) = {
    accountTransactionsDao
      .execute(
        AccountTransactionRequest.IncomingRequest(
          transactionId,
          AccountId,
          23400,
          Some(Targets.Wallet),
          Payload.Empty
        )
      )
      .futureValue
    accountTransactionsDao
      .update(
        AccountId,
        transactionId,
        Patch.ProcessStatus(Statuses.Processed, epoch)
      )
      .futureValue
  }

  private def refundForWalletAddFunds(refundForTransactionId: PaymentSystemAccountTransactionId) = {
    val refundId = PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "refund_id", AccountTransactions.Refund)
    val refundRequest = AccountTransactionRequest.RefundRequest(
      refundId,
      AccountId,
      12300,
      target = Some(Targets.Wallet),
      receiptData = Some(
        ReceiptData(
          goods = Seq(ReceiptGood(name = "test_name", quantity = 1, price = 30000, supplierInfo = None)),
          email = Some("test@yandex.ru"),
          phone = None,
          taxType = None
        )
      ),
      refundFor = Some(refundForTransactionId),
      payload = Payload
        .RefundPayload(user = "test_user", comment = "comment", value = None, OpaquePayload.RefundPayload.Reason.OTHER),
      timestamp = DateTime.now().plusHours(1)
    )
    accountTransactionsDao.execute(refundRequest).futureValue
    accountTransactionsDao
      .update(
        AccountId,
        refundId,
        Patch.ProcessStatus(Statuses.Processed, epoch)
      )
      .futureValue
  }

  private def purchaseUsingWallet(withdrawId: PaymentSystemAccountTransactionId) = {
    val withdraw = AccountTransactionRequest.WithdrawRequest(
      withdrawId,
      AccountId,
      12300,
      Payload.Empty,
      target = Some(Targets.Purchase),
      receiptData = None
    )
    accountTransactionsDao.execute(withdraw).futureValue
    accountTransactionsDao
      .update(
        AccountId,
        withdrawId,
        Patch.ProcessStatus(Statuses.Processed, epoch)
      )
      .futureValue
  }

  private def refundForPurchaseUsingWallet(refundForWithdrawId: PaymentSystemAccountTransactionId) = {
    val refundRequest = AccountTransactionRequest.WithdrawRequest(
      refundForWithdrawId,
      AccountId,
      12300,
      Payload.Empty,
      target = Some(Targets.Purchase),
      receiptData = None,
      activity = Activities.Inactive
    )
    accountTransactionsDao.execute(refundRequest).futureValue
    accountTransactionsDao
      .update(
        AccountId,
        refundForWithdrawId,
        Patch.ProcessStatus(Statuses.Processed, epoch)
      )
      .futureValue
  }

  private def paymentUsingCard(transactionId: PaymentSystemAccountTransactionId, target: Target) = {

    accountTransactionsDao
      .execute(
        AccountTransactionRequest.IncomingRequest(
          transactionId,
          AccountId,
          12300,
          Some(target),
          Payload.Empty
        )
      )
      .futureValue
    accountTransactionsDao
      .update(
        AccountId,
        transactionId,
        Patch.ProcessStatus(Statuses.Processed, epoch)
      )
      .futureValue
  }

  private def refundForPurchaseUsingCard(refundForTransactionId: PaymentSystemAccountTransactionId) = {
    val refundId = PaymentSystemAccountTransactionId(PaymentSystemIds.Trust, "refund_id", AccountTransactions.Refund)
    val refundRequest = AccountTransactionRequest.RefundRequest(
      refundId,
      AccountId,
      12300,
      target = Some(Targets.Purchase),
      receiptData = Some(
        ReceiptData(
          goods = Seq(ReceiptGood(name = "test_name", quantity = 1, price = 30000, supplierInfo = None)),
          email = Some("test@yandex.ru"),
          phone = None,
          taxType = None
        )
      ),
      refundFor = Some(refundForTransactionId),
      payload = Payload
        .RefundPayload(user = "test_user", comment = "comment", value = None, OpaquePayload.RefundPayload.Reason.OTHER),
      timestamp = DateTime.now().plusHours(1)
    )
    accountTransactionsDao.execute(refundRequest).futureValue
    accountTransactionsDao
      .update(
        AccountId,
        refundId,
        Patch.ProcessStatus(Statuses.Processed, epoch)
      )
      .futureValue
  }

  private def purchaseAssertion(spendings: BalanceSpendingsModel.BalanceSpendings) = {
    baseAssertion(spendings)
    spendings.getDeduplicationId shouldBe "8#payment_id"
    spendings.getServiceOrderId shouldBe "8#payment_id"
    spendings.getAmount shouldBe "123.00"
  }

  private def refundAssertion(spendings: BalanceSpendingsModel.BalanceSpendings) = {
    baseAssertion(spendings)
    spendings.getDeduplicationId shouldBe "8#refund_id-refund"
    spendings.getServiceOrderId shouldBe "8#refund_id"
    spendings.getAmount shouldBe "-123.00"
  }

  private def purchaseUsingWalletAssertion(spendings: BalanceSpendingsModel.BalanceSpendings) = {
    baseAssertion(spendings)
    spendings.getDeduplicationId shouldBe "8#withdraw_id-active"
    spendings.getServiceOrderId shouldBe "8#withdraw_id"
    spendings.getAmount shouldBe "123.00"
  }

  private def refundUsingWalletAssertion(spendings: BalanceSpendingsModel.BalanceSpendings) = {
    baseAssertion(spendings)
    spendings.getDeduplicationId shouldBe "8#withdraw_id-inactive"
    spendings.getServiceOrderId shouldBe "8#withdraw_id"
    spendings.getAmount shouldBe "-123.00"
  }

  private def baseAssertion(spendings: BalanceSpendingsModel.BalanceSpendings) = {
    // Дополнительное логирование для https://st.yandex-team.ru/VSBILLING-5599, уберем после отладки бага
    if (spendings.getDt != stringDate) {
      val epoch = keyValueDao.get("epoch_AutoruBalanceSpendings").futureValue
      log.info(s"Current epoch for AutoruBalanceSpendings: $epoch")
    }

    spendings.getClientId shouldBe 1356724192L
    spendings.getServiceId shouldBe 1178L
    spendings.getCurrency shouldBe "RUB"
    spendings.getType shouldBe "main"
    spendings.getDt shouldBe stringDate
  }

  private def initTaskAndBrokerMock(
    ): (AutoruBalanceSpendingsLogTask, BatchAsyncProtoLoggerMock[BalanceSpendingsModel.BalanceSpendings]) = {
    val brokerClientMock = BatchAsyncProtoLoggerMock[BalanceSpendingsModel.BalanceSpendings]()
    val formatter = new BalanceSpendingsFormat(balanceClientId = 1356724192)
    val task = new AutoruBalanceSpendingsLogTask(formatter, accountTransactionService, brokerClientMock, epochService)
    (task, brokerClientMock)
  }

}
