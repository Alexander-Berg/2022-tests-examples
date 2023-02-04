package ru.yandex.vertis.parsing.realty.dao.cache.username

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.dao.cache.CacheRow
import ru.yandex.vertis.parsing.realty.ParsingRealtyModel.ParsedOffer
import ru.yandex.vertis.parsing.realty.dao.model.jooq.parsing.tables.TRealtyActiveForUsername
import ru.yandex.vertis.parsing.realty.dao.offers.ParsedRealtyRow
import ru.yandex.vertis.parsing.realty.parsers.smartagent.avito.SmartAgentAvitoRealtyParser
import ru.yandex.vertis.parsing.realty.util.TestDataUtils._
import ru.yandex.vertis.parsing.realty.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.realty.workers.importers.RealtyCategoryFormatter
import ru.yandex.vertis.parsing.util.DateUtils.{sql2date, RichDateTime}
import ru.yandex.vertis.parsing.util.RandomUtil
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport
import ru.yandex.vertis.parsing.workers.importers.ImportResult
import ru.yandex.vertis.tracing.Traced

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ActiveCountForUserNameCacheDaoImplTest
  extends FunSuite
  with InitTestDbs
  with OptionValues
  with MockitoSupport
  with EmptyTraceSupport {
  initDb()

  private val shard = components.parsingRealtyShard

  private def db = shard.master.jooq

  private val dao = components.parsedRealtyOffersDao

  private val cache = components.activeForUserNameCache

  private val aun = TRealtyActiveForUsername.T_REALTY_ACTIVE_FOR_USERNAME

  private val dayStart = DateTime.now().withMillisOfDay(0)

  test("getOrElseUpdate = 0") {
    setHours(0)
    val userName = RandomUtil.nextHexString(6)
    assert(cache.getOrElseUpdate(userName) == 0)
    val cacheRows = getCacheRows(Seq(userName))
    assert(cacheRows.length == 1)
    assert(cacheRows.head.key == userName)
    assert(cacheRows.head.value == 0)
    assert(cacheRows.head.version == 1)
    assert(cacheRows.head.syncDate.getMillis >= dayStart.plusHours(6).getMillis)
  }

  test("getOrElseUpdate > 0") {
    setHours(0)
    val userName = RandomUtil.nextHexString(6)

    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserName(userName)
    val url: String = testAvitoApartmentUrl
    val hash = SmartAgentAvitoRealtyParser.hash(url)
    val row: ParsedRealtyRow = testRow(url, parsedOffer)

    assertSaveResult(row, inserted = true)

    assert(cache.getOrElseUpdate(userName) == 1)
    val cacheRows = getCacheRows(Seq(userName))
    assert(cacheRows.length == 1)
    val cacheRow = cacheRows.head
    assert(cacheRow.key == userName)
    assert(cacheRow.value == 1)
    assert(cacheRow.version == 1)
    assert(cacheRow.syncDate.getMillis >= dayStart.plusHours(6).getMillis)

    assert(cache.getOrElseUpdate(userName) == 1)
    assert(getCacheRows(Seq(userName)).length == 1)

    val url2: String = testAvitoApartmentUrl
    val hash2 = SmartAgentAvitoRealtyParser.hash(url2)
    val row2: ParsedRealtyRow = testRow(url2, parsedOffer)

    assertSaveResult(row2, inserted = true)
    assert(cache.getOrElseUpdate(userName) == 2)

    dao.setDeactivated(Seq(hash))
    assert(cache.getOrElseUpdate(userName) == 1)

    dao.setDeactivated(Seq(hash2))
    assert(cache.getOrElseUpdate(userName) == 0)
  }

  test("updateActiveForUserNames, syncActiveForUserNames") {
    setHours(0)
    val userName = RandomUtil.nextHexString(6)

    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserName(userName)
    val url: String = testAvitoApartmentUrl
    val row: ParsedRealtyRow = testRow(url, parsedOffer)

    assertSaveResult(row, inserted = true)

    val url2: String = testAvitoApartmentUrl
    val row2: ParsedRealtyRow = testRow(url2, parsedOffer)

    assertSaveResult(row2, inserted = true)

    assert(cache.getOrElseUpdate(userName) == 2)

    cache.updateValues(Map(userName -> -2))(_ + _)

    assert(cache.getOrElseUpdate(userName) == 0)

    cache.syncCache(3)

    assert(cache.getOrElseUpdate(userName) == 0)

    setHours(252)

    cache.syncCache(3)

    assert(cache.getOrElseUpdate(userName) == 2)
  }

  test("case insensitive") {
    setHours(0)
    val userName1 = "Диана"
    val userName2 = "диана"

    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserName(userName1)
    val url: String = testAvitoApartmentUrl
    val row: ParsedRealtyRow = testRow(url, parsedOffer)

    assertSaveResult(row, inserted = true)
    assert(cache.getOrElseUpdate(userName1) == 1)
    assert(cache.getOrElseUpdate(userName2) == 1)

    val url2: String = testAvitoApartmentUrl
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserName(userName2)
    val row2: ParsedRealtyRow = testRow(url2, parsedOffer)

    assertSaveResult(row2, inserted = true)

    assert(cache.getOrElseUpdate(userName1) == 2)
    assert(cache.getOrElseUpdate(userName2) == 2)
  }

  test("spaces") {
    setHours(0)
    val userName1 = "Артем"
    val userName2 = "Артем    "

    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserName(userName1)
    val url: String = testAvitoApartmentUrl
    val row: ParsedRealtyRow = testRow(url, parsedOffer)

    assertSaveResult(row, inserted = true)
    assert(cache.getOrElseUpdate(userName1) == 1)

    val url2: String = testAvitoApartmentUrl
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserName(userName2)
    val row2: ParsedRealtyRow = testRow(url2, parsedOffer)

    assertSaveResult(row2, inserted = true)

    assert(cache.getOrElseUpdate(userName1) == 2)
    assert(cache.getOrElseUpdate(userName2) == 2)
  }

  test("ё") {
    setHours(0)
    val userName1 = "Алена"
    val userName2 = "Алёна"

    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserName(userName1)
    val url: String = testAvitoApartmentUrl
    val row: ParsedRealtyRow = testRow(url, parsedOffer)

    assertSaveResult(row, inserted = true)
    assert(cache.getOrElseUpdate(userName1) == 1)

    val url2: String = testAvitoApartmentUrl
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserName(userName2)
    val row2: ParsedRealtyRow = testRow(url2, parsedOffer)

    assertSaveResult(row2, inserted = true)

    assert(cache.getOrElseUpdate(userName1) == 2)
    assert(cache.getOrElseUpdate(userName2) == 2)
  }

  private def assertSaveResult(row: ParsedRealtyRow, inserted: Boolean = false, updated: Boolean = false): Unit = {
    val result = ImportResult(dao.save(Seq(row)))
    assert((result.insertedSimple == 1) == inserted)
    assert((result.updatedSimple == 1) == updated)
  }

  private def getCacheRows(keys: Seq[String])(implicit trace: Traced): Seq[CacheRow[Long]] = {
    db.query("active_by_user_id_rows") { dsl =>
        dsl.selectFrom(aun).where(aun.USERNAME.in(keys.asJava))
      }
      .map(row => CacheRow(row.component1, row.component2.toLong, sql2date(row.component3), row.component4))
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

  private def getSeconds: Long = components.timeService.getNow.getSeconds
}
