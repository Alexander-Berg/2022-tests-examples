package ru.yandex.vertis.parsing.auto.datareceivers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.Json
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.auto.clients.catalog.CatalogClient
import ru.yandex.vertis.parsing.auto.clients.searcher.SearcherClient
import ru.yandex.vertis.parsing.auto.clients.searchline._
import ru.yandex.vertis.parsing.auto.components.TestDataReceiverComponents
import ru.yandex.vertis.parsing.auto.components.unexpectedvalues.SimpleUnexpectedAutoValuesSupport
import ru.yandex.vertis.parsing.auto.datareceivers.webminer.CarsDataReceiver
import ru.yandex.vertis.parsing.auto.parsers.WebminerJsonUtils
import ru.yandex.vertis.parsing.auto.parsers.webminer.cars.avito.AvitoCarsParser
import ru.yandex.vertis.parsing.extdata.geo.{Region, RegionTypes}
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class CarsDataReceiverTest extends FunSuite with MockitoSupport with OptionValues {
  private val region = Region(11192, 121128, RegionTypes.City, "Урай", "Uray", Some(10800))

  private val optRegion = Some(region)

  implicit private val trace: Traced = TracedUtils.empty

  private val dataReceiver = new CarsDataReceiver with SimpleUnexpectedAutoValuesSupport {
    override def searchlineClient: SearchlineClient = TestDataReceiverComponents.searchlineClient

    override def catalogClient: CatalogClient = TestDataReceiverComponents.catalogClient

    override def searcherClient: SearcherClient = TestDataReceiverComponents.searcherClient
  }

  test("createUnifyRequest") {
    val url = "https://m.avito.ru/alapaevsk/avtomobili/opel_astra_2006_1441314020"
    val rawJson = Json.parse("""[{
        |"owner":["{\"name\":[\"Алексей\"]}"],
        |"address":["Свердловская область, Алапаевск"],
        |"is_dealer":["false"],
        |"year":["2006"],
        |"fn":["Opel Astra"],
        |"description":["Продам свою ласточку.Для своих лет она хороша"],
        |"photo":[
        |  "https://00.img.avito.st/640x480/4386447500.jpg",
        |  "https://81.img.avito.st/640x480/4386447481.jpg",
        |  "https://73.img.avito.st/640x480/4386447473.jpg"],
        |"price":["220 000 руб."],
        |"vin":["W0L0AHL4*75****99"],
        |"views":["{\"all\":[\"862\"],\"today\":[\"5\"]}"],
        |"info":["{
        |  \"condition\":[\"Не битый\"],
        |  \"transmission\":[\"Механика\"],
        |  \"color\":[\"Серый цвет\"],
        |  \"engine\":[\"1.6\"],
        |  \"wheel-drive\":[\"Передний привод\"],
        |  \"fuel\":[\"Бензин\"],
        |  \"wheel\":[\"Левый руль\"],
        |  \"car-type\":[\"С пробегом\"],
        |  \"power\":[\"105 л.с.\"],
        |  \"body\":[\"Хетчбэк\"],
        |  \"mileage\":[\"Пробег 162 320 км\"]}"],
        |"parse_date":["2018-06-05T00:06:36.126+03:00"],
        |"date-published":["2018-05-21"]}]""".stripMargin.replace("\n", ""))
    val json = WebminerJsonUtils.parseJson(rawJson)
    val mark = Mark("OPEL")
    val model = Model("ASTRA")
    val markName = MarkName("Opel")
    val modelName = ModelName("Astra")
    val request = dataReceiver.createUnifyRequest(json, Some(mark), Some(model), Some(markName), Some(modelName))(
      AvitoCarsParser,
      CommonModel.Source.IMPORT,
      url
    )

    assert(request.getEntries(0).getDisplacement == 1600)
    assert(request.getEntries(0).getHorsePower == 105)
    assert(request.getEntries(0).getTransmission == "MECHANICAL")
    assert(request.getEntries(0).getGearType == "FRONT")
    assert(request.getEntries(0).getEngineType == "GASOLINE")
  }
}
