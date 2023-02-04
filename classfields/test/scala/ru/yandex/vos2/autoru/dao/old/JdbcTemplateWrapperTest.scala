package ru.yandex.vos2.autoru.dao.old

import java.sql.{Connection, PreparedStatement, Statement}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.support.GeneratedKeyHolder
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.old.utils.{BatchOperations, RelatedTableInfo}
import ru.yandex.vos2.autoru.dao.old.utils.PreparedStatementUtils.{SqlColumnData, SqlRow}
import ru.yandex.vos2.dao.utils.{JdbcTemplateWrapper, SimpleRowMapper}
import ru.yandex.vos2.util.{DbUtils, RandomUtil}

import java.util.concurrent.ThreadLocalRandom
import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 10/26/16.
  */
@RunWith(classOf[JUnitRunner])
class JdbcTemplateWrapperTest extends AnyFunSuite with InitTestDbs {
  // тут потыкаем палочкой в варианты апдейта и получения айдишника вставленной записи, работают ли, как воображается

  val autoruSalesDao = components.autoruSalesDao

  test("insertUpdate") {
    import ru.yandex.vos2.autoru.dao.old.utils.PreparedStatementUtils._
    val db = components.oldSalesDatabase.master
    val jdbc = components.oldSalesDatabase.master.jdbc
    jdbc.update("drop table if exists tmpTable")
    jdbc.update("""CREATE TABLE `tmpTable` (
        |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
        |  `address` varchar(255) DEFAULT NULL,
        |  PRIMARY KEY (`id`)
        |) ENGINE=InnoDB DEFAULT CHARSET=utf8""".stripMargin)

    val tmpTableOperations = new BatchOperations("tmpTable", db)
    val id1 = tmpTableOperations.execute(None, Seq("address" ~> "1".sqlAnyRef))
    // 1 1
    assert(id1 == 1)

    val id2 = tmpTableOperations.execute(None, Seq("address" ~> "2".sqlAnyRef))
    // 1 1
    // 2 2
    assert(id2 == 2)

    val id3 = tmpTableOperations.execute(None, Seq("address" ~> "3".sqlAnyRef))
    // 1 1
    // 2 2
    // 3 3
    assert(id3 == 3)

    val id3_2 = jdbc.queryForInt("select id from tmpTable where address = '3'")
    assert(id3_2 == 3)

    val id4 = tmpTableOperations.execute(None, Seq("address" ~> "4".sqlAnyRef))
    // 1 1
    // 2 2
    // 3 3
    // 4 4
    assert(id4 == 4)

    val id4_2 = tmpTableOperations.execute(Some(4L), Seq("address" ~> "4".sqlAnyRef))
    // 1 1
    // 2 2
    // 3 3
    // 4 4
    assert(id4_2 == 4)

    val updateId: java.lang.Long = Option(0L).filterNot(_ == 0).map(Long.box).orNull
    jdbc.update("insert into tmpTable (id, address) VALUES (?, ?)", updateId, "890")
    // 1 1
    // 2 2
    // 3 3
    // 5 890

    val id5 = jdbc.queryForInt("select id from tmpTable where address = '890'")
    assert(id5 == 5)

    val id4_3 = tmpTableOperations.execute(Some(4L), Seq("address" ~> "4".sqlAnyRef))
    assert(id4_3 == 4)

    // test batchUpdateWithGeneratedKeys
    val args: Seq[Array[_ <: AnyRef]] = Seq(
      Array(null, Int.box(6)),
      Array(null, Int.box(7)),
      Array(Int.box(5), "120")
    )
    val keys = jdbc.batchUpdateWithGeneratedKeys(
      "insert into tmpTable (id, address) values (?, ?) on duplicate " +
        "key update address = address",
      args
    )
    // 1  | 1
    // 2  | 2
    // 3  | 3
    // 4  | 4
    // 5  | 890
    // 6  | 6
    // 7  | 7

    val generatedIds = keys.sorted.toList
    assert(generatedIds == List(6L, 7L, 8L))
    assert(
      getTmpTableData(jdbc) == List(
        (1L, "1"),
        (2L, "2"),
        (3L, "3"),
        (4L, "4"),
        (5L, "890"),
        (6L, "6"),
        (7L, "7")
      )
    )

    val sql = "insert into tmpTable (id, address) values (?, ?), (?, ?), (?, ?) " +
      "on duplicate key update address = address"
    val data: Seq[AnyRef] = Seq(null, Int.box(9), null, Int.box(10), Int.box(5), "121")
    val psc = new PreparedStatementCreator {
      override def createPreparedStatement(con: Connection): PreparedStatement = {
        val ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        data.zipWithIndex.foreach(kv => ps.setObject(kv._2 + 1, kv._1))
        ps
      }
    }
    val keyHolder2 = new GeneratedKeyHolder()
    jdbc.update(psc, keyHolder2)
    // 1  | 1
    // 2  | 2
    // 3  | 3
    // 4  | 4
    // 5  | 890
    // 6  | 6
    // 7  | 7
    // 9  | 9
    // 10 | 10

    val generatedIds2 = keyHolder2.getKeyList.asScala.map(m => m.asScala("GENERATED_KEY").toString.toLong).sorted.toList
    assert(generatedIds2 == List(9L, 10L, 11L))
    assert(
      getTmpTableData(jdbc) == List(
        (1L, "1"),
        (2L, "2"),
        (3L, "3"),
        (4L, "4"),
        (5L, "890"),
        (6L, "6"),
        (7L, "7"),
        (9L, "9"),
        (10L, "10")
      )
    )

    // тест массовой вставки/обновления с помощью класса BatchOperations
    val generatedIds3 = tmpTableOperations
      .execute(
        Seq(
          SqlRow("address" ~> "11"),
          SqlRow("address" ~> "12"),
          SqlRow(5L, "address" ~> 900),
          SqlRow(6L, "address" !> 900)
        ),
        false
      )
      .toList
    assert(generatedIds3 == List(12L, 13L, 5L, 6L))
    val tmpTableData = getTmpTableData(jdbc)
    assert(
      tmpTableData == List(
        (1L, "1"),
        (2L, "2"),
        (3L, "3"),
        (4L, "4"),
        (5L, "900"),
        (6L, "6"),
        (7L, "7"),
        (9L, "9"),
        (10L, "10"),
        (12L, "11"),
        (13L, "12")
      )
    )

    jdbc.update("drop table if exists tmpTable2")
    jdbc.update("""CREATE TABLE `tmpTable2` (
        |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
        |  `sale_id` int(10),
        |  `address` varchar(255) DEFAULT NULL,
        |  PRIMARY KEY (`id`)
        |) ENGINE=InnoDB DEFAULT CHARSET=utf8""".stripMargin)

    fillTmpTable2WithRandomData(jdbc)
    val filledtmpTable2 = getTmpTable2Data(jdbc)
    val randomData = createRandomData(filledtmpTable2)

    val tmpTable2Operations = new BatchOperations("tmpTable2", db)

    val result = tmpTable2Operations.execute(randomData)

    val insertedRandomData = randomData.map {
      case (saleId, rows) =>
        (saleId, rows.zip(result(saleId)).map { case (row, id) => row.copy(maybeId = Some(id)) }.sortBy(_.maybeId.get))
    }

    val tmpTable2 = getTmpTable2Data(jdbc)

    assert(tmpTable2.size == insertedRandomData.size)
    tmpTable2.foreach {
      case (saleId, rows) =>
        assert(rows == insertedRandomData(saleId))
    }

    // удаление всех записей для данного sale_id
    val result2 = tmpTable2Operations.execute(Map[Long, Seq[SqlRow]](100L -> Seq.empty))
    assert(result2(100).isEmpty)
    assert(getTmpTable2Data(jdbc).get(100).isEmpty)
  }

  test("fullSql test") {
    val sql = "insert into table (description) values (?)"
    val args = Seq(""" @#$%^&*()\${}[]<>,.''""? """)
    val res = DbUtils.fullSql(sql, args)
    assert(res == """insert into table (description) values (' @#$%^&*()\${}[]<>,.''""¿ ')""")
  }

  test("delete related") {
    import ru.yandex.vos2.autoru.dao.old.utils.PreparedStatementUtils._
    val db = components.oldSalesDatabase.master
    val jdbc = components.oldSalesDatabase.master.jdbc
    jdbc.update("drop table if exists tmpMainTable")
    jdbc.update("""CREATE TABLE `tmpMainTable` (
        |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
        |  `sale_id` int(10) unsigned NOT NULL,
        |  `data` varchar(255) DEFAULT NULL,
        |  PRIMARY KEY (`id`)
        |) ENGINE=InnoDB DEFAULT CHARSET=utf8""".stripMargin)

    jdbc.update("drop table if exists tmpRelatedTable1")
    jdbc.update("""CREATE TABLE `tmpRelatedTable1` (
        |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
        |  `parent_id` int(10) unsigned NOT NULL,
        |  `data` varchar(255) DEFAULT NULL,
        |  PRIMARY KEY (`id`)
        |) ENGINE=InnoDB DEFAULT CHARSET=utf8""".stripMargin)

    jdbc.update("drop table if exists tmpRelatedTable2")
    jdbc.update("""CREATE TABLE `tmpRelatedTable2` (
        |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
        |  `parent_id` int(10) unsigned NOT NULL,
        |  `data` varchar(255) DEFAULT NULL,
        |  PRIMARY KEY (`id`)
        |) ENGINE=InnoDB DEFAULT CHARSET=utf8""".stripMargin)

    val tmpMainTableOperations = new BatchOperations(
      "tmpMainTable",
      db,
      Seq(
        RelatedTableInfo("id", "tmpRelatedTable1", "parent_id"),
        RelatedTableInfo("id", "tmpRelatedTable2", "parent_id")
      ),
      "sale_id"
    )

    val tmpRelatedTable1Operations = new BatchOperations("tmpRelatedTable1", db)
    val tmpRelatedTable2Operations = new BatchOperations("tmpRelatedTable2", db)

    // id = 1
    tmpMainTableOperations.execute(None, Seq("sale_id" ~> 1, "data" ~> "data1".sqlAnyRef))
    // id = 2
    tmpMainTableOperations.execute(None, Seq("sale_id" ~> 1, "data" ~> "data2".sqlAnyRef))
    // id = 3
    tmpMainTableOperations.execute(None, Seq("sale_id" ~> 2, "data" ~> "data3".sqlAnyRef))
    // id = 4
    tmpMainTableOperations.execute(None, Seq("sale_id" ~> 3, "data" ~> "data4".sqlAnyRef))

    tmpRelatedTable1Operations.execute(None, Seq("parent_id" ~> 1, "data" ~> "rel data 1"))
    tmpRelatedTable1Operations.execute(None, Seq("parent_id" ~> 1, "data" ~> "rel data 2"))
    tmpRelatedTable1Operations.execute(None, Seq("parent_id" ~> 2, "data" ~> "rel data 3"))
    tmpRelatedTable1Operations.execute(None, Seq("parent_id" ~> 3, "data" ~> "rel data 4"))

    tmpRelatedTable2Operations.execute(None, Seq("parent_id" ~> 2, "data" ~> "rel data 1"))
    tmpRelatedTable2Operations.execute(None, Seq("parent_id" ~> 2, "data" ~> "rel data 2"))
    tmpRelatedTable2Operations.execute(None, Seq("parent_id" ~> 4, "data" ~> "rel data 3"))

    // удаляем из главной и из связанных таблиц все записи про sale_id = 1
    val removeFromMainResult = tmpMainTableOperations.execute(Map[Long, Seq[SqlRow]](1L -> Seq.empty))

    val mainTableResult =
      jdbc
        .query("select id, sale_id from tmpMainTable", new SimpleRowMapper[(Int, Int)](rs => {
          rs.getInt(2) -> rs.getInt(1)
        }))
        .asScala
        .toMap

    val relatedTable1Result =
      jdbc
        .query("select id, parent_id from tmpRelatedTable1", new SimpleRowMapper[(Int, Int)](rs => {
          rs.getInt(2) -> rs.getInt(1)
        }))
        .asScala
        .toMap

    val relatedTable2Result =
      jdbc
        .query("select id, parent_id from tmpRelatedTable2", new SimpleRowMapper[(Int, Int)](rs => {
          rs.getInt(2) -> rs.getInt(1)
        }))
        .asScala
        .toMap

    assert(removeFromMainResult(1).isEmpty)

    assert(!mainTableResult.contains(1))
    assert(mainTableResult.contains(2))
    assert(mainTableResult.contains(3))

    assert(!relatedTable1Result.contains(1))
    assert(!relatedTable1Result.contains(2))
    assert(relatedTable1Result.contains(3))

    assert(!relatedTable2Result.contains(1))
    assert(!relatedTable2Result.contains(2))
    assert(!relatedTable2Result.contains(3))
    assert(relatedTable2Result.contains(4))
  }

  def getTmpTableData(jdbc: JdbcTemplateWrapper): List[(Long, String)] = {
    jdbc
      .query("select * from tmpTable", new SimpleRowMapper[(Long, String)](rs => {
        (rs.getLong(1), rs.getString(2))
      }))
      .asScala
      .toList
  }

  private val random = ThreadLocalRandom.current()

  def fillTmpTable2WithRandomData(jdbc: JdbcTemplateWrapper): Unit = {
    val randomNum = 60
    (1 to randomNum).foreach(_ => {
      val rndAddr = RandomUtil.nextSymbols(10)
      val saleId = random.nextLong(1, 5)
      jdbc.update(s"insert into tmpTable2 (sale_id, address) values ($saleId, '$rndAddr')")
    })
  }

  import ru.yandex.vos2.util.Collections._

  import scala.jdk.CollectionConverters._

  def getTmpTable2Data(jdbc: JdbcTemplateWrapper): Map[Long, Seq[SqlRow]] = {
    jdbc
      .query(
        "select id, sale_id, address from tmpTable2",
        new SimpleRowMapper[(Long, SqlRow)](rs => {
          (rs.getLong(2), SqlRow(Some(rs.getLong(1)), Seq(SqlColumnData("address", rs.getString(3), update = true))))
        })
      )
      .asScala
      .toMultiMap
  }

  def createRandomData(filledTmpTable2: Map[Long, Seq[SqlRow]]): Map[Long, Seq[SqlRow]] = {
    val saleIds = filledTmpTable2.keys.toSeq
    val curBuf = filledTmpTable2.toSeq.flatMap(kv => kv._2.map(row => (kv._1, row))).toBuffer
    val randomNum = 60
    (1 to randomNum)
      .map(_ => {
        val getPresent = random.nextBoolean()
        if (getPresent && curBuf.nonEmpty) {
          curBuf.remove(random.nextInt(curBuf.length))
        } else {
          val rndAddr = RandomUtil.nextSymbols(10)
          val saleId = saleIds(random.nextInt(saleIds.length))
          (saleId, SqlRow(None, Seq(SqlColumnData("address", rndAddr, update = true))))
        }
      })
      .toMultiMap
  }
}
