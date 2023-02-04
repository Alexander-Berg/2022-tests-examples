package ru.yandex.auto.vin.decoder.manager.vin.autocode

import auto.carfax.common.utils.misc.ResourceUtils
import org.joda.time.format.DateTimeFormat
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.vin.AutocodeRegistrationActions
import ru.yandex.auto.vin.decoder.partners.autocode.model.{AutocodeReportResponse, RegAction}
import ru.yandex.auto.vin.decoder.proto.VinHistory.Taxi.LicenseStatus
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory.Status
import ru.yandex.auto.vin.decoder.raw.autocode.AutocodeReportResponseRaw
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator.MarkModelResult

class AutocodeVinRawStorageUpdateGeneratorTest extends AnyFunSuite {

  test("convert non empty taxi") {
    val vin = VinCode.apply("Z94C241BBJR056757")

    val raw = ResourceUtils.getStringFromResources("/autocode/taxi/autoru_taxi_report_Z94C241BBJR05675.json")
    val rawStatus = "200"
    val map = Map(
      (Option("KIA") -> Option("RIO")) -> MarkModelResult("KIA", "RIO", "KIA RIO", false),
      (Option("KIA") -> Option("SORENTO")) -> MarkModelResult("KIA", "SORENTO", "KIA SORENTO", false)
    )

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeRawStorageUpdateGenerator.updateTaxi(vin, response, map)

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getTaxiStatus === Status.OK)
    assert(prepared.getEventType === EventType.AUTOCODE_TAXI)
    assert(prepared.getTaxiCount === 2)

    assert(prepared.getTaxi(0).getMark === "KIA")
    assert(prepared.getTaxi(0).getModel === "RIO")
    assert(prepared.getTaxi(0).getLicense === "0258387")
    assert(prepared.getTaxi(0).getLicenseStatus === LicenseStatus.ACTIVE)
    assert(prepared.getTaxi(0).getFrom === 1553634000000L)
    assert(prepared.getTaxi(0).getTo === 1711400400000L)

    assert(prepared.getTaxi(1).getMark === "KIA")
    assert(prepared.getTaxi(1).getModel === "SORENTO")
    assert(prepared.getTaxi(1).getLicense === "0206933")
    assert(prepared.getTaxi(1).getLicenseStatus === LicenseStatus.ANNULLED)
    assert(prepared.getTaxi(1).getFrom === 1521579600000L)
    assert(prepared.getTaxi(1).getTo === 1679259600000L)
  }

  test("convert taxi with error") {
    val vin = VinCode.apply("JTJCV00W704001364")

    val raw = ResourceUtils.getStringFromResources("/autocode/taxi/autoru_taxi_report_JTJCV00W704001364.json")
    val rawStatus = "200"

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeRawStorageUpdateGenerator.updateTaxi(vin, response, Map.empty)

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getTaxiStatus === Status.ERROR)
    assert(prepared.getEventType === EventType.AUTOCODE_TAXI)
    assert(prepared.getTaxiCount === 0)
  }

  test("convert empty taxi") {
    val vin = VinCode.apply("XTH430100R0757916")

    val raw = ResourceUtils.getStringFromResources("/autocode/taxi/autoru_taxi_report_XTH430100R0757916.json")
    val rawStatus = "200"

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeRawStorageUpdateGenerator.updateTaxi(vin, response, Map.empty)

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getTaxiStatus === Status.OK)
    assert(prepared.getEventType === EventType.AUTOCODE_TAXI)
    assert(prepared.getTaxiCount === 0)
  }

  test("convert mileages report") {
    val vin = VinCode.apply("JTJCV00W704001364")

    val raw = ResourceUtils.getStringFromResources("/autocode/eaisto/autoru_mileages_report_JTJCV00W704001364.json")
    val rawStatus = "200"

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeVinRawStorageUpdateGenerator.updateForTechInspections(vin, response)

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getMileagesStatus === Status.OK)
    assert(prepared.getStatuses.getDiagnosticCardsStatus === Status.OK)
    assert(prepared.getEventType === EventType.AUTOCODE_MILEAGE)

    assert(prepared.getMileageCount === 1)
    assert(prepared.getMileage(0).getValue === 40000)
    assert(prepared.getMileage(0).getDate === 1577221200000L)
    assert(prepared.getMileage(0).getCity === "")

    assert(prepared.getDiagnosticCardsCount === 1)
    assert(prepared.getDiagnosticCards(0).getFrom === 1577221200000L)
    assert(prepared.getDiagnosticCards(0).getTo === 1640379600000L)
    assert(prepared.getDiagnosticCards(0).getType === "Диагностическая карта")
    assert(prepared.getDiagnosticCards(0).getRegNum === "X255XT56")
  }

  test("convert empty mileages report") {
    val vin = VinCode.apply("JTJCV00W704001364")

    val raw = ResourceUtils.getStringFromResources("/autocode/eaisto/autoru_mileages_report_JTJCV00W704001364.json")
    val rawStatus = "200"

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeVinRawStorageUpdateGenerator.updateForTechInspections(vin, response)

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getMileagesStatus === Status.OK)
    assert(prepared.getStatuses.getDiagnosticCardsStatus === Status.OK)
    assert(prepared.getEventType === EventType.AUTOCODE_MILEAGE)

    assert(prepared.getMileageCount === 1)
    assert(prepared.getMileage(0).getValue === 40000)
    assert(prepared.getMileage(0).getDate === 1577221200000L)
    assert(prepared.getMileage(0).getCity === "")

    assert(prepared.getDiagnosticCardsCount === 1)
    assert(prepared.getDiagnosticCards(0).getFrom === 1577221200000L)
    assert(prepared.getDiagnosticCards(0).getTo === 1640379600000L)
    assert(prepared.getDiagnosticCards(0).getType === "Диагностическая карта")
    assert(prepared.getDiagnosticCards(0).getRegNum === "X255XT56")
  }

  test("convert main update") {
    val vin = VinCode.apply("JMBSTCY4A8U001750")

    val raw =
      ResourceUtils.getStringFromResources("/autocode/main_update/autoru_main_update_report_JMBSTCY4A8U001750.json")
    val rawStatus = "200"

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeVinRawStorageUpdateGenerator.updateForMainUpdate(vin, response)

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === Status.OK)
    assert(prepared.getStatuses.getWantedStatus === Status.OK)
    assert(prepared.getStatuses.getConstraintsStatus === Status.OK)
    assert(prepared.getEventType === EventType.AUTOCODE_MAIN_UPDATE)

    assert(prepared.getAccidentsCount === 0)
    assert(prepared.getWantedCount === 0)
    assert(prepared.getConstraintsCount === 0)

    assert(prepared.getRegistration.getTimestamp === response.model.getUpdatedMillis)
    assert(prepared.getRegistration.getPeriodsCount === 3)
  }

  test("convert main update with errors") {
    val vin = VinCode.apply("TMBGL41U332769860")

    val raw =
      ResourceUtils.getStringFromResources("/autocode/main_update/autoru_main_update_report_TMBGL41U332769860.json")
    val rawStatus = "200"

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeVinRawStorageUpdateGenerator.updateForMainUpdate(vin, response)

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === Status.ERROR)
    assert(prepared.getStatuses.getWantedStatus === Status.ERROR)
    assert(prepared.getStatuses.getConstraintsStatus === Status.ERROR)
    assert(prepared.getEventType === EventType.AUTOCODE_MAIN_UPDATE)

    assert(prepared.getAccidentsCount === 0)
    assert(prepared.getWantedCount === 0)
    assert(prepared.getConstraintsCount === 0)

    assert(prepared.getRegistration.getTimestamp === response.model.getUpdatedMillis)
    assert(prepared.getRegistration.getPeriodsCount === 5)

    assert(prepared.getRegistration.getPeriods(0).getFrom === 1350417600000L)
    assert(prepared.getRegistration.getPeriods(0).getTo === 0L)
    assert(prepared.getRegistration.getPeriods(0).getOwner === "PERSON")
  }

  test("convert main with errors") {
    val vin = VinCode.apply("SALLMAMH4CA385107")

    val raw =
      ResourceUtils.getStringFromResources("/autocode/main/autoru_main_report_SALLMAMH4CA385107.json")
    val rawStatus = "200"

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeVinRawStorageUpdateGenerator.updateForMain(
      vin,
      response,
      MarkModelResult("LAND ROVER", "RANGE ROVER", "ЛЕНД РОВЕР РЕНДЖ РОВЕР", false),
      List.empty
    )

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === Status.ERROR)
    assert(prepared.getStatuses.getWantedStatus === Status.ERROR)
    assert(prepared.getStatuses.getConstraintsStatus === Status.ERROR)
    assert(prepared.getEventType === EventType.AUTOCODE_MAIN)

    assert(prepared.getAccidentsCount === 0)
    assert(prepared.getWantedCount === 0)
    assert(prepared.getConstraintsCount === 0)

    assert(prepared.getRegistration.getTimestamp === response.model.getUpdatedMillis)
    assert(prepared.getRegistration.getPeriodsCount === 3)

    assert(prepared.getRegistration.getPeriods(0).getFrom === 1470517200000L)
    assert(prepared.getRegistration.getPeriods(0).getTo === 0L)
    assert(prepared.getRegistration.getPeriods(0).getOwner === "PERSON")

    assert(prepared.getRegistration.getMark === "LAND ROVER")
    assert(prepared.getRegistration.getModel === "RANGE ROVER")
    assert(prepared.getRegistration.getRawMarkModel === "ЛЕНД РОВЕР РЕНДЖ РОВЕР")
    assert(prepared.getRegistration.getColor === "Темно-Коричневый")
  }

  test("convert main") {
    val vin = VinCode.apply("Z8NFBAJ11ES017019")

    val raw =
      ResourceUtils.getStringFromResources("/autocode/main/autoru_main_report_Z8NFBAJ11ES017019.json")
    val rawStatus = "200"

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeVinRawStorageUpdateGenerator.updateForMain(
      vin,
      response,
      MarkModelResult("NISSAN", "QASHQAI", "НИССАН КАШКАЙ", false),
      List.empty
    )

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === Status.OK)
    assert(prepared.getStatuses.getWantedStatus === Status.OK)
    assert(prepared.getStatuses.getConstraintsStatus === Status.OK)
    assert(prepared.getEventType === EventType.AUTOCODE_MAIN)

    assert(prepared.getAccidentsCount === 1)
    assert(prepared.getWantedCount === 0)
    assert(prepared.getConstraintsCount === 0)

    assert(prepared.getRegistration.getTimestamp === response.model.getUpdatedMillis)
    assert(prepared.getRegistration.getPeriodsCount === 1)

    assert(prepared.getRegistration.getPeriods(0).getFrom === 1476392400000L)
    assert(prepared.getRegistration.getPeriods(0).getTo === 0L)
    assert(prepared.getRegistration.getPeriods(0).getOwner === "PERSON")

    assert(prepared.getRegistration.getMark === "NISSAN")
    assert(prepared.getRegistration.getModel === "QASHQAI")
    assert(prepared.getRegistration.getRawMarkModel === "НИССАН КАШКАЙ")
  }

  test("covert main: unexisting vin") {
    val vin = VinCode.apply("NSP120-2029533")

    val raw =
      ResourceUtils.getStringFromResources(
        "/autocode/main/autoru_main_report_eyJ0eXBlIjoiQk9EWSIsImJvZHkiOiJOU1AxMjAtMjAyOTUzMyJ9.json"
      )
    val rawStatus = "200"

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeVinRawStorageUpdateGenerator.updateForUnexistingVinInMain(vin, response, EventType.AUTOCODE_MAIN)

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === Status.ERROR)
    assert(prepared.getStatuses.getWantedStatus === Status.ERROR)
    assert(prepared.getStatuses.getConstraintsStatus === Status.ERROR)
    assert(prepared.getEventType === EventType.AUTOCODE_MAIN)

    assert(res.prepared.data.getRegistration.getMark === "UNKNOWN_MARK")
    assert(res.prepared.data.getRegistration.getModel === "UNKNOWN_MODEL")
    assert(res.prepared.data.getRegistration.getTimestamp === response.model.getUpdatedMillis)
  }

  test("convert main with last operation") {
    val vin = VinCode.apply("WBA3Y510X0GZ92038")

    val raw =
      ResourceUtils.getStringFromResources("/autocode/main/autoru_main_report_WBA3Y510X0GZ92038@autoru.json")
    val rawStatus = "200"

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val res = AutocodeVinRawStorageUpdateGenerator.updateForMainUpdate(vin, response)

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data
    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === Status.OK)
    assert(prepared.getStatuses.getWantedStatus === Status.OK)
    assert(prepared.getStatuses.getConstraintsStatus === Status.OK)
    assert(prepared.getEventType === EventType.AUTOCODE_MAIN_UPDATE)

    assert(prepared.getAccidentsCount === 1)
    assert(prepared.getAccidents(0).getYear === 2015)
    assert(prepared.getAccidents(0).getRegion === "Москва")
    assert(prepared.getAccidents(0).getMark === "Bmw")
    assert(prepared.getAccidents(0).getDamagePoints(0) === 110)
    assert(prepared.getWantedCount === 0)
    assert(prepared.getConstraintsCount === 0)

    assert(prepared.getRegistration.getTimestamp === response.model.getUpdatedMillis)
    assert(prepared.getRegistration.getPeriodsCount === 2)
    assert(
      prepared.getRegistration
        .getPeriods(0)
        .getOperationType === "Регистрация ранее зарегистрированных в регистрирующих органах"
    )
    assert(prepared.getRegistration.getPeriods(1).getOperationType === "Прекращение регистрации в том числе")

  }

  private val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  private def convertRegAction(
      regActions: List[RegAction]): List[AutocodeRegistrationActions] = {
    regActions.map { regAction =>
      AutocodeRegistrationActions(
        dateFormat.parseDateTime(regAction.from).getMillis,
        regAction.to.map(dateFormat.parseDateTime(_).getMillis),
        Option(regAction.owner),
        regAction.typeOperation,
        regAction.typeOperationId,
        regAction.geoOpt,
        None,
        None,
        None,
        regAction.orgName,
        regAction.orgInn
      )
    }
  }

  test("convert autoru_full_set_of_documents_for_vehicle") {

    val vin = VinCode.apply("XTA210600N2822640")

    val raw =
      ResourceUtils.getStringFromResources(
        "/autocode/full_set_of_documents_for_vehicle/autoru_full_set_of_documents_for_vehicle_XTA210600N2822640@autoru.json"
      )
    val rawStatus = "200"

    val response = AutocodeReportResponseRaw(raw, rawStatus, Json.parse(raw).as[AutocodeReportResponse])

    val mm = MarkModelResult("Chevrolet", "Kalos / Aveo / Sonic", "Kalos / Aveo / Sonic", unclear = false)

    val autocodeRegistrationActions =
      convertRegAction(response.model.data.content.map(_.regActions).getOrElse(List.empty))

    val res =
      AutocodeVinRawStorageUpdateGenerator.updateFullSetOfDocuments(vin, mm, response, autocodeRegistrationActions)

    assert(res.identifier === vin)
    assert(res.meta.timestampCreate === response.model.getUpdatedMillis)
    assert(res.raw.data === raw)
    assert(res.raw.status === rawStatus)

    val prepared = res.prepared.data

    assert(prepared.getStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getRegistrationStatus === Status.OK)
    assert(prepared.getStatuses.getAccidentsStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getWantedStatus === Status.UNKNOWN)
    assert(prepared.getStatuses.getConstraintsStatus === Status.UNKNOWN)
    assert(prepared.getEventType === EventType.AUTOCODE_FULL_SET_OF_DOCUMENTS)

    assert(prepared.getRegistration.getTimestamp === response.model.getUpdatedMillis)
    assert(prepared.getRegistration.getPts === "77КХ151968")
    assert(
      prepared.getRegistration
        .getRegActions(0)
        .getOperationType === "В связи с утилизацией"
    )
    assert(
      prepared.getRegistration.getRegActions(1).getOperationType === "Регистрация ТС, прибывших из других регионов РФ"
    )
    assert(prepared.getRegistration.getRegActionsList.size === 5)
    assert(prepared.getVehicleIdentifiers.getHasDuplicatePts.getValue === false)
    assert(prepared.getVehicleIdentifiers.getStsDataReceive === 1336176000000L)
    assert(prepared.getVehicleIdentifiers.getPtsDataReceive === 1153785600000L)
    assert(prepared.getRegistration.getWasUtilization.getValue === true)
    assert(prepared.getVehicleIdentifiers.getWasModificated.getValue === false)
    assert(prepared.getRegistration.getDateOfUtilizationsList.size === 1)
    assert(prepared.getRegistration.getDateOfUtilizationsList.get(0) === 1447707600000L)

  }

}
