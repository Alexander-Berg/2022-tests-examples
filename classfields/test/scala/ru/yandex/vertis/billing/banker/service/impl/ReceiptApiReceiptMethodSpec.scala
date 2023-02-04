package ru.yandex.vertis.billing.banker.service.impl

import org.joda.time.DateTime
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.config.ReceiptParameters
import ru.yandex.vertis.billing.banker.config.ReceiptSettings.{
  ReceiptParametersProvider,
  StaticReceiptParametersProvider
}
import ru.yandex.vertis.billing.banker.model.AccountTransaction.{Activities, Activity}
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, ReceiptData, Targets}
import ru.yandex.vertis.billing.banker.model.Receipt.fingerPrintOf
import ru.yandex.vertis.billing.banker.model.State.{StateStatus, StateStatuses}
import ru.yandex.vertis.billing.banker.model.gens.{
  accountTransactionGen,
  paymentForRefundGen,
  paymentRequestGen,
  refundRequestGen,
  requestReceiptGen,
  AccountTransactionGen,
  BooleanGen,
  PaymentRequestParams,
  PaymentSystemIdGen,
  Producer,
  RefundRequestParams,
  StateParams
}
import ru.yandex.vertis.billing.banker.model.{
  AccountTransaction,
  AccountTransactions,
  Funds,
  HashAccountTransactionId,
  PaymentRequest,
  PaymentSystemAccountTransactionId,
  Receipt,
  ReceiptSentStatuses,
  ReceiptStatuses,
  RefundPaymentRequest,
  SourceWithReceiptData,
  State
}
import ru.yandex.vertis.billing.banker.service.ReceiptDeliveryService
import ru.yandex.vertis.billing.banker.service.impl.ReceiptApiReceiptMethodSpec.{
  expectedReceipt,
  AccountTransactionWithInvalidIdGen,
  AccountTransactionWithInvalidReceiptIdGen,
  AccountTransactionWithInvalidTargetGen,
  PaymentRequestSourceWithInvalidReceiptDataGen,
  PaymentRequestWithInvalidStateStatusGen,
  PaymentRequestWithInvalidTargetGen,
  PaymentRequestWithoutStateGen,
  RefundPaymentRequestWithInvalidStateStatusGen,
  RefundPaymentRequestWithInvalidTargetGen,
  RefundPaymentRequestWithoutStateGen,
  ValidAccountTransactionGen,
  ValidPaymentRequestGen,
  ValidRefundPaymentRequestGen
}
import ru.yandex.vertis.billing.receipt.BalanceReceipt
import ru.yandex.vertis.billing.receipt.model.{TaxTypes, TaxationTypes}

import scala.concurrent.Future

class ReceiptApiReceiptMethodSpec
  extends AnyWordSpec
  with Matchers
  with MockFactory
  with AsyncSpecBase
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  val balanceApiMock = mock[BalanceReceipt]

  private def mockReceiptMethodCall(content: Array[Byte]): Unit = {
    (balanceApiMock.receipt _).expects(*).returns(Future.successful(content)): Unit
  }

  val receiptDeliveryService = mock[ReceiptDeliveryService]

  val receiptParameters = ReceiptParameters(
    "0",
    TaxationTypes.OSN,
    TaxTypes.NdsNone,
    TaxTypes.NdsNone,
    "test",
    "test"
  )

  val provider = new StaticReceiptParametersProvider(receiptParameters)

  val receiptApi = new ReceiptApiImpl(
    balanceApiMock,
    receiptDeliveryService,
    provider
  )

  private val ContentGen: Gen[Array[Byte]] = {
    Gen.nonEmptyListOf(Gen.alphaLowerChar).map(_.mkString.getBytes)
  }

  private def checkPaymentRequestFail(paymentRequestGen: Gen[PaymentRequest]): Unit = {
    forAll(paymentRequestGen) { paymentRequest =>
      intercept[IllegalArgumentException] {
        receiptApi.receipt(paymentRequest).await
      }
    }
  }

  private def checkTransactionFail(transactionGen: Gen[AccountTransaction]): Unit = {
    forAll(transactionGen) { transaction =>
      intercept[IllegalArgumentException] {
        receiptApi.receipt(transaction).await
      }
    }
  }

  private def checkRefundPaymentRequestFail(refundPaymentRequestGen: Gen[RefundPaymentRequest]): Unit = {
    forAll(refundPaymentRequestGen) { refundPaymentRequest =>
      intercept[IllegalArgumentException] {
        val payment = paymentForRefundGen(refundPaymentRequest).next
        receiptApi.receipt(refundPaymentRequest, payment).await
      }
    }
  }

  "ReceiptApi" should {
    "process receipt" when {
      "valid payment request passed" in {
        forAll(ValidPaymentRequestGen) { paymentRequest =>
          val content = ContentGen.next
          mockReceiptMethodCall(content)
          val actualReceipt = receiptApi.receipt(paymentRequest).futureValue
          actualReceipt shouldBe expectedReceipt(paymentRequest, content)
        }
      }
      "valid transaction passed" in {
        forAll(ValidAccountTransactionGen) { transaction =>
          val content = ContentGen.next
          mockReceiptMethodCall(content)
          val actualReceipt = receiptApi.receipt(transaction).futureValue
          actualReceipt shouldBe expectedReceipt(transaction, content)
        }
      }
      "valid refund payment request passed" in {
        forAll(ValidRefundPaymentRequestGen) { refundPaymentRequest =>
          val content = ContentGen.next
          mockReceiptMethodCall(content)
          val payment = paymentForRefundGen(refundPaymentRequest).next
          val actualReceipt = receiptApi.receipt(refundPaymentRequest, payment).futureValue
          actualReceipt shouldBe expectedReceipt(refundPaymentRequest, content)
        }
      }
    }
    "fail receipt call" when {
      "payment request without state" in {
        checkPaymentRequestFail(PaymentRequestWithoutStateGen)
      }
      "payment request with invalid receipt data" in {
        checkPaymentRequestFail(PaymentRequestSourceWithInvalidReceiptDataGen)
      }
      "payment request with invalid state status" in {
        checkPaymentRequestFail(PaymentRequestWithInvalidStateStatusGen)
      }
      "payment request with invalid target" in {
        checkPaymentRequestFail(PaymentRequestWithInvalidTargetGen)
      }
      "transaction with invalid id" in {
        checkTransactionFail(AccountTransactionWithInvalidIdGen)
      }
      "transaction with invalid receipt data" in {
        checkTransactionFail(AccountTransactionWithInvalidReceiptIdGen)
      }
      "transaction with invalid target" in {
        checkTransactionFail(AccountTransactionWithInvalidTargetGen)
      }
      "refund payment request without state" in {
        checkRefundPaymentRequestFail(RefundPaymentRequestWithoutStateGen)
      }
      "refund payment request with invalid state status" in {
        checkRefundPaymentRequestFail(RefundPaymentRequestWithInvalidStateStatusGen)
      }
      "refund payment request with invalid target" in {
        checkRefundPaymentRequestFail(RefundPaymentRequestWithInvalidTargetGen)
      }
    }
  }

}

object ReceiptApiReceiptMethodSpec {

  private def invalidReceiptDataGen(amount: Funds): Gen[Option[ReceiptData]] = {
    for {
      withoutReceipt <- BooleanGen
      invalidReceipt <-
        if (withoutReceipt) {
          requestReceiptGen(amount).map { receipt =>
            Some(receipt.copy(email = None, phone = None))
          }
        } else {
          Gen.const(None)
        }
    } yield invalidReceipt
  }

  private val DefaultEmail = "default@mail.ru"
  private val DefaultPhone = "8800553535"

  private def validReceiptDataGen(amount: Funds): Gen[ReceiptData] = {
    for {
      receipt <- requestReceiptGen(amount)
      receiptDataType <- Gen.chooseNum(0, 2)
      validReceipt = receiptDataType match {
        case 0 =>
          receipt.copy(email = Some(DefaultEmail), phone = Some(DefaultPhone))
        case 1 =>
          receipt.copy(email = Some(DefaultEmail), phone = None)
        case _ =>
          receipt.copy(email = None, phone = Some(DefaultPhone))
      }
    } yield validReceipt
  }

  private def ValidTargetGen: Gen[PaymentRequest.Targets.Value] = {
    Gen.oneOf(Targets.Purchase, Targets.Wallet, Targets.Binding)
  }

  private def paymentRequestWithInvalidReceiptData(paymentRequestGen: Gen[PaymentRequest]): Gen[PaymentRequest] = {
    for {
      paymentRequest <- paymentRequestGen
      invalidReceipt <- invalidReceiptDataGen(paymentRequest.source.amount)
      invalidSource = paymentRequest.source.copy(optReceiptData = invalidReceipt)
    } yield paymentRequest.copy(source = invalidSource)
  }

  private def paymentRequestWithValidReceiptData(paymentRequestGen: Gen[PaymentRequest]): Gen[PaymentRequest] = {
    for {
      paymentRequest <- paymentRequestGen
      validReceipt <- validReceiptDataGen(paymentRequest.source.amount)
      validSource = paymentRequest.source.copy(optReceiptData = Some(validReceipt))
    } yield paymentRequest.copy(source = validSource)
  }

  private def paymentRequestWithValidTarget(paymentRequestGen: Gen[PaymentRequest]): Gen[PaymentRequest] = {
    for {
      paymentRequest <- paymentRequestGen
      validTarget <- ValidTargetGen
      validSource = paymentRequest.source.copy(
        context = Context(validTarget)
      )
    } yield paymentRequest.copy(source = validSource)
  }

  private def paymentRequestWithInvalidTarget(paymentRequestGen: Gen[PaymentRequest]): Gen[PaymentRequest] = {
    for {
      paymentRequest <- paymentRequestGen
      validSource = paymentRequest.source.copy(
        context = Context(Targets.ExternalTransfer)
      )
    } yield paymentRequest.copy(source = validSource)
  }

  private def paymentRequestWithStateStatus(stateStatuses: Set[StateStatuses.Value]): Gen[PaymentRequest] = {
    val stateParams = StateParams(
      stateStatus = stateStatuses
    )
    paymentRequestGen(
      PaymentRequestParams(
        state = Some(stateParams)
      )
    )
  }

  private val PaymentRequestWithoutStateGen: Gen[PaymentRequest] = {
    paymentRequestGen()
  }

  private val PaymentRequestSourceWithInvalidReceiptDataGen: Gen[PaymentRequest] = {
    paymentRequestWithInvalidReceiptData {
      paymentRequestWithStateStatus(Set.empty)
    }
  }

  private val PaymentRequestWithInvalidStateStatusGen: Gen[PaymentRequest] = {
    paymentRequestWithValidReceiptData {
      paymentRequestWithStateStatus(
        Set(StateStatuses.Cancelled, StateStatuses.PartlyRefunded)
      )
    }
  }

  private val PaymentRequestWithInvalidTargetGen: Gen[PaymentRequest] = {
    paymentRequestWithInvalidTarget {
      paymentRequestWithValidReceiptData {
        paymentRequestWithStateStatus(
          Set(StateStatuses.Valid, StateStatuses.Refunded)
        )
      }
    }
  }

  private val ValidPaymentRequestGen: Gen[PaymentRequest] = {
    paymentRequestWithValidTarget {
      paymentRequestWithValidReceiptData {
        paymentRequestWithStateStatus(
          Set(StateStatuses.Valid, StateStatuses.Refunded)
        )
      }
    }
  }

  private def withHashAccountTransactionId(transactionGen: Gen[AccountTransaction]): Gen[AccountTransaction] = {
    transactionGen.map { tr =>
      tr.copy(id = HashAccountTransactionId(tr.id.value, tr.id.`type`))
    }
  }

  private def withInvalidTransactionId(transactionGen: Gen[AccountTransaction]): Gen[AccountTransaction] = {
    for {
      tr <- transactionGen
      invalidId <- tr.id match {
        case id: HashAccountTransactionId if id.`type` == AccountTransactions.Withdraw =>
          PaymentSystemIdGen.map { psId =>
            PaymentSystemAccountTransactionId(psId, id.id, id.`type`)
          }
        case id =>
          Gen.const(id)
      }
    } yield tr.copy(id = invalidId)
  }

  private def transactionWithInvalidReceiptData(transactionGen: Gen[AccountTransaction]): Gen[AccountTransaction] = {
    for {
      transaction <- transactionGen
      invalidReceipt <- invalidReceiptDataGen(1000L)
    } yield transaction.copy(receiptData = invalidReceipt)
  }

  private def transactionWithValidReceiptData(transactionGen: Gen[AccountTransaction]): Gen[AccountTransaction] = {
    for {
      transaction <- transactionGen
      invalidReceipt <- validReceiptDataGen(1000L)
    } yield transaction.copy(receiptData = Some(invalidReceipt))
  }

  private def transactionWithValidTarget(transactionGen: Gen[AccountTransaction]): Gen[AccountTransaction] = {
    for {
      transaction <- transactionGen
      validTarget <- ValidTargetGen
    } yield transaction.copy(target = Some(validTarget))
  }

  private def transactionWithInvalidTarget(transactionGen: Gen[AccountTransaction]): Gen[AccountTransaction] = {
    for {
      transaction <- transactionGen
    } yield transaction.copy(target = Some(Targets.ExternalTransfer))
  }

  private val AccountTransactionWithInvalidIdGen: Gen[AccountTransaction] = {
    withInvalidTransactionId {
      AccountTransactionGen
    }
  }

  private val AccountTransactionWithInvalidReceiptIdGen: Gen[AccountTransaction] = {
    transactionWithInvalidReceiptData {
      withHashAccountTransactionId {
        AccountTransactionGen
      }
    }
  }

  private val AccountTransactionWithInvalidTargetGen: Gen[AccountTransaction] = {
    transactionWithInvalidTarget {
      transactionWithValidReceiptData {
        withHashAccountTransactionId {
          accountTransactionGen(AccountTransactions.Withdraw)
        }
      }
    }
  }

  private val ValidAccountTransactionGen: Gen[AccountTransaction] = {
    transactionWithValidTarget {
      transactionWithValidReceiptData {
        withHashAccountTransactionId {
          accountTransactionGen(AccountTransactions.Withdraw)
        }
      }
    }
  }

  private def refundPaymentRequestWithValidReceiptData(
      refundPaymentRequestGen: Gen[RefundPaymentRequest]): Gen[RefundPaymentRequest] = {
    for {
      refundPaymentRequest <- refundPaymentRequestGen
      validReceipt <- validReceiptDataGen(refundPaymentRequest.source.amount)
      validSource = refundPaymentRequest.source.copy(optReceiptData = Some(validReceipt))
    } yield refundPaymentRequest.copy(source = validSource)
  }

  private def refundPaymentRequestWithValidTarget(
      refundPaymentRequestGen: Gen[RefundPaymentRequest]): Gen[RefundPaymentRequest] = {
    for {
      refundPaymentRequest <- refundPaymentRequestGen
      validTarget <- ValidTargetGen
      validSource = refundPaymentRequest.source.copy(
        context = Context(validTarget)
      )
    } yield refundPaymentRequest.copy(source = validSource)
  }

  private def refundPaymentRequestWithInvalidTarget(
      refundPaymentRequestGen: Gen[RefundPaymentRequest]): Gen[RefundPaymentRequest] = {
    for {
      refundPaymentRequest <- refundPaymentRequestGen
      validSource = refundPaymentRequest.source.copy(
        context = Context(Targets.ExternalTransfer)
      )
    } yield refundPaymentRequest.copy(source = validSource)
  }

  private def refundPaymentRequestWithStateStatus(
      stateStatuses: Set[StateStatuses.Value]): Gen[RefundPaymentRequest] = {
    val stateParams = StateParams(
      stateStatus = stateStatuses
    )
    refundRequestGen(
      RefundRequestParams(
        state = Some(stateParams)
      )
    )
  }

  private val RefundPaymentRequestWithoutStateGen: Gen[RefundPaymentRequest] = {
    refundRequestGen()
  }

  private val RefundPaymentRequestWithInvalidStateStatusGen: Gen[RefundPaymentRequest] = {
    refundPaymentRequestWithValidReceiptData {
      refundPaymentRequestWithStateStatus(
        Set(StateStatuses.Cancelled, StateStatuses.PartlyRefunded, StateStatuses.Refunded)
      )
    }
  }

  private val RefundPaymentRequestWithInvalidTargetGen: Gen[RefundPaymentRequest] = {
    refundPaymentRequestWithInvalidTarget {
      refundPaymentRequestWithValidReceiptData {
        refundPaymentRequestWithStateStatus(
          Set(StateStatuses.Valid)
        )
      }
    }
  }

  private val ValidRefundPaymentRequestGen: Gen[RefundPaymentRequest] = {
    refundPaymentRequestWithValidTarget {
      refundPaymentRequestWithValidReceiptData {
        refundPaymentRequestWithStateStatus(
          Set(StateStatuses.Valid)
        )
      }
    }
  }

  private def statusOf(s: StateStatus): Option[ReceiptStatuses.Value] = s match {
    case StateStatuses.Valid =>
      Some(ReceiptStatuses.Active)
    case StateStatuses.Refunded | StateStatuses.PartlyRefunded =>
      Some(ReceiptStatuses.Refund)
    case _ =>
      None
  }

  private def statusOf(activity: Activity): ReceiptStatuses.Value = activity match {
    case Activities.Active =>
      ReceiptStatuses.Active
    case Activities.Inactive =>
      ReceiptStatuses.Refund
  }

  private def chooseDestination(source: SourceWithReceiptData): (Option[String], Option[String]) = {
    val receipt = source.optReceiptData.get
    chooseDestination(receipt)
  }

  private def chooseDestination(source: ReceiptData): (Option[String], Option[String]) = {
    val sourceEmail = source.email
    val sourcePhone = source.phone
    if (sourceEmail.isDefined) (sourceEmail, None) else (None, sourcePhone)
  }

  private def expectedReceipt(paymentRequest: PaymentRequest, content: Array[Byte]): Receipt = {
    val paymentId = paymentRequest.id
    val psId = paymentRequest.method.ps
    val accountId = paymentRequest.source.account
    val status = paymentRequest.state.map(_.stateStatus).flatMap(statusOf).get
    val (email, phone) = chooseDestination(paymentRequest.source)
    Receipt(
      Receipt.fingerPrintOf(paymentId, psId, accountId, status),
      paymentId,
      Some(psId),
      accountId,
      content,
      None,
      None,
      email,
      phone,
      status,
      None,
      ReceiptSentStatuses.Ready,
      None
    )
  }

  private def expectedReceipt(transaction: AccountTransaction, content: Array[Byte]): Receipt = {
    val accountId = transaction.account
    val status = statusOf(transaction.activity)
    val (email, phone) = chooseDestination(transaction.receiptData.get)
    Receipt(
      fingerPrintOf(transaction.id, accountId, status),
      transaction.id.value,
      None,
      accountId,
      content,
      None,
      None,
      email,
      phone,
      status,
      None,
      ReceiptSentStatuses.Ready,
      None
    )
  }

  private def expectedReceipt(refundPaymentRequest: RefundPaymentRequest, content: Array[Byte]): Receipt = {
    val paymentId = refundPaymentRequest.id
    val psId = refundPaymentRequest.method.ps
    val accountId = refundPaymentRequest.account
    val status = refundPaymentRequest.state.map(_.stateStatus).flatMap(statusOf).get
    val (email, phone) = chooseDestination(refundPaymentRequest.source)
    Receipt(
      Receipt.fingerPrintOf(paymentId, psId, accountId, status),
      paymentId,
      Some(psId),
      accountId,
      content,
      None,
      None,
      email,
      phone,
      status,
      None,
      ReceiptSentStatuses.Ready,
      None
    )
  }

}
