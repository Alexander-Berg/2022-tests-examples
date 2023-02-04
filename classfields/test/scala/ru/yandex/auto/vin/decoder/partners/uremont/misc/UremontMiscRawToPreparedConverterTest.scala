package ru.yandex.auto.vin.decoder.partners.uremont.misc

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.VinReportModel.InsuranceType
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class UremontMiscRawToPreparedConverterTest extends AnyFunSuite {

  val converter = new UremontMiscRawToPreparedConverter
  implicit val t: Traced = Traced.empty

  test("success response") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/uremont/misc/success.json")
    val model = UremontMiscRawModel.apply(vin, 200, raw)
    val converted = converter.convert(model).await

    assert(converted.getStatus == VinInfoHistory.Status.OK)
    assert(converted.getVin == "SJNFDAJ11U1084830")
    assert(converted.getGroupId == "")
    assert(converted.getEventType == EventType.UREMONT_MISC)

    val sb = converted.getServiceBook
    assert(sb.getMark == "Kia")
    assert(sb.getModel == "Rio")
    assert(sb.getYear == 2014)
    assert(sb.getOrdersCount == 2)

    assert(sb.getOrders(0).getOrderDate == 1592497181000L)
    assert(sb.getOrders(0).getStoCity == "Санкт-Петербург")
    assert(sb.getOrders(0).getStoName == "СТО ТЕХНИКА")
    assert(sb.getOrders(0).getSummaryPrice == 2500)
    assert(sb.getOrders(0).getCustomerReview == "Все хорошо.")
    assert(sb.getOrders(0).getMileage == 150000)
    assert(sb.getOrders(0).getDescription == "Замена сайлентблоков задней балки")

    assert(sb.getOrders(1).getOrderDate == 1613728008000L)
    assert(sb.getOrders(1).getStoCity == "Ульяновка")
    assert(sb.getOrders(1).getStoName == "Автоцех")
    assert(sb.getOrders(1).getSummaryPrice == 1800)
    assert(sb.getOrders(1).getCustomerReview == "")
    assert(sb.getOrders(1).getMileage == 269210)
    assert(
      sb.getOrders(1)
        .getDescription == "Замена задних тормозных дисков и колодок.\nТак же необходимо обслужить один суппорт(Закис)"
    )

    assert(converted.getInsurancePaymentsCount == 4)
    assert(converted.getInsurancePayments(0).getDate == 0L)
    assert(converted.getInsurancePayments(0).getAmount == 383900)
    assert(converted.getInsurancePayments(0).getInsurerName == "РЕСО-ГАРАНТИЯ")
    assert(converted.getInsurancePayments(0).getInsuranceType == InsuranceType.OSAGO)
    assert(converted.getInsurancePayments(0).getPolicyStartDate == 1561334400000L)
    assert(converted.getInsurancePayments(0).getPolicyFinishDate == 1592956800000L)

    assert(converted.getInsurancePayments(1).getDate == 1451606400000L)
    assert(converted.getInsurancePayments(1).getAmount == 842324.53)
    assert(converted.getInsurancePayments(1).getInsurerName == "РЕСО-ГАРАНТИЯ")
    assert(converted.getInsurancePayments(1).getInsuranceType == InsuranceType.KASKO)
    assert(converted.getInsurancePayments(1).getPolicyStartDate == 1561334400000L)
    assert(converted.getInsurancePayments(1).getPolicyFinishDate == 1592956800000L)

    assert(converted.getInsurancePayments(2).getDate == 1543622400000L)
    assert(converted.getInsurancePayments(2).getAmount == 401250)
    assert(converted.getInsurancePayments(2).getInsurerName == "ТИНЬКОФФ")
    assert(converted.getInsurancePayments(2).getInsuranceType == InsuranceType.UNKNOWN_INSURANCE)
    assert(converted.getInsurancePayments(2).getPolicyStartDate == 0L)
    assert(converted.getInsurancePayments(2).getPolicyFinishDate == 0L)
  }

}
