package ru.yandex.vertis.parsing.auto.dao.parsedoffers

import org.joda.time.DateTime
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
import ru.yandex.vertis.parsing.auto.dao.model.{ParsedRow, QueryParams}
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.parsers.scrapinghub.cars.avito.ScrapingHubAvitoCarsParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils.testAvitoCarsUrl
import ru.yandex.vertis.parsing.auto.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.extdata.geo.{Region, RegionTypes}
import ru.yandex.vertis.parsing.importrow.ImportRow
import ru.yandex.vertis.parsing.util.DateUtils
import ru.yandex.vertis.parsing.util.DateUtils.RichDateTime
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport
import ru.yandex.vertis.parsing.validators.FilterReason.NoPhones
import ru.yandex.vertis.parsing.validators.ValidationResult
import ru.yandex.vertis.parsing.workers.importers.ImportResult

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ParsedOffersDaoImplIntTest
  extends FunSuite
  with InitTestDbs
  with OptionValues
  with MockitoSupport
  with EmptyTraceSupport {
  initDb()

  private val importConverter = components.importConverter

  private val searchlineClient = components.searchlineClient
  when(searchlineClient.suggest(?)(?)).thenReturn(
    Future.successful(
      ParsedSearchlineResponse(
        MarkModel(Some(Mark("GAZ")), Some(Model("GAZEL_3302")), "", category = Category.CARS),
        MarkModel(None, None, "", category = Category.TRUCKS)
      )
    )
  )

  private val catalogClient = components.catalogClient
  when(catalogClient.find(?, Mark(?), Model(?))(?)).thenReturn(
    Future.successful(
      CatalogResponse(MarkName("Gaz"), ModelName("Gazel 3302"), RuMarkName("Газ"), RuModelName("Газель 3302"))
    )
  )

  private val searcherClient = components.searcherClient
  private val unifyResult = CarsUnificationCollection.newBuilder()
  unifyResult.addEntriesBuilder().setMark("GAZ").setModel("GAZEL_3302")
  when(searcherClient.carsUnify(?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(unifyResult.build()))

  private val geocoder = components.geocoder
  private val optRegion = Some(Region(11192, 121128, RegionTypes.City, "Урай", "Uray", Some(10800)))
  when(geocoder.getRegion(?)(?)).thenReturn(Future.successful(optRegion))

  private val dayStart = DateTime.now().withMillisOfDay(0)

  private val parsedOffersDao: ParsedOffersDao = components.parsedOffersDao

  implicit private val parser: ScrapingHubAvitoCarsParser.type = ScrapingHubAvitoCarsParser
  implicit private val source: CommonModel.Source = CommonModel.Source.SCRAPING_HUB_FRESH

  test("do not update filtered if only parse date changed") {
    setMinutes(0)

    val url = testAvitoCarsUrl
    val hash = CommonAutoParser.hash(url)
    val row = assertSuccessImportConvert(
      ImportRow(
        url,
        Json.parse(s"""{
         |"car_name":"ГАЗ Газель",
         |"car_extrainfo":"Description1",
         |"sh_last_visited":"${DateUtils.jodaFormat(getNow)}"}""".stripMargin.replace("\n", ""))
      )
    )

    assertSaveResult(row, inserted = true)

    setMinutes(1)

    assertFailedFiltration(hash)(NoPhones)

    setMinutes(2)

    val row2 = assertSuccessImportConvert(
      ImportRow(
        url,
        Json.parse(s"""{
         |"car_name":"ГАЗ Газель",
         |"car_extrainfo":"Description1",
         |"sh_last_visited":"${DateUtils.jodaFormat(getNow)}"}""".stripMargin.replace("\n", ""))
      )
    )

    assertSkip(row2)
  }

  test("do not update sent if only parse date changed") {
    setMinutes(0)

    val callCenter = "te_ex"
    val url = testAvitoCarsUrl
    val hash = CommonAutoParser.hash(url)
    val row = assertSuccessImportConvert(
      ImportRow(
        url,
        Json.parse(s"""{
         |"car_name":"ГАЗ Газель",
         |"car_extrainfo":"Description1",
         |"sh_last_visited":"${DateUtils.jodaFormat(getNow)}"}""".stripMargin.replace("\n", ""))
      )
    )

    assertSaveResult(row, inserted = true)

    setMinutes(1)

    assertSuccessFiltration(hash)

    setMinutes(2)

    assertSuccessCallCenterSet(hash, callCenter)

    setMinutes(3)
    assertSuccessSending(callCenter, hash)

    setMinutes(4)
    assertSuccessSent(hash)

    setMinutes(5)
    val row2 = assertSuccessImportConvert(
      ImportRow(
        url,
        Json.parse(s"""{
         |"car_name":"ГАЗ Газель",
         |"car_extrainfo":"Description1",
         |"sh_last_visited":"${DateUtils.jodaFormat(getNow)}"}""".stripMargin.replace("\n", ""))
      )
    )

    assertSkip(row2)
  }

  private def assertSuccessImportConvert(importRow: ImportRow, optExistingRow: Option[ParsedRow] = None): ParsedRow = {
    val convertResult = importConverter.toParsedRow(importRow, optExistingRow)
    assert(convertResult.isRight)
    convertResult.toOption.get
  }

  private def assertSaveResult(row: ParsedRow, inserted: Boolean = false, updated: Boolean = false): Unit = {
    val result = ImportResult(parsedOffersDao.save(Seq(row)))(_.name())
    assert((result.insertedSimple == 1) == inserted)
    assert((result.updatedSimple == 1) == updated)
  }

  private def assertSkip(row: ParsedRow): Unit = {
    assertSaveResult(row)
  }

  private def assertFailedFiltration(hash: String)(errors: String*): Unit = {
    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    val result = parsedOffersDao.filter(Seq(row)) { rows =>
      rows.map(row => row -> ValidationResult.Invalid(errors)).toMap
    }
    assert(result(hash))
    val row2 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row2.data.getFilterReasonList.asScala.sorted == errors.sorted)
    assert(row2.data.getStatusHistoryList.asScala.last.getComment.split(",").sorted.toSeq == errors.sorted)
    errors.foreach(error => {
      parsedOffersDao.getHashesByParams(QueryParams(filterReasons = Seq(error), limit = Some(1))).contains(hash)
    })
  }

  private def assertSuccessFiltration(hash: String): Unit = {
    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    val result = parsedOffersDao.filter(Seq(row)) { rows =>
      rows.map(row => row -> ValidationResult.Valid).toMap
    }
    assert(result(hash))
  }

  private def assertSuccessCallCenterSet(hash: String, callCenter: String): Unit = {
    val result = parsedOffersDao.setCallCenter(Seq(hash), callCenter)
    assert(result(hash))
  }

  private def assertSuccessSending(callCenter: String, hash: String) = {
    val result = parsedOffersDao.getOrSetSendingForCallCenter(callCenter, Category.CARS, 0, 100)
    assert(result.length == 1)
    assert(result.head.status == CommonModel.Status.SENDING)
    assert(result.head.hash == hash)
    assert(result.head.sentDate.isEmpty)
  }

  private def assertSuccessSent(hash: String): Unit = {
    val result = parsedOffersDao.setSent(Seq(hash))
    assert(result(hash))
  }

  private def setMinutes(minutes: Int) = {
    setTime(dayStart.plusMinutes(minutes))
  }

  private def setTime(date: DateTime) = {
    when(components.timeService.getNow).thenReturn(date)
  }

  private def getNow: DateTime = components.timeService.getNow

  private def getSeconds: Long = components.timeService.getNow.getSeconds
}
