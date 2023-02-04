package ru.yandex.realty.rent.backend.converter

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.rent.model.Payment
import ru.yandex.realty.rent.model.enums.PaymentStatus.PaymentStatus
import ru.yandex.realty.rent.model.enums.{PaymentStatus, PaymentType}
import ru.yandex.realty.rent.proto.api.payment.Payment.TenantPaymentViewStatusNamespace.TenantPaymentViewStatus
import ru.yandex.realty.rent.proto.api.payment.Payment.OwnerPaymentViewStatusNamespace.OwnerPaymentViewStatus
import ru.yandex.realty.rent.proto.model.payment.PaymentData
import ru.yandex.realty.rent.proto.model.payment.PayoutStatusNamespace.PayoutStatus
import ru.yandex.realty.rent.util.NowMomentProvider
import ru.yandex.vertis.util.time.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class PaymentConverterSpec extends SpecBase {
  val Id = "12345FSADS@!223"

  val converter: PaymentConverter.type = PaymentConverter
  val Now: DateTime = DateTime.parse("2020-01-10T10:00:00.000+03:00", DateTimeUtil.IsoDateTimeFormatter)
  val NowAtDayStart: DateTime = Now.withTimeAtStartOfDay()
  def paymentData: PaymentData.Builder = PaymentData.newBuilder()
  val successfulPaymentData: PaymentData = paymentData.setPayoutStatus(PayoutStatus.NOT_STARTED).build()
  val recoverableErrorPaymentData: PaymentData = paymentData.setPayoutStatus(PayoutStatus.RECOVERABLE_ERROR).build()
  val failurePaymentData: PaymentData = paymentData.setPayoutStatus(PayoutStatus.UNRECOVERABLE_ERROR).build()

  private def buildPayment(
    paymentDate: DateTime,
    status: PaymentStatus,
    payoutStatus: PayoutStatus = PayoutStatus.NOT_STARTED
  ): Payment =
    Payment(
      Id,
      Id,
      PaymentType.Rent,
      isPaidOutUnderGuarantee = false,
      paymentDate,
      paymentDate,
      paymentDate.plusMonths(1),
      status,
      PaymentData.newBuilder().setPayoutStatus(payoutStatus).build(),
      createTime = DateTimeUtil.now(),
      updateTime = DateTimeUtil.now()
    )

  val paymentWithRecoverableError: Payment =
    buildPayment(NowAtDayStart, PaymentStatus.PaidByTenant, PayoutStatus.RECOVERABLE_ERROR)

  val paymentWithUnrecoverableError: Payment =
    buildPayment(NowAtDayStart, PaymentStatus.PaidByTenant, PayoutStatus.UNRECOVERABLE_ERROR)

  val paidOutPayment: Payment = buildPayment(NowAtDayStart, PaymentStatus.PaidToOwner, PayoutStatus.PAID_OUT)
  val paidByTenantPayment: Payment = buildPayment(NowAtDayStart, PaymentStatus.PaidByTenant)
  val newPayment: Payment = buildPayment(NowAtDayStart.plusDays(1), PaymentStatus.New)
  val futurePayment: Payment = buildPayment(NowAtDayStart.plusDays(11), PaymentStatus.FuturePayment)

  val isFirstPayment = false
  implicit val nowMomentProvider: NowMomentProvider = NowMomentProvider(Now)

  "PaymentConverter" should {
    "convert payment to Paid status in common case" in {
      converter.getTenantPaymentStatus(paidOutPayment, isFirstPayment) should be(TenantPaymentViewStatus.PAID)
      converter.getOwnerPaymentStatus(paidOutPayment) should be(OwnerPaymentViewStatus.PAID_TO_OWNER)
    }
    "convert payment to Future status in common case" in {
      converter.getTenantPaymentStatus(futurePayment, isFirstPayment) should be(TenantPaymentViewStatus.FUTURE_PAYMENT)
      converter.getOwnerPaymentStatus(futurePayment) should be(OwnerPaymentViewStatus.FUTURE_PAYMENT)
    }
    "convert payment from New status to several cases" should {
      "missing payment before the actual date of payment" in {
        converter.getTenantPaymentStatus(newPayment, isFirstPayment) should be(TenantPaymentViewStatus.READY_TO_PAY)
        converter.getOwnerPaymentStatus(newPayment) should be(OwnerPaymentViewStatus.WAITING_FOR_PAYMENT)
      }
      "missing payment during first contract payment" in {
        val tomorrowProvider = NowMomentProvider(Now.plusDays(1))
        val outdatedProvider = NowMomentProvider(Now.plusDays(2))
        converter.getTenantPaymentStatus(newPayment, isFirstPayment = true) should be(
          TenantPaymentViewStatus.FIRST_PAYMENT
        )
        converter.getTenantPaymentStatus(newPayment, isFirstPayment = true)(tomorrowProvider) should be(
          TenantPaymentViewStatus.FIRST_PAYMENT
        )
        converter.getTenantPaymentStatus(newPayment, isFirstPayment = true)(outdatedProvider) should be(
          TenantPaymentViewStatus.FIRST_PAYMENT
        )
      }
      "missing payment during the actual date of payment" in {
        implicit val nowMomentProvider: NowMomentProvider = NowMomentProvider(Now.plusDays(1))
        converter.getTenantPaymentStatus(newPayment, isFirstPayment) should be(TenantPaymentViewStatus.TODAY)
        converter.getOwnerPaymentStatus(newPayment) should be(OwnerPaymentViewStatus.WAITING_FOR_PAYMENT_TODAY)
      }
      "missing payment that is in fact outdated" in {
        implicit val nowMomentProvider: NowMomentProvider = NowMomentProvider(Now.plusDays(2))
        converter.getTenantPaymentStatus(newPayment, isFirstPayment) should be(TenantPaymentViewStatus.OUTDATED)
        converter.getOwnerPaymentStatus(newPayment) should be(OwnerPaymentViewStatus.WAITING_FOR_PAYMENT_OUTDATED)
      }
    }

    "convert payment from Paid By Tenant status to several cases" should {
      "default case" in {
        converter.getTenantPaymentStatus(paidByTenantPayment, isFirstPayment) should be(TenantPaymentViewStatus.PAID)
        converter.getOwnerPaymentStatus(paidByTenantPayment) should be(OwnerPaymentViewStatus.PAID_BY_TENANT)
      }
      "postponed Payment case" in {
        implicit val nowMomentProvider: NowMomentProvider = NowMomentProvider(Now.minusHours(4))
        converter.getTenantPaymentStatus(paidByTenantPayment, isFirstPayment) should be(TenantPaymentViewStatus.PAID)
        converter.getOwnerPaymentStatus(paidByTenantPayment) should be(OwnerPaymentViewStatus.PAID_BY_TENANT_HOLDING)
      }
      "case with recoverable error" in {
        converter.getTenantPaymentStatus(paymentWithRecoverableError, isFirstPayment) should be(
          TenantPaymentViewStatus.PAID
        )
        converter.getOwnerPaymentStatus(paymentWithRecoverableError) should be(OwnerPaymentViewStatus.PAID_BY_TENANT)
      }

      "case with unrecoverable error" in {
        converter.getTenantPaymentStatus(paymentWithUnrecoverableError, isFirstPayment) should be(
          TenantPaymentViewStatus.PAID
        )
        converter.getOwnerPaymentStatus(paymentWithUnrecoverableError) should be(
          OwnerPaymentViewStatus.PAYOUT_UNRECOVERABLE_ERROR
        )
      }
    }
  }
}
