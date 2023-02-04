package ru.yandex.realty.rent.util

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.gen.RealtyGenerators
import ru.yandex.realty.proto.offer.Platform
import ru.yandex.realty.rent.model.Payment
import ru.yandex.realty.rent.model.enums.{PaymentStatus, PaymentType}
import ru.yandex.realty.rent.proto.api.common.PaymentMethodNameSpace.PaymentMethod
import ru.yandex.realty.rent.proto.api.payment.RentPaymentInitRequest
import ru.yandex.realty.rent.proto.model.payment.TransactionStatusNamespace.TransactionStatus
import ru.yandex.realty.rent.proto.model.payment.{
  ManualTenantTransactionInfo,
  PaymentData,
  TenantPaymentTransactionInfo
}
import ru.yandex.realty.rent.util.PaymentUtil.{
  findTenantTransactionInfo,
  getTransactionInfoPaymentMethod,
  BankTransferTransactionInfo,
  CardTransactionInfo,
  PhoneTransactionInfo,
  SbpTransactionInfo
}
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.vertis.util.time.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class PaymentUtilSpec extends SpecBase with RealtyGenerators {
  private val Date = DateTime.now.withDate(2022, 10, 1)
  private val CardPanMask = "356600******0505"
  private val PhonePanMask = "+7 (900) 000-**-**"
  private val DefaultPayment =
    Payment(
      "",
      "",
      PaymentType.Rent,
      false,
      Date,
      Date,
      Date,
      PaymentStatus.PaidByTenant,
      PaymentData.newBuilder.build,
      createTime = DateTimeUtil.now(),
      updateTime = DateTimeUtil.now()
    )

  private def createTenantTransactionInfo(paymentMethod: RentPaymentInitRequest.PaymentMethod, panMask: String) = {
    val tenantTransactionInfo = TenantPaymentTransactionInfo.newBuilder
      .setPaymentMethod(paymentMethod)
      .setStatus(TransactionStatus.CONFIRMED)
      .setPlatform(Platform.PLATFORM_DESKTOP)
      .setPanMask(panMask)
      .build
    tenantTransactionInfo
  }

  "PaymentUtil.findTenantTransactionInfo" should {
    "return SBP" in {
      val transaction = createTenantTransactionInfo(RentPaymentInitRequest.PaymentMethod.SBP, PhonePanMask)
      val data = PaymentData.newBuilder.addTenantTransactions(transaction).build
      val payment = DefaultPayment.copy(data = data)

      val transactionInfo = findTenantTransactionInfo(payment)
      val paymentMethod = transactionInfo.map(getTransactionInfoPaymentMethod)

      transactionInfo shouldBe Some(
        SbpTransactionInfo(
          transaction.getPaymentDate,
          Some(transaction.getPlatform),
          transaction.getPanMask
        )
      )
      paymentMethod shouldBe Some(PaymentMethod.SBP)
    }

    "return PHONE" in {
      val transaction = createTenantTransactionInfo(RentPaymentInitRequest.PaymentMethod.CARD, PhonePanMask)
      val data = PaymentData.newBuilder.addTenantTransactions(transaction).build
      val payment = DefaultPayment.copy(data = data)

      val transactionInfo = findTenantTransactionInfo(payment)
      val paymentMethod = transactionInfo.map(getTransactionInfoPaymentMethod)

      transactionInfo shouldBe Some(
        PhoneTransactionInfo(
          transaction.getPaymentDate,
          Some(transaction.getPlatform),
          transaction.getPanMask
        )
      )
      paymentMethod shouldBe Some(PaymentMethod.PHONE)
    }

    "return CARD" in {
      val transaction = createTenantTransactionInfo(RentPaymentInitRequest.PaymentMethod.CARD, CardPanMask)
      val data = PaymentData.newBuilder.addTenantTransactions(transaction).build
      val payment = DefaultPayment.copy(data = data)

      val transactionInfo = findTenantTransactionInfo(payment)
      val paymentMethod = transactionInfo.map(getTransactionInfoPaymentMethod)

      transactionInfo shouldBe Some(
        CardTransactionInfo(
          transaction.getPaymentDate,
          Some(transaction.getPlatform),
          transaction.getPanMask
        )
      )
      paymentMethod shouldBe Some(PaymentMethod.CARD)
    }

    "return MANUAL" in {
      val transaction = ManualTenantTransactionInfo.newBuilder.setPaymentDate(DateTimeFormat.write(Date))
      val data = PaymentData.newBuilder.setManualTenantTransaction(transaction).build
      val payment = DefaultPayment.copy(data = data)

      val transactionInfo = findTenantTransactionInfo(payment)
      val paymentMethod = transactionInfo.map(getTransactionInfoPaymentMethod)

      transactionInfo shouldBe Some(BankTransferTransactionInfo(transaction.getPaymentDate))
      paymentMethod shouldBe Some(PaymentMethod.MANUAL)
    }
  }
}
