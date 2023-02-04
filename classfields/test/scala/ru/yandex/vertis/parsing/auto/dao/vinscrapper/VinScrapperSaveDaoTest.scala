package ru.yandex.vertis.parsing.auto.dao.vinscrapper

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.CatalogModel.TechInfo
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.enums.TVinScrapperQueueStatus
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils.{testAvitoCarsUrl, testDromCarsUrl, testRow}
import ru.yandex.vertis.parsing.auto.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.parsing.workers.importers.ImportResult
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class VinScrapperSaveDaoTest extends FunSuite with InitTestDbs with OptionValues with MockitoSupport {
  initDb()

  implicit private val trace: Traced = TracedUtils.empty

  private val techInfo = TechInfo.newBuilder()
  techInfo.getMarkInfoBuilder.setName("MERCEDES")
  techInfo.getModelInfoBuilder.setName("814")
  when(components.searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(Some(techInfo.build())))

  test("no avito cars") {
    setSecond(0)
    assert(components.vinScrapperDao.getInProgress(10).isEmpty)

    val url: String = testDromCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    ensureInsert(parsedRow)
    assert(components.vinScrapperDao.getInProgress(10).isEmpty)

    setSecond(1)
    assert(components.vinScrapperDao.getInProgress(10).isEmpty)
  }

  test("feature disabled") {
    components.features.VinScrapperAdd.setEnabled(false)
    setSecond(0)
    assert(components.vinScrapperDao.getInProgress(10).isEmpty)

    val url: String = testAvitoCarsUrl
    val hash: String = CommonAutoParser.hash(url)
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    ensureInsert(parsedRow)
    assert(components.vinScrapperDao.getInProgress(10).isEmpty)

    setSecond(1)
    assert(components.vinScrapperDao.getInProgress(10).isEmpty)
  }

  test("double addition") {
    components.features.VinScrapperAdd.setEnabled(true)
    setSecond(0)
    assert(components.vinScrapperDao.getInProgress(10).isEmpty)

    val url: String = testAvitoCarsUrl
    val hash: String = CommonAutoParser.hash(url)
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    ensureInsert(parsedRow)
    assert(
      components.vinScrapperDao.getInProgress(10) == Seq(VinScrapperRow(hash, url, getSecond(0), None, getSecond(0)))
    )

    setSecond(1)
    assert(
      components.vinScrapperDao.getInProgress(10) == Seq(VinScrapperRow(hash, url, getSecond(0), None, getSecond(0)))
    )

    setSecond(2)
    updateOffer(parsedOffer, "test2")
    ensureUpdate(parsedRow)
    assert(
      components.vinScrapperDao.getInProgress(10) == Seq(VinScrapperRow(hash, url, getSecond(0), None, getSecond(0)))
    )

    setSecond(3)
    assert(
      components.vinScrapperDao.getInProgress(10) == Seq(VinScrapperRow(hash, url, getSecond(0), None, getSecond(0)))
    )

    setSecond(4)
    components.vinScrapperDao.updateStatus(Seq((hash, TVinScrapperQueueStatus.NO_RESULT)))
    assert(components.vinScrapperDao.getInProgress(10).isEmpty)
    components.features.VinScrapperAdd.setEnabled(false)
  }

  test("avito cars") {
    components.features.VinScrapperAdd.setEnabled(true)
    setSecond(0)
    assert(components.vinScrapperDao.getInProgress(10).isEmpty)

    val url: String = testAvitoCarsUrl
    val hash: String = CommonAutoParser.hash(url)
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    ensureInsert(parsedRow)
    assert(
      components.vinScrapperDao.getInProgress(10) == Seq(VinScrapperRow(hash, url, getSecond(0), None, getSecond(0)))
    )

    setSecond(1)
    assert(
      components.vinScrapperDao.getInProgress(10) == Seq(VinScrapperRow(hash, url, getSecond(0), None, getSecond(0)))
    )

    setSecond(2)
    components.vinScrapperDao.updateStatus(Seq((hash, TVinScrapperQueueStatus.NO_RESULT)))
    assert(components.vinScrapperDao.getInProgress(10).isEmpty)

    setSecond(3)
    assert(components.vinScrapperDao.getInProgress(10).isEmpty)
    components.features.VinScrapperAdd.setEnabled(false)
  }

  private val dayStart = DateTime.now().withMillisOfDay(0)

  private def setSecond(second: Int) = {
    when(components.timeService.getNow).thenReturn(dayStart.withSecondOfMinute(second))
  }

  private def getSecond(second: Int) = {
    dayStart.withSecondOfMinute(second)
  }

  private def ensureInsert(parsedRow: ParsedRow): Unit = {
    val saveResult2 = ImportResult(components.parsedOffersDao.save(Seq(parsedRow)))(_.name())
    assert(saveResult2.insertedSimple == 1)
    assert(saveResult2.updatedSimple == 0)
  }

  private def ensureUpdate(parsedRow: ParsedRow, expectedHoloQueueSize: Int = 1): Unit = {
    val saveResult2 = ImportResult(components.parsedOffersDao.save(Seq(parsedRow)))(_.name())
    assert(saveResult2.insertedSimple == 0)
    assert(saveResult2.updatedSimple == 1)
  }

  private def updateOffer(parsedOffer: ParsedOffer.Builder, text: String) = {
    parsedOffer.getOfferBuilder.setDescription(text)
    parsedOffer.getParseDateBuilder.setSeconds(components.timeService.getNow.getMillis / 1000)
  }
}
