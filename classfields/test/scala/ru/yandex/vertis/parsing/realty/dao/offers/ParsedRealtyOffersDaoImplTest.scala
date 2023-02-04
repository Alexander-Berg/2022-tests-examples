package ru.yandex.vertis.parsing.realty.dao.offers

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel.Status
import ru.yandex.vertis.parsing.dao.watchers.{IndexData, IndexDataUpdateResult}
import ru.yandex.vertis.parsing.realty.ParsingRealtyModel.ParsedOffer
import ru.yandex.vertis.parsing.realty.dao.watchers.PhonesWatcher
import ru.yandex.vertis.parsing.realty.diffs.OfferFields
import ru.yandex.vertis.parsing.realty.parsers.smartagent.avito.SmartAgentAvitoRealtyParser
import ru.yandex.vertis.parsing.realty.util.TestDataUtils._
import ru.yandex.vertis.parsing.realty.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.realty.validators.RealtyValidationResult
import ru.yandex.vertis.parsing.realty.workers.importers.RealtyCategoryFormatter
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport
import ru.yandex.vertis.parsing.validators.ValidationResult
import ru.yandex.vertis.parsing.workers.importers.ImportResult
import ru.yandex.vertis.parsing.util.DateUtils.RichDateTime

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ParsedRealtyOffersDaoImplTest
  extends FunSuite
  with InitTestDbs
  with OptionValues
  with MockitoSupport
  with EmptyTraceSupport {
  initDb()

  private val shard = components.parsingRealtyShard

  private def db = shard.master.jooq

  private val dao = components.parsedRealtyOffersDao

  private val dayStart = DateTime.now().withMillisOfDay(0)

  test("index tables insert delete select") {
    val url: String = testAvitoApartmentUrl
    val hash: String = SmartAgentAvitoRealtyParser.hash(url)
    val row: ParsedRealtyRow = testRow(url)

    assertSaveResult(row, inserted = true)

    val watcher = PhonesWatcher
    val data1: IndexData = IndexData(hash, "123456")
    val insertResult = db.insert("")(dsl => watcher.insertQuery(dsl, data1))
    assert(insertResult == 1)
    val result = db.query("")(dsl => watcher.selectQuery(dsl, Seq(hash))).map(watcher.mapRow)
    assert(result.length == 1)
    assert(result.head == data1)
    val deleteResult = db.delete("")(dsl => watcher.deleteQuery(dsl, data1))
    assert(deleteResult == 1)
    val result2 = db.query("")(dsl => watcher.selectQuery(dsl, Seq(hash))).map(watcher.mapRow)
    assert(result2.isEmpty)
  }

  test("index update for phones with leading zeros") {
    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.addPhone("09002200")
    val url: String = testAvitoApartmentUrl
    val hash: String = SmartAgentAvitoRealtyParser.hash(url)
    val row: ParsedRealtyRow = testRow(url, parsedOffer)

    assertSaveResult(row, inserted = true)

    val IndexDataUpdateResult(toInsert, toDelete) = dao
      .asInstanceOf[ParsedRealtyOffersDaoImpl]
      .updateIndexFields(Seq(hash))
    assert(toInsert.isEmpty)
    assert(toDelete.isEmpty)
  }

  test("less parse date") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testAvitoApartmentUrl
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если parse_date меньше, то не обновляем
    parsedOffer.getOfferBuilder.setDescription("test1")
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(2).getMillis / 1000)
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow)
  }

  test("parse date - the only change") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testAvitoApartmentUrl
    val hash: String = SmartAgentAvitoRealtyParser.hash(url)
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если parse_date единственное изменение, то не обновляем
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().getMillis / 1000)
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow)

    // однако если объявление неактивно - обновляем, объявление очевидно снова активно
    dao.setDeactivated(Seq(hash))

    assertSaveResult(newRow, updated = true)
  }

  test("same parse date") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testAvitoApartmentUrl
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если parse_date не изменился, то обновляем
    parsedOffer.getOfferBuilder.setDescription("test1")
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow, updated = true)
  }

  test("set comment on update") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testAvitoApartmentUrl
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если parse_date не изменился, то обновляем
    parsedOffer.getOfferBuilder.setDescription("test1")
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResultWithComment(newRow, comment = "test", updated = true, dbComment = "test")
  }

  test("unable to set comment on insert") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testAvitoApartmentUrl
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResultWithComment(existingRow, comment = "test", inserted = true, dbComment = "received first time")
  }

  test("ignored diffs") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testAvitoApartmentUrl
    val hash: String = SmartAgentAvitoRealtyParser.hash(url)
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val phone: String = "79297771122"
    parsedOffer.getOfferBuilder.getSellerBuilder.addPhone(phone)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если все изменения либо ignored, либо parse_date - не обновляем
    parsedOffer.getOfferBuilder.getSellerBuilder.clearPhone()
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).plusHours(1).getMillis / 1000)
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow)

    // если есть что-то еще - обновляем, ignored дифы в истории также остаются
    parsedOffer.getOfferBuilder.setDescription("test1")
    val newRow2 = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow2, updated = true)

    val row = dao.getParsedOffers(Seq(newRow.hash)).head
    assert(row.data.getStatusHistoryCount == 2)
    assert(row.data.getStatusHistory(1).getDiffCount == 3)
    assert(
      row.data
        .getStatusHistory(1)
        .getDiffList
        .asScala
        .exists(d => {
          d.getName == OfferFields.Phones && d.getIgnored.getValue && d.getOldValue == phone
        })
    )
    assert(
      row.data
        .getStatusHistory(1)
        .getDiffList
        .asScala
        .exists(d => {
          d.getName == OfferFields.Description
        })
    )
    assert(
      row.data
        .getStatusHistory(1)
        .getDiffList
        .asScala
        .exists(d => {
          d.getName == OfferFields.ParseDate
        })
    )
    assert(row.data.getOffer.getSeller.getPhone(0) == phone)

    // если объявление неактивно - обновляем
    dao.setDeactivated(Seq(hash))

    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).plusHours(2).getMillis / 1000)
    val newRow3 = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow3, updated = true)
  }

  test("save: no phones") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testAvitoApartmentUrl
    val hash = SmartAgentAvitoRealtyParser.hash(url)
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    parsedOffer.getOfferBuilder.getSellerBuilder.addPhone("79293334455")
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // в апдейте пропали телефоны - сохраняем, но телефоны оставляем
    parsedOffer.getOfferBuilder.getSellerBuilder.clearPhone()
    parsedOffer.getOfferBuilder.setDescription("test1")
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).plusHours(1).getMillis / 1000)
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow, updated = true)

    val row = dao.getParsedOffers(Seq(hash)).head
    assert(row.data.getOffer.getDescription == "test1")
    assert(row.data.getOffer.getSeller.getPhoneCount == 1)
    assert(row.data.getOffer.getSeller.getPhone(0) == "79293334455")
    assert(row.data.getStatusHistoryCount == 2)
    assert(row.data.getStatusHistory(1).getDiff(1).getName == "Phones")
    assert(row.data.getStatusHistory(1).getDiff(1).getIgnored.getValue)
  }

  test("setSent") {
    val url: String = testAvitoApartmentUrl
    val hash = SmartAgentAvitoRealtyParser.hash(url)
    val callCenter = "te_ex"
    val row = testRow(url, phone = Some(getRandomPhone))

    setMinutes(1)
    assertSaveResult(row, inserted = true)

    setMinutes(2)
    assertSuccessFiltration(hash)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(2), dayStart.plusMinutes(2))

    setMinutes(3)
    assertSuccessCallCenterSet(hash, callCenter)

    setMinutes(4)
    val result4 = dao.getOrSetSendingForCallCenter(callCenter, 0, 100, isBizdev = false)
    assert(result4.length == 1)
    assert(result4.head.hash == hash)
    assert(result4.head.sentDate.isEmpty)

    setMinutes(5)
    assertSuccessSent(hash)

    val row3 = dao.getParsedOffers(Seq(hash)).head
    assert(row3.sentDate.value == dayStart.plusMinutes(5))
  }

  test("setSent: by phones") {
    val callCenter = "te_ex"
    val phone = getRandomPhone
    val rows = (0 until 10).map(_ => {
      val url: String = testAvitoApartmentUrl
      testRow(url, phone = Some(phone))
    })
    val hashes = rows.map(_.hash)

    setMinutes(1)
    assertSaveResults(rows, inserted = 10)

    setMinutes(2)
    assertSuccessFiltrations(hashes)
    rows.foreach(row => {
      assertRow(row.hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(2), dayStart.plusMinutes(2))
    })

    setMinutes(3)
    assertSuccessCallCenterSets(hashes, callCenter)

    setMinutes(4)
    // передаем maxSize = 1, но вернуться должны все READY объявления с этим телефоном
    val result4 = dao.getOrSetSendingForCallCenter(callCenter, 0, 1, isBizdev = false)
    assert(result4.length == 10)
    assert(result4.map(_.hash).toSet == hashes.toSet)
    assert(result4.forall(_.sentDate.isEmpty))

    setMinutes(5)
    assertSuccessSents(hashes)

    val result5 = dao.getParsedOffers(hashes)
    assert(result5.forall(_.sentDate.value == dayStart.plusMinutes(5)))
  }

  private def assertSaveResult(row: ParsedRealtyRow, inserted: Boolean = false, updated: Boolean = false): Unit = {
    val result = ImportResult(dao.save(Seq(row)))
    assert((result.insertedSimple == 1) == inserted)
    assert((result.updatedSimple == 1) == updated)
  }

  private def assertSaveResults(rows: Seq[ParsedRealtyRow], inserted: Int = 0, updated: Int = 0): Unit = {
    val result = ImportResult(dao.save(rows))
    assert(result.insertedSimple == inserted)
    assert(result.updatedSimple == updated)
  }

  private def assertSaveResultWithComment(row: ParsedRealtyRow,
                                          comment: String = "",
                                          inserted: Boolean = false,
                                          updated: Boolean = false,
                                          dbComment: String = ""): Unit = {
    val result = ImportResult(dao.save(Seq(row), comment))
    assert((result.insertedSimple == 1) == inserted)
    assert((result.updatedSimple == 1) == updated)
    if (inserted) {
      val dbRow = dao.getParsedOffers(Seq(row.hash)).head
      assert(dbRow.data.getStatusHistoryList.asScala.last.getComment == dbComment)
    } else if (updated) {
      val dbRow = dao.getParsedOffers(Seq(row.hash)).head
      assert(dbRow.data.getStatusHistoryList.asScala.last.getComment == dbComment)
    }
  }

  private def assertSuccessFiltration(hash: String): Unit = {
    assertSuccessFiltrations(Seq(hash))
  }

  private def assertSuccessFiltrations(hashes: Seq[String]): Unit = {
    val rows = dao.getParsedOffers(hashes)
    val result = dao.filter(rows) { rows =>
      rows
        .map(row => {
          row -> RealtyValidationResult(ValidationResult.Valid, ValidationResult.Valid)
        })
        .toMap
    }
    hashes.foreach(hash => assert(result(hash)))
  }

  private def assertFailedFiltration(hash: String)(errors: String*): Unit = {
    val row = dao.getParsedOffers(Seq(hash)).head
    val result = dao.filter(Seq(row)) { rows =>
      rows
        .map(row => {
          row -> RealtyValidationResult(ValidationResult.Invalid(errors), ValidationResult.Invalid(errors))
        })
        .toMap
    }
    assert(result(hash))
    val row2 = dao.getParsedOffers(Seq(hash)).head
    assert(row2.data.getFilterReasonList.asScala.sorted == errors.sorted)
    assert(row2.data.getStatusHistoryList.asScala.last.getComment.split(",").sorted.toSeq == errors.sorted)
    errors.foreach(error => {
      dao.getHashesByParams(QueryParams(filterReasons = Seq(error))).contains(hash)
    })
  }

  private def assertSuccessCallCenterSet(hash: String, callCenter: String): Unit = {
    assertSuccessCallCenterSets(Seq(hash), callCenter)
  }

  private def assertSuccessCallCenterSets(hashes: Seq[String], callCenter: String): Unit = {
    val result = dao.setCallCenter(hashes, callCenter, isBizdev = false)
    hashes.foreach(hash => assert(result(hash)))
  }

  private def assertSuccessSent(hash: String): Unit = {
    assertSuccessSents(Seq(hash))
  }

  private def assertSuccessSents(hashes: Seq[String]): Unit = {
    val result = dao.setSent(hashes)
    hashes.foreach(hash => assert(result(hash)))
  }

  private def assertRow(hash: String,
                        expectedStatus: Status,
                        expectedCreateDate: DateTime,
                        expectedUpdateDate: DateTime,
                        expectedStatusUpdateDate: DateTime,
                        expectedDeactivateDate: Option[DateTime] = None): ParsedRealtyRow = {
    val row = dao.getParsedOffers(Seq(hash)).head
    assert(row.status == expectedStatus)
    assert(row.data.getStatusHistoryList.asScala.last.getStatus == expectedStatus)
    assert(row.createDate == expectedCreateDate)
    assert(row.data.getStatusHistoryList.asScala.head.getUpdateDate.getSeconds == expectedCreateDate.getMillis / 1000)
    assert(row.data.getStatusHistoryList.asScala.last.getUpdateDate.getSeconds == expectedUpdateDate.getMillis / 1000)

    val statusMap = getStatusMap(row)

    val lastStatusChangeMomentSeconds = getStatusHistoryMoments(row).head._2
    assert(lastStatusChangeMomentSeconds == expectedStatusUpdateDate.getMillis / 1000)
    assert(row.statusUpdateDate == expectedStatusUpdateDate)

    if (row.sentDate.nonEmpty || statusMap.contains(Status.SENT)) {
      assert(row.sentDate.nonEmpty)
      assert(statusMap.contains(Status.SENT))
      assert(statusMap(Status.SENT) == row.sentDate.value.getMillis / 1000)
    }

    if (row.openDate.nonEmpty || statusMap.contains(Status.OPENED)) {
      assert(row.openDate.nonEmpty)
      assert(statusMap.contains(Status.OPENED))
      assert(statusMap(Status.OPENED) == row.openDate.value.getMillis / 1000)
    }

    if (row.status == Status.SENDING || row.status == Status.SENT || row.status == Status.OPENED ||
        row.status == Status.PUBLISHED) {
      assert(row.callCenter.nonEmpty)
    }

    if (row.status == Status.PUBLISHED) {
      assert(row.offerId.nonEmpty)
    }

    assert(row.deactivateDate == expectedDeactivateDate)

    row
  }

  private def getStatusMap(row: ParsedRealtyRow): Map[Status, Long] = {
    getStatusHistoryMoments(row)
      .groupBy(h => h._1)
      .mapValues(h => h.map(_._2).min)
  }

  private def getStatusHistoryMoments(row: ParsedRealtyRow): List[(Status, Long)] = {
    val l = row.data.getStatusHistoryList.asScala.map(h => (h.getStatus, h.getUpdateDate.getSeconds)).sortBy(_._2)
    l.foldLeft[List[(Status, Long)]](Nil) {
      case (res, e) =>
        if (res.isEmpty) List(e)
        else {
          val prevStatus = res.head._1
          val newStatus = e._1
          if (prevStatus == newStatus) res
          else {
            if (newStatus == Status.NEW &&
                (prevStatus == Status.SENT ||
                prevStatus == Status.OPENED ||
                prevStatus == Status.PUBLISHED ||
                prevStatus == Status.NOT_PUBLISHED)) {
              res
            } else e :: res
          }
        }
    }
  }

  private def setMinutes(minutes: Int) = {
    setTime(dayStart.plusMinutes(minutes))
  }

  private def setTime(date: DateTime) = {
    when(components.timeService.getNow).thenReturn(date)
  }

  private def getSeconds: Long = components.timeService.getNow.getSeconds
}
