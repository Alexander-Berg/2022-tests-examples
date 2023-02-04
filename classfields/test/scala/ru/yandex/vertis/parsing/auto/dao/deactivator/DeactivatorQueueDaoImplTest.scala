package ru.yandex.vertis.parsing.auto.dao.deactivator

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.CatalogModel.TechInfo
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils.{testAvitoCarsUrl, testRow}
import ru.yandex.vertis.parsing.auto.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.common.{AscDesc, Site}
import ru.yandex.vertis.parsing.dao.deactivator.{ExternalOfferCheckResult, ExternalOfferStatus}
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
class DeactivatorQueueDaoImplTest extends FunSuite with InitTestDbs with MockitoSupport {
  initDb()

  private val dayStart = DateTime.now().withMillisOfDay(0)

  implicit private val trace: Traced = TracedUtils.empty

  private val parsedOffersDao = components.parsedOffersDao

  private val deactivatorQueueDao = components.deactivatorQueueDao

  private val techInfo = TechInfo.newBuilder()
  techInfo.getMarkInfoBuilder.setName("MERCEDES")
  techInfo.getModelInfoBuilder.setName("814")
  when(components.searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(Some(techInfo.build())))

  test("cannot add hash which is not in t_parsed_offers") {
    val url: String = testAvitoCarsUrl
    val hash = CommonAutoParser.hash(url)

    setSecond(0)
    ensureFailedQueueAdd(hash)
  }

  test("for reactivation order by id") {
    val url: String = testAvitoCarsUrl
    val hash = CommonAutoParser.hash(url)
    val parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    val url2: String = testAvitoCarsUrl
    val hash2 = CommonAutoParser.hash(url2)
    val parsedRow2: ParsedRow = testRow(url2, category = Category.CARS)

    setSecond(0)
    ensureInsert(parsedRow)
    ensureInsert(parsedRow2)

    setSecond(1)
    ensureSuccessQueueAdd(hash)
    ensureSuccessQueueAdd(hash2)

    val unprocessed = deactivatorQueueDao.getUnprocessed(Site.Avito, Some(Category.CARS), 10)
    assert(unprocessed.length == 2)
    assert(unprocessed.head.id == 2)
    assert(unprocessed(1).id == 3)

    val unprocessed2 = deactivatorQueueDao.getUnprocessedForReactivation(10, 0)
    assert(unprocessed2.length == 2)
    assert(unprocessed2.head.id == 2)
    assert(unprocessed2(1).id == 3)

    val unprocessed3 = deactivatorQueueDao.getUnprocessedForReactivation(10, idGte = 3L)
    assert(unprocessed3.length == 1)
    assert(unprocessed3.head.id == 3)

    val unprocessed4 = deactivatorQueueDao.getUnprocessedForReactivation(10, idGte = 2)
    assert(unprocessed4.length == 2)
    assert(unprocessed4.head.id == 2)
    assert(unprocessed4(1).id == 3)

    ensureSetProcessed(hash)
    ensureSetProcessed(hash2)
  }

  test("for reactivation with id") {
    val url: String = testAvitoCarsUrl
    val hash = CommonAutoParser.hash(url)

    def parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    setSecond(0)
    ensureInsert(parsedRow)

    setSecond(1)
    ensureSuccessQueueAdd(hash)

    val unprocessed = deactivatorQueueDao.getUnprocessed(Site.Avito, Some(Category.CARS), 10)
    assert(unprocessed.length == 1)
    assert(unprocessed.head.id == 4)

    val unprocessed2 = deactivatorQueueDao.getUnprocessedForReactivation(10, idGte = unprocessed.head.id)
    assert(unprocessed2.length == 1)

    val unprocessed3 = deactivatorQueueDao.getUnprocessedForReactivation(10, idGte = unprocessed.head.id + 1)
    assert(unprocessed3.isEmpty)

    ensureSetProcessed(hash)
  }

  test("cannot add unprocessed twice") {
    val url: String = testAvitoCarsUrl
    val hash = CommonAutoParser.hash(url)

    def parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    setSecond(0)
    ensureInsert(parsedRow)

    setSecond(1)
    ensureSuccessQueueAdd(hash)

    setSecond(2)
    ensureFailedQueueAdd(hash)

    setSecond(3)
    ensureUnprocessed(hash)
    ensureOtherSiteCategoryUnprocessedEmpty()

    setSecond(4)
    ensureSetProcessed(hash)

    setSecond(5)
    ensureUnprocessedEmpty()

    setSecond(6)
    ensureSuccessQueueAdd(hash)

    setSecond(7)
    ensureSetProcessed(hash)
  }

  private def ensureSetProcessed(hash: String) = {
    val result =
      deactivatorQueueDao.setProcessed(Seq(ExternalOfferCheckResult(hash, ExternalOfferStatus.INACTIVE, "reason")))
    assert(result(hash))
  }

  private def ensureUnprocessed(hash: String) = {
    assert(deactivatorQueueDao.getUnprocessed(Site.Avito, Some(Category.CARS), 10).length == 1)
    assert(deactivatorQueueDao.getUnprocessed(Site.Avito, Some(Category.CARS), 10).head.hash == hash)
  }

  private def ensureOtherSiteCategoryUnprocessedEmpty() = {
    assert(deactivatorQueueDao.getUnprocessed(Site.Drom, Some(Category.CARS), 10).isEmpty)
    assert(deactivatorQueueDao.getUnprocessed(Site.Amru, Some(Category.CARS), 10).isEmpty)
    assert(deactivatorQueueDao.getUnprocessed(Site.Avito, Some(Category.TRUCKS), 10).isEmpty)
  }

  private def ensureUnprocessedEmpty() = {
    assert(deactivatorQueueDao.getUnprocessed(Site.Avito, Some(Category.CARS), 10).isEmpty)
  }

  private def ensureSuccessQueueAdd(hash: String) = {
    val result = deactivatorQueueDao.add(Seq(hash))
    assert(result(hash))
  }

  private def ensureFailedQueueAdd(hash: String) = {
    val result = deactivatorQueueDao.add(Seq(hash))
    assert(!result(hash))
  }

  private def setSecond(second: Int) = {
    when(components.timeService.getNow).thenReturn(dayStart.withSecondOfMinute(second))
  }

  private def ensureInsert(parsedRow: ParsedRow): Unit = {
    val saveResult2 = ImportResult(parsedOffersDao.save(Seq(parsedRow)))(_.name())
    assert(saveResult2.insertedSimple == 1)
    assert(saveResult2.updatedSimple == 0)
  }
}
