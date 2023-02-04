package ru.yandex.vertis.parsing.auto.converters

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.CatalogModel.TechInfo
import ru.auto.api.unification.Unification.CarsUnificationCollection
import ru.auto.api.{ApiOfferModel, TrucksModel}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.CommonModel.Source
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.clients.catalog.CatalogClient.CatalogResponse
import ru.yandex.vertis.parsing.auto.clients.searchline._
import ru.yandex.vertis.parsing.auto.clients.searchline.SearchlineClient.ParsedSearchlineResponse
import ru.yandex.vertis.parsing.auto.components.TestConvertersComponents
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.diffs.OfferFields
import ru.yandex.vertis.parsing.auto.importrow
import ru.yandex.vertis.parsing.auto.importrow.{CarsCatalogData, EnrichedImportRow}
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.parsers.scrapinghub.cars.avito.ScrapingHubAvitoCarsParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.cars.avito.AvitoCarsParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.avito.AvitoTrucksParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.drom.DromTrucksParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.extdata.geo.{Region, RegionTypes}
import ru.yandex.vertis.parsing.importrow.ImportRow
import ru.yandex.vertis.parsing.parsers.OfferUrl
import ru.yandex.vertis.parsing.util.DateUtils
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport

import scala.concurrent.Future

/**
  * Created by andrey on 1/12/18.
  */
@RunWith(classOf[JUnitRunner])
class ImportConverterImplTest extends FunSuite with OptionValues with MockitoSupport with EmptyTraceSupport {
  private val components = TestConvertersComponents
  private val searchlineClient = components.searchlineClient
  private val catalogClient = components.catalogClient
  private val geocoder = components.geocoder
  private val searcherClient = components.searcherClient
  private val importConverter = components.importConverter
  private val optRegion = Some(Region(11192, 121128, RegionTypes.City, "Урай", "Uray", Some(10800)))
  private val parseDate = new DateTime(0).withMillisOfDay(0)

  test("toParsedRow: wheel drive") {
    val url = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    val rawJson =
      """[{
        |"fn":["ГАЗ Газель"],
        |"info":["{
        |  \"wheel-drive\":[\"8x2\"]
        |}"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    val row = ImportRow(url, Json.parse(rawJson))
    when(searchlineClient.suggest(?)(?)).thenReturn(
      Future.successful(
        ParsedSearchlineResponse(
          MarkModel(None, None, "", category = Category.CARS),
          MarkModel(Some(Mark("GAZ")), Some(Model("GAZEL_3302")), "", category = Category.TRUCKS)
        )
      )
    )
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val parsedRow =
      importConverter.toParsedRow(row, None)(DromTrucksParser, CommonModel.Source.HTTP, trace).toOption.value
    assert(parsedRow.data.getOffer.getTruckInfo.getGear == TrucksModel.GearType.GT_UNKNOWN)
    assert(parsedRow.data.getOffer.getTruckInfo.getWheelDrive == TrucksModel.WheelDrive.WD_8x2)
  }

  test("toParsedRow: gear type") {
    val url = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    val rawJson =
      """[{
        |"fn":["ГАЗ Газель"],
        |"info":["{
        |  \"wheel-drive\":[\"Полный\"]
        |}"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    val row = ImportRow(url, Json.parse(rawJson))
    when(searchlineClient.suggest(?)(?)).thenReturn(
      Future.successful(
        ParsedSearchlineResponse(
          MarkModel(None, None, "", category = Category.CARS),
          MarkModel(Some(Mark("GAZ")), Some(Model("GAZEL_3302")), "", category = Category.TRUCKS)
        )
      )
    )
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val parsedRow =
      importConverter.toParsedRow(row, None)(DromTrucksParser, CommonModel.Source.HTTP, trace).toOption.value
    assert(parsedRow.data.getOffer.getTruckInfo.getGear == TrucksModel.GearType.FULL)
    assert(parsedRow.data.getOffer.getTruckInfo.getWheelDrive == TrucksModel.WheelDrive.WD_UNKNOWN)
  }

  test("toParsedRow: ownersCount") {
    val url = testAvitoCarsUrl
    val rawJson =
      """[{
        |"fn":["ГАЗ Газель"],
        |"info":["{
        |  \"owners-count\":[\"4+\"]
        |}"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    val row = ImportRow(url, Json.parse(rawJson))
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
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val unifyResult = CarsUnificationCollection.newBuilder()
    unifyResult.addEntriesBuilder().setMark("GAZ").setModel("GAZEL_3302")
    when(searcherClient.carsUnify(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(unifyResult.build()))
    val res = importConverter.toParsedRow(row, None)(AvitoCarsParser, CommonModel.Source.HTTP, trace)
    val parsedRow = withClue(res) {
      res.toOption.value
    }
    assert(parsedRow.data.getOffer.getDocuments.getOwnersNumber == 3)
  }

  test("fillVehicleInfo: mark model from tech info") {
    val url = testAvitoCarsUrl
    val offer = ApiOfferModel.Offer.newBuilder()
    val unifyResult = CarsUnificationCollection.newBuilder()
    unifyResult.addEntriesBuilder()
    val techInfo = TechInfo.newBuilder()
    techInfo.getMarkInfoBuilder.setCode("HONDA")
    techInfo.getMarkInfoBuilder.setName("Honda")
    techInfo.getMarkInfoBuilder.setRuName("Хонда")
    techInfo.getModelInfoBuilder.setCode("PARTNER")
    techInfo.getModelInfoBuilder.setName("Partner")
    techInfo.getModelInfoBuilder.setRuName("Партнер")
    val catalogData = CarsCatalogData(
      unifyResult.build(),
      Some(techInfo.build())
    )
    val enrichedRow = importrow.EnrichedImportRow(OfferUrl(url), Json.obj(), Json.obj(), catalogData, None, parseDate)
    importConverter.fillVehicleInfo(offer)(enrichedRow, AvitoCarsParser, CommonModel.Source.IMPORT, insertOnly = false)
    assert(offer.getCarInfo.getMark == "HONDA")
    assert(offer.getCarInfo.getMarkInfo.getCode == "HONDA")
    assert(offer.getCarInfo.getMarkInfo.getName == "Honda")
    assert(offer.getCarInfo.getMarkInfo.getRuName == "Хонда")
    assert(offer.getCarInfo.getModel == "PARTNER")
    assert(offer.getCarInfo.getModelInfo.getCode == "PARTNER")
    assert(offer.getCarInfo.getModelInfo.getName == "Partner")
    assert(offer.getCarInfo.getModelInfo.getRuName == "Партнер")
  }

  test("fillVehicleInfo: cars gear type") {
    val url = testAvitoCarsUrl
    val offer = ApiOfferModel.Offer.newBuilder()
    val unifyResult = CarsUnificationCollection.newBuilder()
    unifyResult.addEntriesBuilder().setGearType("FRONT")
    val catalogData = CarsCatalogData(unifyResult.build(), None)
    val enrichedRow = EnrichedImportRow(OfferUrl(url), Json.obj(), Json.obj(), catalogData, None, parseDate)
    importConverter.fillVehicleInfo(offer)(enrichedRow, AvitoCarsParser, CommonModel.Source.IMPORT, insertOnly = false)
    assert(offer.getCarInfo.getDrive == "FORWARD_CONTROL")
  }

  test("fillVehicleInfo: cars gear type from tech info") {
    val url = testAvitoCarsUrl
    val offer = ApiOfferModel.Offer.newBuilder()
    val unifyResult = CarsUnificationCollection.newBuilder()
    unifyResult.addEntriesBuilder().setGearType("FRONT")
    val techInfo = TechInfo.newBuilder()
    techInfo.getTechParamBuilder.setGearType("TECH_INFO_GEAR_TYPE")
    val catalogData =
      CarsCatalogData(unifyResult.build(), Some(techInfo.build()))
    val enrichedRow = importrow.EnrichedImportRow(OfferUrl(url), Json.obj(), Json.obj(), catalogData, None, parseDate)
    importConverter.fillVehicleInfo(offer)(enrichedRow, AvitoCarsParser, CommonModel.Source.IMPORT, insertOnly = false)
    assert(offer.getCarInfo.getDrive == "TECH_INFO_GEAR_TYPE")
  }

  test("fillVehicleInfo: cars transmission") {
    val url = testAvitoCarsUrl
    val offer = ApiOfferModel.Offer.newBuilder()
    val unifyResult = CarsUnificationCollection.newBuilder()
    unifyResult.addEntriesBuilder().setTransmission("ROBOT_2CLUTCH")
    val catalogData = CarsCatalogData(unifyResult.build(), None)
    val enrichedRow = importrow.EnrichedImportRow(OfferUrl(url), Json.obj(), Json.obj(), catalogData, None, parseDate)
    importConverter.fillVehicleInfo(offer)(enrichedRow, AvitoCarsParser, CommonModel.Source.IMPORT, insertOnly = false)
    assert(offer.getCarInfo.getTransmission == "ROBOT")
  }

  test("fillVehicleInfo: cars transmission from tech info") {
    val url = testAvitoCarsUrl
    val offer = ApiOfferModel.Offer.newBuilder()
    val unifyResult = CarsUnificationCollection.newBuilder()
    unifyResult.addEntriesBuilder().setTransmission("ROBOT_2CLUTCH")
    val techInfo = TechInfo.newBuilder()
    techInfo.getTechParamBuilder.setTransmission("TECH_INFO_TRANSMISSION")
    val catalogData =
      CarsCatalogData(unifyResult.build(), Some(techInfo.build()))
    val enrichedRow = importrow.EnrichedImportRow(OfferUrl(url), Json.obj(), Json.obj(), catalogData, None, parseDate)
    importConverter.fillVehicleInfo(offer)(enrichedRow, AvitoCarsParser, CommonModel.Source.IMPORT, insertOnly = false)
    assert(offer.getCarInfo.getTransmission == "TECH_INFO_TRANSMISSION")
  }

  test("toParsedRow: priceInfo currency") {
    val url = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    val rawJson =
      """[{
        |"owner":["{
        |  \"id\":[\"4059334\"],
        |  \"name\":[\"Петр\"],
        |  \"email\":[\"test@example.org\"],
        |  \"login\":[\"usamig84\"]}"],
        |"address":["Урай"],
        |"year":["1998"],
        |"phone":["+7 992 351-28-59 "],
        |"price":["120 000"],
        |"fn":["ГАЗ Газель"],
        |"photo":["https://static.baza.farpost.ru/v/1514823200077_bulletin","https://static.baza.farpost.ru/v/1514823224243_bulletin","https://static.baza.farpost.ru/v/1514823236196_bulletin","https://static.baza.farpost.ru/v/1514823250698_bulletin"],
        |"description":["Продам газель термобудка,высота 180 см,магнитола."],
        |"offer_id":["59940174"],
        |"info":["{
        |  \"transmission\":[\"Механическая\"],
        |  \"engine\":[\"2 400 куб. см.\"],
        |  \"wheel-drive\":[\"4x2\"],
        |  \"mileage\":[\"45000\"],
        |  \"seats\":[\"8\"],
        |  \"mileage_in_russia\":[\"С пробегом\"],
        |  \"documents\":[\"Есть ПТС\"],
        |  \"wheel\":[\"Левый\"],
        |  \"fuel\":[\"Дизель\"],
        |  \"state\":[\"Хорошее\"],
        |  \"type\":[\"Изотермический фургон\"],
        |  \"category\":[\"Грузовики и спецтехника\"],
        |  \"capacity\":[\"2 000 кг.\"]}"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    val row = ImportRow(url, Json.parse(rawJson))
    when(searchlineClient.suggest(?)(?)).thenReturn(
      Future.successful(
        ParsedSearchlineResponse(
          MarkModel(None, None, "", category = Category.CARS),
          MarkModel(Some(Mark("GAZ")), Some(Model("GAZEL_3302")), "", category = Category.TRUCKS)
        )
      )
    )
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val parsedRow =
      importConverter.toParsedRow(row, None)(DromTrucksParser, CommonModel.Source.HTTP, trace).toOption.value
    assert(parsedRow.data.getOffer.getPriceInfo.getCurrency == "RUR")
  }

  test("toParsedRow: state") {
    val url = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    val rawJson =
      """[{
        |"owner":["{
        |  \"id\":[\"4059334\"],
        |  \"name\":[\"Петр\"],
        |  \"email\":[\"test@example.org\"],
        |  \"login\":[\"usamig84\"]}"],
        |"address":["Урай"],
        |"year":["1998"],
        |"phone":["+7 992 351-28-59 "],
        |"price":["120 000"],
        |"fn":["ГАЗ Газель"],
        |"photo":["https://static.baza.farpost.ru/v/1514823200077_bulletin","https://static.baza.farpost.ru/v/1514823224243_bulletin","https://static.baza.farpost.ru/v/1514823236196_bulletin","https://static.baza.farpost.ru/v/1514823250698_bulletin"],
        |"description":["Продам газель термобудка,высота 180 см,магнитола."],
        |"offer_id":["59940174"],
        |"info":["{
        |  \"transmission\":[\"Механическая\"],
        |  \"engine\":[\"2 400 куб. см.\"],
        |  \"wheel-drive\":[\"4x2\"],
        |  \"mileage\":[\"45000\"],
        |  \"seats\":[\"8\"],
        |  \"mileage_in_russia\":[\"С пробегом\"],
        |  \"documents\":[\"Есть ПТС\"],
        |  \"wheel\":[\"Левый\"],
        |  \"fuel\":[\"Дизель\"],
        |  \"type\":[\"Изотермический фургон\"],
        |  \"category\":[\"Грузовики и спецтехника\"],
        |  \"capacity\":[\"2 000 кг.\"]}"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    val row = ImportRow(url, Json.parse(rawJson))
    when(searchlineClient.suggest(?)(?)).thenReturn(
      Future.successful(
        ParsedSearchlineResponse(
          MarkModel(None, None, "", category = Category.CARS),
          MarkModel(Some(Mark("GAZ")), Some(Model("GAZEL_3302")), "", category = Category.TRUCKS)
        )
      )
    )
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val parsedRow =
      importConverter.toParsedRow(row, None)(DromTrucksParser, CommonModel.Source.HTTP, trace).toOption.value
    assert(parsedRow.data.getOffer.getState.getStateNotBeaten) // если состояние не передано, считаем что ок
    assert(parsedRow.data.getOffer.getState.getCondition == ApiOfferModel.Condition.CONDITION_OK)
  }

  test("keep avito photo sizes: feature disabled") {
    val url = testAvitoCarsUrl
    val rawJson =
      """[{
        |"fn":["ГАЗ Газель"],
        |"photo":["https://84.img.avito.st/640x480/3852083484.jpg", "https://12.img.avito.st/640x480/3852090112.jpg"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    val row = ImportRow(url, Json.parse(rawJson))
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
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val unifyResult = CarsUnificationCollection.newBuilder()
    unifyResult.addEntriesBuilder().setMark("GAZ").setModel("GAZEL_3302")
    when(searcherClient.carsUnify(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(unifyResult.build()))
    val res = importConverter.toParsedRow(row, None)(AvitoCarsParser, CommonModel.Source.HTTP, trace)
    val parsedRow = withClue(res) {
      res.toOption.value
    }
    assert(parsedRow.data.getPhotoCount == 2)
    assert(parsedRow.data.getPhoto(0) == "https://84.img.avito.st/640x480/3852083484.jpg")
    assert(parsedRow.data.getPhoto(1) == "https://12.img.avito.st/640x480/3852090112.jpg")
  }

  test("keep avito photo sizes: feature enabled") {
    components.features.ReplaceAvitoPhotoSizes.setEnabled(true)
    val url = testAvitoCarsUrl
    val rawJson =
      """[{
        |"fn":["ГАЗ Газель"],
        |"photo":["https://84.img.avito.st/640x480/3852083484.jpg", "https://12.img.avito.st/640x480/3852090112.jpg"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    val row = ImportRow(url, Json.parse(rawJson))
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
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val unifyResult = CarsUnificationCollection.newBuilder()
    unifyResult.addEntriesBuilder().setMark("GAZ").setModel("GAZEL_3302")
    when(searcherClient.carsUnify(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(unifyResult.build()))
    val res = importConverter.toParsedRow(row, None)(AvitoCarsParser, CommonModel.Source.HTTP, trace)
    val parsedRow = withClue(res) {
      res.toOption.value
    }
    assert(parsedRow.data.getPhotoCount == 2)
    assert(parsedRow.data.getPhoto(0) == "https://84.img.avito.st/1280x960/3852083484.jpg")
    assert(parsedRow.data.getPhoto(1) == "https://12.img.avito.st/1280x960/3852090112.jpg")
    components.features.ReplaceAvitoPhotoSizes.setEnabled(false)
  }

  test("fix photos") {
    val url = testAvitoCarsUrl
    val rawJson =
      """[{
        |"fn":["ГАЗ Газель"],
        |"photo":["https:https://84.img.avito.st/640x480/3852083484.jpg", "https:https://12.img.avito.st/640x480/3852090112.jpg"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    val row = ImportRow(url, Json.parse(rawJson))
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
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val unifyResult = CarsUnificationCollection.newBuilder()
    unifyResult.addEntriesBuilder().setMark("GAZ").setModel("GAZEL_3302")
    when(searcherClient.carsUnify(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(unifyResult.build()))
    val res = importConverter.toParsedRow(row, None)(AvitoCarsParser, CommonModel.Source.HTTP, trace)
    val parsedRow = withClue(res) {
      res.toOption.value
    }
    assert(parsedRow.data.getPhotoCount == 2)
    assert(parsedRow.data.getPhoto(0) == "https://84.img.avito.st/640x480/3852083484.jpg")
    assert(parsedRow.data.getPhoto(1) == "https://12.img.avito.st/640x480/3852090112.jpg")
  }

  test("set section: year check") {
    val parsedRow: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"year":["${DateTime.now().getYear - 2}"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    )
    assert(parsedRow.data.getOffer.getSection == Section.USED)
  }

  test("set section: current year, mileage is above zero") {
    val parsedRow: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"year":["${DateTime.now().getYear}"],
          |"info":["{
          |  \\"mileage\\":[\\"45000\\"]
          |}"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    )
    assert(parsedRow.data.getOffer.getSection == Section.USED)
  }

  test("set section: current year, mileage is zero") {
    val parsedRow: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"year":["${DateTime.now().getYear}"],
          |"info":["{
          |  \\"mileage\\":[\\"0\\"]
          |}"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    )
    assert(parsedRow.data.getOffer.getSection == Section.NEW)
  }

  test("set section: current year, mileage is unknown") {
    val parsedRow: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"year":["${DateTime.now().getYear}"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    )
    assert(parsedRow.data.getOffer.getSection == Section.NEW)
  }

  test("set section: unknown year") {
    val parsedRow: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    )
    assert(parsedRow.data.getOffer.getSection == Section.SECTION_UNKNOWN)

    val parsedRow2: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"info":["{
          |  \\"mileage\\":[\\"45000\\"]
          |}"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    )
    assert(parsedRow2.data.getOffer.getSection == Section.USED)

    val parsedRow3: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"info":["{
          |  \\"mileage\\":[\\"0\\"]
          |}"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    )
    assert(parsedRow3.data.getOffer.getSection == Section.NEW)
  }

  test("set year: if section is new") {
    val parsedRow: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"info":["{
          |  \\"mileage\\":[\\"0\\"]
          |}"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    )
    assert(parsedRow.data.getOffer.getSection == Section.NEW)
    assert(parsedRow.data.getOffer.getDocuments.getYear == components.timeService.getNow.getYear)
  }

  test("set section: unknown year and mileage, but it is dealer and last generation by catalog") {
    val parsedRow: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"is_dealer":["true"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", ""),
      techParamId = Some(21031652),
      yearTo = Some(0)
    )
    assert(parsedRow.data.getOffer.getSection == Section.NEW)
    assert(parsedRow.data.getOffer.getDocuments.getYear == components.timeService.getNow.getYear)

    val parsedRow2: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"is_dealer":["false"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", ""),
      techParamId = Some(21031652),
      yearTo = Some(0)
    )
    assert(parsedRow2.data.getOffer.getSection == Section.SECTION_UNKNOWN)
    assert(parsedRow2.data.getOffer.getDocuments.getYear == 0)

    val parsedRow3: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"is_dealer":["true"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", ""),
      techParamId = Some(21031652),
      yearTo = Some(2019)
    )
    assert(parsedRow3.data.getOffer.getSection == Section.SECTION_UNKNOWN)
    assert(parsedRow3.data.getOffer.getDocuments.getYear == 0)

    val parsedRow4: ParsedRow = parseJson(
      s"""|[{
          |"fn":["ГАЗ Газель"],
          |"is_dealer":["true"],
          |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", ""),
      techParamId = None
    )
    assert(parsedRow4.data.getOffer.getSection == Section.SECTION_UNKNOWN)
    assert(parsedRow4.data.getOffer.getDocuments.getYear == 0)
  }

  test("vin mask") {
    val parsedRow: ParsedRow = parseJson(
      """[{
        |"fn":["ГАЗ Газель"],
        |"vin":["JTNBV58E*0J****89"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    )
    assert(parsedRow.data.getOffer.getDocuments.getVin.isEmpty)
    assert(parsedRow.data.getVinMask == "JTNBV58E*0J****89")
    assert(parsedRow.data.getOffer.getDocuments.getVin.isEmpty)
  }

  test("set year, mileage and section") {
    val parsedRow: ParsedRow = parseJson(
      """[{
        |"fn":["ГАЗ Газель"],
        |"year":["1998"],
        |"info":["{
        |  \"mileage\":[\"45000\"],
        |  \"car-type\":[\"с пробегом\"]
        |}"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    )
    assert(parsedRow.data.getOffer.getSection == Section.USED)
    assert(parsedRow.data.getOffer.getDocuments.getYear == 1998)
    assert(parsedRow.data.getOffer.getState.getMileage == 45000)

    val parsedRow2: ParsedRow = parseJson(
      """[{
        |"fn":["ГАЗ Газель"],
        |"year":["1998"],
        |"info":["{
        |  \"mileage\":[\"45000\"],
        |  \"car-type\":[\"новые\"]
        |}"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    )
    assert(parsedRow2.data.getOffer.getSection == Section.NEW)
  }

  test("max parse date") {
    val url = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, source = Source.AV100)

    parsedOffer
      .addStatusHistoryBuilder()
      .addDiffBuilder()
      .setName(OfferFields.ParseDate)
      .setOldValue("2019-02-07T10:54:31.000+03:00")
      .setNewValue("2019-02-20T14:29:59.000+03:00")

    parsedOffer.getParseDateBuilder.setSeconds(DateUtils.jodaParse("2019-02-20T14:29:59.000+03:00").getMillis / 1000)

    assert(parsedRow.data.getMaxParseDate.getSeconds == 0)

    val parsedRow2: ParsedRow = parseJson(
      """[{
        |"fn":["ГАЗ Газель"],
        |"year":["1998"],
        |"info":["{
        |  \"mileage\":[\"45000\"],
        |  \"car-type\":[\"новые\"]
        |}"],
        |"parse_date":["2016-04-17T12:00:08.000+03:00"]}]""".stripMargin.replace("\n", ""),
      prevRow = Some(parsedRow)
    )

    assert(
      parsedRow2.data.getParseDate.getSeconds ==
        DateUtils.jodaParse("2016-04-17T12:00:08.000+03:00").getMillis / 1000
    )
    assert(
      parsedRow2.data.getMaxParseDate.getSeconds ==
        DateUtils.jodaParse("2019-02-20T14:29:59.000+03:00").getMillis / 1000
    )
  }

  test("max parse date: fix 2") {
    val url = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, source = Source.AV100)

    parsedOffer
      .addStatusHistoryBuilder()
      .addDiffBuilder()
      .setName(OfferFields.ParseDate)
      .setOldValue("2019-02-07T10:54:31.000+03:00")
      .setNewValue("2019-02-20T14:29:59.000+03:00")

    parsedOffer
      .addStatusHistoryBuilder()
      .addDiffBuilder()
      .setName(OfferFields.ParseDate)
      .setOldValue("2019-02-20T14:29:59.000+03:00")
      .setNewValue("2016-04-17T12:00:08.000+03:00")

    parsedOffer.getParseDateBuilder.setSeconds(DateUtils.jodaParse("2016-04-17T12:00:08.000+03:00").getMillis / 1000)

    parsedOffer.getParseDateBuilder.setSeconds(DateUtils.jodaParse("2016-04-17T12:00:08.000+03:00").getMillis / 1000)

    assert(parsedRow.data.getMaxParseDate.getSeconds == 0)

    val parsedRow2: ParsedRow = parseJson(
      """[{
        |"fn":["ГАЗ Газель"],
        |"year":["1998"],
        |"info":["{
        |  \"mileage\":[\"45000\"],
        |  \"car-type\":[\"новые\"]
        |}"],
        |"parse_date":["2016-04-17T12:00:08.000+03:00"]}]""".stripMargin.replace("\n", ""),
      prevRow = Some(parsedRow)
    )

    assert(
      parsedRow2.data.getParseDate.getSeconds ==
        DateUtils.jodaParse("2016-04-17T12:00:08.000+03:00").getMillis / 1000
    )
    assert(
      parsedRow2.data.getMaxParseDate.getSeconds ==
        DateUtils.jodaParse("2019-02-20T14:29:59.000+03:00").getMillis / 1000
    )
  }

  test("insertOnly: url") {
    val mobileUrl = testAvitoMobileCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(mobileUrl, parsedOffer, category = Category.CARS, source = Source.AV100)

    parsedOffer
      .addStatusHistoryBuilder()
      .addDiffBuilder()
      .setName(OfferFields.ParseDate)
      .setOldValue("2019-02-07T10:54:31.000+03:00")
      .setNewValue("2019-02-20T14:29:59.000+03:00")

    parsedOffer.getParseDateBuilder.setSeconds(DateUtils.jodaParse("2019-02-20T14:29:59.000+03:00").getMillis / 1000)

    assert(parsedRow.data.getMaxParseDate.getSeconds == 0)

    val parsedRow2: ParsedRow = parseJson(
      """[{
        |"fn":["ГАЗ Газель"],
        |"year":["1998"],
        |"info":["{
        |  \"mileage\":[\"45000\"],
        |  \"car-type\":[\"новые\"]
        |}"],
        |"parse_date":["2016-04-17T12:00:08.000+03:00"]}]""".stripMargin.replace("\n", ""),
      optUrl = Some(toAvitoCarsUrl(mobileUrl)),
      prevRow = Some(parsedRow)
    )

    assert(parsedRow2.url == mobileUrl)
    assert(parsedRow2.data.getOffer.getAdditionalInfo.getRemoteUrl == mobileUrl)
  }

  test("insertOnly: category") {
    val avitoCarsUrl = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(avitoCarsUrl, parsedOffer, category = Category.CARS, source = Source.AV100)

    parsedOffer
      .addStatusHistoryBuilder()
      .addDiffBuilder()
      .setName(OfferFields.ParseDate)
      .setOldValue("2019-02-07T10:54:31.000+03:00")
      .setNewValue("2019-02-20T14:29:59.000+03:00")

    parsedOffer.getParseDateBuilder.setSeconds(DateUtils.jodaParse("2019-02-20T14:29:59.000+03:00").getMillis / 1000)

    assert(parsedRow.data.getMaxParseDate.getSeconds == 0)

    val avitoTrucksUrl = testAvitoTrucksUrl
    val parsedRow2: ParsedRow = parseTrucksJson(
      """[{
        |"fn":["ГАЗ Газель"],
        |"year":["1998"],
        |"info":["{
        |  \"mileage\":[\"45000\"],
        |  \"car-type\":[\"новые\"]
        |}"],
        |"parse_date":["2016-04-17T12:00:08.000+03:00"]}]""".stripMargin.replace("\n", ""),
      optUrl = Some(avitoTrucksUrl),
      prevRow = Some(parsedRow)
    )

    assert(parsedRow2.url == avitoCarsUrl)
    assert(parsedRow2.data.getOffer.getAdditionalInfo.getRemoteUrl == avitoCarsUrl)
    assert(parsedRow2.category == Category.CARS)
    assert(parsedRow2.data.getOffer.getCategory == Category.CARS)
  }

  test("insertOnly=false: dealer_name") {
    val mobileUrl = testAvitoMobileCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    val parsedRow: ParsedRow = testRow(mobileUrl, parsedOffer, category = Category.CARS, source = Source.AV100)

    assert(parsedRow.data.getDealerName.isEmpty)
    assert(parsedRow.data.getDealerUrl.isEmpty)

    parsedOffer
      .addStatusHistoryBuilder()
      .addDiffBuilder()
      .setName(OfferFields.ParseDate)
      .setOldValue("2019-02-07T10:54:31.000+03:00")
      .setNewValue("2019-02-20T14:29:59.000+03:00")

    parsedOffer.getParseDateBuilder.setSeconds(DateUtils.jodaParse("2019-02-20T14:29:59.000+03:00").getMillis / 1000)

    assert(parsedRow.data.getMaxParseDate.getSeconds == 0)

    val parsedRow2: ParsedRow = parseJson(
      """{
          |"car_name":"Хендай Элантра",
          |"car_year":"1998",
          |"car_mileage":"45000",
          |"dealership_name":"dealer name",
          |"dealership_url":"dealer url",
          |"sh_last_visited":"2016-04-17T12:00:08.000+03:00"}""".stripMargin.replace("\n", ""),
      optUrl = Some(toAvitoCarsUrl(mobileUrl)),
      prevRow = Some(parsedRow),
      parser = ScrapingHubAvitoCarsParser
    )

    assert(parsedRow2.data.getDealerName == "dealer name")
    assert(parsedRow2.data.getDealerUrl == "dealer url")
  }

  test("insertOnly=false: category and url") {
    val avitoCarsUrl = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(avitoCarsUrl, parsedOffer, category = Category.CARS, source = Source.AV100)

    parsedOffer
      .addStatusHistoryBuilder()
      .addDiffBuilder()
      .setName(OfferFields.ParseDate)
      .setOldValue("2019-02-07T10:54:31.000+03:00")
      .setNewValue("2019-02-20T14:29:59.000+03:00")

    parsedOffer.getParseDateBuilder.setSeconds(DateUtils.jodaParse("2019-02-20T14:29:59.000+03:00").getMillis / 1000)

    assert(parsedRow.data.getMaxParseDate.getSeconds == 0)

    val avitoTrucksUrl = testAvitoTrucksUrl
    val parsedRow2: ParsedRow = parseTrucksJson(
      """[{
        |"fn":["ГАЗ Газель"],
        |"year":["1998"],
        |"info":["{
        |  \"mileage\":[\"45000\"],
        |  \"car-type\":[\"новые\"]
        |}"],
        |"parse_date":["2019-04-17T12:00:08.000+03:00"]}]""".stripMargin.replace("\n", ""),
      optUrl = Some(avitoTrucksUrl),
      prevRow = Some(parsedRow)
    )

    assert(parsedRow2.url == avitoTrucksUrl)
    assert(parsedRow2.data.getOffer.getAdditionalInfo.getRemoteUrl == avitoTrucksUrl)
    assert(parsedRow2.category == Category.TRUCKS)
    assert(parsedRow2.data.getOffer.getCategory == Category.TRUCKS)
  }

  private def parseJson(rawJson: String,
                        optUrl: Option[String] = None,
                        techParamId: Option[Int] = None,
                        yearTo: Option[Int] = None,
                        prevRow: Option[ParsedRow] = None,
                        source: Source = CommonModel.Source.HTTP,
                        parser: CommonAutoParser = AvitoCarsParser) = {
    val url = optUrl.orElse(prevRow.map(_.url)).getOrElse(testAvitoCarsUrl)
    val row = ImportRow(url, Json.parse(rawJson))
    when(searchlineClient.suggest(?)(?)).thenReturn(
      Future.successful(
        ParsedSearchlineResponse(
          MarkModel(Some(Mark("GAZ")), Some(Model("GAZEL_3302")), "", category = Category.CARS),
          MarkModel(None, None, "", category = Category.TRUCKS)
        )
      )
    )
    val unifyResult = CarsUnificationCollection.newBuilder()
    val entry = unifyResult.addEntriesBuilder()
    entry.setMark("GAZ").setModel("GAZEL_3302")
    techParamId.foreach(x => entry.setTechParamId(x))
    when(searcherClient.carsUnify(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(unifyResult.build()))
    val techInfo = TechInfo.newBuilder()
    yearTo.foreach(x => techInfo.getSuperGenBuilder.setYearTo(x))
    when(searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(techParamId.map(_ => techInfo.build())))
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    when(catalogClient.find(?, Mark(?), Model(?))(?)).thenReturn(
      Future.successful(
        CatalogResponse(MarkName("Gaz"), ModelName("Gazel 3302"), RuMarkName("Газ"), RuModelName("Газель 3302"))
      )
    )
    val res = importConverter.toParsedRow(row, prevRow)(parser, source, trace)
    val parsedRow = withClue(res) {
      res.toOption.value
    }
    parsedRow
  }

  private def parseTrucksJson(rawJson: String,
                              optUrl: Option[String] = None,
                              techParamId: Option[Int] = None,
                              yearTo: Option[Int] = None,
                              prevRow: Option[ParsedRow] = None,
                              source: Source = CommonModel.Source.HTTP,
                              parser: CommonAutoParser = AvitoTrucksParser) = {
    val url = optUrl.orElse(prevRow.map(_.url)).getOrElse(testAvitoCarsUrl)
    val row = ImportRow(url, Json.parse(rawJson))
    when(searchlineClient.suggest(?)(?)).thenReturn(
      Future.successful(
        ParsedSearchlineResponse(
          MarkModel(None, None, "", category = Category.CARS),
          MarkModel(Some(Mark("GAZ")), Some(Model("GAZEL_3302")), "", category = Category.TRUCKS)
        )
      )
    )
    when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))
    val res = importConverter.toParsedRow(row, prevRow)(parser, source, trace)
    val parsedRow = withClue(res) {
      res.toOption.value
    }
    parsedRow
  }
}
