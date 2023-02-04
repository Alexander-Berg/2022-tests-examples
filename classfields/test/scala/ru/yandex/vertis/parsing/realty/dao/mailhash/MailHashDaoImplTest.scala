package ru.yandex.vertis.parsing.realty.dao.mailhash

import org.junit.runner.RunWith
import org.scalatest.{FunSuite, OptionValues}
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.realty.dao.offers.ParsedRealtyRow
import ru.yandex.vertis.parsing.realty.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport
import ru.yandex.vertis.parsing.workers.importers.ImportResult
import ru.yandex.vertis.parsing.realty.util.TestDataUtils._
import ru.yandex.vertis.parsing.realty.workers.importers.RealtyCategoryFormatter

@RunWith(classOf[JUnitRunner])
class MailHashDaoImplTest
  extends FunSuite
  with InitTestDbs
  with OptionValues
  with MockitoSupport
  with EmptyTraceSupport {
  initDb()

  private val shard = components.parsingRealtyShard

  private def db = shard.master.jooq

  private val offersDao = components.parsedRealtyOffersDao
  private val dao = components.mailHashDao

  test("setHash, getIds") {
    val rows = (1 to 200).map(_ => {
      val url: String = testAvitoApartmentUrl
      testRow(url)
    })
    assertSaveResults(rows, inserted = 200)
    val ids = (1L to 200L)
    val hash = dao.createHash(ids)
    assert(hash.length == 32)
    val ids2 = dao.getIds(hash)
    assert(ids2.length == 200)
    assert(ids2.diff(ids).isEmpty)
    assert(ids.diff(ids2).isEmpty)
  }

  private def assertSaveResults(rows: Seq[ParsedRealtyRow], inserted: Int = 0, updated: Int = 0): Unit = {
    val result = ImportResult(offersDao.save(rows))
    assert(result.insertedSimple == inserted)
    assert(result.updatedSimple == updated)
  }
}
