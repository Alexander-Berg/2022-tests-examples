package ru.yandex.vertis.parsing.realty.dao.cache.userid

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.dao.cache.CacheRow
import ru.yandex.vertis.parsing.realty.ParsingRealtyModel.ParsedOffer
import ru.yandex.vertis.parsing.realty.dao.model.jooq.parsing.tables.TRealtyActiveForUserId
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
class ActiveCountForUserIdCacheDaoImplTest
  extends FunSuite
  with InitTestDbs
  with OptionValues
  with MockitoSupport
  with EmptyTraceSupport {
  initDb()

  private val shard = components.parsingRealtyShard

  private def db = shard.master.jooq

  private val dao = components.parsedRealtyOffersDao

  private val cache = components.activeForUserIdCache

  private val aui = TRealtyActiveForUserId.T_REALTY_ACTIVE_FOR_USER_ID

  private val dayStart = DateTime.now().withMillisOfDay(0)

  test("getOrElseUpdate = 0") {
    setMinutes(0)
    val userId = RandomUtil.nextHexString(6)
    assert(cache.getOrElseUpdate(userId) == 0)
    val cacheRows = getCacheRows(Seq(userId))
    assert(cacheRows.length == 1)
    assert(cacheRows.head.key == userId)
    assert(cacheRows.head.value == 0)
    assert(cacheRows.head.version == 1)
    assert(cacheRows.head.syncDate.getMillis >= dayStart.plusHours(6).getMillis)
  }

  test("getOrElseUpdate > 0") {
    setMinutes(0)
    val userId = RandomUtil.nextHexString(6)

    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserId(userId)
    val url: String = testAvitoApartmentUrl
    val hash = SmartAgentAvitoRealtyParser.hash(url)
    val row: ParsedRealtyRow = testRow(url, parsedOffer)

    assertSaveResult(row, inserted = true)

    assert(cache.getOrElseUpdate(userId) == 1)
    val cacheRows = getCacheRows(Seq(userId))
    assert(cacheRows.length == 1)
    val cacheRow = cacheRows.head
    assert(cacheRow.key == userId)
    assert(cacheRow.value == 1)
    assert(cacheRow.version == 1)
    assert(cacheRow.syncDate.getMillis >= dayStart.plusHours(6).getMillis)

    assert(cache.getOrElseUpdate(userId) == 1)
    assert(getCacheRows(Seq(userId)).length == 1)

    val url2: String = testAvitoApartmentUrl
    val hash2 = SmartAgentAvitoRealtyParser.hash(url2)
    val row2: ParsedRealtyRow = testRow(url2, parsedOffer)

    assertSaveResult(row2, inserted = true)
    assert(cache.getOrElseUpdate(userId) == 2)

    dao.setDeactivated(Seq(hash))
    assert(cache.getOrElseUpdate(userId) == 1)

    dao.setDeactivated(Seq(hash2))
    assert(cache.getOrElseUpdate(userId) == 0)
  }

  test("updateActiveForUserIds, syncActiveForUserIds") {
    setMinutes(0)
    val userId = RandomUtil.nextHexString(6)

    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserId(userId)
    val url: String = testAvitoApartmentUrl
    val hash = SmartAgentAvitoRealtyParser.hash(url)
    val row: ParsedRealtyRow = testRow(url, parsedOffer)

    assertSaveResult(row, inserted = true)

    val url2: String = testAvitoApartmentUrl
    val hash2 = SmartAgentAvitoRealtyParser.hash(url2)
    val row2: ParsedRealtyRow = testRow(url2, parsedOffer)

    assertSaveResult(row2, inserted = true)

    assert(cache.getOrElseUpdate(userId) == 2)

    cache.updateValues(Map(userId -> -2))(_ + _)

    assert(cache.getOrElseUpdate(userId) == 0)

    cache.syncCache(3)

    assert(cache.getOrElseUpdate(userId) == 0)

    setHours(252)

    cache.syncCache(3)

    assert(cache.getOrElseUpdate(userId) == 2)
  }

  private def assertSaveResult(row: ParsedRealtyRow, inserted: Boolean = false, updated: Boolean = false): Unit = {
    val result = ImportResult(dao.save(Seq(row)))
    assert((result.insertedSimple == 1) == inserted)
    assert((result.updatedSimple == 1) == updated)
  }

  private def getCacheRows(keys: Seq[String])(implicit trace: Traced): Seq[CacheRow[Long]] = {
    db.query("active_by_user_id_rows") { dsl =>
        dsl.selectFrom(aui).where(aui.USER_ID.in(keys.asJava))
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
