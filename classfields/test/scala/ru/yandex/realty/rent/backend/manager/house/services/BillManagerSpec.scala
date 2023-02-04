package ru.yandex.realty.rent.backend.manager.house.services

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.backend.converter.house.services.BillConverter
import ru.yandex.realty.rent.backend.manager.{Data, Wiring}
import ru.yandex.realty.rent.model.enums.BillStatus
import ru.yandex.realty.rent.model.house.services.Period
import ru.yandex.realty.rent.model.{OwnerRequest, RentContract}
import ru.yandex.realty.rent.proto.api.image.Image
import ru.yandex.realty.rent.proto.api.internal.house.services.InternalCreateBillResponse
import ru.yandex.realty.rent.proto.api.periods.BillRequest
import ru.yandex.realty.rent.proto.model.house.service.periods.Bill
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class BillManagerSpec extends SpecBase {

  private val mockBillConverter = mock[BillConverter]
  private val mockPeriodManager = mock[PeriodManager]

  "Bill manager" should {
    "update visit time for rent contract when sending bill" in new Wiring with Data with RentPaymentsData {
      val billManager = new BillManager(mockPeriodDao, mockContractDao, mockBillConverter, mockPeriodManager)

      val now: DateTime = DateTimeUtil.now()
      val contract: RentContract = createContract(now, now.getDayOfMonth)

      val passportUser: PassportUser = PassportUser(123L)
      val flatId: String = readableString.next
      val request: BillRequest = BillRequest
        .newBuilder()
        .setAmount(100L)
        .addPhotos(Image.getDefaultInstance)
        .build()

      val period: Period = periodGen(contract.contractId).map {
        _.copy(billStatus = BillStatus.CanBeSent)
      }.next

      (mockPeriodManager
        .checkOwnerConditions(_: PassportUser, _: String, _: String)(_: OwnerRequest => Boolean)(_: Traced))
        .expects(*, *, *, *, *)
        .returns(Future.successful(period))
      (mockBillConverter
        .createInternalBill(_: BillRequest))
        .expects(request)
        .returns(Bill.newBuilder().setAmount(request.getAmount).build())
      (mockPeriodDao
        .update(_: String)(_: Period => Period)(_: Traced))
        .expects(*, *, *)
        .returns(Future.successful(period))
      (mockBillConverter
        .convertCreateBillResponse(_: Bill))
        .expects(*)
        .returns(InternalCreateBillResponse.getDefaultInstance)
      (mockContractDao
        .refresh(_: String)(_: Traced))
        .expects(contract.contractId, *)
        .returns(Future.successful(contract))

      billManager.sendBill(passportUser, flatId, period.periodId, request).futureValue
    }
  }
}
