package ru.yandex.vertis.parsing.realty.dao.cache.phone

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.realty.ParsingRealtyModel.ParsedOffer
import ru.yandex.vertis.parsing.realty.dao.offers.ParsedRealtyRow
import ru.yandex.vertis.parsing.realty.parsers.smartagent.avito.SmartAgentAvitoRealtyParser
import ru.yandex.vertis.parsing.realty.util.TestDataUtils
import ru.yandex.vertis.parsing.realty.util.TestDataUtils.{testAvitoApartmentUrl, testRow}
import ru.yandex.vertis.parsing.realty.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.realty.workers.importers.RealtyCategoryFormatter
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport
import ru.yandex.vertis.parsing.workers.importers.ImportResult

@RunWith(classOf[JUnitRunner])
class ActiveCountForPhoneCacheDaoImplTest
  extends FunSuite
  with InitTestDbs
  with OptionValues
  with MockitoSupport
  with EmptyTraceSupport {
  initDb()

  private val shard = components.parsingRealtyShard

  private val dao = components.parsedRealtyOffersDao

  private val cache = components.activeForPhoneCache

  private val dayStart = DateTime.now().withMillisOfDay(0)

  test("getActiveForPhoneCount = 0") {
    setHours(0)
    val phone = TestDataUtils.getRandomPhone
    assert(cache.getActiveForPhoneCount(Seq(phone)) == Map.empty)
  }

  test("getActiveForPhoneCount > 0") {
    setHours(0)
    val phone = TestDataUtils.getRandomPhone

    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.addPhone(phone)
    val url: String = testAvitoApartmentUrl
    val hash = SmartAgentAvitoRealtyParser.hash(url)
    val row: ParsedRealtyRow = testRow(url, parsedOffer)

    assertSaveResult(row, inserted = true)

    assert(cache.getActiveForPhoneCount(Seq(phone)) == Map(phone -> 1))

    val url2: String = testAvitoApartmentUrl
    val hash2 = SmartAgentAvitoRealtyParser.hash(url2)
    val row2: ParsedRealtyRow = testRow(url2, parsedOffer)

    assertSaveResult(row2, inserted = true)
    assert(cache.getActiveForPhoneCount(Seq(phone)) == Map(phone -> 2))

    dao.setDeactivated(Seq(hash))
    assert(cache.getActiveForPhoneCount(Seq(phone)) == Map(phone -> 1))

    dao.setDeactivated(Seq(hash2))
    assert(cache.getActiveForPhoneCount(Seq(phone)) == Map.empty)
  }

  private def assertSaveResult(row: ParsedRealtyRow, inserted: Boolean = false, updated: Boolean = false): Unit = {
    val result = ImportResult(dao.save(Seq(row)))
    assert((result.insertedSimple == 1) == inserted)
    assert((result.updatedSimple == 1) == updated)
  }

  private def setHours(hours: Int) = {
    setTime(dayStart.plusHours(hours))
  }

  private def setTime(date: DateTime) = {
    when(components.timeService.getNow).thenReturn(date)
  }
}
