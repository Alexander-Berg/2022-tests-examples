package ru.yandex.auto.vin.decoder.manager.vin.autocode

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.vin.adaperio.VinAdaperioManager
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.vin.Event
import ru.yandex.auto.vin.decoder.partners.adaperio.model.AdaperioReportResponse
import ru.yandex.auto.vin.decoder.partners.adaperio.model.report.block.MvdRestrictionBlock
import ru.yandex.auto.vin.decoder.partners.adaperio.model.report.{
  AdaperioReportMain,
  AdaperioReportMainUpdate,
  AdaperioReportTechInspections
}
import ru.yandex.auto.vin.decoder.proto.VinHistory.{RegistrationEvent, VinInfoHistory}
import ru.yandex.auto.vin.decoder.raw.adaperio.AdaperioRawModel
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator.MarkModelResult
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.ListHasAsScala

class VinAdaperioManagerTest extends AnyFunSuite with MockitoSupport {

  private val unificator = mock[Unificator]
  private val rawStorageManager = mock[RawStorageManager[VinCode]]
  implicit val t: Traced = Traced.empty

  val manager = new VinAdaperioManager(unificator, rawStorageManager)

  test("adaperio main update") {
    val vin = VinCode.apply("JMBXTGF2WDZ003425")

    val raw = ResourceUtils.getStringFromResources("/adaperio/main_update/main_update_JMBXTGF2WDZ003425.json")
    val rawStatus = "200"

    val model = Json.parse(raw).as[AdaperioReportResponse].report.as[AdaperioReportMainUpdate]
    val rawModel =
      AdaperioRawModel(raw, rawStatus, vin, model)

    val row = manager.updateForMainUpdate(rawModel, 123L).await

    assert(row.identifier === vin)
    assert(row.meta.source === EventType.ADAPERIO_MAIN_UPDATE)
    assert(row.meta.timestampCreate === 123L)
    assert(row.meta.timestampUpdate === 123L)

    assert(row.raw.status === rawStatus)
    assert(row.raw.data === raw)

    val prepared = row.prepared.data
    assert(prepared.getEventType === EventType.ADAPERIO_MAIN_UPDATE)
    assert(prepared.getVin === vin.toString)
    assert(prepared.getGroupId === "")

    assert(prepared.getRegistration.getPeriodsCount === 1)
    assert(prepared.getRegistration.getTimestamp === 123L)
    assert(prepared.getAccidentsCount === 2)
    assert(prepared.getWantedCount === 0)
    assert(prepared.getConstraintsCount === 1)
    val registrationEvent = prepared.getRegistration.getPeriodsList.asScala
      .sorted(Ordering[RegistrationEvent]((a, b) => a.getOperationTypeId.compare(b.getOperationTypeId)))
      .head
    assert(registrationEvent.getOperationTypeId == "03")
    assert(registrationEvent.getOperationType == "Смена владельца")

    assert(prepared.getStatus === VinInfoHistory.Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getConstraintsStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getWantedStatus === VinInfoHistory.Status.OK)

  }

  test("adaperio main: empty mark and model in response") {
    when(unificator.unifyHeadOption(?)(?)).thenReturn(Future.successful(None))

    val vin = VinCode.apply("JMBXTGF2WDZ003425")

    val raw = ResourceUtils.getStringFromResources("/adaperio/main/empty_mark.json")
    val rawStatus = "200"

    val model = Json.parse(raw).as[AdaperioReportResponse].report.as[AdaperioReportMain]
    val rawModel =
      AdaperioRawModel(raw, rawStatus, vin, model)

    val row = manager.updateForMain(EventType.ADAPERIO_MAIN, rawModel, 123L).await

    assert(row.identifier === vin)
    assert(row.meta.source === EventType.ADAPERIO_MAIN)
    assert(row.meta.timestampCreate === 123L)
    assert(row.meta.timestampUpdate === 123L)

    assert(row.raw.status === rawStatus)
    assert(row.raw.data === raw)

    val prepared = row.prepared.data
    assert(prepared.getEventType === EventType.ADAPERIO_MAIN)
    assert(prepared.getVin === vin.toString)
    assert(prepared.getGroupId === "")

    assert(prepared.getRegistration.getMark == "")
    assert(prepared.getRegistration.getModel == "")
    assert(prepared.getRegistration.getRawMarkModel == "")

    assert(prepared.getRegistration.getPeriodsCount === 7)
    assert(prepared.getRegistration.getTimestamp === 123L)
    assert(prepared.getAccidentsCount === 1)
    assert(prepared.getWantedCount === 0)
    assert(prepared.getConstraintsCount === 0)

    assert(prepared.getStatus === VinInfoHistory.Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getConstraintsStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getWantedStatus === VinInfoHistory.Status.OK)
  }

  test("adaperio main: not registered in gibdd") {
    val vin = VinCode.apply("JMBXTGF2WDZ003425")

    val raw = ResourceUtils.getStringFromResources("/adaperio/main/not_found.json")
    val rawStatus = "200"

    val model = Json.parse(raw).as[AdaperioReportResponse].report.as[AdaperioReportMain]
    val rawModel =
      AdaperioRawModel(raw, rawStatus, vin, model)

    val row = manager.updateForMain(EventType.ADAPERIO_MAIN, rawModel, 123L).await

    assert(row.identifier === vin)
    assert(row.meta.source === EventType.ADAPERIO_MAIN)
    assert(row.meta.timestampCreate === 123L)
    assert(row.meta.timestampUpdate === 123L)

    assert(row.raw.status === rawStatus)
    assert(row.raw.data === raw)

    val prepared = row.prepared.data
    assert(prepared.getEventType === EventType.ADAPERIO_MAIN)
    assert(prepared.getVin === vin.toString)
    assert(prepared.getGroupId === "")

    assert(prepared.getRegistration.getMark == Event.NoMark)
    assert(prepared.getRegistration.getModel == Event.NoModel)
    assert(prepared.getRegistration.getRawMarkModel == "")

    assert(prepared.getRegistration.getPeriodsCount === 0)
    assert(prepared.getRegistration.getTimestamp === 123L)
    assert(prepared.getAccidentsCount === 0)
    assert(prepared.getWantedCount === 0)
    assert(prepared.getConstraintsCount === 0)

    assert(prepared.getStatus === VinInfoHistory.Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getConstraintsStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getWantedStatus === VinInfoHistory.Status.OK)
  }

  test("adaperio main update with errors") {
    val vin = VinCode.apply("JMBXTGF2WDZ003425")

    val raw = ResourceUtils.getStringFromResources("/adaperio/main_update/main_update_errors_JMBXTGF2WDZ003425.json")
    val rawStatus = "200"

    val model = Json.parse(raw).as[AdaperioReportResponse].report.as[AdaperioReportMainUpdate]
    val rawModel =
      AdaperioRawModel(raw, rawStatus, vin, model)

    val row = manager.updateForMainUpdate(rawModel, 123L).await

    assert(row.identifier === vin)
    assert(row.meta.source === EventType.ADAPERIO_MAIN_UPDATE)
    assert(row.meta.timestampCreate === 123L)
    assert(row.meta.timestampUpdate === 123L)

    val prepared = row.prepared.data
    assert(prepared.getStatus === VinInfoHistory.Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === VinInfoHistory.Status.ERROR)
    assert(prepared.getStatuses.getConstraintsStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getWantedStatus === VinInfoHistory.Status.ERROR)

  }

  test("adaperio main") {
    val vin = VinCode.apply("XTA21124060371510")

    val raw = ResourceUtils.getStringFromResources("/adaperio/main/main_XTA21124060371510.json")
    val rawStatus = "200"

    when(unificator.unifyHeadOption(?)(?)).thenReturn(
      Future.successful(
        Some(
          MarkModelResult(
            mark = "VAZ",
            model = "2114",
            raw = "ВАЗ 21124",
            unclear = false
          )
        )
      )
    )

    val model = Json.parse(raw).as[AdaperioReportResponse].report.as[AdaperioReportMain]
    val rawModel =
      AdaperioRawModel(raw, rawStatus, vin, model)

    val row = manager.updateForMain(EventType.ADAPERIO_MAIN, rawModel, 123L).await

    assert(row.identifier === vin)
    assert(row.meta.source === EventType.ADAPERIO_MAIN)
    assert(row.meta.timestampCreate === 123L)
    assert(row.meta.timestampUpdate === 123L)

    assert(row.raw.status === rawStatus)
    assert(row.raw.data === raw)

    val prepared = row.prepared.data
    assert(prepared.getEventType === EventType.ADAPERIO_MAIN)
    assert(prepared.getVin === vin.toString)
    assert(prepared.getGroupId === "")

    assert(prepared.getRegistration.getMark === "VAZ")
    assert(prepared.getRegistration.getModel === "2114")
    assert(prepared.getRegistration.getPeriodsCount === 6)
    assert(prepared.getRegistration.getTimestamp === 123L)
    assert(prepared.getAccidentsCount === 1)
    assert(prepared.getWantedCount === 0)
    assert(prepared.getConstraintsCount === 1)
    val registrationEvent = prepared.getRegistration.getPeriodsList.asScala
      .sorted(Ordering[RegistrationEvent]((a, b) => a.getOperationTypeId.compare(b.getOperationTypeId)))
      .head
    assert(registrationEvent.getOperationTypeId == "03")
    assert(registrationEvent.getOperationType == "Смена владельца")

    assert(prepared.getStatus === VinInfoHistory.Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getConstraintsStatus === VinInfoHistory.Status.OK)
    assert(prepared.getStatuses.getWantedStatus === VinInfoHistory.Status.OK)
  }

  test("mileages") {
    val vin = VinCode.apply("XTA21124060371510")

    val raw = ResourceUtils.getStringFromResources("/adaperio/mileages/mileages_XTA21140064164283.json")
    val rawStatus = "200"

    val model = Json.parse(raw).as[AdaperioReportResponse].report.as[AdaperioReportTechInspections]

    val rawModel = AdaperioRawModel(raw, rawStatus, vin, model)

    val row = manager.updateTechInspectionsReport(rawModel, 123L).await

    assert(row.identifier === vin)
    assert(row.meta.source === EventType.ADAPERIO_MILEAGE)
    assert(row.meta.timestampCreate === 123L)
    assert(row.meta.timestampUpdate === 123L)

    assert(row.raw.status === rawStatus)
    assert(row.raw.data === raw)

    val prepared = row.prepared.data

    assert(prepared.getEventType === EventType.ADAPERIO_MILEAGE)
    assert(prepared.getVin === vin.toString)
    assert(prepared.getGroupId === "")

    assert(prepared.getMileageCount == 1)
    assert(prepared.getMileage(0).getValue == 182470)
    assert(prepared.getMileage(0).getDate == 1585256400000L)

    assert(prepared.getDiagnosticCardsCount == 3)
    assert(prepared.getDiagnosticCards(0).getNumber == "012170071801782")
    assert(prepared.getDiagnosticCards(0).getFrom == 1536872400000L)
    assert(prepared.getDiagnosticCards(0).getTo == 1568494800000L)
    assert(prepared.getDiagnosticCards(0).getRegNum == "А873МТ73")
  }

  test("good MvdRestriction") {
    val block =
      """{"MvdRestrictions":[{"RegistrationDate":"2019-10-16T00:00:00","RestrictionType":"Запрет на регистрационные действия","WhoImposed":"Судебный пристав","Region":"Краснодарский край","Document":"Документ 129312764/2368 от 16.10.2019, Чукавин Никита Валерьевич, СПИ 3681046217943, ИП 67255/19/23068-ИП от 11.10.2019"}],"StatusCode":0,"Status":"OK"}"""
    assert(Json.parse(block).as[MvdRestrictionBlock].mvdRestrictions.size == 1)
  }
}
