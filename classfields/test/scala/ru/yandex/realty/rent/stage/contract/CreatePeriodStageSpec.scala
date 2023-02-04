package ru.yandex.realty.rent.stage.contract

import org.joda.time.{DateTimeZone, LocalTime}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.dao.{FlatDao, HouseServiceDao, MeterReadingsDao, OwnerRequestDao, PeriodDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{ContractWithPayments, Flat, OwnerRequest, RentContract}
import ru.yandex.realty.rent.model.enums.{ContractStatus, OwnerRequestSettingStatus, PeriodType}
import ru.yandex.realty.rent.model.house.services.{HouseService, MeterReadings, Period}
import ru.yandex.realty.rent.proto.model.house.service.{HouseServiceData, Meter}
import ru.yandex.realty.rent.util.NowMomentProvider
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TimeUtils.RichDateTime
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.util.time.DateTimeUtil

import java.time.ZoneId
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CreatePeriodStageSpec extends AsyncSpecBase with RentModelsGen with RentPaymentsData {

  private val periodDao = mock[PeriodDao]
  private val flatDao = mock[FlatDao]
  private val meterReadingsDao = mock[MeterReadingsDao]
  private val houseServiceDao = mock[HouseServiceDao]
  private val ownerRequestDao = mock[OwnerRequestDao]
  private val houseServiceNumber = 6
  implicit val traced: Traced = Traced.empty

  private def invokeStage(contract: ContractWithPayments): ProcessingState[ContractWithPayments] = {
    val state = ProcessingState(contract)
    val HouseServiceEnableStage =
      new CreatePeriodStage(periodDao, flatDao, meterReadingsDao, houseServiceDao, ownerRequestDao)
    HouseServiceEnableStage.doProcess(state).futureValue
  }

  "CreatePeriodStage" should {
    "create period for contract without termination date" in {
      val paymentDate = DateTimeUtil.now().withDayOfMonth(1).withTimeAtStartOfDay()
      val flat = flatGen().next
      val ownerRequest = ownerRequestGen.next
        .copy(shouldSendMetrics = true, settingsStatus = OwnerRequestSettingStatus.ConfirmedByTenant)
      val contract =
        createContract(paymentDate, paymentDate.getDayOfMonth).copy(ownerRequestId = Some(ownerRequest.ownerRequestId))
      val houseServices = generateHouseServices(ownerRequest.ownerRequestId, contract)
      handleMocks(houseServiceNumber, flat, ownerRequest, contract, houseServices)
      val contractWithPayments = ContractWithPayments(contract, Nil)
      invokeStage(contractWithPayments)
    }

    "create meter readings for existing period" in {
      val paymentDate = DateTimeUtil.now().withDayOfMonth(1).withTimeAtStartOfDay()
      val flat = flatGen().next
      val ownerRequest = ownerRequestGen.next
        .copy(shouldSendMetrics = true, settingsStatus = OwnerRequestSettingStatus.ConfirmedByTenant)
      val contract =
        createContract(paymentDate, paymentDate.getDayOfMonth).copy(ownerRequestId = Some(ownerRequest.ownerRequestId))
      val period = periodGen(contractId = contract.contractId).next
      val houseServices = generateHouseServices(ownerRequest.ownerRequestId, contract)
      val meterReadings = meterReadingsGen(houseServices.head.houseServiceId, period.periodId).next
      handleMocks(houseServiceNumber, flat, ownerRequest, contract, houseServices, Seq(period), Seq(meterReadings))
      val contractWithPayments = ContractWithPayments(contract, Nil)
      invokeStage(contractWithPayments)
    }

    "create period for contract with termination date" in {
      val paymentDate = DateTimeUtil.now().withDayOfMonth(1).withTimeAtStartOfDay()
      val ownerRequest = ownerRequestGen.next
        .copy(shouldSendMetrics = true, settingsStatus = OwnerRequestSettingStatus.ConfirmedByTenant)
      val flat = flatGen().next
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth)
      val contractWithTerminationDate =
        contract.copy(
          status = ContractStatus.FixedTerm,
          ownerRequestId = Some(ownerRequest.ownerRequestId),
          terminationDate = Some(DateTimeUtil.now())
        )
      val houseServices = generateHouseServices(ownerRequest.ownerRequestId, contract)
      handleMocks(houseServiceNumber, flat, ownerRequest, contractWithTerminationDate, houseServices)
      val contractWithPayments = ContractWithPayments(contractWithTerminationDate, Nil)
      val result = invokeStage(contractWithPayments)
      result.entry.contract.visitTime shouldBe None
    }

    "create period for flat at the different time zone" in {
      val currentMoment = DateTimeUtil.now()
      val now = currentMoment.withDayOfMonth(currentMoment.dayOfMonth().getMaximumValue).withTime(new LocalTime(20, 1))
      val paymentDate = currentMoment.withDayOfMonth(1).withTimeAtStartOfDay()
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
      val ownerRequest = ownerRequestGen.next
        .copy(shouldSendMetrics = true, settingsStatus = OwnerRequestSettingStatus.ConfirmedByTenant)
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth, nowMoment = Some(now))
        .copy(ownerRequestId = Some(ownerRequest.ownerRequestId))
      val houseServices = generateHouseServices(ownerRequest.ownerRequestId, contract)
      handleMocks(houseServiceNumber, flat, ownerRequest, contract, houseServices)
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val result = invokeStage(contractWithPayments).entry
      result.visitTime shouldBe Some(
        now
          .withZone(DateTimeZone.forID(ZoneId.of("Asia/Novosibirsk").getId))
          .withDayOfMonth(CreatePeriodStage.dayOfStatusHandling)
          .withTimeAtStartOfDay()
      )
    }
  }

  private def handleMocks(
    createMetricsSize: Int,
    flat: Flat,
    ownerRequest: OwnerRequest,
    contract: RentContract,
    houseServices: Seq[HouseService],
    existedPeriod: Seq[Period] = Seq.empty,
    existedMeterReadings: Seq[MeterReadings] = Seq.empty
  ): Unit = {
    val nowMomentProvider: NowMomentProvider = NowMomentProvider(flat.subjectFederationId, contract)
    val periodTime =
      DateTimeUtil.now().withMonthOfYear(nowMomentProvider.provideWithZone().monthOfYear().get()).withFirstDayOfMonth
    (flatDao
      .findById(_: String)(_: Traced))
      .expects(contract.flatId, *)
      .once()
      .returning(Future.successful(flat))
    (ownerRequestDao
      .findByIdOpt(_: String)(_: Traced))
      .expects(*, *)
      .returning(Future.successful(Some(ownerRequest)))
    (houseServiceDao
      .findByOwnerRequest(_: String)(_: Traced))
      .expects(ownerRequest.ownerRequestId, *)
      .returning(Future.successful(houseServices))
    (periodDao
      .getByContractId(_: String)(_: Traced))
      .expects(contract.contractId, *)
      .returning(Future.successful(existedPeriod))
    if (existedPeriod.isEmpty) {
      (periodDao
        .create(_: Iterable[Period])(_: Traced))
        .expects(where { (periods: Iterable[Period], _) =>
          periods.map(_.periodId).toSet.size == 1 &&
          periods.exists(_.period == periodTime) &&
          periods.exists(_.periodType == PeriodType.Regular)
        })
        .returning(Future.unit)
    }
    (meterReadingsDao
      .findByPeriodId(_: String))
      .expects(*)
      .returning(Future.successful(existedMeterReadings))
    (meterReadingsDao
      .create(_: Iterable[MeterReadings]))
      .expects(where { meterReadings: Iterable[MeterReadings] =>
        meterReadings.map(_.meterReadingsId).toSet.size == createMetricsSize
      })
      .returning(Future.unit)
  }

  def generateHouseServices(ownerRequestId: String, contract: RentContract): Seq[HouseService] =
    houseServiceGen(ownerRequestId)
      .next(houseServiceNumber)
      .toSeq
      .map(
        _.copy(
          data = HouseServiceData
            .newBuilder()
            .setMeter(
              Meter
                .newBuilder()
                .setDeliverFromDay(1)
                .setDeliverToDay(contract.getRentStartDate.get.getDayOfMonth)
                .build()
            )
            .build()
        )
      )
}
