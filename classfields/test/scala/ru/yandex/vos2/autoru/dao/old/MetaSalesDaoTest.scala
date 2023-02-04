package ru.yandex.vos2.autoru.dao.old

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.dao.utils.SimpleRowMapper

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 11/1/16.
  */
@RunWith(classOf[JUnitRunner])
class MetaSalesDaoTest extends AnyFunSuite with InitTestDbs with BeforeAndAfterAll {
  initOldSalesDbs()

  val metaSalesDao = new MetaSalesDao("all7", components.oldSalesDatabase)

  def showTables: Set[String] = {
    components.oldSalesDatabase.master.jdbc.query("show tables in all7", SimpleRowMapper(_.getString(1))).asScala.toSet
  }

  test("tablesSet") {
    val tablesList = metaSalesDao.tablesSet.asScala.toList.sorted
    val l = {
      val x = showTables.toList
      (x ::: x.map(y => s"all7.$y")).sorted
    }
    assert(
      tablesList == l,
      s"\ntablesList.sorted.diff(l)=${tablesList.sorted.diff(l).mkString(", ")}\n" +
        s"l.sorted.diff(tablesList)=${l.sorted.diff(tablesList).mkString(", ")}"
    )
  }

  test("ensureTableExists") {
    metaSalesDao.ensureTableExists("sales_images_blabla", "sales_images")
    assert(metaSalesDao.tablesSet.contains("sales_images_blabla"))
    assert(showTables.contains("sales_images_blabla"))
  }
}
