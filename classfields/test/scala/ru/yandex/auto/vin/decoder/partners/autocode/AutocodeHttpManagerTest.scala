package ru.yandex.auto.vin.decoder.partners.autocode

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AsyncFunSuite
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.autocode.model.Token
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.vertis.mockito.MockitoSupport._

import java.time.LocalDateTime
import scala.concurrent.Future

class AutocodeHttpManagerTest extends AsyncFunSuite {
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val t: Traced = Traced.empty

  val client: DefaultAutocodeClient = mock[DefaultAutocodeClient]
  val httpManager = new AutocodeHttpManager(client)

  when(client.login(?)(?)).thenReturn(Future.successful(Token("", LocalDateTime.now(), 12)))

  test("mileages and diagnostic cards deserialization") {
    val json = Json.parse(
      """{
        |	"state": "ok",
        |	"size": 1,
        |	"stamp": "2019-02-28T11:23:37.837Z",
        |	"data": [
        |		{
        |			"domain_uid": "autoru",
        |			"report_type_uid": "autoru_only_eaisto_report_2019@autoru",
        |			"vehicle_id": "XTA211020Y0208819",
        |			"query": {
        |				"type": "VIN",
        |				"body": "XTA211020Y0208819"
        |			},
        |			"progress_ok": 3,
        |			"progress_wait": 0,
        |			"progress_error": 0,
        |			"state": {
        |				"sources": [
        |					{
        |						"_id": "base",
        |						"state": "OK",
        |						"extended_state": "OK"
        |					},
        |					{
        |						"_id": "gibdd.eaisto",
        |						"state": "OK",
        |						"extended_state": "OK"
        |					},
        |					{
        |						"_id": "tech.ext",
        |						"state": "OK",
        |						"extended_state": "OK"
        |					}
        |				]
        |			},
        |			"content": {
        |				"identifiers": {
        |					"vehicle": {
        |						"vin": "XTA211020Y0208819",
        |						"reg_num": "Н605НХ71"
        |					}
        |				},
        |				"mileages": {
        |					"items": [
        |						{
        |							"date": {
        |								"event": "2017-03-21 00:00:00"
        |							},
        |							"mileage": 290000
        |						},
        |						{
        |							"date": {
        |								"event": "2017-10-24 00:00:00"
        |							},
        |							"mileage": 105000
        |						},
        |						{
        |							"date": {
        |								"event": "2018-05-15 00:00:00"
        |							},
        |							"mileage": 127000
        |						},
        |						{
        |							"date": {
        |								"event": "2018-05-31 00:00:00"
        |							},
        |							"mileage": 248693
        |						}
        |					]
        |				},
        |				"diagnostic_cards": {
        |					"items": [
        |						{
        |							"date": {
        |								"from": "2018-05-31 00:00:00",
        |								"to": "2019-05-31 00:00:00"
        |							},
        |							"doc": {
        |								"number": "077380011805805",
        |								"type": "Диагностическая карта"
        |							},
        |							"reg_num": "Н605НХ71"
        |						},
        |						{
        |							"date": {
        |								"from": "2018-05-15 00:00:00",
        |								"to": "2019-05-15 00:00:00"
        |							},
        |							"doc": {
        |								"number": "086100011803744",
        |								"type": "Диагностическая карта"
        |							}
        |						},
        |						{
        |							"date": {
        |								"from": "2017-10-24 00:00:00",
        |								"to": "2018-10-24 00:00:00"
        |							},
        |							"doc": {
        |								"number": "084260171712549",
        |								"type": "Диагностическая карта"
        |							},
        |							"reg_num": "Н605НХ71"
        |						},
        |						{
        |							"date": {
        |								"from": "2017-03-21 00:00:00",
        |								"to": "2018-03-21 00:00:00"
        |							},
        |							"doc": {
        |								"number": "001180211700428",
        |								"type": "Диагностическая карта"
        |							},
        |							"reg_num": "Н605НХ71"
        |						}
        |					]
        |				}
        |			},
        |			"uid": "autoru_only_eaisto_report_2019_XTA211020Y0208819@autoru",
        |			"name": "NONAME",
        |			"comment": "",
        |			"tags": "",
        |			"created_at": "2019-02-28T10:56:22.486Z",
        |			"created_by": "system",
        |			"updated_at": "2019-02-28T11:03:19.944Z",
        |			"updated_by": "system",
        |			"active_from": "1900-01-01T00:00:00.000Z",
        |			"active_to": "3000-01-01T00:00:00.000Z"
        |		}
        |	]
        |}""".stripMargin
    )
    when(client.getResult(?, ?)(?, ?)).thenReturn(Future.successful((200, json)))
    httpManager.getResult(AutocodeRequest("", VinCode("XTA211020Y0208819"), AutocodeReportType.TechInspections)).map {
      result =>
        assert(result.model.data.content.nonEmpty)
        assert(result.model.data.content.get.mileages.nonEmpty)
        assert(result.model.data.content.get.diagnosticCards.nonEmpty)
    }
  }
}
