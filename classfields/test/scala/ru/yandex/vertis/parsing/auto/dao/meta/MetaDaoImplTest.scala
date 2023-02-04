package ru.yandex.vertis.parsing.auto.dao.meta

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel.Source
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.dao.parsedoffers.ParsedOffersDao
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.auto.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.clients.mds.MdsImage
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.dao.meta.{MetaPumpResults, ParsedPhotosMetaRow}
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport
import ru.yandex.vertis.parsing.workers.importers.ImportResult

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class MetaDaoImplTest extends FunSuite with InitTestDbs with OptionValues with MockitoSupport with EmptyTraceSupport {
  initDb()

  private val parsedOffersDao: ParsedOffersDao = components.parsedOffersDao
  private val metaDao = components.metaDao
  private val dayStart = DateTime.now().withMillisOfDay(0)
  private val numTries = 2

  when(components.searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(None))

  test("checkArchive") {
    when(components.timeService.getNow).thenReturn(dayStart)
    val (photoUrl: String, metaRow: ParsedPhotosMetaRow) = createNewMetaRow

    val m = metaDao.checkArchive(Seq(metaRow))
    assert(!m(photoUrl))

    assert(metaDao.insertFromRows(Seq(metaRow)) == 1)

    val m2 = metaDao.checkArchive(Seq(metaRow))
    assert(!m2(photoUrl))

    metaDao.insertToArchive(Seq(metaRow))

    val m3 = metaDao.checkArchive(Seq(metaRow))
    assert(m3(photoUrl))
  }

  test("getWorkBatch") {
    when(components.timeService.getNow).thenReturn(dayStart)

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)

    val (_, metaRow: ParsedPhotosMetaRow) = createNewMetaRow

    assert(metaDao.insertFromRows(Seq(metaRow)) == 1)

    when(components.timeService.getNow).thenReturn(dayStart.plusSeconds(1))

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).length == 1)

    metaDao.updatePumpResults(
      MetaPumpResults(successResults = List((metaRow, MdsImage(100, "image", Some(Json.obj()))))),
      1.minute
    )

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)

    metaDao.insertToArchive(Seq(metaRow))
  }

  test("getNewNotSentMeta and getOldNotSentMeta") {
    when(components.timeService.getNow).thenReturn(dayStart)

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)

    val (_, newMetaRow: ParsedPhotosMetaRow) = createNewMetaRow
    val (_, oldMetaRow: ParsedPhotosMetaRow) = createOldMetaRow

    assert(metaDao.insertFromRows(Seq(newMetaRow, oldMetaRow)) == 2)

    when(components.timeService.getNow).thenReturn(dayStart.plusSeconds(1))

    assert(metaDao.getNotSentMeta(2).isEmpty)
    assert(metaDao.getNewNotSentMeta(2).isEmpty)
    assert(metaDao.getOldNotSentMeta(2).isEmpty)

    metaDao.updatePumpResults(
      MetaPumpResults(successResults = List((newMetaRow, MdsImage(100, "image", Some(Json.obj()))))),
      1.minute
    )

    assert(metaDao.getNotSentMeta(2).length == 1)
    assert(metaDao.getNewNotSentMeta(2).length == 1)
    assert(metaDao.getOldNotSentMeta(2).isEmpty)

    metaDao.updatePumpResults(
      MetaPumpResults(successResults = List((oldMetaRow, MdsImage(100, "image", Some(Json.obj()))))),
      1.minute
    )

    assert(metaDao.getNotSentMeta(2).length == 2)
    assert(metaDao.getNewNotSentMeta(2).length == 1)
    assert(metaDao.getOldNotSentMeta(2).length == 1)

    metaDao.setSent(Seq(newMetaRow.photoUrl))

    assert(metaDao.getNotSentMeta(2).length == 1)
    assert(metaDao.getNewNotSentMeta(2).isEmpty)
    assert(metaDao.getOldNotSentMeta(2).length == 1)

    metaDao.setSent(Seq(oldMetaRow.photoUrl))

    assert(metaDao.getNotSentMeta(2).isEmpty)
    assert(metaDao.getNewNotSentMeta(2).isEmpty)
    assert(metaDao.getOldNotSentMeta(2).isEmpty)

    metaDao.insertToArchive(Seq(newMetaRow, oldMetaRow))
  }

  test("getNewWorkBatch and getOldWorkBatch") {
    when(components.timeService.getNow).thenReturn(dayStart)

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)

    val (_, newMetaRow: ParsedPhotosMetaRow) = createNewMetaRow
    val (_, oldMetaRow: ParsedPhotosMetaRow) = createOldMetaRow

    assert(metaDao.insertFromRows(Seq(newMetaRow, oldMetaRow)) == 2)

    when(components.timeService.getNow).thenReturn(dayStart.plusSeconds(1))

    assert(metaDao.getUploadWorkBatch(Site.Avito, 2).length == 2)
    assert(metaDao.getUploadWorkBatch(Site.Drom, 2).isEmpty)

    assert(metaDao.getNewWorkBatch(Site.Avito, 2).length == 1)
    assert(metaDao.getNewWorkBatch(Site.Drom, 2).isEmpty)

    assert(metaDao.getOldWorkBatch(Site.Avito, 2).length == 1)
    assert(metaDao.getOldWorkBatch(Site.Drom, 2).isEmpty)
    assert(metaDao.getOldWorkBatchAnySite(2).length == 1)

    metaDao.updatePumpResults(
      MetaPumpResults(successResults = List((newMetaRow, MdsImage(100, "image", Some(Json.obj()))))),
      1.minute
    )

    assert(metaDao.getUploadWorkBatch(Site.Avito, 2).length == 1)
    assert(metaDao.getUploadWorkBatch(Site.Drom, 2).isEmpty)

    assert(metaDao.getNewWorkBatch(Site.Avito, 2).isEmpty)
    assert(metaDao.getNewWorkBatch(Site.Drom, 2).isEmpty)

    assert(metaDao.getOldWorkBatch(Site.Avito, 2).length == 1)
    assert(metaDao.getOldWorkBatchAnySite(2).length == 1)
    assert(metaDao.getOldWorkBatch(Site.Drom, 2).isEmpty)

    metaDao.updatePumpResults(
      MetaPumpResults(successResults = List((oldMetaRow, MdsImage(100, "image", Some(Json.obj()))))),
      1.minute
    )

    assert(metaDao.getUploadWorkBatch(Site.Avito, 2).isEmpty)
    assert(metaDao.getUploadWorkBatch(Site.Drom, 2).isEmpty)

    assert(metaDao.getNewWorkBatch(Site.Avito, 2).isEmpty)
    assert(metaDao.getNewWorkBatch(Site.Drom, 2).isEmpty)

    assert(metaDao.getOldWorkBatch(Site.Avito, 2).isEmpty)
    assert(metaDao.getOldWorkBatch(Site.Drom, 2).isEmpty)
    assert(metaDao.getOldWorkBatchAnySite(2).isEmpty)

    metaDao.insertToArchive(Seq(newMetaRow, oldMetaRow))
  }

  test("getWorkBatch2") {
    when(components.timeService.getNow).thenReturn(dayStart)

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)

    val (_, metaRow: ParsedPhotosMetaRow) = createNewMetaRow

    assert(metaDao.insertFromRows(Seq(metaRow)) == 1)

    when(components.timeService.getNow).thenReturn(dayStart.plusSeconds(1))

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).length == 1)

    assert(metaDao.insertToArchive(Seq(metaRow)) == 1)

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)
  }

  test("getWorkBatch: different site") {
    when(components.timeService.getNow).thenReturn(dayStart)

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)

    val (_, metaRow: ParsedPhotosMetaRow) = createNewMetaRow

    assert(metaDao.insertFromRows(Seq(metaRow)) == 1)

    when(components.timeService.getNow).thenReturn(dayStart.plusSeconds(1))

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).length == 1)
    assert(metaDao.getUploadWorkBatch(Site.Drom, 1).isEmpty)

    assert(metaDao.insertToArchive(Seq(metaRow)) == 1)

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)
  }

  test("getFinishedBatch") {
    when(components.timeService.getNow).thenReturn(dayStart)

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)

    assert(metaDao.getFinishedBatch(1).isEmpty)

    val (_, metaRow: ParsedPhotosMetaRow) = createNewMetaRow

    assert(metaDao.insertFromRows(Seq(metaRow)) == 1)

    when(components.timeService.getNow).thenReturn(dayStart.plusSeconds(1))

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).length == 1)
    assert(metaDao.getFinishedBatch(1).isEmpty)

    metaDao.updatePumpResults(
      MetaPumpResults(successResults = List((metaRow, MdsImage(100, "image", Some(Json.obj()))))),
      1.minute
    )

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)
    assert(metaDao.getFinishedBatch(1).isEmpty)

    metaDao.setSent(Seq(metaRow.photoUrl))

    assert(metaDao.getFinishedBatch(1).length == 1)

    metaDao.insertToArchive(Seq(metaRow))
  }

  test("getWorkBatch, getFinishedBatch: on error") {
    when(components.timeService.getNow).thenReturn(dayStart)

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)

    assert(metaDao.getFinishedBatch(1).isEmpty)

    val (_, metaRow: ParsedPhotosMetaRow) = createNewMetaRow

    assert(metaDao.insertFromRows(Seq(metaRow)) == 1)

    when(components.timeService.getNow).thenReturn(dayStart.plusSeconds(1))

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).length == 1)
    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).head.remainAttempts == 2)
    assert(metaDao.getFinishedBatch(1).isEmpty)

    metaDao.updatePumpResults(MetaPumpResults(errorResults = List((metaRow, new RuntimeException("Error!")))), 1.minute)

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty) // retry delay 1 minute

    when(components.timeService.getNow).thenReturn(dayStart.plusMinutes(1).plusSeconds(1))

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).length == 1)
    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).head.remainAttempts == 1)
    assert(metaDao.getFinishedBatch(1).isEmpty)

    metaDao.updatePumpResults(MetaPumpResults(errorResults = List((metaRow, new RuntimeException("Error!")))), 1.minute)

    when(components.timeService.getNow).thenReturn(dayStart.plusMinutes(2).plusSeconds(1))

    assert(metaDao.getUploadWorkBatch(Site.Avito, 1).isEmpty)

    assert(metaDao.getFinishedBatch(1).length == 1)
    assert(metaDao.getFinishedBatch(1).head.workDate.isEmpty)
    assert(metaDao.getFinishedBatch(1).head.sendDate.isEmpty)
    assert(metaDao.getFinishedBatch(1).head.meta.isEmpty)
    assert(metaDao.getFinishedBatch(1).head.remainAttempts == 0)

    metaDao.insertToArchive(Seq(metaRow))
  }

  private def createNewMetaRow = {
    val url: String = testAvitoCarsUrl
    val photoUrl = testAvitoPhotoUrl
    val parsedOffer = ParsedOffer.newBuilder()
    parsedOffer.addPhoto(photoUrl)
    val parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)
    val saveResult = ImportResult(parsedOffersDao.save(Seq(parsedRow)))(_.name())
    assert(saveResult.insertedSimple == 1)
    assert(saveResult.updatedSimple == 0)
    val metaRow = ParsedPhotosMetaRow(parsedRow, photoUrl, dayStart, numTries)
    (photoUrl, metaRow)
  }

  private def createOldMetaRow = {
    val url: String = testAvitoCarsUrl
    val photoUrl = testAvitoPhotoUrl
    val parsedOffer = ParsedOffer.newBuilder()
    parsedOffer.addPhoto(photoUrl)
    val parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS, source = Source.PARSING_SALES)
    val saveResult = ImportResult(parsedOffersDao.save(Seq(parsedRow)))(_.name())
    assert(saveResult.insertedSimple == 1)
    assert(saveResult.updatedSimple == 0)
    val metaRow = ParsedPhotosMetaRow(parsedRow, photoUrl, dayStart, numTries)
    (photoUrl, metaRow)
  }
}
