package ru.yandex.auto.vin.decoder.partners.autocode

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.{LicensePlate, VinCode}
import ru.yandex.auto.vin.decoder.partners.autocode.model.AutocodeReportResponse
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageModel.{MetaData, PreparedData, RawData}
import ru.yandex.auto.vin.decoder.ydb.raw.model.RowModel
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class MockedAutocodeClientIntTest extends AnyFunSuite with MockitoSupport {

  implicit val t: Traced = Traced.empty
  private val token = MockedAutocodeClient.UnlimitedToken
  implicit val trigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  private val rawStoageManager = mock[RawStorageManager[VinCode]]
  private val client = new MockedAutocodeClient(rawStoageManager)
  private val vin = VinCode("WBAUE11040E238571")
  private val lp = LicensePlate("T700TT62")
  private val reportId = ""

  test("login return unlimited token") {
    val res = client.login(123L).await
    assert(res == MockedAutocodeClient.UnlimitedToken)
  }

  test("get tech report return not found json") {
    val (code, json) = client.getResult(token, AutocodeRequest(reportId, vin, AutocodeReportType.Tech)).await

    assert(code == 200)
    val model = json.as[AutocodeReportResponse]

    assert(model.data.query.body == vin.toString)
    assert(model.data.stOk == 1)
    assert(model.data.stWait == 0)
    assert(model.isEmpty)
    assert(Math.abs(model.getUpdatedMillis - System.currentTimeMillis()) <= 10.seconds.toMillis)
  }

  test("get not found response for identifiers if there are no old responses") {
    when(rawStoageManager.getAllByIdentifierAndSource(?, ?)(?)).thenReturn(
      Future.successful(Seq.empty)
    )

    val (code, json) = client.getResult(token, AutocodeRequest(reportId, lp, AutocodeReportType.Identifiers)).await
    val model = json.as[AutocodeReportResponse]

    assert(code == 200)
    assert(model.data.query.body == lp.toString)
    assert(model.data.stOk == 1)
    assert(model.data.stWait == 0)
    assert(model.data.content.get.identifiers.isEmpty)
    assert(Math.abs(model.getUpdatedMillis - System.currentTimeMillis()) <= 10.seconds.toMillis)
  }

  test("get old response for identifiers if there are old responses") {
    when(rawStoageManager.getAllByIdentifierAndSource[LicensePlate](?, ?)(?)).thenReturn(
      Future.successful(
        Seq(
          RowModel(
            lp,
            RawData("{}", "200"),
            PreparedData(VinInfoHistory.newBuilder().build()),
            MetaData("", EventType.AUTOCODE_IDENTIFIERS, "", 123L, 123L, 123L)
          )
        )
      )
    )

    val (code, json) = client.getResult(token, AutocodeRequest(reportId, lp, AutocodeReportType.Identifiers)).await

    assert(code == 200)
    assert(json.toString() == "{}")
  }

}
