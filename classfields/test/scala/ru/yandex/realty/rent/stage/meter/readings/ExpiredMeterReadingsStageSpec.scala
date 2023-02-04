package ru.yandex.realty.rent.stage.meter.readings

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.dao.{FlatDao, HouseServiceDao, PeriodDao, RentContractDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.RentContract
import ru.yandex.realty.rent.model.enums.{ContractStatus, MeterReadingsStatus}
import ru.yandex.realty.rent.model.house.services.MeterReadings
import ru.yandex.realty.rent.proto.model.house.service.Meter
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TimeUtils.RichDateTime
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ExpiredMeterReadingsStageSpec extends AsyncSpecBase with RentModelsGen {

  private val flatDao = mock[FlatDao]
  private val houseServiceDao = mock[HouseServiceDao]
  private val periodDao = mock[PeriodDao]
  private val contractDao = mock[RentContractDao]
  implicit val traced: Traced = Traced.empty

  private def invokeStage(meterReadings: MeterReadings): ProcessingState[MeterReadings] = {
    val state = ProcessingState(meterReadings)
    val expiredMeterReadingsStage = new ExpiredMeterReadingsStage(flatDao, houseServiceDao, periodDao, contractDao)
    expiredMeterReadingsStage.process(state).futureValue
  }

  "ExpiredMeterReadingsStage" should {
    "Process meter readings in valid status not in time interval" in {
      val from = 1
      val to = 25
      val houseServiceId = readableString.next
      val period = DateTimeUtil.now().withFirstDayOfMonth
      handleMocks(houseServiceId, from, to, period, period.withDayOfMonth(to + 1))
      val meterReadings = meterReadingsGen(houseServiceId, readableString.next, MeterReadingsStatus.ShouldBeSent).next
        .copy(visitTime = Some(DateTimeUtil.now()))
      val updatedMeterReadings = invokeStage(meterReadings).entry
      updatedMeterReadings.status shouldBe MeterReadingsStatus.Expired
      updatedMeterReadings.visitTime shouldBe None
    }

    "Process meter readings in valid status in time interval" in {
      val from = 1
      val to = 10
      val houseServiceId = readableString.next
      val period = DateTimeUtil.now().withMonthOfYear(9).withFirstDayOfMonth
      handleMocks(houseServiceId, from, to, period, period.withDayOfMonth(to - 1))
      val meterReadings = meterReadingsGen(houseServiceId, readableString.next, MeterReadingsStatus.ShouldBeSent).next
        .copy(visitTime = Some(DateTimeUtil.now()))
      val updatedMeterReadings = invokeStage(meterReadings).entry
      updatedMeterReadings.status shouldBe MeterReadingsStatus.ShouldBeSent
      updatedMeterReadings.visitTime shouldBe Some(period.withDayOfMonth(to).plusDays(1).withTimeAtStartOfDay)
    }

    "Process meter readings in valid status in time interval for last day of month" in {
      val realNow = DateTimeUtil.now()
      val now = realNow.withDayOfMonth(realNow.dayOfMonth().getMaximumValue).withHourOfDay(12)
      val from = 1
      val to = now.dayOfMonth().getMaximumValue
      val houseServiceId = readableString.next
      val period = now.withFirstDayOfMonth.withTimeAtStartOfDay()
      handleMocks(houseServiceId, from, to, period, now)
      val meterReadings = meterReadingsGen(houseServiceId, readableString.next, MeterReadingsStatus.ShouldBeSent).next
        .copy(visitTime = Some(now))
      val updatedMeterReadings = invokeStage(meterReadings).entry
      updatedMeterReadings.status shouldBe MeterReadingsStatus.ShouldBeSent
      updatedMeterReadings.visitTime shouldBe Some(period.withDayOfMonth(to).plusDays(1).withTimeAtStartOfDay)
    }

    "Process meter readings in not valid status " in {
      val now = DateTimeUtil.now()
      val houseServiceId = readableString.next
      val meterReadings =
        meterReadingsGen(houseServiceId, readableString.next, MeterReadingsStatus.Sent).next.copy(visitTime = Some(now))
      val updatedMeterReadings = invokeStage(meterReadings).entry
      updatedMeterReadings.status shouldBe MeterReadingsStatus.Sent
      updatedMeterReadings.visitTime shouldBe None
    }
  }

  private def handleMocks(houseServiceId: String, from: Int, to: Int, period: DateTime, now: DateTime) = {
    val houseService = houseServiceGen(readableString.next).next
    val houseServicesWithInterval = houseService.copy(
      houseServiceId = houseServiceId,
      data = houseService.data.toBuilder
        .setMeter(Meter.newBuilder().setDeliverFromDay(from).setDeliverToDay(to).build())
        .build()
    )
    val contract = rentContractGen(ContractStatus.Active, nowMomentForTesting = now).next
    (flatDao
      .findById(_: String)(_: Traced))
      .expects(contract.flatId, *)
      .once()
      .returning(Future.successful(flatGen().next))
    (houseServiceDao
      .findById(_: String)(_: Traced))
      .expects(houseServiceId, *)
      .once()
      .returning(Future.successful(Some(houseServicesWithInterval)))
    (periodDao
      .findById(_: String)(_: Traced))
      .expects(*, *)
      .anyNumberOfTimes()
      .returning(Future.successful(Some(periodGen(readableString.next, period = period).next)))
    (contractDao
      .update(_: String)(_: RentContract => RentContract)(_: Traced))
      .expects(*, *, *)
      .anyNumberOfTimes()
      .returning(Future.successful(contract))
    (contractDao
      .findById(_: String)(_: Traced))
      .expects(*, *)
      .anyNumberOfTimes()
      .returning(Future.successful(contract))
  }
}
