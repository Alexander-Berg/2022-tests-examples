package ru.yandex.auto.vin.decoder.report.converters.raw.common

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.VinReportModel.OwnerItem.RegistrationStatus
import ru.auto.api.vin.VinReportModel.OwnerType
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.RegistrationEvent
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.history.entities.PreparedHistoryEntity
import ru.yandex.auto.vin.decoder.report.converters.raw.common.PreparedRegistrationPeriodsData.{
  RegActionData,
  RegistrationPeriodData
}
import ru.yandex.vertis.mockito.MockitoSupport

class PreparedRegistrationPeriodsDataTest extends AnyFunSuite with MockitoSupport {

  test("empty events history & periods without wholes") {
    val period1 = personRegisterdPeriod(
      from = Some(100),
      to = Some(200),
      lastOperation = Some("первичная регистрация"),
      idx = 0,
      globalIdx = 1
    )
    val period2 = personRegisterdPeriod(Some(200), None, Some("постановка на постоянный учет в связи со сверкой"), 1, 2)

    val reg = buildRegistration(Seq(period1, period2))
    val res = PreparedRegistrationPeriodsData.apply(reg, Seq.empty, Seq.empty, 1970)

    assert(res.allPeriods.size == 3)
    assert(res.ownersCount == 2)
    assert(res.gibddPeriods.size == 2)
    assert(isFirstPeriodUnknown(res))

    assert(res.gibddPeriods == Seq(period1, period2))
  }

  test("hole between gibdd periods") {
    val period1 = legalRegisteredPeriod(Some(100), Some(200), Some("первичная регистрация"), 0, 1)
    val notRegisteredPeriod = notRegistered(Some(200), Some(500), 2)
    val period2 =
      personRegisterdPeriod(Some(500), Some(600), Some("постановка на постоянный учет в связи со сверкой"), 1, 3)

    val reg = buildRegistration(Seq(period1, period2))
    val res = PreparedRegistrationPeriodsData.apply(reg, Seq.empty, Seq.empty, 1970)

    assert(isFirstPeriodUnknown(res))
    assert(res.allPeriods.drop(1) == Seq(period1, notRegisteredPeriod, period2))
    assert(res.gibddPeriods == Seq(period1, period2))
  }

  test("single period") {
    val period1 = legalRegisteredPeriod(Some(100), None, Some("постановка на постоянный учет в связи со сверкой"), 0, 1)
    val reg = buildRegistration(Seq(period1))
    val res = PreparedRegistrationPeriodsData.apply(reg, Seq.empty, Seq.empty, 1970)

    assert(isFirstPeriodUnknown(res))
    assert(res.allPeriods.drop(1) == Seq(period1))
  }

  test("first event before first registration period") {
    val manufacturePeriod = unknownRegistration(Some(-10800000), Some(100), 0)
    val period1 = personRegisterdPeriod(Some(100), Some(200), Some("первичная регистрация"), 0, 1)
    val period2 = personRegisterdPeriod(Some(200), None, Some("постановка на постоянный учет в связи со сверкой"), 1, 2)
    val event1 = buildEvent(1)
    val event2 = buildEvent(150)

    val reg = buildRegistration(Seq(period1, period2))
    val res = PreparedRegistrationPeriodsData.apply(reg, Seq.empty, Seq(event1, event2), 1970)

    assert(res.allPeriods == Seq(manufacturePeriod, period1, period2))
    assert(res.gibddPeriods == Seq(period1, period2))
  }

  test("first & last events not in registered periods") {
    val manufacturePeriod = unknownRegistration(Some(-10800000), Some(100), 0)
    val period1 = personRegisterdPeriod(Some(100), Some(200), Some("первичная регистрация"), 0, 1)
    val period2 =
      personRegisterdPeriod(Some(200), Some(300), Some("постановка на постоянный учет в связи со сверкой"), 1, 2)
    val event1 = buildEvent(1)
    val event2 = buildEvent(350)

    val notRegisteredPeriod = notRegistered(Some(300), None, 3)

    val reg = buildRegistration(Seq(period1, period2))
    val res = PreparedRegistrationPeriodsData.apply(reg, Seq.empty, Seq(event1, event2), 1970)

    assert(res.allPeriods == Seq(manufacturePeriod, period1, period2, notRegisteredPeriod))
    assert(res.gibddPeriods == Seq(period1, period2))
  }

  test("last reg period is not finished") {
    val manufacturePeriod = unknownRegistration(Some(-10800000), Some(100), 0)
    val period1 = legalRegisteredPeriod(Some(100), Some(200), Some("первичная регистрация"), 0, 1)
    val period2 = personRegisterdPeriod(Some(200), None, Some("постановка на постоянный учет в связи со сверкой"), 1, 2)
    val event = buildEvent(500)

    val reg = buildRegistration(Seq(period1, period2))
    val res = PreparedRegistrationPeriodsData.apply(reg, Seq.empty, Seq(event), 1970)

    assert(res.allPeriods == Seq(manufacturePeriod, period1, period2))
    assert(res.gibddPeriods == Seq(period1, period2))
  }

  test("without gibdd registration periods") {
    val manufacturePeriod = unknownRegistration(Some(31525200000L), None, 0)
    val event = buildEvent(500)
    val res = PreparedRegistrationPeriodsData.apply(None, Seq.empty, Seq(event), 1971)

    assert(res.allPeriods == Seq(manufacturePeriod))
    assert(!res.hasRealPeriodsData)
  }

  test("with registration periods, multiple events and single event without date") {
    val manufacturePeriod = unknownRegistration(Some(-10800000), Some(100), 0)
    val period1 = legalRegisteredPeriod(Some(100), Some(200), Some("первичная регистрация"), 0, 1)
    val period2 =
      legalRegisteredPeriod(Some(200), Some(300), Some("постановка на постоянный учет в связи со сверкой"), 1, 2)
    val event1 = buildEvent(0)
    val event2 = buildEvent(150)
    val event3 = buildEvent(250)

    val reg = buildRegistration(Seq(period1, period2))
    val res = PreparedRegistrationPeriodsData.apply(reg, Seq.empty, Seq(event1, event2, event3), 1970)

    assert(res.allPeriods == Seq(manufacturePeriod, period1, period2))
  }

  test("with single event with empty date and no registration periods") {
    val manufacturePeriod = unknownRegistration(Some(-10800000), None, 0)
    val event = buildEvent(0)

    val reg = buildRegistration(Seq.empty)
    val res = PreparedRegistrationPeriodsData.apply(reg, Seq.empty, Seq(event), 1970)

    assert(res.allPeriods == Seq(manufacturePeriod))
  }

  test("with events (one of them with empty date) and no registration periods") {
    val manufacturePeriod = unknownRegistration(Some(-10800000), None, 0)
    val event1 = buildEvent(0)
    val event2 = buildEvent(100)

    val reg = buildRegistration(Seq.empty)
    val res = PreparedRegistrationPeriodsData.apply(reg, Seq.empty, Seq(event1, event2), 1970)

    assert(res.allPeriods == Seq(manufacturePeriod))
  }

  test("with event with empty date and registration periods") {
    val manufacturePeriod = unknownRegistration(Some(-10800000), Some(100), 0)
    val period1 = personRegisterdPeriod(Some(100), Some(200), Some("первичная регистрация"), 0, 1)
    val period2 = personRegisterdPeriod(Some(200), None, Some("постановка на постоянный учет в связи со сверкой"), 1, 2)
    val event = buildEvent(0)

    val reg = buildRegistration(Seq(period1, period2))
    val res = PreparedRegistrationPeriodsData.apply(reg, Seq.empty, Seq(event), 1970)

    assert(res.allPeriods == Seq(manufacturePeriod, period1, period2))
  }

  test("with event with empty date and registration periods one of them without from date") {
    val manufacturePeriod = unknownRegistration(Some(-10800000), Some(0), 0)
    val period1 = personRegisterdPeriod(None, Some(200), Some("первичная регистрация"), 0, 1)
    val period2 = personRegisterdPeriod(Some(200), None, Some("постановка на постоянный учет в связи со сверкой"), 1, 2)
    val event = buildEvent(0)

    val reg = buildRegistration(Seq(period1, period2))
    val res = PreparedRegistrationPeriodsData.apply(reg, Seq.empty, Seq(event), 1970)

    assert(res.allPeriods == Seq(manufacturePeriod, period1, period2))
  }

  test("enrich RegistrationPeriod by some RegistrationAction") {
    val manufacturePeriod = unknownRegistration(Some(-10800000), Some(110), 0)
    val period1 = personRegisterdPeriod(
      from = Some(110),
      to = Some(200),
      lastOperation = Some("первичная регистрация"),
      idx = 0,
      globalIdx = 1,
      orgNameOpt = Some("Roga and Kopyta")
    )
    val period2 = personRegisterdPeriod(
      from = Some(200),
      to = None,
      lastOperation = Some("постановка на постоянный учет в связи со сверкой"),
      idx = 1,
      globalIdx = 2
    )

    val action1 = personRegisterdPeriod(
      from = Some(150),
      to = Some(180),
      lastOperation = Some("постановка на постоянный учет в связи со сверкой"),
      idx = 1,
      globalIdx = 1,
      geoNameOpt = Some("Москва"),
      orgNameOpt = Some("Рога и копыта")
    )
    val action2 = personRegisterdPeriod(
      from = Some(250),
      to = None,
      lastOperation = Some("наложение ограничений"),
      idx = 1,
      globalIdx = 1,
      geoNameOpt = Some("Сызрань"),
      orgNameOpt = None
    )

    val resPeriod1 = personRegisterdPeriod(
      Some(110),
      Some(200),
      Some("первичная регистрация"),
      0,
      1,
      geoNameOpt = Some("Москва"),
      orgNameOpt = Some("Рога и копыта"),
      actions = List(regPeriod2RegAction(action1))
    )
    val resPeriod2 = personRegisterdPeriod(
      Some(200),
      None,
      Some("постановка на постоянный учет в связи со сверкой"),
      1,
      2,
      geoNameOpt = Some("Сызрань"),
      orgNameOpt = None,
      actions = List(regPeriod2RegAction(action2))
    )

    val registrations = buildRegistration(Seq(period1, period2))
    val actions = Seq(action1, action2).map(buildRawPeriod)
    val res = PreparedRegistrationPeriodsData.apply(registrations, actions, Seq.empty, 1970)

    assert(res.allPeriods == Seq(manufacturePeriod, resPeriod1, resPeriod2))
  }

  test("reg actions distributed correctly between periods") {
    val manufacturePeriod = unknownRegistration(Some(-10800000), Some(100), 0)

    val period1 = personRegisterdPeriod(from = Some(100), to = Some(200), lastOperation = None, idx = 0, globalIdx = 1)
    val period2 = personRegisterdPeriod(from = Some(210), to = Some(220), lastOperation = None, idx = 1, globalIdx = 2)
    val period3 = personRegisterdPeriod(from = Some(220), to = None, lastOperation = None, idx = 2, globalIdx = 3)

    val action1 = personRegisterdPeriod(from = Some(100), to = Some(200), lastOperation = None, idx = 0, globalIdx = 0)
    val action2 = personRegisterdPeriod(from = Some(200), to = Some(210), lastOperation = None, idx = 0, globalIdx = 0)
    val action3 = personRegisterdPeriod(from = Some(210), to = Some(215), lastOperation = None, idx = 0, globalIdx = 0)
    val action4 = personRegisterdPeriod(from = Some(215), to = Some(220), lastOperation = None, idx = 0, globalIdx = 0)
    val action5 = personRegisterdPeriod(from = Some(220), to = None, lastOperation = None, idx = 0, globalIdx = 0)
    val resPeriod1 = personRegisterdPeriod(
      from = Some(100),
      to = Some(200),
      lastOperation = Some(""),
      idx = 0,
      globalIdx = 1,
      actions = List(action1, action2).map(regPeriod2RegAction)
    )
    val resPeriod2 = notRegistered(Some(200), Some(210), 2)
    val resPeriod3 = personRegisterdPeriod(
      from = Some(210),
      to = Some(220),
      lastOperation = Some(""),
      idx = 1,
      globalIdx = 3,
      actions = List(action3, action4).map(regPeriod2RegAction)
    )
    val resPeriod4 = personRegisterdPeriod(
      from = Some(220),
      to = None,
      lastOperation = Some(""),
      idx = 2,
      globalIdx = 4,
      actions = List(action5).map(regPeriod2RegAction)
    )

    val registrations = buildRegistration(Seq(period1, period2, period3))
    val actions = Seq(action1, action2, action3, action4, action5).map(buildRawPeriod)
    val res = PreparedRegistrationPeriodsData.apply(registrations, actions, Seq.empty, 1970)

    assert(res.allPeriods == Seq(manufacturePeriod, resPeriod1, resPeriod2, resPeriod3, resPeriod4))
  }

  private def buildEvent(timestamp: Long): PreparedHistoryEntity = {
    val mocked = mock[PreparedHistoryEntity]
    when(mocked.maybeTimestamp).thenReturn(Some(timestamp))
    mocked
  }

  private def buildRegistration(periods: Seq[RegistrationPeriodData]): Option[Seq[RegistrationEvent]] = {
    Some(periods.map(period => buildRawPeriod(period)))
  }

  private def buildRawPeriod(p: RegistrationPeriodData): RegistrationEvent = {
    val strType = p.ownerType match {
      case OwnerType.Type.PERSON => "PERSON"
      case OwnerType.Type.LEGAL => "LEGAL"
      case _ => "UNKNOWN"
    }

    val builder = RegistrationEvent
      .newBuilder()
      .setOwner(strType)

    p.lastOperation.foreach(builder.setOperationType)
    p.geoNameOpt.foreach(geoName => builder.setGeo(VinHistory.Geo.newBuilder.setCityName(geoName).build))
    p.orgNameOpt.foreach(orgName => builder.setOwnerInfo(VinHistory.Owner.newBuilder.setName(orgName).build))
    p.from.map(builder.setFrom)

    p.to.foreach(builder.setTo)
    builder.build()
  }

  private def personRegisterdPeriod(
      from: Option[Long],
      to: Option[Long],
      lastOperation: Option[String],
      idx: Int,
      globalIdx: Int,
      geoNameOpt: Option[String] = None,
      orgNameOpt: Option[String] = None,
      actions: List[RegActionData] = List.empty) = {
    RegistrationPeriodData(
      orgNameOpt.map(_ => OwnerType.Type.LEGAL).getOrElse(OwnerType.Type.PERSON),
      RegistrationStatus.REGISTERED,
      from,
      to,
      lastOperation,
      geoNameOpt,
      orgNameOpt,
      Some(idx),
      actions,
      globalIdx
    )
  }

  private def legalRegisteredPeriod(
      from: Option[Long],
      to: Option[Long],
      lastOperation: Option[String],
      idx: Int,
      globalIdx: Int,
      geoNameOpt: Option[String] = None,
      orgNameOpt: Option[String] = None) = {
    RegistrationPeriodData(
      OwnerType.Type.LEGAL,
      RegistrationStatus.REGISTERED,
      from,
      to,
      lastOperation,
      geoNameOpt,
      orgNameOpt,
      Some(idx),
      List.empty,
      globalIdx
    )
  }

  private def notRegistered(from: Option[Long], to: Option[Long], globalIndex: Int) = {
    registrationWithStatus(from, to, None, globalIndex, RegistrationStatus.NOT_REGISTERED)
  }

  private def unknownRegistration(from: Option[Long], to: Option[Long], globalIndex: Int) = {
    registrationWithStatus(from, to, None, globalIndex, RegistrationStatus.UNKNOWN_REGISTRATION_STATUS)
  }

  private def registrationWithStatus(
      from: Option[Long],
      to: Option[Long],
      lastOperation: Option[String],
      globalIndex: Int,
      registrationStatus: RegistrationStatus) = {
    RegistrationPeriodData(
      OwnerType.Type.UNKNOWN_OWNER_TYPE,
      registrationStatus,
      from,
      to,
      lastOperation,
      None,
      None,
      None,
      List.empty,
      globalIndex
    )
  }

  private def regPeriod2RegAction(period: RegistrationPeriodData): RegActionData = {
    RegActionData(
      dateFrom = period.from.get,
      dateTo = period.to.getOrElse(0L),
      region = period.geoNameOpt,
      operation = period.lastOperation.getOrElse(""),
      ownerType = Some(period.ownerType.name),
      ownerName = period.orgNameOpt
    )
  }

  private def isFirstPeriodUnknown(data: PreparedRegistrationPeriodsData): Boolean = {
    data.allPeriods.headOption.exists(_.registeredStatus == RegistrationStatus.UNKNOWN_REGISTRATION_STATUS)
  }
}
