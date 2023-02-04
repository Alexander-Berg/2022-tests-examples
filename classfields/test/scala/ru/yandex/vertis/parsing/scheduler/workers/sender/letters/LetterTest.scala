package ru.yandex.vertis.parsing.scheduler.workers.sender.letters

import org.junit.runner.RunWith
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
import ru.yandex.vertis.parsing.auto.components.TestDataReceiverComponents.catalogClient
import ru.yandex.vertis.parsing.auto.components.unexpectedvalues.SimpleUnexpectedAutoValuesSupport
import ru.yandex.vertis.parsing.auto.components.vehiclename.VehicleName
import ru.yandex.vertis.parsing.auto.converters.ImportConverterImpl
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRowUtils.RichParsedOfferOrBuilder
import ru.yandex.vertis.parsing.auto.datareceivers.DataReceiver
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.cars.avito.AvitoCarsParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.avito.AvitoTrucksParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.drom.DromTrucksParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils
import ru.yandex.vertis.parsing.extdata.geo.{Region, RegionTypes}
import ru.yandex.vertis.parsing.importrow.ImportRow
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport

import scala.concurrent.Future

/**
  * Created by andrey on 1/17/18.
  */
@RunWith(classOf[JUnitRunner])
class LetterTest extends FunSuite with OptionValues with MockitoSupport with EmptyTraceSupport {
  private val components = TestDataReceiverComponents
  private val searchlineClient = components.searchlineClient
  private val searcherClient = components.searcherClient
  private val geocoder = components.geocoder
  private val dataReceiver: DataReceiver = components.dataReceiver

  private val importConverter = new ImportConverterImpl(dataReceiver)
    with SimpleUnexpectedAutoValuesSupport
    with components.TimeAwareImpl
    with components.FeaturesAwareImpl

  implicit private val source: CommonModel.Source = CommonModel.Source.HTTP

  test("fromRows: cars") {
    implicit val parser: AvitoCarsParser.type = AvitoCarsParser
    val url1 = TestDataUtils.testAvitoCarsUrl
    val hash1 = CommonAutoParser.hash(url1)

    val url2 = TestDataUtils.testAvitoCarsUrl
    val hash2 = CommonAutoParser.hash(url2)

    val url3 = TestDataUtils.testAvitoCarsUrl
    val hash3 = CommonAutoParser.hash(url3)

    val row1: LetterData = generateRow(
      url1,
      """[{
        |"owner":["{\"name\":[\"Петр, Петрович\"]}"],
        |"address":["Урай"],
        |"year":["1998"],
        |"phone":["+7 992 351-28-59 "],
        |"price":["120 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"],
        |"newUrl":["ftp://test"]}]""".stripMargin.replace("\n", "")
    )
    val row2: LetterData = generateRow(
      url2,
      """[{
        |"owner":["{\"name\":[\"Вячеслав; Викторович\"]}"],
        |"address":["Урай"],
        |"year":["1999"],
        |"phone":["+7 992 351-28-59 "],
        |"price":["127 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"],
        |"newUrl":["ftp://test"]}]""".stripMargin.replace("\n", "")
    )
    val row3: LetterData = generateRow(
      url3,
      """[{
        |"owner":["{\"name\":[\"Василий\"]}"],
        |"address":["Урай"],
        |"year":["2001"],
        |"phone":["+7 992 351-28-60 "],
        |"price":["128 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"],
        |"newUrl":["ftp://test"]}]""".stripMargin.replace("\n", "")
    )
    val letter = Letter.fromRows(Category.CARS, Seq(row1, row2, row3), "https://test.avto.ru/sales-parsing/info/")

    val dd = letter.csv
    assert(letter.asCarsLetter.rows.length == 2)
    assert(letter.asCarsLetter.rows.head.site == "\"avito,avito\"")
    assert(letter.asCarsLetter.rows.head.address == "\"Урай,Урай\"")
    assert(letter.asCarsLetter.rows.head.vehicleName == "\"ГАЗ ГАЗель (3302),ГАЗ ГАЗель (3302)\"")
    assert(letter.asCarsLetter.rows.head.vehicleRuName == "\"ГАЗ ГАЗель (3302),ГАЗ ГАЗель (3302)\"")
    assert(letter.asCarsLetter.rows.head.price == "\"120000,127000\"")
    assert(letter.asCarsLetter.rows.head.phone == "79923512859")
    assert(
      letter.asCarsLetter.rows.head.url == s""""https://test.avto.ru/sales-parsing/info/$hash1,https://test.avto.ru/sales-parsing/info/$hash2""""
    )
    assert(letter.asCarsLetter.rows.head.regionId == "\"11193,11193\"")
    assert(letter.asCarsLetter.rows.head.timeZoneOffset == "\"+3,+3\"")

    assert(letter.asCarsLetter.rows(1).site == "\"avito\"")
    assert(letter.asCarsLetter.rows(1).address == "\"Урай\"")
    assert(letter.asCarsLetter.rows(1).vehicleName == "\"ГАЗ ГАЗель (3302)\"")
    assert(letter.asCarsLetter.rows(1).vehicleRuName == "\"ГАЗ ГАЗель (3302)\"")
    assert(letter.asCarsLetter.rows(1).price == "\"128000\"")
    assert(letter.asCarsLetter.rows(1).phone == "79923512860")
    assert(letter.asCarsLetter.rows(1).url == s""""https://test.avto.ru/sales-parsing/info/$hash3"""")
    assert(letter.asCarsLetter.rows(1).regionId == "\"11193\"")
    assert(letter.asCarsLetter.rows(1).timeZoneOffset == "\"+3\"")
    assert(letter.asCarsLetter.rows(1).newUrl == "\"ftp://test\"")

    assert(
      letter.csv ==
        s"""site;region;mark;price;phone;add_sale_url;category;url;region_id;ru_name;timezone;newUrl
        |"avito,avito";"Урай,Урай";"ГАЗ ГАЗель (3302),ГАЗ ГАЗель (3302)";"120000,127000";79923512859;"https://test.avto.ru/sales-parsing/info/$hash1,https://test.avto.ru/sales-parsing/info/$hash2";2;"$url1,$url2";"11193,11193";"ГАЗ ГАЗель (3302),ГАЗ ГАЗель (3302)";"+3,+3";"ftp://test,ftp://test"
        |"avito";"Урай";"ГАЗ ГАЗель (3302)";"128000";79923512860;"https://test.avto.ru/sales-parsing/info/$hash3";2;"$url3";"11193";"ГАЗ ГАЗель (3302)";"+3";"ftp://test"""".stripMargin
    )
  }

  test("fromRows: trucks") {
    val row1: LetterData = generateRow(
      "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html",
      """[{
        |"owner":["{\"name\":[\"Петр, Петрович\"]}"],
        |"address":["Урай"],
        |"year":["1998"],
        |"phone":["+7 992 351-28-59 "],
        |"price":["120 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"],
        |"newUrl":["ftp://test"]}]""".stripMargin.replace("\n", "")
    )(DromTrucksParser)
    val row2: LetterData = generateRow(
      "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940173.html",
      """[{
        |"owner":["{\"name\":[\"Вячеслав; Викторович\"]}"],
        |"address":["Урай"],
        |"year":["1999"],
        |"phone":["+7 992 351-28-59 "],
        |"price":["127 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"],
        |"newUrl":["ftp://test"]}]""".stripMargin.replace("\n", "")
    )(DromTrucksParser)
    val row3: LetterData = generateRow(
      "https://m.avito.ru/urai/gruzoviki_i_spetstehnika/gazel_termobudka_1197051303",
      """[{
        |"owner":["{\"name\":[\"Василий\"]}"],
        |"address":["Урай"],
        |"year":["2001"],
        |"phone":["+7 992 351-28-60 "],
        |"price":["128 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"],
        |"newUrl":["ftp://test"]}]""".stripMargin.replace("\n", "")
    )(AvitoTrucksParser)
    val letter = Letter.fromRows(Category.TRUCKS, Seq(row1, row2, row3), "https://test.avto.ru/sales/parsing-ct/info/")
    assert(letter.asTrucksLetter.rows.length == 2)
    assert(letter.asTrucksLetter.rows.head.site == "\"drom,drom\"")
    assert(letter.asTrucksLetter.rows.head.address == "\"Урай,Урай\"")
    assert(letter.asTrucksLetter.rows.head.vehicleName == "\"ГАЗ ГАЗель (3302),ГАЗ ГАЗель (3302)\"")
    assert(letter.asTrucksLetter.rows.head.vehicleRuName == "\"ГАЗ ГАЗель (3302),ГАЗ ГАЗель (3302)\"")
    assert(letter.asTrucksLetter.rows.head.price == "\"120000,127000\"")
    assert(letter.asTrucksLetter.rows.head.phone == "79923512859")
    assert(
      letter.asTrucksLetter.rows.head.url == "\"https://test.avto.ru/sales/parsing-ct/info/9c8768f8a325dae2839557d1f095eb85," +
        "https://test.avto.ru/sales/parsing-ct/info/b6c5485c2abd628099db53001f821b24\""
    )
    assert(letter.asTrucksLetter.rows.head.regionId == "\"11193,11193\"")
    assert(letter.asTrucksLetter.rows.head.newUrl == "\"ftp://test,ftp://test\"")

    assert(letter.asTrucksLetter.rows(1).site == "\"avito\"")
    assert(letter.asTrucksLetter.rows(1).address == "\"Урай\"")
    assert(letter.asTrucksLetter.rows(1).vehicleName == "\"ГАЗ ГАЗель (3302)\"")
    assert(letter.asTrucksLetter.rows(1).vehicleRuName == "\"ГАЗ ГАЗель (3302)\"")
    assert(letter.asTrucksLetter.rows(1).price == "\"128000\"")
    assert(letter.asTrucksLetter.rows(1).phone == "79923512860")
    assert(
      letter.asTrucksLetter
        .rows(1)
        .url == "\"https://test.avto.ru/sales/parsing-ct/info/c9fc31c6f7de2f5e0772aeb831c79ce6\""
    )
    assert(letter.asTrucksLetter.rows(1).regionId == "\"11193\"")
    assert(letter.asTrucksLetter.rows(1).newUrl == "\"ftp://test\"")

    assert(
      letter.csv ==
        """site;address;name;price;phone;url;category;region_id;newUrl
        |"drom,drom";"Урай,Урай";"ГАЗ ГАЗель (3302),ГАЗ ГАЗель (3302)";"120000,127000";79923512859;"https://test.avto.ru/sales/parsing-ct/info/9c8768f8a325dae2839557d1f095eb85,https://test.avto.ru/sales/parsing-ct/info/b6c5485c2abd628099db53001f821b24";1;"11193,11193";"ftp://test,ftp://test"
        |"avito";"Урай";"ГАЗ ГАЗель (3302)";"128000";79923512860;"https://test.avto.ru/sales/parsing-ct/info/c9fc31c6f7de2f5e0772aeb831c79ce6";1;"11193";"ftp://test"""".stripMargin
    )
  }

  test("mergeByPhone: empty phone") {
    val row1: LetterData = generateRow(
      "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html",
      """[{
        |"owner":["{\"name\":[\"Петр, Петрович\"]}"],
        |"address":["Урай"],
        |"year":["1998"],
        |"phone":["+7 992 351-28-59 "],
        |"price":["120 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"],
        |"newUrl":["ftp://test"]}]""".stripMargin.replace("\n", "")
    )(DromTrucksParser)
    val row2: LetterData = generateRow(
      "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940173.html",
      """[{
        |"owner":["{\"name\":[\"Вячеслав; Викторович\"]}"],
        |"address":["Урай"],
        |"year":["1999"],
        |"phone":["+7 992 351-28-59 "],
        |"price":["127 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"],
        |"newUrl":["ftp://test"]}]""".stripMargin.replace("\n", "")
    )(DromTrucksParser)
    val row3: LetterData = generateRow(
      "https://m.avito.ru/urai/gruzoviki_i_spetstehnika/gazel_termobudka_1197051303",
      """[{
        |"owner":["{\"name\":[\"Василий\"]}"],
        |"address":["Урай"],
        |"year":["2001"],
        |"phone":["+7 992 351-28-60 "],
        |"price":["128 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"],
        |"newUrl":["ftp://test"]}]""".stripMargin.replace("\n", "")
    )(AvitoTrucksParser)

    val row4: LetterData = generateRow(
      "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940175.html",
      """[{
        |"owner":["{\"name\":[\"Петр, Петрович\"]}"],
        |"address":["Урай"],
        |"year":["1998"],
        |"phone":[],
        |"price":["120 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"],
        |"newUrl":["ftp://test"]}]""".stripMargin.replace("\n", "")
    )(DromTrucksParser)
    val letter =
      Letter.fromRows(Category.TRUCKS, Seq(row1, row2, row3, row4), "https://test.avto.ru/sales/parsing-ct/info/")
    assert(letter.asTrucksLetter.rows.length == 3)
    assert(letter.asTrucksLetter.rows.head.site == "\"drom,drom\"")
    assert(letter.asTrucksLetter.rows.head.address == "\"Урай,Урай\"")
    assert(letter.asTrucksLetter.rows.head.vehicleName == "\"ГАЗ ГАЗель (3302),ГАЗ ГАЗель (3302)\"")
    assert(letter.asTrucksLetter.rows.head.vehicleRuName == "\"ГАЗ ГАЗель (3302),ГАЗ ГАЗель (3302)\"")
    assert(letter.asTrucksLetter.rows.head.price == "\"120000,127000\"")
    assert(letter.asTrucksLetter.rows.head.phone == "79923512859")
    assert(
      letter.asTrucksLetter.rows.head.url == "\"https://test.avto.ru/sales/parsing-ct/info/9c8768f8a325dae2839557d1f095eb85," +
        "https://test.avto.ru/sales/parsing-ct/info/b6c5485c2abd628099db53001f821b24\""
    )
    assert(letter.asTrucksLetter.rows.head.regionId == "\"11193,11193\"")
    assert(letter.asTrucksLetter.rows.head.newUrl == "\"ftp://test,ftp://test\"")

    assert(letter.asTrucksLetter.rows(1).site == "\"avito\"")
    assert(letter.asTrucksLetter.rows(1).address == "\"Урай\"")
    assert(letter.asTrucksLetter.rows(1).vehicleName == "\"ГАЗ ГАЗель (3302)\"")
    assert(letter.asTrucksLetter.rows(1).vehicleRuName == "\"ГАЗ ГАЗель (3302)\"")
    assert(letter.asTrucksLetter.rows(1).price == "\"128000\"")
    assert(letter.asTrucksLetter.rows(1).phone == "79923512860")
    assert(
      letter.asTrucksLetter
        .rows(1)
        .url == "\"https://test.avto.ru/sales/parsing-ct/info/c9fc31c6f7de2f5e0772aeb831c79ce6\""
    )
    assert(letter.asTrucksLetter.rows(1).regionId == "\"11193\"")
    assert(letter.asTrucksLetter.rows(1).newUrl == "\"ftp://test\"")

    assert(letter.asTrucksLetter.rows(2).site == "\"drom\"")
    assert(letter.asTrucksLetter.rows(2).address == "\"Урай\"")
    assert(letter.asTrucksLetter.rows(2).vehicleName == "\"ГАЗ ГАЗель (3302)\"")
    assert(letter.asTrucksLetter.rows(2).vehicleRuName == "\"ГАЗ ГАЗель (3302)\"")
    assert(letter.asTrucksLetter.rows(2).price == "\"120000\"")
    assert(letter.asTrucksLetter.rows(2).phone == "")
    assert(
      letter.asTrucksLetter
        .rows(2)
        .url == "\"https://test.avto.ru/sales/parsing-ct/info/7592075677018c0069abb12f46575101\""
    )
    assert(letter.asTrucksLetter.rows(2).regionId == "\"11193\"")
    assert(letter.asTrucksLetter.rows(2).newUrl == "\"ftp://test\"")

    assert(
      letter.csv ==
        """site;address;name;price;phone;url;category;region_id;newUrl
        |"drom,drom";"Урай,Урай";"ГАЗ ГАЗель (3302),ГАЗ ГАЗель (3302)";"120000,127000";79923512859;"https://test.avto.ru/sales/parsing-ct/info/9c8768f8a325dae2839557d1f095eb85,https://test.avto.ru/sales/parsing-ct/info/b6c5485c2abd628099db53001f821b24";1;"11193,11193";"ftp://test,ftp://test"
        |"avito";"Урай";"ГАЗ ГАЗель (3302)";"128000";79923512860;"https://test.avto.ru/sales/parsing-ct/info/c9fc31c6f7de2f5e0772aeb831c79ce6";1;"11193";"ftp://test"
        |"drom";"Урай";"ГАЗ ГАЗель (3302)";"120000";;"https://test.avto.ru/sales/parsing-ct/info/7592075677018c0069abb12f46575101";1;"11193";"ftp://test"""".stripMargin
    )
  }

  private def generateRow(url: String, rawJson: String)(implicit parser: CommonAutoParser): LetterData = {
    val row = ImportRow(url, Json.parse(rawJson))
    if (parser.category == Category.TRUCKS) {
      when(searchlineClient.suggest(?)(?)).thenReturn(
        Future.successful(
          ParsedSearchlineResponse(
            carsMarkModel = MarkModel(None, None, "", category = Category.CARS),
            trucksMarkModel = MarkModel(Some(Mark("GAZ")), Some(Model("GAZEL_3302")), "", category = Category.TRUCKS)
          )
        )
      )
    } else {
      val unifyResult = CarsUnificationCollection.newBuilder()
      unifyResult.addEntriesBuilder().setMark("GAZ").setModel("GAZEL_3302")
      when(catalogClient.find(?, Mark(?), Model(?))(?)).thenReturn(
        Future.successful(
          CatalogResponse(MarkName("Gaz"), ModelName("Gazel 3302"), RuMarkName("Газ"), RuModelName("Газель 3302"))
        )
      )
      when(searcherClient.carsUnify(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(unifyResult.build()))
      when(searchlineClient.suggest(?)(?)).thenReturn(
        Future.successful(
          ParsedSearchlineResponse(
            carsMarkModel = MarkModel(Some(Mark("GAZ")), Some(Model("GAZEL_3302")), "", category = Category.CARS),
            trucksMarkModel = MarkModel(None, None, "", category = Category.TRUCKS)
          )
        )
      )
    }
    val optRegion = Some(Region(11192, 121128, RegionTypes.City, "Урай", "Uray", Some(10800)))
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val enrichedRow = dataReceiver.enrichRow(row, None).toOption.value
    val parsedRow = importConverter.toParsedRow(row, None).toOption.value
    val phone = parsedRow.data.getCurrentPhones.headOption.getOrElse("")
    val federalSubjectId = 11193
    val newUrl = "ftp://test"
    val vehicleName = VehicleName("ГАЗ ГАЗель (3302)", "ГАЗ ГАЗель (3302)")
    LetterData(parsedRow, vehicleName, phone, federalSubjectId, "+3", newUrl)
  }
}
