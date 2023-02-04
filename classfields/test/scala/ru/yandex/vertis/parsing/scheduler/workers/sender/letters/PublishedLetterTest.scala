package ru.yandex.vertis.parsing.scheduler.workers.sender.letters

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.unification.Unification.CarsUnificationCollection
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.auto.clients.searchline._
import ru.yandex.vertis.parsing.auto.clients.searchline.SearchlineClient.ParsedSearchlineResponse
import ru.yandex.vertis.parsing.auto.components.TestDataReceiverComponents
import ru.yandex.vertis.parsing.auto.components.unexpectedvalues.SimpleUnexpectedAutoValuesSupport
import ru.yandex.vertis.parsing.auto.converters.ImportConverterImpl
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow.PublishedLetterData
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRowUtils.RichParsedOfferOrBuilder
import ru.yandex.vertis.parsing.auto.datareceivers.DataReceiver
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.cars.avito.AvitoCarsParser
import ru.yandex.vertis.parsing.extdata.geo.{Region, RegionTypes}
import ru.yandex.vertis.parsing.importrow.ImportRow
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class PublishedLetterTest extends FunSuite with OptionValues with MockitoSupport with EmptyTraceSupport {
  private val components = TestDataReceiverComponents
//  private val wizardClient = components.wizardClient
  private val searchlineClient = components.searchlineClient
  private val searcherClient = components.searcherClient
  private val geocoder = components.geocoder
  private val dataReceiver: DataReceiver = components.dataReceiver

  private val importConverter = new ImportConverterImpl(dataReceiver)
    with SimpleUnexpectedAutoValuesSupport
    with components.TimeAwareImpl
    with components.FeaturesAwareImpl

  implicit private val source: CommonModel.Source = CommonModel.Source.HTTP

  implicit private val parser: AvitoCarsParser.type = AvitoCarsParser

  test("fromRows") {
    val url1 = "https://www.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_60873378"
    val hash1 = CommonAutoParser.hash(url1)

    val url2 = "https://www.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_60873379"
    val hash2 = CommonAutoParser.hash(url2)

    val url3 = "https://www.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_60873380"
    val hash3 = CommonAutoParser.hash(url3)

    val row1: PublishedLetterData = generateRow(
      url1,
      """[{
        |"owner":["{\"name\":[\"Петр, Петрович\"]}"],
        |"address":["Урай"],
        |"year":["1998"],
        |"phone":["+7 992 351-28-59 "],
        |"price":["120 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", ""),
      "100501-hash1",
      new DateTime(2019, 7, 17, 1, 0, 0, 0),
      Seq("phone1", "phone2"),
      Seq("photo1", "photo2"),
      vosActive = true,
      vosPhoneRedirect = true
    )
    val row2: PublishedLetterData = generateRow(
      url2,
      """[{
        |"owner":["{\"name\":[\"Вячеслав; Викторович\"]}"],
        |"address":["Урай"],
        |"year":["1999"],
        |"phone":["+7 992 351-28-60 "],
        |"price":["127 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", ""),
      "100502-hash2",
      new DateTime(2019, 7, 17, 2, 0, 0, 0),
      Seq("phone3", "phone4", "phone5"),
      Seq("photo3"),
      vosActive = false,
      vosPhoneRedirect = false
    )
    val row3: PublishedLetterData = generateRow(
      url3,
      """[{
        |"owner":["{\"name\":[\"Василий\"]}"],
        |"address":["Урай"],
        |"year":["2001"],
        |"phone":["+7 992 351-28-61 "],
        |"price":["128 000"],
        |"fn":["ГАЗ Газель"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", ""),
      "100503-hash3",
      new DateTime(2019, 7, 17, 3, 0, 0, 0),
      Seq("phone6"),
      Seq("photo5", "photo6", "photo7"),
      vosActive = true,
      vosPhoneRedirect = false
    )
    val letter =
      PublishedLetter.fromRows(Category.CARS, Seq(row1, row2, row3), "https://test.avto.ru/sales-parsing/info/")
    assert(letter.rows.length == 3)
    assert(letter.rows.head.publishedDate == new DateTime(2019, 7, 17, 1, 0, 0, 0))
    assert(letter.rows.head.callcenterUrl == s"https://test.avto.ru/sales-parsing/info/$hash1")
    assert(letter.rows.head.autoruUrl == "https://auto.ru/cars/used/sale/100501-hash1")
    assert(letter.rows.head.siteUrl == "https://www.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_60873378")
    assert(letter.rows.head.publishedPhone == "phone1")
    assert(letter.rows.head.sentPhone == "79923512859")
    assert(letter.rows.head.publishedPhotosCount == 2)
    assert(letter.rows.head.region == "Урай")

    assert(
      letter.csv ==
        """published_date;callcenter_url;autoru_url;site_url;published_phone;sent_phone;published_photos_count;region;status;redirect_phone
        |2019-07-17 01:00:00;https://test.avto.ru/sales-parsing/info/80360aa23e6e94a34c3236c091b3ed66;https://auto.ru/cars/used/sale/100501-hash1;https://www.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_60873378;phone1;79923512859;2;Урай;Активно;есть
        |2019-07-17 02:00:00;https://test.avto.ru/sales-parsing/info/3ffdb7efc7f21d34e25f8017f0488815;https://auto.ru/cars/used/sale/100502-hash2;https://www.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_60873379;phone3;79923512860;1;Урай;Неактивно;нет
        |2019-07-17 03:00:00;https://test.avto.ru/sales-parsing/info/6ae8c38b4f1612e33048d592598e1c91;https://auto.ru/cars/used/sale/100503-hash3;https://www.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_60873380;phone6;79923512861;3;Урай;Активно;нет""".stripMargin
    )
  }

  //scalastyle:off parameter.number
  private def generateRow(
      url: String,
      rawJson: String,
      offerId: String,
      publishDate: DateTime,
      publishedPhones: Seq[String],
      publishedPhotos: Seq[String],
      vosActive: Boolean,
      vosPhoneRedirect: Boolean
  )(implicit parser: CommonAutoParser): PublishedLetterData = {
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
    val parsedRow = importConverter
      .toParsedRow(row, None)
      .toOption
      .value
      .update("publish")(
        status = CommonModel.Status.PUBLISHED,
        data = builder => {
          publishedPhones.foreach(builder.addPublishedPhone)
          publishedPhotos.foreach(builder.addPublishedPhoto)
        }
      )
      .copy(
        statusUpdateDate = publishDate,
        offerId = Some(offerId)
      )
    PublishedLetterData(
      parsedRow.statusUpdateDate,
      parsedRow.hash,
      parsedRow.offerId,
      parsedRow.url,
      parsedRow.data.getPublishedPhones.headOption,
      parsedRow.data.getCurrentPhones.headOption,
      parsedRow.data.getPublishedPhotoCount,
      optRegion,
      vosActive,
      vosPhoneRedirect
    )
  }
}
