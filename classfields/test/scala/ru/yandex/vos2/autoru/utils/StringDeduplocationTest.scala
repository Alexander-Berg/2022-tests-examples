package ru.yandex.vos2.autoru.utils

import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.deduplication.DeduplicationDaoImpl

/**
  * Created by sievmi on 25.02.19
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StringDeduplocationTest extends AnyFunSuite with InitTestDbs with BeforeAndAfter {
  initDbs()

  val dedup: StringDeduplication = components.stringDeduplication
  val dao = new DeduplicationDaoImpl(components.mySql)

  before {
    val sql = "DELETE FROM t_string_deduplication"
    components.mySql.shard(0).master.jdbc.update(sql)
  }

  test("CRUD") {
    assert(dedup.data.get().int2Str.isEmpty)
    assert(dedup.data.get().str2Int.isEmpty)
    // вставляем дефолтное значение
    dedup.getIntValue("", insertOnNotFound = true)

    assert(dedup.getIntValue("ab", insertOnNotFound = false).isEmpty)
    assert(dedup.getIntValue("ab", insertOnNotFound = true) == dao.get("ab"))

    dedup.reload()
    assert(dedup.getStrValue(dao.get("ab").get).contains("ab"))

    assert(new StringDeduplication(dao).data.get().int2Str.size == 2)
    assert(new StringDeduplication(dao).data.get().str2Int.size == 2)
  }
}
