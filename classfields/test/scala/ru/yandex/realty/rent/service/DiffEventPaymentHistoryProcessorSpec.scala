package ru.yandex.realty.rent.service

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.dao.{CleanSchemaBeforeEach, RentSpecBase}
import ru.yandex.realty.rent.model.Payment
import ru.yandex.realty.rent.model.enums.PaymentType
import ru.yandex.realty.rent.model.enums.PaymentStatus
import ru.yandex.realty.rent.proto.api.payment.PaymentStatusNamespace
import ru.yandex.realty.rent.proto.model.diffevent.{DiffEvent, PaymentDiffEvent, PaymentProtoView}
import ru.yandex.realty.rent.proto.model.payment.PaymentTypeNamespace
import ru.yandex.realty.rent.service.impl.DiffEventPaymentHistoryProcessorImpl
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.vertis.util.time.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class DiffEventPaymentHistoryProcessorSpec extends AsyncSpecBase with RentSpecBase with CleanSchemaBeforeEach {

  implicit val trace = Traced.empty

  "DiffEventPaymentHistoryProcessorSpec.process" should {

    "create payment" in new Wiring {
      val initialPayment = paymentGen().next
      paymentDao.insert(initialPayment).futureValue
      val paymentDiffEvent = createPaymentDiffEvent(initialPayment)
      processor.process(paymentDiffEvent).futureValue
      val histories = paymentHistoryDao.get(initialPayment.id).futureValue
      histories.nonEmpty shouldBe true
      val history = histories.head
      history.paymentId shouldBe initialPayment.id
      history.contractId shouldBe initialPayment.contractId
      history.paymentType shouldBe initialPayment.`type`
      history.isPaidOutUnderGuarantee shouldBe initialPayment.isPaidOutUnderGuarantee
      history.paymentDate shouldBe initialPayment.paymentDate
      history.startTime shouldBe initialPayment.startTime
      history.endTime shouldBe initialPayment.endTime
      history.status shouldBe initialPayment.status
      history.data shouldBe initialPayment.data
      history.createTime shouldBe initialPayment.createTime
      history.updateTime shouldBe initialPayment.updateTime
      history.isDeleted shouldBe false
    }

  }

  private def createPaymentDiffEvent(
    payment: Payment
  ): DiffEvent = {
    val newPaymentProto = PaymentProtoView.newBuilder
      .setPaymentId(payment.id)
      .setData(payment.data)
      .setPaymentType(getPaymentType(payment))
      .setStatus(getPaymentStatus(payment))
      .setContractId(payment.contractId)
      .setPaymentDate(DateTimeFormat.write(payment.paymentDate))
      .setStartTime(DateTimeFormat.write(payment.startTime))
      .setEndTime(DateTimeFormat.write(payment.endTime))
      .setUpdateTime(DateTimeFormat.write(payment.updateTime))
      .setData(payment.data)
      .setCreateTime(DateTimeFormat.write(payment.createTime))
      .setUpdateTime(DateTimeFormat.write(payment.updateTime))
    val paymentEvent = PaymentDiffEvent.newBuilder.setNew(newPaymentProto)
    DiffEvent.newBuilder().setPaymentEvent(paymentEvent).build()
  }

  private def getPaymentType(payment: Payment): PaymentTypeNamespace.PaymentType =
    payment.`type` match {
      case PaymentType.Rent => PaymentTypeNamespace.PaymentType.RENT
      case PaymentType.HouseServices => PaymentTypeNamespace.PaymentType.HOUSE_SERVICES
      case PaymentType.Unknown => PaymentTypeNamespace.PaymentType.UNKNOWN_TYPE
    }

  private def getPaymentStatus(payment: Payment): PaymentStatusNamespace.PaymentStatus =
    payment.status match {
      case PaymentStatus.FuturePayment => PaymentStatusNamespace.PaymentStatus.FUTURE_PAYMENT
      case PaymentStatus.New => PaymentStatusNamespace.PaymentStatus.NEW
      case PaymentStatus.PaidToOwner => PaymentStatusNamespace.PaymentStatus.PAID_TO_OWNER
      case PaymentStatus.PaidByTenant => PaymentStatusNamespace.PaymentStatus.PAID_BY_TENANT
      case PaymentStatus.PaidOutUnderGuarantee => PaymentStatusNamespace.PaymentStatus.PAID_OUT_UNDER_GUARANTEE
      case PaymentStatus.Unknown => PaymentStatusNamespace.PaymentStatus.UNKNOWN
    }

  private trait Wiring {

    val processor = new DiffEventPaymentHistoryProcessorImpl(paymentHistoryDao)
  }
}
