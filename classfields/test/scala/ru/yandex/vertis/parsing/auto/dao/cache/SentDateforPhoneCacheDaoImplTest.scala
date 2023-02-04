package ru.yandex.vertis.parsing.auto.dao.cache

import org.joda.time.DateTime
import org.scalatest.{FunSuite, OptionValues}
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.tables.TLastSentDateForPhone
import ru.yandex.vertis.parsing.auto.parsers.scrapinghub.cars.avito.ScrapingHubAvitoCarsParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.auto.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.dao.cache.CacheRow
import ru.yandex.vertis.parsing.util.DateUtils.sql2date
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport
import ru.yandex.vertis.parsing.validators.ValidationResult
import ru.yandex.vertis.parsing.workers.importers.ImportResult
import ru.yandex.vertis.tracing.Traced
import SentDateforPhoneCacheDaoImpl.DefaultSentDate
import ru.yandex.vertis.parsing.util.DateUtils.RichDateTime

import scala.collection.JavaConverters._

class SentDateforPhoneCacheDaoImplTest
  extends FunSuite
  with InitTestDbs
  with OptionValues
  with MockitoSupport
  with EmptyTraceSupport {
  initDb()

  private val shard = components.parsingShard

  private def db = shard.master.jooq

  private val dao = components.parsedOffersDao

  private val cache = components.sentDateforPhoneCache

  private val lsd = TLastSentDateForPhone.T_LAST_SENT_DATE_FOR_PHONE

  private val dayStart = DateTime.now().withMillisOfDay(0)

  test("getOrElseUpdate = default") {
    setMinutes(0)
    val phone = TestDataUtils.getRandomPhone
    assert(cache.getOrElseUpdate(phone) == DefaultSentDate)
    assertDate(phone)
  }

  test("getOrElseUpdate non-default") {
    val callCenter = "te_ex"

    setMinutes(0)
    val phone = TestDataUtils.getRandomPhone

    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setOriginal(phone).setPhone(phone)
    val url: String = testAvitoCarsUrl
    val hash = ScrapingHubAvitoCarsParser.hash(url)
    val row: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    assertSaveResult(row, inserted = true)

    assertDate(phone)

    assertSuccessFiltration(hash)
    assertDate(phone)
    assertSuccessCallCenterSet(hash, callCenter)
    assertDate(phone)
    dao.getOrSetSendingForCallCenter(callCenter, Category.CARS, 0, 10, Seq.empty)
    assertDate(phone)
    dao.setSent(Seq(hash))
    assertDate(phone, dayStart, 2)
  }

  test("updateValues, syncCache") {
    val callCenter = "te_ex"

    setMinutes(0)
    val phone = TestDataUtils.getRandomPhone

    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setOriginal(phone).setPhone(phone)
    val url: String = testAvitoCarsUrl
    val hash = ScrapingHubAvitoCarsParser.hash(url)
    val row: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    assertSaveResult(row, inserted = true)
    assertSuccessFiltration(hash)
    assertSuccessCallCenterSet(hash, callCenter)
    dao.getOrSetSendingForCallCenter(callCenter, Category.CARS, 0, 10, Seq.empty)
    dao.setSent(Seq(hash))
    assertDate(phone, dayStart, 2)

    cache.updateValues(Map(phone -> DefaultSentDate)) { case (prevV, newV) => prevV.max(newV) }
    assertDate(phone, DefaultSentDate, 3)

    cache.syncCache(3)
    assertDate(phone, DefaultSentDate, 3)

    setHours(252)

    cache.syncCache(3)

    assertDate(phone, dayStart, 4)
  }

  private def assertDate(phone: String, date: DateTime = DefaultSentDate, version: Long = 1L): Unit = {
    val cacheRows = getCacheRows(Seq(phone))
    assert(cacheRows.length == 1)
    assert(cacheRows.head.key == phone)
    assert(cacheRows.head.value == date)
    assert(cacheRows.head.version == version)
    assert(cacheRows.head.syncDate.getMillis >= dayStart.plusHours(6).getMillis)
  }

  private def getCacheRows(keys: Seq[String])(implicit trace: Traced): Seq[CacheRow[DateTime]] = {
    db.query("active_by_user_id_rows") { dsl =>
        dsl.selectFrom(lsd).where(lsd.PHONE.in(keys.asJava))
      }
      .map(row => CacheRow(row.component1.toString, sql2date(row.component2), sql2date(row.component3), row.component4))
  }

  private def assertSaveResult(row: ParsedRow, inserted: Boolean = false, updated: Boolean = false): Unit = {
    val result = ImportResult(dao.save(Seq(row)))(_.name())
    assert((result.insertedSimple == 1) == inserted)
    assert((result.updatedSimple == 1) == updated)
  }

  private def assertSuccessFiltration(hash: String): Unit = {
    val row = dao.getParsedOffers(Seq(hash)).head
    val result = dao.filter(Seq(row)) { rows =>
      rows.map(row => row -> ValidationResult.Valid).toMap
    }
    assert(result(hash))
  }

  private def assertSuccessCallCenterSet(hash: String, callCenter: String): Unit = {
    val result = dao.setCallCenter(Seq(hash), callCenter)
    assert(result(hash))
  }

  private def setMinutes(minutes: Int) = {
    setTime(dayStart.plusMinutes(minutes))
  }

  private def setHours(hours: Int) = {
    setTime(dayStart.plusHours(hours))
  }

  private def setTime(date: DateTime) = {
    when(components.timeService.getNow).thenReturn(date)
  }
}
