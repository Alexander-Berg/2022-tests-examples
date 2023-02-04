package ru.yandex.vertis.parsing.auto.datareceivers

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.unification.Unification.CarsUnificationCollection
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.auto.clients.catalog.CatalogClient.CatalogResponse
import ru.yandex.vertis.parsing.auto.clients.searchline._
import ru.yandex.vertis.parsing.auto.clients.searchline.SearchlineClient.ParsedSearchlineResponse
import ru.yandex.vertis.parsing.auto.components.TestDataReceiverComponents
import ru.yandex.vertis.parsing.auto.parsers.scrapinghub.cars.drom.ScrapingHubDromCarsParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.drom.DromTrucksParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils
import ru.yandex.vertis.parsing.common.SkipReason
import ru.yandex.vertis.parsing.extdata.geo.{Region, RegionTypes}
import ru.yandex.vertis.parsing.importrow.ImportRow
import ru.yandex.vertis.parsing.util.TestUtils
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class DataReceiverTest extends FunSuite with MockitoSupport with OptionValues {

  import TestDataReceiverComponents._

  private val region = Region(11192, 121128, RegionTypes.City, "Урай", "Uray", Some(10800))
  private val optRegion = Some(region)

  implicit private val trace: Traced = TracedUtils.empty

  test("enrichRow: incorrect url") {
    val url = "https://spec.drom.ru/urai/xyxy/prodam-gazel-termobudka-59940174.html"
    val row = ImportRow(url, Json.obj())
    val res = dataReceiver.enrichRow(row, None)(DromTrucksParser, CommonModel.Source.IMPORT, trace)
    assert(res.left.toOption.value == SkipReason.IncorrectUrl)
  }

  test("enrichRow: skipped: no fn") {
    val url = TestDataUtils.testDromCarsUrl
    val row = ImportRow(
      url,
      Json.obj(
        "sh_last_visited" -> "2018-12-20T05:06:06+00:00"
      )
    )
    val res = dataReceiver.enrichRow(row, None)(ScrapingHubDromCarsParser, CommonModel.Source.IMPORT, trace)
    assert(res.left.toOption.value == SkipReason.NoFn)
  }

  test("enrichRow: skipped: incorrect json") {
    val url = TestDataUtils.testDromCarsUrl
    val row = ImportRow(
      url,
      Json.arr(
        Json.obj(
          "sh_last_visited" -> "2018-12-20T05:06:06+00:00"
        )
      )
    )
    val res = dataReceiver.enrichRow(row, None)(ScrapingHubDromCarsParser, CommonModel.Source.IMPORT, trace)
    assert(res.left.toOption.value == SkipReason.IncorrectJson)
  }

  test("enrichRow: skipped: no parse date") {
    val url = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    val row = ImportRow(url, Json.arr(Json.obj()))
    val res = dataReceiver.enrichRow(row, None)(DromTrucksParser, CommonModel.Source.IMPORT, trace)
    assert(res.left.toOption.value == SkipReason.NoParseDate)
  }

  test("enrichRow: skipped: failed to get catalog card") {
    val url = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    val row = ImportRow(
      url,
      Json.arr(
        Json.obj(
          "fn" -> Json.arr("ГАЗ Газель"),
          "parse_date" -> Json.arr("2018-01-02T08:21:18.138+03:00")
        )
      )
    )
    when(searchlineClient.suggest(?)(?)).thenReturn(
      Future.successful(
        ParsedSearchlineResponse(
          MarkModel(None, None, "", category = Category.CARS),
          MarkModel(None, None, "", category = Category.TRUCKS)
        )
      )
    )
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val res = dataReceiver.enrichRow(row, None)(DromTrucksParser, CommonModel.Source.IMPORT, trace)
    assert(res.left.toOption.value == SkipReason.NoMarkModel("ГАЗ Газель"))
  }

  test("enrichRow: skipped: failed to find valid catalog card") {
    pending
    // TODO невалидных каточек в каталоге нету
    val url = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    val row = ImportRow(url, Json.arr(Json.obj("fn" -> Json.arr("ГАЗ Газель"))))
    when(searchlineClient.suggest(?)(?)).thenReturn(
      Future.successful(
        ParsedSearchlineResponse(
          MarkModel(None, None, "", category = Category.CARS),
          MarkModel(Some(Mark("AVIA")), Some(Model("SAMOPOGRUZCHIKI")), "", category = Category.TRUCKS)
        )
      )
    )
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val res = dataReceiver.enrichRow(row, None)(DromTrucksParser, CommonModel.Source.IMPORT, trace)
    withClue(res) {
      assert(res.left.toOption.value == SkipReason.NoValidTruckCard("AVIA", "SAMOPOGRUZCHIKI"))
    }
  }

  test("parseRegion") {
    val url = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    val row = ImportRow(
      url,
      Json.arr(
        Json.obj(
          "fn" -> Json.arr("ГАЗ Газель"),
          "address" -> Json.arr("Урай"),
          "parse_date" -> Json.arr("2018-01-02T08:21:18.138+03:00")
        )
      )
    )
    when(searchlineClient.suggest(?)(?)).thenReturn(
      Future.successful(
        ParsedSearchlineResponse(
          MarkModel(None, None, "", category = Category.CARS),
          MarkModel(Some(Mark("GAZ")), Some(Model("GAZEL_3302")), "", category = Category.TRUCKS)
        )
      )
    )
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val res = dataReceiver.enrichRow(row, None)(DromTrucksParser, CommonModel.Source.IMPORT, trace).toOption.value
    assert(res.optRegion.value == (region, "Урай"))
  }

  test("parseRegion from several sources") {
    val url = "https://spb.drom.ru/renault/kaptur/34019777.html"
    val row = ImportRow(
      url,
      Json.obj(
        "car_name" -> "ГАЗ Газель",
        "listing_city" -> "Санкт-Петербург 1",
        "dealership" -> Json.obj(
          "dealership_city" -> "Санкт-Петербург 2"
        ),
        "sh_last_visited" -> "2018-12-20T05:06:06+00:00"
      )
    )
    when(searchlineClient.suggest(?)(?)).thenReturn(
      Future.successful(
        ParsedSearchlineResponse(
          MarkModel(Some(Mark("GAZ")), Some(Model("GAZEL_3302")), "", category = Category.CARS),
          MarkModel(None, None, "", category = Category.TRUCKS)
        )
      )
    )
    when(catalogClient.find(?, Mark(?), Model(?))(?)).thenReturn(
      Future.successful(
        CatalogResponse(MarkName("Gaz"), ModelName("Gazel 3302"), RuMarkName("Газ"), RuModelName("Газель 3302"))
      )
    )
    val unifyResult = CarsUnificationCollection.newBuilder()
    unifyResult.addEntriesBuilder().setMark("GAZ").setModel("GAZEL_3302")
    when(searcherClient.carsUnify(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(unifyResult.build()))
    stub(geocoder.getRegion(_: String)(_: Traced)) {
      case (address, _) =>
        if (address == "Санкт-Петербург 1") Future.successful(None)
        else if (address == "Санкт-Петербург 2") Future.successful(None)
        else if (address == "SPB") Future.successful(optRegion)
        else sys.error(s"unexpected address $address")
    }
    val res = dataReceiver
      .enrichRow(row, None)(ScrapingHubDromCarsParser, CommonModel.Source.SCRAPING_HUB_FRESH, trace)
      .toOption
      .value
    assert(res.optRegion.value == (region, "SPB"))
    verify(geocoder).getRegion(eq("Санкт-Петербург 1"))(?)
    verify(geocoder).getRegion(eq("Санкт-Петербург 2"))(?)
    verify(geocoder).getRegion(eq("SPB"))(?)
    reset(geocoder)
  }
}
