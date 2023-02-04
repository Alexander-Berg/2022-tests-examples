package ru.yandex.auto.vin.decoder.manager

import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito.verify
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.auto.vin.decoder.model.IdentifierGenerators.{LicensePlateGen, VinCodeGen}
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.partners.scrapinghub.rsa.ScrapinghubRsaReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.service.licenseplate.LicensePlateUpdateService
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class IdentifiersManagerTest extends AsyncFunSuite with Matchers with MockitoSupport {
  implicit val t: Traced = Traced.empty
  val vinCodesEventHistoryManager = mock[VinCodesEventHistoryManager]
  val lpUpdateService = mock[LicensePlateUpdateService]
  val identifiersManager = new IdentifiersManager(vinCodesEventHistoryManager, lpUpdateService)

  val vin = VinCodeGen.sample.get
  val lp = LicensePlateGen.sample.get
  val timestamp = System.currentTimeMillis()

  test("connect and create state") {
    when(vinCodesEventHistoryManager.upsertHistory(eq(lp), eq(vin), eq(timestamp), eq(true))(?))
      .thenReturn(Future.successful(true))
    when(lpUpdateService.getState(eq(lp), eq(true))(?)).thenReturn(None)
    when(lpUpdateService.insertUpdate(eq(lp), ?)(?)).thenReturn(true)

    identifiersManager.connect(lp, vin, timestamp, trusted = true).map { _ =>
      verify(lpUpdateService).insertUpdate(eq(lp), ?)(?)
      succeed
    }
  }

  test("check in progress = true when autocode identifiers is in progress") {
    val stateBuilder = CompoundState.newBuilder()
    stateBuilder.getAutocodeStateBuilder
      .getReportBuilder(AutocodeReportType.Identifiers)
      .setShouldProcess(true)

    IdentifiersManager.isVinStateInProgress(stateBuilder.build()) shouldBe true
  }

  test("check in progress = true when current insurances is in progress") {
    pending
    val stateBuilder = CompoundState.newBuilder()
    stateBuilder.getScrapinghubRsaStateBuilder
      .getReportBuilder(ScrapinghubRsaReportType.CurrentInsurances)
      .setShouldProcess(true)

    IdentifiersManager.isVinStateInProgress(stateBuilder.build()) shouldBe true
  }

  test("check in progress = true when autocode identifiers and current insurances are in progress") {
    val stateBuilder = CompoundState.newBuilder()
    stateBuilder.getAutocodeStateBuilder
      .getReportBuilder(AutocodeReportType.Identifiers)
      .setShouldProcess(true)
    stateBuilder.getScrapinghubRsaStateBuilder
      .getReportBuilder(ScrapinghubRsaReportType.CurrentInsurances)
      .setShouldProcess(true)

    IdentifiersManager.isVinStateInProgress(stateBuilder.build()) shouldBe true
  }

  test("check in progress = false when neither autocode identifiers nor current identifiers is in progress") {
    val stateBuilder = CompoundState.newBuilder()
    stateBuilder.getAutocodeStateBuilder
      .getReportBuilder(AutocodeReportType.Identifiers)
      .setShouldProcess(false)
    stateBuilder.getScrapinghubRsaStateBuilder
      .getReportBuilder(ScrapinghubRsaReportType.CurrentInsurances)
      .setShouldProcess(false)

    IdentifiersManager.isVinStateInProgress(stateBuilder.build()) shouldBe false
  }
}
