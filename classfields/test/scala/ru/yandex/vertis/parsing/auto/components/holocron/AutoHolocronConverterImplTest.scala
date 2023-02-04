package ru.yandex.vertis.parsing.auto.components.holocron

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FunSuite, OptionValues}
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.CatalogModel.TechInfo
import ru.vertis.holocron.autoru.external.ChangeSource
import ru.vertis.holocron.common.{Action, Classified, HoloOffer}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.CommonModel.Source
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.dao.holocron.HolocronSaveDao
import ru.yandex.vertis.parsing.auto.dao.model.{ParsedRow, QueryParams}
import ru.yandex.vertis.parsing.auto.dao.parsedoffers.{ParsedOffersDao, ParsedOffersDaoImpl}
import ru.yandex.vertis.parsing.auto.diffs.OfferFields
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.auto.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.validators.FilterReason._
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.components.time.TimeService
import ru.yandex.vertis.parsing.holocron.HolocronSendResult
import ru.yandex.vertis.parsing.holocron.validation.HolocronValidationResult
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.parsing.util.{DateUtils, RandomUtil}
import ru.yandex.vertis.parsing.validators.ValidationResult
import ru.yandex.vertis.parsing.workers.importers.ImportResult
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.validation.model.MissingRequiredField
import com.google.protobuf.util.{JsonFormat, Timestamps}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

/**
  * TODO
  *
  * @author aborunov
  */
//noinspection ScalaStyle
@RunWith(classOf[JUnitRunner])
class AutoHolocronConverterImplTest
  extends FunSuite
  with InitTestDbs
  with OptionValues
  with MockitoSupport
  with ScalaFutures
  with Eventually {
  initDb()

  private val parsedOffersDao: ParsedOffersDao =
    new ParsedOffersDaoImpl
      with components.ParsingShardsAwareImpl
      with components.TimeAwareImpl
      with components.DiffAnalyzerFactoryAwareImpl
      with components.WatchersAwareImpl
      with components.CatalogsAwareImpl
      with components.HolocronConverterAwareImpl
      with HolocronSaveDao
      with components.ExecutionContextAwareImpl

  private val holocronDao = components.holocronDao

  private val holocronConverter = components.holocronConverter

  private val techInfo = TechInfo.newBuilder()
  techInfo.getMarkInfoBuilder.setName("MERCEDES")
  techInfo.getModelInfoBuilder.setName("814")
  when(components.searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(Some(techInfo.build())))

  private val dayStart = DateTime.now().withMillisOfDay(0)

  implicit private val trace: Traced = TracedUtils.empty

  private val timeService: TimeService = components.timeService

  test("do not call searcherClient.carsCatalogData if techParamId is zero") {
    reset(components.searcherClient)
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()
    updateOffer(parsedOffer, section = Section.USED)

    val parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    assert(parsedRow.data.getOffer.getCarInfo.getTechParamId == 0)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getSection == Section.USED)

    verifyZeroInteractions(components.searcherClient)
  }

  test("call searcherClient.carsCatalogData if techParamId is not zero") {
    when(components.searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(Some(techInfo.build())))
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()
    updateOffer(parsedOffer, section = Section.USED)
    val techParamId = RandomUtil.nextInt(1000000, 10000000)
    parsedOffer.getOfferBuilder.getCarInfoBuilder.setTechParamId(techParamId)

    val parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    assert(parsedRow.data.getOffer.getCarInfo.getTechParamId != 0)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getSection == Section.USED)

    verify(components.searcherClient).carsCatalogData(eq(techParamId))(?)
  }

  test("do not fail conversion if call to searcherClient.carsCatalogData failed") {
    when(components.searcherClient.carsCatalogData(?)(?)).thenReturn(Future.failed(new RuntimeException("error!")))
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()
    updateOffer(parsedOffer, section = Section.USED)
    val techParamId = RandomUtil.nextInt(1000000, 10000000)
    parsedOffer.getOfferBuilder.getCarInfoBuilder.setTechParamId(techParamId)

    val parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    assert(parsedRow.data.getOffer.getCarInfo.getTechParamId != 0)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getSection == Section.USED)

    verify(components.searcherClient).carsCatalogData(eq(techParamId))(?)
  }

  test("setSection: Section set in offer") {
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()
    updateOffer(parsedOffer, section = Section.USED)

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getSection == Section.USED)
  }

  test("setSection: Section not set, year is old") {
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()
    updateOffer(parsedOffer, year = timeService.getNow.getYear - 2)

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getSection == Section.USED)
  }

  test("setSection: Section not set, year is not so old, mileage is above zero") {
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()
    updateOffer(parsedOffer, year = timeService.getNow.getYear - 1, mileage = 1000)

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getSection == Section.USED)
  }

  test("setSection: Section not set, year is not so old, mileage is zero") {
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()
    updateOffer(parsedOffer, year = timeService.getNow.getYear - 1)

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getSection == Section.NEW)
  }

  test("setSection: Section not set, year is not set, mileage is above zero") {
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()
    updateOffer(parsedOffer, mileage = 1000)

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getSection == Section.USED)
  }

  test("setSection: Section not set, year is not set, mileage is zero") {
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()
    updateOffer(parsedOffer)

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getSection == Section.NEW)
  }

  test("Action.ACTIVATE on empty status history") {
    setSecond(0)

    val url: String = testAvitoTrucksUrl
    val parsedOffer = ParsedOffer.newBuilder()
    updateOffer(parsedOffer, "test1")

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.TRUCKS, now = timeService.getNow)

    assert(holocronConverter.calculateAction(parsedRow) == Action.ACTIVATE)
  }

  test("non cars offer: convertResult isLeft") {
    setSecond(0)

    val url: String = testAvitoTrucksUrl
    val parsedOffer = ParsedOffer.newBuilder()
    updateOffer(parsedOffer, "test1")

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.TRUCKS, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue
    assert(convertResult.isUnableToConvert)
    assert(convertResult.asUnableToConvert.errors == Seq("non_cars_category"))
  }

  test("timestamp and change_version") {
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val hash: String = CommonAutoParser.hash(url)
    val parsedOffer = ParsedOffer.newBuilder()
    var source: CommonModel.Source = CommonModel.Source.HTTP
    updateOffer(parsedOffer, "test1")

    def parsedRow: ParsedRow =
      testRow(url, parsedOffer, category = Category.CARS, source = source, now = timeService.getNow)

    ensureInsert(parsedRow)

    setSecond(1)
    ensureActions(Action.ACTIVATE)
    ensureCalculatedAction(hash, Action.ACTIVATE)
    ensureTimestampAndChangeVersion(0, 1)
    ensureCreatedUpdated((0, 0))
    sendToHolocron()

    setSecond(2)
    ensureWorkBatchEmpty()
    updateOffer(parsedOffer, "test2")
    ensureUpdate(parsedRow)

    setSecond(3)
    ensureActions(Action.UPDATE)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureTimestampAndChangeVersion(2, 2)
    ensureCreatedUpdated((0, 2))
    sendToHolocron()

    setSecond(4)
    ensureWorkBatchEmpty()
    source = CommonModel.Source.SCRAPPING_HUB
    updateOffer(parsedOffer, "test4", getSecond(0))
    ensureUpdate(parsedRow)

    setSecond(5)
    ensureActions(Action.UPDATE)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureTimestampAndChangeVersion(2, 3)
    ensureCreatedUpdated((0, 2))
    sendToHolocron()

    setSecond(6)
    ensureWorkBatchEmpty()
    ensureDeactivated(hash)

    setSecond(7)
    ensureActions(Action.DEACTIVATE)
    ensureCalculatedAction(hash, Action.DEACTIVATE)
    ensureTimestampAndChangeVersion(6, 4)
    ensureCreatedUpdatedArchived((0, 6, 6))
    sendToHolocron()

    setSecond(8)
    source = CommonModel.Source.HTTP
    updateOffer(parsedOffer, "test8")
    ensureUpdate(parsedRow)

    setSecond(9)
    ensureActions(Action.ACTIVATE)
    ensureCalculatedAction(hash, Action.ACTIVATE)
    ensureTimestampAndChangeVersion(8, 5)
    ensureCreatedUpdatedArchived((0, 8, 6))
    sendToHolocron()

    setSecond(10)
    ensureWorkBatchEmpty()
    updateOffer(parsedOffer, "test10")
    ensureUpdate(parsedRow)

    setSecond(11)
    ensureActions(Action.UPDATE)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureTimestampAndChangeVersion(10, 6)
    ensureCreatedUpdatedArchived((0, 10, 6))
    sendToHolocron()

    setSecond(12)
    ensureWorkBatchEmpty()
    ensureVinSet(hash, "VIN1")

    setSecond(13)
    ensureActions(Action.UPDATE)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureTimestampAndChangeVersion(12, 7)
    ensureCreatedUpdatedArchived((0, 10, 6))
    sendToHolocron()
  }

  test("action calculation") {
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val hash: String = CommonAutoParser.hash(url)
    val callCenter = "te_ex"
    val offerId: String = "100500-hash"
    val phone: String = "79291112233"
    val parsedOffer = ParsedOffer.newBuilder()
    val source: CommonModel.Source = CommonModel.Source.HTTP
    updateOffer(parsedOffer, "test1")

    def parsedRow: ParsedRow =
      testRow(url, parsedOffer, category = Category.CARS, source = source, now = timeService.getNow)

    ensureInsert(parsedRow)

    setSecond(1)
    ensureActions(Action.ACTIVATE)
    ensureNoCallCenter()
    ensureChangeSource(ChangeSource.WEBMINING_API)
    ensureCalculatedAction(hash, Action.ACTIVATE)
    ensureCreatedUpdated((0, 0))

    setSecond(2)
    assertFailedFiltration(hash)(Older20Days, NoPhones)
    ensureActions(Action.ACTIVATE)
    ensureNoCallCenter()
    ensureCalculatedAction(hash, Action.ACTIVATE)
    ensureCreatedUpdated((0, 0))

    setSecond(3)
    updateOffer(parsedOffer, "test3")
    ensureUpdate(parsedRow, expectedHoloQueueSize = 2)
    ensureActions(Action.ACTIVATE)
    ensureNoCallCenter()
    ensureCreatedUpdated((0, 0))

    setSecond(4)
    ensureActions(Action.ACTIVATE, Action.UPDATE)
    ensureNoCallCenter()
    ensureChangeSource(ChangeSource.WEBMINING_API)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdated((0, 0), (0, 3))
    sendToHolocron()

    setSecond(5)
    ensureWorkBatchEmpty()
    ensureCalculatedAction(hash, Action.UPDATE)

    setSecond(6)
    updateOffer(parsedOffer, "test6")
    ensureUpdate(parsedRow)
    ensureWorkBatchEmpty()

    setSecond(7)
    ensureActions(Action.UPDATE)
    ensureNoCallCenter()
    ensureChangeSource(ChangeSource.WEBMINING_API)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdated((0, 6))

    setSecond(8)
    assertSuccessFiltration(hash)
    ensureActions(Action.UPDATE)
    ensureNoCallCenter()
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdated((0, 6))

    setSecond(9)
    assertSuccessCallCenterSet(hash, callCenter)
    ensureActions(Action.UPDATE)
    ensureNoCallCenter()
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdated((0, 6))

    setSecond(10)
    assertNonEmptySendingRows(callCenter)
    ensureActions(Action.UPDATE)
    ensureNoCallCenter()
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdated((0, 6))

    setSecond(11)
    assertSuccessSent(hash)
    ensureActions(Action.UPDATE)
    ensureNoCallCenter()
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdated((0, 6))

    setSecond(12)
    ensureCallCenter(callCenter)
    ensureChangeSource(ChangeSource.PARSING)
    assertSuccessPublished(hash, offerId, phone)
    ensureActions(Action.UPDATE, Action.UPDATE)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdated((0, 6), (0, 6))

    setSecond(13)
    ensureDeactivated(hash, expectedHoloQueueSize = 3)
    ensureActions(Action.UPDATE, Action.UPDATE)
    ensureCallCenter(callCenter)
    ensureChangeSource(ChangeSource.PARSING)

    setSecond(14)
    ensureActions(Action.UPDATE, Action.UPDATE, Action.DEACTIVATE)
    ensureCallCenter(callCenter)
    ensureChangeSource(ChangeSource.PARSING)
    ensureCalculatedAction(hash, Action.DEACTIVATE)
    ensureDates((0, 6, None), (0, 6, None), (0, 13, Some(13)))
    sendToHolocron()

    setSecond(15)
    ensureWorkBatchEmpty()
    updateOffer(parsedOffer, "test15")
    ensureUpdate(parsedRow)
    ensureWorkBatchEmpty()

    setSecond(16)
    ensureActions(Action.ACTIVATE)
    ensureCallCenter(callCenter)
    ensureChangeSource(ChangeSource.WEBMINING_API)
    ensureCalculatedAction(hash, Action.ACTIVATE)
    ensureCreatedUpdatedArchived((0, 15, 13))

    setSecond(17)
    updateOffer(parsedOffer, "test17")
    ensureUpdate(parsedRow, expectedHoloQueueSize = 2)
    ensureActions(Action.ACTIVATE)
    ensureCallCenter(callCenter)
    ensureCreatedUpdatedArchived((0, 15, 13))

    setSecond(18)
    ensureActions(Action.ACTIVATE, Action.UPDATE)
    ensureCallCenter(callCenter)
    ensureChangeSource(ChangeSource.WEBMINING_API)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdatedArchived((0, 15, 13), (0, 17, 13))
    sendToHolocron()

    setSecond(19)
    ensureWorkBatchEmpty()
    updateOffer(parsedOffer, "test19")
    ensureUpdate(parsedRow)
    ensureWorkBatchEmpty()

    setSecond(20)
    ensureActions(Action.UPDATE)
    ensureCallCenter(callCenter)
    ensureChangeSource(ChangeSource.WEBMINING_API)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdatedArchived((0, 19, 13))

    setSecond(21)
    updateOffer(parsedOffer, "test21")
    ensureUpdate(parsedRow, expectedHoloQueueSize = 2)
    ensureActions(Action.UPDATE)
    ensureCallCenter(callCenter)
    ensureCreatedUpdatedArchived((0, 19, 13))

    setSecond(22)
    ensureActions(Action.UPDATE, Action.UPDATE)
    ensureCallCenter(callCenter)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdatedArchived((0, 19, 13), (0, 21, 13))
    sendToHolocron()

    setSecond(23)
    ensureWorkBatchEmpty()
    ensureVinSet(hash, "VIN1")
    ensureWorkBatchEmpty()

    setSecond(24)
    ensureActions(Action.UPDATE)
    ensureCallCenter(callCenter)
    ensureChangeSource(ChangeSource.VINSCRAPPER)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdatedArchived((0, 21, 13))
    sendToHolocron()
  }

  test("set vin after deactivation") {
    setSecond(0)

    val url: String = testAvitoCarsUrl
    val hash: String = CommonAutoParser.hash(url)
    val parsedOffer = ParsedOffer.newBuilder()
    val source: CommonModel.Source = CommonModel.Source.HTTP
    updateOffer(parsedOffer, "test1")

    def parsedRow: ParsedRow =
      testRow(url, parsedOffer, category = Category.CARS, source = source, now = timeService.getNow)

    ensureInsert(parsedRow)

    setSecond(1)
    ensureActions(Action.ACTIVATE)
    ensureCalculatedAction(hash, Action.ACTIVATE)
    ensureCreatedUpdated((0, 0))
    ensureTimestampAndChangeVersion(0, 1)
    sendToHolocron()
    ensureVinSet(hash, "VIN2")

    // setVin после ACTIVATE
    setSecond(2)
    ensureActions(Action.UPDATE)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdated((0, 0))
    ensureTimestampAndChangeVersion(1, 2)
    sendToHolocron()
    updateOffer(parsedOffer, "test2")
    ensureUpdate(parsedRow)

    setSecond(3)
    ensureActions(Action.UPDATE)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdated((0, 2))
    ensureTimestampAndChangeVersion(2, 3)
    sendToHolocron()
    ensureVinSet(hash, "VIN3")

    // setVin после UPDATE
    setSecond(4)
    ensureActions(Action.UPDATE)
    ensureCalculatedAction(hash, Action.UPDATE)
    ensureCreatedUpdated((0, 2))
    ensureTimestampAndChangeVersion(3, 4)
    sendToHolocron()
    ensureDeactivated(hash)

    setSecond(5)
    ensureActions(Action.DEACTIVATE)
    ensureCalculatedAction(hash, Action.DEACTIVATE)
    ensureCreatedUpdatedArchived((0, 4, 4))
    ensureTimestampAndChangeVersion(4, 5)
    sendToHolocron()
    ensureVinSet(hash, "VIN6")

    // setVin после DEACTIVATE
    setSecond(6)
    ensureActions(Action.DEACTIVATE)
    ensureCalculatedAction(hash, Action.DEACTIVATE)
    ensureCreatedUpdatedArchived((0, 4, 4))
    ensureTimestampAndChangeVersion(5, 6)
    sendToHolocron()
  }

  test("set vin") {
    setSecond(0)

    val url: String = "https://m.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_1546910785"
    val parsedOffer = ParsedOffer.newBuilder()
    val source: CommonModel.Source = CommonModel.Source.HTTP
    parsedOffer.getOfferBuilder.getDocumentsBuilder.setVin("VIN1")

    val parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, source = source)

    ensureVin(parsedRow, "VIN1")
  }

  test("set vin from vinMask") {
    setSecond(0)

    val url: String = "https://m.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_1546910785"
    val parsedOffer = ParsedOffer.newBuilder()
    val source: CommonModel.Source = CommonModel.Source.HTTP
    parsedOffer.setVinMask("VIN1")

    val parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, source = source)

    ensureVin(parsedRow, "VIN1")
  }

  test("set vin when vin and vinMask both set") {
    setSecond(0)

    val url: String = "https://m.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_1546910785"
    val parsedOffer = ParsedOffer.newBuilder()
    val source: CommonModel.Source = CommonModel.Source.HTTP
    parsedOffer.getOfferBuilder.getDocumentsBuilder.setVin("VIN1")
    parsedOffer.setVinMask("VIN2")

    val parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, source = source)

    ensureVin(parsedRow, "VIN1")
  }

  test("setId") {
    setSecond(0)

    val url: String = "https://m.avito.ru/taganrog/avtomobili/prodam_gazel_biznes_1546910785"
    val parsedOffer = ParsedOffer.newBuilder()
    val source: CommonModel.Source = CommonModel.Source.HTTP
    updateOffer(parsedOffer, "test1")

    def parsedRow(remoteId: String): ParsedRow = {
      testRow(url, parsedOffer, category = Category.CARS, source = source)
        .update("update remote Id")(data = data => data.getOfferBuilder.getAdditionalInfoBuilder.setRemoteId(remoteId))
    }

    ensureCarId(parsedRow("1546910785"), "1546910785")
    ensureCarId(parsedRow("avito|1546910785"), "avito|1546910785")
    ensureCarId(parsedRow("avito|cars|1546910785"), "avito|cars|1546910785")
    ensureCarId(parsedRow("pewpew|1546910785"), "pewpew|1546910785")
    ensureCarId(parsedRow("pewpew|trucks|1546910785"), "pewpew|trucks|1546910785")
    ensureCarId(parsedRow("pewpew|abc|1546910785"), "pewpew|abc|1546910785")
    ensureCarId(parsedRow("blabla"), "blabla")
  }

  test("strict validation: invalid") {
    when(components.holocronValidator.validate(?, ?, ?))
      .thenReturn(HolocronValidationResult.Invalid(Seq(MissingRequiredField("fieldName"))))
    components.features.StrictHolocronValidation.setEnabled(false)

    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, now = timeService.getNow)

    val convertResult0 = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult0.isConverted)

    components.features.StrictHolocronValidation.setEnabled(true)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isUnableToValidate)
    assert(convertResult.asUnableToValidate.errors == Seq(MissingRequiredField("fieldName")))

    eventually {
      verify(components.holocronValidator, times(2))
        .validate(eq(Site.Avito), eq(Source.HTTP), eq(convertResult0.asConverted.holoOffer))
    }

    components.features.StrictHolocronValidation.setEnabled(false)
    when(components.holocronValidator.validate(?, ?, ?)).thenReturn(HolocronValidationResult.Valid)
  }

  test("strict validation: exception") {
    reset(components.holocronValidator)
    when(components.holocronValidator.validate(?, ?, ?)).thenThrow(classOf[RuntimeException])
    components.features.StrictHolocronValidation.setEnabled(false)

    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, now = timeService.getNow)

    val convertResult0 = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult0.isConverted)

    components.features.StrictHolocronValidation.setEnabled(true)

    intercept[RuntimeException] {
      holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    }

    eventually {
      verify(components.holocronValidator, times(2)).validate(eq(Site.Avito), eq(Source.HTTP), ?)
    }

    components.features.StrictHolocronValidation.setEnabled(false)
    reset(components.holocronValidator)
    when(components.holocronValidator.validate(?, ?, ?)).thenReturn(HolocronValidationResult.Valid)
  }

  test("strict validation: skipValidation") {
    when(components.holocronValidator.validate(?, ?, ?))
      .thenReturn(HolocronValidationResult.Invalid(Seq(MissingRequiredField("fieldName"))))
    components.features.StrictHolocronValidation.setEnabled(false)

    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, now = timeService.getNow)

    val convertResult0 = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult0.isConverted)

    components.features.StrictHolocronValidation.setEnabled(true)

    val convertResult =
      holocronConverter.convert(parsedRow, skipValidation = true).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(convertResult.asConverted == convertResult0.asConverted)

    eventually {
      verify(components.holocronValidator, times(2))
        .validate(eq(Site.Avito), eq(Source.HTTP), eq(convertResult.asConverted.holoOffer))
    }

    when(components.holocronValidator.validate(?, ?, ?)).thenReturn(HolocronValidationResult.Valid)
    components.features.StrictHolocronValidation.setEnabled(false)
  }

  test("strict validation new classifieds: valid") {
    when(components.holocronValidator.validate(?, ?, ?)).thenReturn(HolocronValidationResult.Valid)
    components.features.StrictHolocronValidation.setEnabled(false)

    setSecond(0)

    val url: String = testE1CarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, now = timeService.getNow)

    val convertResult0 = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult0.isConverted)

    components.features.StrictHolocronValidation.setEnabled(true)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(convertResult.asConverted == convertResult0.asConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getClassified == Classified.E1)

    eventually {
      verify(components.holocronValidator, times(2))
        .validate(eq(Site.E1), eq(Source.HTTP), eq(convertResult.asConverted.holoOffer))
    }

    components.features.StrictHolocronValidation.setEnabled(false)
  }

  test("strict validation: valid") {
    when(components.holocronValidator.validate(?, ?, ?)).thenReturn(HolocronValidationResult.Valid)
    components.features.StrictHolocronValidation.setEnabled(false)

    setSecond(0)

    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, now = timeService.getNow)

    val convertResult0 = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult0.isConverted)

    components.features.StrictHolocronValidation.setEnabled(true)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(convertResult.asConverted == convertResult0.asConverted)

    eventually {
      verify(components.holocronValidator, times(2))
        .validate(eq(Site.Avito), eq(Source.HTTP), eq(convertResult.asConverted.holoOffer))
    }

    components.features.StrictHolocronValidation.setEnabled(false)
  }

  test("max parse date from history") {
    val url = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow =
      testRow(url, parsedOffer, category = Category.CARS, source = Source.AV100, now = timeService.getNow)

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

    assert(parsedRow.data.getMaxParseDate.getSeconds == 0)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(
      convertResult.asConverted.holoOffer.getTimestamp.getSeconds ==
        DateUtils.jodaParse("2019-02-20T14:29:59.000+03:00").getMillis / 1000
    )
  }

  test("displayed_publish_date") {
    val url = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow =
      testRow(url, parsedOffer, category = Category.CARS, source = Source.SCRAPING_HUB_FRESH, now = timeService.getNow)

    parsedOffer.getDisplayedPublishDateBuilder.setSeconds(new DateTime(2019, 7, 25, 0, 0, 0, 0).getMillis / 1000)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(
      convertResult.asConverted.holoOffer.getCar.getDisplayedPublishDate.getSeconds ==
        new DateTime(2019, 7, 25, 0, 0, 0, 0).getMillis / 1000
    )
  }

  test("keep deactivated on reimport") {
    val url = testAvitoCarsUrl
    val json = scala.io.Source
      .fromInputStream(this.getClass.getResourceAsStream("/keep_deactivated_on_reimport.json"))
      .getLines()
      .mkString("\n")
    val parsedOffer = ParsedOffer.newBuilder()
    JsonFormat.parser().merge(json, parsedOffer)

    def parsedRow: ParsedRow =
      testRow(url, parsedOffer, category = Category.CARS, source = Source.SCRAPING_HUB_FULL, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))

    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getAction == Action.DEACTIVATE)
    assert(convertResult.asConverted.holoOffer.getChangeVersion == 9)
    assert(
      convertResult.asConverted.holoOffer.getTimestamp == Timestamps
        .fromMillis(DateTime.parse("2021-06-10T09:08:13Z").getMillis)
    )
  }

  test("deactivation event after reimport") {
    val url = testAvitoCarsUrl
    val json = scala.io.Source
      .fromInputStream(this.getClass.getResourceAsStream("/deactivation_event_after_reimport.json"))
      .getLines()
      .mkString("\n")
    val parsedOffer = ParsedOffer.newBuilder()
    JsonFormat.parser().merge(json, parsedOffer)

    def parsedRow: ParsedRow =
      testRow(
        url,
        parsedOffer,
        category = Category.CARS,
        source = Source.SCRAPING_HUB_FULL,
        now = timeService.getNow,
        deactivateDate = Some(DateTime.parse("2021-06-10T09:08:13Z"))
      )

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))

    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getAction == Action.DEACTIVATE)
    assert(convertResult.asConverted.holoOffer.getChangeVersion == 10)
    val holoTimestamp = convertResult.asConverted.holoOffer.getTimestamp
    val holoTimestampMillis = Timestamps.toMillis(holoTimestamp)
    val holoTimestampDateTime = new DateTime(holoTimestampMillis)
    withClue(holoTimestampDateTime.toString + " : " + DateTime.parse("2021-06-10T09:08:13Z")) {
      assert(
        Timestamps
          .toMillis(holoTimestamp) == DateTime.parse("2021-06-10T09:08:13Z").getMillis
      )
    }
  }

  test("don't set license plate") {
    val url = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow =
      testRow(url, parsedOffer, category = Category.CARS, source = Source.AV100, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getLicensePlate == "")
  }

  test("set license plate with more photos") {
    val url = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    val lp1 = randomLicensePlate
    val lp2 = randomLicensePlate
    val confidence11 = 0.5
    val confidence12 = 0.4
    val confidence2 = 0.9
    parsedOffer
      .addRecognizedLicensePlateBuilder()
      .setLicensePlate(lp1)
      .setPhoto(testAvitoPhotoUrl)
      .setConfidence(confidence11)
    parsedOffer
      .addRecognizedLicensePlateBuilder()
      .setLicensePlate(lp1)
      .setPhoto(testAvitoPhotoUrl)
      .setConfidence(confidence12)
    parsedOffer
      .addRecognizedLicensePlateBuilder()
      .setLicensePlate(lp2)
      .setPhoto(testAvitoPhotoUrl)
      .setConfidence(confidence2)

    def parsedRow: ParsedRow =
      testRow(url, parsedOffer, category = Category.CARS, source = Source.AV100, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getLicensePlate == lp1)
  }

  test("set license plate with max confidence") {
    val url = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    val lp1 = randomLicensePlate
    val lp2 = randomLicensePlate
    val confidence1 = 0.5
    val confidence2 = 0.4
    parsedOffer
      .addRecognizedLicensePlateBuilder()
      .setLicensePlate(lp1)
      .setPhoto(testAvitoPhotoUrl)
      .setConfidence(confidence1)
    parsedOffer
      .addRecognizedLicensePlateBuilder()
      .setLicensePlate(lp2)
      .setPhoto(testAvitoPhotoUrl)
      .setConfidence(confidence2)

    def parsedRow: ParsedRow =
      testRow(url, parsedOffer, category = Category.CARS, source = Source.AV100, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))
    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getCar.getLicensePlate == lp1)
  }

  test("parse after deactivate but older") {
    val url = testAvitoCarsUrl
    val json = scala.io.Source
      .fromInputStream(this.getClass.getResourceAsStream("/parse_after_deactivate_but_older.json"))
      .getLines()
      .mkString("\n")
    val parsedOffer = ParsedOffer.newBuilder()
    JsonFormat.parser().merge(json, parsedOffer)
    def parsedRow: ParsedRow =
      testRow(url, parsedOffer, category = Category.CARS, source = Source.SCRAPING_HUB_FULL, now = timeService.getNow)

    val convertResult = holocronConverter.convert(parsedRow).futureValue(Timeout(Span(10, Seconds)))

    assert(convertResult.isConverted)
    assert(convertResult.asConverted.holoOffer.getAction == Action.DEACTIVATE)
    assert(convertResult.asConverted.holoOffer.getChangeVersion == 5)
    assert(
      convertResult.asConverted.holoOffer.getTimestamp == Timestamps
        .fromMillis(DateTime.parse("2021-01-19T05:35:07Z").getMillis)
    )
  }

  private def setSecond(second: Int) = {
    when(timeService.getNow).thenReturn(getSecond(second))
  }

  private def getSecond(second: Int) = {
    dayStart.withSecondOfMinute(second)
  }

  private def ensureInsert(parsedRow: ParsedRow, expectedHoloQueueSize: Int = 1): Unit = {
    val saveResult2 = ImportResult(parsedOffersDao.save(Seq(parsedRow)))(_.name())
    assert(saveResult2.insertedSimple == 1)
    assert(saveResult2.updatedSimple == 0)
    ensureHolocronInsert(expectedHoloQueueSize)
  }

  private def ensureUpdate(parsedRow: ParsedRow, expectedHoloQueueSize: Int = 1): Unit = {
    val saveResult2 = ImportResult(parsedOffersDao.save(Seq(parsedRow)))(_.name())
    assert(saveResult2.insertedSimple == 0)
    assert(saveResult2.updatedSimple == 1)
    ensureHolocronInsert(expectedHoloQueueSize)
  }

  private def ensureVinSet(hash: String, vin: String, expectedHoloQueueSize: Int = 1): Unit = {
    val result = parsedOffersDao.setVins(Map(hash -> vin))
    assert(result(hash))
    ensureHolocronInsert(expectedHoloQueueSize)
  }

  private def ensureDeactivated(hash: String, expectedHoloQueueSize: Int = 1): Unit = {
    val result = parsedOffersDao.setDeactivated(Seq(hash))
    assert(result(hash))
    ensureHolocronInsert(expectedHoloQueueSize)
  }

  private def ensureHolocronInsert(expectedSize: Int = 1): Unit = {
    eventually(Timeout(Span(2, Seconds))) {
      assert(components.statsDao.holocronQueueSize() == expectedSize)
    }
  }

  private def assertSuccessCallCenterSet(hash: String, callCenter: String): Unit = {
    val result = parsedOffersDao.setCallCenter(Seq(hash), callCenter)
    assert(result(hash))
  }

  private def assertNonEmptySendingRows(callCenter: String): Unit = {
    val rows = parsedOffersDao.getOrSetSendingForCallCenter(callCenter, Category.CARS, 0, 10)
    assert(rows.nonEmpty)
  }

  private def assertSuccessSent(hash: String): Unit = {
    val result = parsedOffersDao.setSent(Seq(hash))
    assert(result(hash))
  }

  private def assertSuccessPublished(hash: String, offerId: String, phone: String) = {
    val result = parsedOffersDao.setPublished(hash, offerId, Seq(phone))
    assert(result.value)
  }

  private def assertSuccessFiltration(hash: String): Unit = {
    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    val result = parsedOffersDao.filter(Seq(row)) { rows =>
      rows.map(row => row -> ValidationResult.Valid).toMap
    }
    assert(result(hash))
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

  private def ensureCalculatedAction(hash: String, action: Action): Unit = {
    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    val calculated = holocronConverter.calculateAction(row)
    assert(calculated == action)
  }

  private def ensureVin(row: ParsedRow, vin: String): Unit = {
    val convertResult = withClue(row.data.getStatusHistoryList.asScala) {
      holocronConverter.convert(row).futureValue
    }
    assert(convertResult.isConverted)
    val holoOffer = convertResult.asConverted.holoOffer
    assert(holoOffer.getCar.getVin == vin)
  }

  private def ensureCarId(row: ParsedRow, id: String): Unit = {
    val convertResult = withClue(row.data.getStatusHistoryList.asScala) {
      holocronConverter.convert(row).futureValue
    }
    assert(convertResult.isConverted)
    val holoOffer = convertResult.asConverted.holoOffer
    assert(holoOffer.getCar.getId == id)
  }

  private def ensureActions(actions: Action*): Unit = {
    eventually(Timeout(Span(2, Seconds))) {
      val rows = holocronDao.getWorkBatch(10)
      assert(rows.length == actions.length)
      rows.zipWithIndex.zip(actions).foreach {
        case ((row, idx), action) =>
          assert(row.action == action, s"row $idx")
      }
    }
  }

  private def ensureNoCallCenter(): Unit = {
    eventually(Timeout(Span(2, Seconds))) {
      val row = holocronDao.getWorkBatch(10).lastOption.value
      assert(row.optHoloOffer.value.getRawExternalAuto.getCallCenter.isEmpty)
    }
  }

  private def ensureChangeSource(changeSource: ChangeSource): Unit = {
    eventually(Timeout(Span(2, Seconds))) {
      val row = holocronDao.getWorkBatch(10).lastOption.value
      assert(row.optHoloOffer.value.getRawExternalAuto.getChangeSource == changeSource)
    }
  }

  private def ensureCallCenter(callCenter: String): Unit = {
    eventually(Timeout(Span(2, Seconds))) {
      val row = holocronDao.getWorkBatch(10).lastOption.value
      assert(row.optHoloOffer.value.getRawExternalAuto.getCallCenter == callCenter)
    }
  }

  private def ensureCreatedUpdatedArchived(dates: (Int, Int, Int)*): Unit = {
    ensureDates(dates.map(d => (d._1, d._2, Some(d._3))): _*)
  }

  private def ensureCreatedUpdated(dates: (Int, Int)*): Unit = {
    ensureDates(dates.map(d => (d._1, d._2, None)): _*)
  }

  private def ensureDates(dates: (Int, Int, Option[Int])*): Unit = {
    val rows = holocronDao.getWorkBatch(10)
    assert(rows.length == dates.length)
    rows.zipWithIndex.zip(dates).foreach {
      case ((row, idx), (created, updated, archived)) =>
        withClue(row.row.data.getStatusHistoryList.asScala) {
          assert(
            row.optHoloOffer.value.getCar.getCreated.getSeconds == getSecond(created).getMillis / 1000,
            s"row $idx"
          )
          assert(
            row.optHoloOffer.value.getCar.getUpdated.getSeconds == getSecond(updated).getMillis / 1000,
            s"row $idx"
          )
          archived match {
            case None =>
              assert(!row.optHoloOffer.value.getCar.hasArchived, s"row $idx")
            case Some(a) =>
              assert(row.optHoloOffer.value.getCar.getArchived.getSeconds == getSecond(a).getMillis / 1000, s"row $idx")
          }
        }
    }
  }

  private def ensureUnableToConvert(): Unit = {
    val rows = holocronDao.getWorkBatch(10)
    assert(rows.length == 1)
    val row = rows.headOption.value
    val convertResult = holocronConverter.convert(row.row).futureValue
    assert(convertResult.isUnableToConvert)
  }

  private def ensureTimestampAndChangeVersion(expectedSecond: Int, exptectedChangeVersion: Int): Unit = {
    val expectedTimestamp: DateTime = getSecond(expectedSecond)
    val rows = holocronDao.getWorkBatch(10)
    assert(rows.length == 1)
    val row = rows.headOption.value
    val convertResult = holocronConverter.convert(row.row).futureValue
    withClue(row.row) {
      assert(convertResult.isConverted)
      val holoOffer = convertResult.asConverted.holoOffer
      assert(holoOffer.getTimestamp.getSeconds == expectedTimestamp.getMillis / 1000)
      assert(holoOffer.getChangeVersion == exptectedChangeVersion)
      assert(holoOffer.getAction == row.action)
    }
  }

  private def ensureWorkBatchEmpty() = {
    assert(holocronDao.getWorkBatch(10).isEmpty)
  }

  private def sendToHolocron(): Unit = {
    val rows = holocronDao.getWorkBatch(10)
    val sendResults = rows.map(row => {
      HolocronSendResult.SendAttempt(row.id, HoloOffer.getDefaultInstance, Success(()))
    })
    holocronDao.updateResults(sendResults, 5.minutes)
  }

  private def updateOffer(parsedOffer: ParsedOffer.Builder,
                          text: String = "",
                          parseDate: DateTime = timeService.getNow,
                          section: Section = Section.SECTION_UNKNOWN,
                          year: Int = 0,
                          mileage: Int = 0) = {
    parsedOffer.getOfferBuilder.setDescription(text)
    parsedOffer.getOfferBuilder.setSection(section)
    parsedOffer.getOfferBuilder.getDocumentsBuilder.setYear(year)
    parsedOffer.getOfferBuilder.getStateBuilder.setMileage(mileage)
    parsedOffer.getParseDateBuilder.setSeconds(parseDate.getMillis / 1000)
    parsedOffer.getMaxParseDateBuilder.setSeconds(
      parsedOffer.getParseDate.getSeconds.max(parsedOffer.getMaxParseDate.getSeconds)
    )
  }
}
