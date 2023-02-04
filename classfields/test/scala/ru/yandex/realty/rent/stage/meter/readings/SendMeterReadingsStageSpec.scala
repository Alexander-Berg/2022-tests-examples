package ru.yandex.realty.rent.stage.meter.readings

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.dao.{FlatDao, PeriodDao, RentContractDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.RentContract
import ru.yandex.realty.rent.model.enums.{ContractStatus, MeterReadingsStatus}
import ru.yandex.realty.rent.model.house.services.{MeterReadings, MeterReadingsUtils}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.realty.util.TimeUtils.RichDateTime

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class SendMeterReadingsStageSpec extends AsyncSpecBase with RentModelsGen {

  private val flatDao = mock[FlatDao]
  private val periodDao = mock[PeriodDao]
  private val contractDao = mock[RentContractDao]
  implicit val traced: Traced = Traced.empty

  private def invokeStage(
    meterReadings: MeterReadings,
    period: DateTime,
    now: DateTime
  ): ProcessingState[MeterReadings] = {
    val contract = rentContractGen(ContractStatus.Active, nowMomentForTesting = now).next
    (flatDao
      .findById(_: String)(_: Traced))
      .expects(contract.flatId, *)
      .anyNumberOfTimes()
      .returning(Future.successful(flatGen().next))
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
    val state = ProcessingState(meterReadings)
    val sendMeterReadingsStage = new SendMeterReadingsStage(flatDao, periodDao, contractDao)
    sendMeterReadingsStage.process(state).futureValue
  }

  "SendMeterReadingsStage" should {
    "Process meter readings to sent updated fixed time ago" in {
      val now = DateTimeUtil.now()
      val time = now.minus(MeterReadingsUtils.intervalForSending.toMillis)
      val meterReadings =
        meterReadingsGen(readableString.next, readableString.next, MeterReadingsStatus.Sending).next
          .copy(updateTime = time, visitTime = Some(now))
      val updatedMeterReadings = invokeStage(meterReadings, now.withFirstDayOfMonth, now).entry
      updatedMeterReadings.status shouldBe MeterReadingsStatus.Sent
      updatedMeterReadings.visitTime shouldBe None
    }

    "Process meter readings to sent updated now" in {
      val now = DateTimeUtil.now()
      val meterReadings =
        meterReadingsGen(readableString.next, readableString.next, MeterReadingsStatus.Sending).next
          .copy(updateTime = now, visitTime = Some(now))
      val updatedMeterReadings = invokeStage(meterReadings, now.withFirstDayOfMonth, now).entry
      updatedMeterReadings.status shouldBe MeterReadingsStatus.Sending
      updatedMeterReadings.visitTime shouldBe Some(now.plus(MeterReadingsUtils.intervalForSending.toMillis))
    }

    "Process meter readings with declined status" in {
      val now = DateTimeUtil.now()
      val meterReadings =
        meterReadingsGen(readableString.next, readableString.next, MeterReadingsStatus.Declined).next
          .copy(updateTime = now, visitTime = Some(now))
      val updatedMeterReadings = invokeStage(meterReadings, now.withFirstDayOfMonth, now).entry
      updatedMeterReadings.status shouldBe MeterReadingsStatus.Declined
      updatedMeterReadings.visitTime shouldBe None
    }
  }
}
