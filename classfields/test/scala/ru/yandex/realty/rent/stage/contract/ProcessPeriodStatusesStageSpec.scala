package ru.yandex.realty.rent.stage.contract

import org.joda.time.{DateTime, DateTimeZone, LocalTime}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.dao.{FlatDao, MeterReadingsDao, OwnerRequestDao, PeriodDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{ContractWithPayments, Flat, RentContract}
import ru.yandex.realty.rent.model.enums.MeterReadingsStatus
import ru.yandex.realty.rent.model.house.services.Period
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TimeUtils.RichDateTime
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.util.time.DateTimeUtil

import java.time.ZoneId
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ProcessPeriodStatusesStageSpec extends AsyncSpecBase with RentModelsGen with RentPaymentsData {

  private val flatDao = mock[FlatDao]
  private val periodDao = mock[PeriodDao]
  private val meterReadingsDao = mock[MeterReadingsDao]
  private val ownerRequestDao = mock[OwnerRequestDao]
  implicit val traced: Traced = Traced.empty

  private def invokeStage(contract: ContractWithPayments): ProcessingState[ContractWithPayments] = {
    val state = ProcessingState(contract)
    val processPeriodStatusesStage =
      new ProcessPeriodStatusesStage(flatDao, ownerRequestDao, periodDao, meterReadingsDao)
    processPeriodStatusesStage.doProcess(state).futureValue
  }

  "ProcessPeriodStatusesStage" should {
    "process statuses at the end of month and set visit time to the next month" in {
      val currentMoment = DateTimeUtil.now()
      val now = currentMoment.withDayOfMonth(currentMoment.dayOfMonth().getMaximumValue).withTime(new LocalTime(23, 59))
      val flat = flatGen(true).next
      val ownerRequest = ownerRequestGen.next.copy(shouldSendMetrics = true)
      val contract =
        createContract(now, now.getDayOfMonth, nowMoment = Some(now))
          .copy(ownerRequestId = Some(ownerRequest.ownerRequestId))
      handleMocks(now.getMonthOfYear, now.getYear, flat, contract) //MSK timezone
      val contractWithPayments = invokeStage(ContractWithPayments(contract, Nil)).entry
      contractWithPayments.visitTime shouldBe Some(now.plusMonths(1).withFirstDayOfMonth)
    }

    "process statuses at the end of month with different time zone and set visit time to the next month" in {
      val currentMoment = DateTimeUtil.now()
      val now = currentMoment.withDayOfMonth(currentMoment.dayOfMonth().getMaximumValue).withTime(new LocalTime(20, 1))
      val ownerRequest = ownerRequestGen.next.copy(shouldSendMetrics = true)
      val flat = flatGen(true).next.applyTransform { e =>
        e.copy(
          data = e.data.toBuilder
            .applySideEffect(
              _.getLocationBuilder
                .setSubjectFederationGeoid(Regions.NOVOSIBIRSKAYA_OBLAST)
                .setSubjectFederationRgid(NodeRgid.NOVOSIBIRSKAYA_OBLAST)
            )
            .build()
        )
      }
      val contract = createContract(
        now,
        now.getDayOfMonth,
        nowMoment = Some(now)
      ).copy(ownerRequestId = Some(ownerRequest.ownerRequestId))
      handleMocks(currentMoment.plusMonths(1).getMonthOfYear, currentMoment.plusMonths(1).getYear, flat, contract) //NSK timezone
      val contractWithPayments = invokeStage(ContractWithPayments(contract, Nil)).entry
      contractWithPayments.visitTime shouldBe Some(
        now
          .withZone(DateTimeZone.forID(ZoneId.of("Asia/Novosibirsk").getId))
          .withDayOfMonth(CreatePeriodStage.dayOfStatusHandling)
          .withTimeAtStartOfDay()
      )
    }

    "process statuses at the end of month with different time zone and set visit time to the current month" in {
      val currentMoment = DateTimeUtil.now().plusMonths(1) // + months because method withNotBeforeNowInTestingVisitTime
      val now = currentMoment.withDayOfMonth(currentMoment.dayOfMonth().getMaximumValue).withTime(new LocalTime(19, 59))
      val ownerRequest = ownerRequestGen.next.copy(shouldSendMetrics = true)
      val flat = flatGen(true).next.applyTransform { e =>
        e.copy(
          data = e.data.toBuilder
            .applySideEffect(
              _.getLocationBuilder
                .setSubjectFederationGeoid(Regions.NOVOSIBIRSKAYA_OBLAST)
                .setSubjectFederationRgid(NodeRgid.NOVOSIBIRSKAYA_OBLAST)
            )
            .build()
        )
      }
      val contract = createContract(
        now,
        now.getDayOfMonth,
        nowMoment = Some(now)
      ).copy(ownerRequestId = Some(ownerRequest.ownerRequestId))
      handleMocks(currentMoment.getMonthOfYear, currentMoment.getYear, flat, contract) //NSK timezone
      val contractWithPayments = invokeStage(ContractWithPayments(contract, Nil)).entry
      contractWithPayments.visitTime shouldBe Some(
        now
          .withZone(DateTimeZone.forID(ZoneId.of("Asia/Novosibirsk").getId))
          .withFirstDayOfMonth
          .plusMonths(1)
      )
    }
  }

  private def handleMocks(
    currentMonth: Int,
    currentYear: Int,
    flat: Flat,
    contract: RentContract
  ): Unit = {
    val periodTime = DateTimeUtil.now().withFirstDayOfMonth.withMonthOfYear(currentMonth).withYear(currentYear)
    val period = periodGen(contractId = contract.contractId, period = periodTime).next
    (ownerRequestDao
      .findLastByFlatId(_: String)(_: Traced))
      .expects(*, *)
      .once()
      .returning(
        Future.successful(
          Some(
            ownerRequestGen.next.copy(
              shouldSendReceiptPhotos = true,
              shouldSendMetrics = true,
              shouldTenantRefund = true,
              paymentConfirmation = true
            )
          )
        )
      )
    (flatDao
      .findById(_: String)(_: Traced))
      .expects(contract.flatId, *)
      .once()
      .returning(Future.successful(flat))
    (periodDao
      .findByContractIdForPeriod(_: String, _: DateTime)(_: Traced))
      .expects(contract.contractId, periodTime, *)
      .once()
      .returning(Future.successful(Seq(period)))
    (periodDao
      .findByContractIdForPeriod(_: String, _: DateTime)(_: Traced))
      .expects(contract.contractId, periodTime.minusMonths(1), *)
      .once()
      .returning(Future.successful(Seq(period.copy(period = period.period.minusMonths(1)))))
    (periodDao
      .update(_: String)(_: Period => Period)(_: Traced))
      .expects(period.periodId, *, *)
      .twice()
      .returning(Future.successful(period))
    (meterReadingsDao
      .findByPeriodId(_: String))
      .expects(*)
      .twice()
      .returning(
        Future.successful(
          Seq(
            meterReadingsGen(readableString.next, period.periodId, MeterReadingsStatus.NotSent).next,
            meterReadingsGen(readableString.next, period.periodId, MeterReadingsStatus.Sending).next
          )
        )
      )
  }
}
