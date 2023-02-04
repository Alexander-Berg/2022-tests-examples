package ru.yandex.vertis.parsing.auto.dao.holocron

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.Mockito.reset
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FunSuite, OptionValues}
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.CatalogModel.TechInfo
import ru.vertis.holocron.common.{Action, HoloOffer}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.dao.parsedoffers.{ParsedOffersDao, ParsedOffersDaoImpl}
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.auto.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.holocron.HolocronSendResult
import ru.yandex.vertis.parsing.holocron.validation.HolocronValidationResult
import ru.yandex.vertis.parsing.util.RandomUtil
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.parsing.workers.importers.ImportResult
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class HolocronDaoImplTest extends FunSuite with InitTestDbs with OptionValues with MockitoSupport with Eventually {
  initDb()

  implicit private val trace: Traced = TracedUtils.empty

  private val parsingTransactions = components.parsingShard.master

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

  private val techInfo = TechInfo.newBuilder()
  techInfo.getMarkInfoBuilder.setName("MERCEDES")
  techInfo.getModelInfoBuilder.setName("814")
  when(components.searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(Some(techInfo.build())))

  private val dayStart = DateTime.now().withMillisOfDay(0)

  test("rollback if failed to insert to holocron queue") {
    when(components.holocronValidator.validate(?, ?, ?)).thenThrow(new RuntimeException("error!"))
    components.features.StrictHolocronValidation.setEnabled(true)
    val url: String = testAvitoCarsUrl
    val hash: String = CommonAutoParser.hash(url)

    val parsedOffer = ParsedOffer.newBuilder()
    val techParamId = RandomUtil.nextInt(1000000, 10000000)
    parsedOffer.getOfferBuilder.getCarInfoBuilder.setTechParamId(techParamId)

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    setSecond(0)
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).isEmpty)
    intercept[RuntimeException] {
      ensureInsert(parsedRow)
    }
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).isEmpty)

    setSecond(1)
    reset(components.holocronValidator)
    when(components.holocronValidator.validate(?, ?, ?)).thenReturn(HolocronValidationResult.Valid)
    updateOffer(parsedOffer, "test1")
    ensureInsert(parsedRow)
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).length == 1)
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).head.data.getOffer.getDescription == "test1")

    setSecond(2)
    reset(components.holocronValidator)
    when(components.holocronValidator.validate(?, ?, ?)).thenThrow(new RuntimeException("error!"))
    updateOffer(parsedOffer, "test2")
    intercept[RuntimeException] {
      ensureUpdate(parsedRow)
    }
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).length == 1)
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).head.data.getOffer.getDescription == "test1")

    sendToHolocron()
    reset(components.holocronValidator)
    when(components.holocronValidator.validate(?, ?, ?)).thenReturn(HolocronValidationResult.Valid)
    components.features.StrictHolocronValidation.setEnabled(false)
  }

  test("do not rollback if searcherClient.carsCatalogData throws error") {
    when(components.searcherClient.carsCatalogData(?)(?)).thenReturn(Future.failed(new RuntimeException("error!")))
    val url: String = testAvitoCarsUrl
    val hash: String = CommonAutoParser.hash(url)

    val parsedOffer = ParsedOffer.newBuilder()
    val techParamId = RandomUtil.nextInt(1000000, 10000000)
    parsedOffer.getOfferBuilder.getCarInfoBuilder.setTechParamId(techParamId)
    updateOffer(parsedOffer, "test0")

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    setSecond(0)
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).isEmpty)
    ensureInsert(parsedRow)
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).length == 1)
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).head.data.getOffer.getDescription == "test0")

    setSecond(1)
    sendToHolocron()

    setSecond(2)
    when(components.searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(Some(techInfo.build())))
    updateOffer(parsedOffer, "test1")
    ensureUpdate(parsedRow)
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).length == 1)
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).head.data.getOffer.getDescription == "test1")

    setSecond(3)
    sendToHolocron()

    setSecond(4)
    when(components.searcherClient.carsCatalogData(?)(?)).thenReturn(Future.failed(new RuntimeException("error!")))
    updateOffer(parsedOffer, "test2")
    ensureUpdate(parsedRow)
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).length == 1)
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).head.data.getOffer.getDescription == "test2")

    setSecond(5)
    sendToHolocron()

    when(components.searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(Some(techInfo.build())))
  }

  test("activate car offer") {
    val url: String = testAvitoCarsUrl

    def parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    setSecond(0)
    ensureInsert(parsedRow)

    setSecond(1)
    ensureActions(Action.ACTIVATE)
    sendToHolocron()

    setSecond(2)
    ensureWorkBatchEmpty()
  }

  test("update car offer after activate sent") {
    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    setSecond(0)
    ensureInsert(parsedRow)

    setSecond(1)
    ensureActions(Action.ACTIVATE)
    sendToHolocron()

    setSecond(2)
    ensureWorkBatchEmpty()
    updateOffer(parsedOffer, "test3")
    ensureUpdate(parsedRow)

    setSecond(3)
    ensureActions(Action.UPDATE)
    sendToHolocron()
  }

  test("deactivate car offer") {
    val url: String = testAvitoCarsUrl
    val hash: String = CommonAutoParser.hash(url)
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    setSecond(0)
    ensureInsert(parsedRow)

    setSecond(1)
    ensureActions(Action.ACTIVATE)
    sendToHolocron()

    setSecond(2)
    ensureWorkBatchEmpty()
    ensureSetDeactivated(hash)

    setSecond(3)
    ensureActions(Action.DEACTIVATE)
    sendToHolocron()
  }

  test("activate car offer instead of update after deactivate") {
    val url: String = testAvitoCarsUrl
    val hash: String = CommonAutoParser.hash(url)
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    setSecond(0)
    ensureInsert(parsedRow)

    setSecond(1)
    ensureActions(Action.ACTIVATE)
    sendToHolocron()

    setSecond(2)
    ensureWorkBatchEmpty()
    ensureSetDeactivated(hash)

    setSecond(3)
    ensureActions(Action.DEACTIVATE)
    sendToHolocron()

    setSecond(4)
    updateOffer(parsedOffer, "test4")
    ensureUpdate(parsedRow)

    setSecond(5)
    ensureActions(Action.ACTIVATE)
    sendToHolocron()
  }

  test("add offer again even if not sent") {
    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    setSecond(0)
    ensureInsert(parsedRow)

    setSecond(1)
    ensureActions(Action.ACTIVATE)

    setSecond(2)
    updateOffer(parsedOffer, "test5")
    ensureUpdate(parsedRow, expectedHoloQueueSize = 2)

    setSecond(3)
    ensureActions(Action.ACTIVATE, Action.UPDATE)
    sendToHolocron()
  }

  test("insert deactivate then update and deactivate in transaction") {
    val url: String = testAvitoCarsUrl
    val hash: String = CommonAutoParser.hash(url)
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    setSecond(0)
    ensureInsert(parsedRow)

    setSecond(1)
    ensureActions(Action.ACTIVATE)
    sendToHolocron()

    setSecond(2)
    ensureWorkBatchEmpty()
    ensureActiveStatus(hash)
    ensureSetDeactivated(hash)

    setSecond(3)
    ensureActions(Action.DEACTIVATE)
    ensureDeactivatedStatus(hash)
    ensureDeactivatedDate(hash, dayStart)
    sendToHolocron()

    setSecond(86401)
    parsingTransactions.withTransactionReadCommitted {
      updateOffer(parsedOffer, "test4")
      ensureUpdate(parsedRow, expectedHoloQueueSize = 1)
      ensureActiveStatus(hash)
      ensureSetDeactivated(hash, expectedHoloQueueSize = 2)
      ensureDeactivatedStatus(hash)
      ensureDeactivatedDate(hash, dayStart.plusDays(1))
    }

    setSecond(86402)
    ensureHolocronInsert(2)
    ensureDeactivatedStatus(hash)
    ensureDeactivatedDate(hash, dayStart.plusDays(1))
    sendToHolocron()
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

  private def ensureSetDeactivated(hash: String, expectedHoloQueueSize: Int = 1): Unit = {
    val result = parsedOffersDao.setDeactivated(Seq(hash))
    assert(result(hash))
    ensureHolocronInsert(expectedHoloQueueSize)
  }

  private def ensureDeactivatedStatus(hash: String): Unit = {
    val result = parsedOffersDao.getParsedOffers(Seq(hash), fromMaster = true).head
    assert(result.deactivateDate.nonEmpty)
  }

  private def ensureDeactivatedDate(hash: String, date: DateTime): Unit = {
    val result = parsedOffersDao.getParsedOffers(Seq(hash), fromMaster = true).head
    assert(result.deactivateDate.contains(date))
  }

  private def ensureActiveStatus(hash: String): Unit = {
    val result = parsedOffersDao.getParsedOffers(Seq(hash), fromMaster = true).head
    assert(result.deactivateDate.isEmpty)
  }

  private def ensureHolocronInsert(expectedSize: Int = 1): Unit = {
    eventually(Timeout(Span(2, Seconds))) {
      assert(components.statsDao.holocronQueueSize(fromMaster = true) == expectedSize)
    }
  }

  private def updateOffer(parsedOffer: ParsedOffer.Builder, text: String) = {
    parsedOffer.getOfferBuilder.setDescription(text)
    parsedOffer.getParseDateBuilder.setSeconds(components.timeService.getNow.getMillis / 1000)
  }

  private def ensureWorkBatchEmpty() = {
    assert(holocronDao.getWorkBatch(10).isEmpty)
  }

  private def setSecond(second: Int) = {
    val millis = second * 1000
    when(components.timeService.getNow).thenReturn(
      dayStart
        .plusDays(millis / 86400000)
        .withMillisOfDay(millis % 86400000)
    )
  }

  private def sendToHolocron(): Unit = {
    val rows = holocronDao.getWorkBatch(10)
    val sendResult = rows.map(row => HolocronSendResult.SendAttempt(row.id, HoloOffer.getDefaultInstance, Success(())))
    holocronDao.updateResults(sendResult, 5.minutes)
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
}
