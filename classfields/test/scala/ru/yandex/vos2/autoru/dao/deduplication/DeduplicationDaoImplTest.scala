package ru.yandex.vos2.autoru.dao.deduplication

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import ru.yandex.vos2.autoru.InitTestDbs

/**
  * Created by sievmi on 26.02.19
  */
class DeduplicationDaoImplTest extends AnyFunSuite with InitTestDbs with BeforeAndAfter with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    initDbs()
  }

  before {
    components.mySql.shards.head.master.jdbc.update("DELETE FROM t_string_deduplication")
  }

  val deduplicationDao = new DeduplicationDaoImpl(components.mySql)

  test("insert duplication value") {
    assert(deduplicationDao.addValue("a"))
    assert(!deduplicationDao.addValue("a"))

    val fullDedup = deduplicationDao.getFullDeduplication
    assert(fullDedup.size == 1)
    assert(fullDedup.contains("a"))
    assert(fullDedup.get("a") == deduplicationDao.get("a"))
  }

  test("insert unique values") {
    assert(deduplicationDao.addValue("a"))
    assert(deduplicationDao.addValue("b"))

    val fullDedup = deduplicationDao.getFullDeduplication
    assert(fullDedup.size == 2)
    assert(fullDedup.contains("a"))
    assert(fullDedup.contains("b"))
    assert(fullDedup.get("a") == deduplicationDao.get("a"))
    assert(fullDedup.get("b") == deduplicationDao.get("b"))
  }
}
