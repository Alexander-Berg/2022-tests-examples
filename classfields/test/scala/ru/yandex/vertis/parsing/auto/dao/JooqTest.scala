package ru.yandex.vertis.parsing.auto.dao

import org.jooq.impl.DSL
import org.jooq.types.ULong
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.enums.TTestStatus
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.tables._
import ru.yandex.vertis.parsing.auto.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced

/**
  * Created by andrey on 11/8/17.
  */
@RunWith(classOf[JUnitRunner])
class JooqTest extends FunSuite with InitTestDbs {
  initDb()

  private val shard = components.parsingShard
  private val t1: TTest = TTest.T_TEST
  private val t2 = TTest2.T_TEST2
  private val t3 = TTest3.T_TEST3

  private def master = shard.master.jooq

  private def slave = shard.slave.jooq

  implicit private val trace: Traced = TracedUtils.empty

  test("jooq insert select update delete") {
    // вставка
    master.insert("")(
      _.insertInto(t1, t1.STATUS)
        .values(TTestStatus.Status1)
        .values(TTestStatus.Status2)
    )

    // чтение
    {
      val rows = master.query("")(_.select(t1.ID, t1.STATUS).from(t1))
      assert(rows.lengthCompare(2) == 0)
      assert(rows.head.getValue(t1.ID).longValue() == 1L)
      assert(rows.head.getValue(t1.STATUS) == TTestStatus.Status1)
      assert(rows(1).getValue(t1.ID).longValue() == 2L)
      assert(rows(1).getValue(t1.STATUS) == TTestStatus.Status2)
    }

    // обновление
    master.update("")(_.update(t1).set(t1.STATUS, TTestStatus.Status3).where(t1.ID.equal(ULong.valueOf(1))))

    {
      val rows = master.query("")(_.select(t1.ID, t1.STATUS).from(t1))
      assert(rows.lengthCompare(2) == 0)
      assert(rows.head.getValue(t1.ID).longValue() == 1L)
      assert(rows.head.getValue(t1.STATUS) == TTestStatus.Status3)
      assert(rows(1).getValue(t1.ID).longValue() == 2L)
      assert(rows(1).getValue(t1.STATUS) == TTestStatus.Status2)
    }

    // удаление
    master.delete("")(_.deleteFrom(t1).where(t1.ID.equal(ULong.valueOf(2))))

    {
      val rows = master.query("")(_.select(t1.ID, t1.STATUS).from(t1))
      assert(rows.lengthCompare(1) == 0)
      assert(rows.head.getValue(t1.ID).longValue() == 1L)
      assert(rows.head.getValue(t1.STATUS) == TTestStatus.Status3)
    }

    master.delete("")(_.deleteFrom(t1))

    {
      val rows = master.query("")(_.select(t1.ID, t1.STATUS).from(t1))
      assert(rows.isEmpty)
    }
  }

  test("jooq rollback") {
    master.insert("")(_.insertInto(t1, t1.STATUS).values(TTestStatus.Status1))

    try {
      shard.master.withTransaction {
        master.insert("")(
          _.insertInto(t1, t1.STATUS)
            .values(TTestStatus.Status2)
            .values(TTestStatus.Status3)
        )
        sys.error("error")
      }
    } catch {
      case _: Throwable => // ignore
    }

    {
      val rows = master.query("")(_.select(t1.ID, t1.STATUS).from(t1))
      assert(rows.lengthCompare(1) == 0)
      assert(rows.head.getValue(t1.STATUS) == TTestStatus.Status1)
    }
  }

  test("nested transactions") {
    master.batch("")(dsl => Seq(dsl.truncate(t1)))
    master.batch("")(dsl => Seq(dsl.truncate(t3)))

    master.insert("")(_.insertInto(t1, t1.STATUS).values(TTestStatus.Status1))

    shard.master.withTransactionReadCommitted {
      shard.master.withTransactionReadCommitted {
        shard.master.withTransactionReadCommitted {
          master.update("")(dsl => dsl.update(t1).set(t1.STATUS, TTestStatus.Status2).where(t1.ID.eq(ULong.valueOf(1))))
        }
        master.insert("")(_.insertInto(t3, t3.HASH, t3.DATA, t3.VERSION).values("hash2", "data2", Long.box(2)))
      }
      val t1Data = master.query("")(_.selectFrom(t1))
      assert(t1Data.length == 1)
      assert(t1Data.head.component1.longValue() == 1L)
      assert(t1Data.head.component2 == TTestStatus.Status2)

      val t2Data = master.query("")(_.selectFrom(t3))
      assert(t2Data.length == 1)
      assert(t2Data.head.component1.longValue() == 1L)
      assert(t2Data.head.component2 == "hash2")
      assert(t2Data.head.component3 == "data2")
      assert(t2Data.head.component4 == 2L)
    }

    master.batch("")(dsl => Seq(dsl.truncate(t1)))
    master.batch("")(dsl => Seq(dsl.truncate(t3)))
  }

  test("option[string] and mediumblob") {
    master.insert("")(
      _.insertInto(t2, t2.DATA, t2.OFFER_ID)
        .values(Array[Byte](65, 66, 67, 68, 69), "offerId1".getBytes)
        .values(Array[Byte](70, 71, 72, 73, 74), null)
    )

    val rows = master.query("")(_.select(t2.ID, t2.OFFER_ID, t2.DATA).from(t2))
    assert(rows.lengthCompare(2) == 0)
    println(rows.head.getValue(t2.DATA).asInstanceOf[Array[Byte]].mkString(", "))
    println(rows.head.getValue(t2.DATA).getClass)
    assert(rows.head.getValue(t2.DATA).asInstanceOf[Array[Byte]].sameElements("ABCDE".getBytes))
    assert(new String(rows.head.getValue(t2.OFFER_ID).asInstanceOf[Array[Byte]]) == "offerId1")
    assert(rows(1).getValue(t2.DATA).asInstanceOf[Array[Byte]].sameElements("FGHIJ".getBytes))
    assert(rows(1).getValue(t2.OFFER_ID).asInstanceOf[Array[Byte]] == null)
  }

  test("insert ignore, batchInsert etc") {
    master.insert("")(_.insertInto(t3, t3.HASH, t3.DATA, t3.VERSION).values("hash1", "data1", Long.box(1)))

    val result = master.batch("")(dsl =>
      Seq(
        dsl
          .insertInto(t3, t3.HASH, t3.DATA, t3.VERSION)
          .values("hash1", "data1_2", Long.box(1))
          .onDuplicateKeyIgnore(),
        dsl.insertInto(t3, t3.HASH, t3.DATA, t3.VERSION).values("hash2", "data2", Long.box(1)).onDuplicateKeyIgnore(),
        dsl.insertInto(t3, t3.HASH, t3.DATA, t3.VERSION).values("hash3", "data3", Long.box(1)).onDuplicateKeyIgnore()
      )
    )

    assert(result.sameElements(Array(0, 1, 1)))
  }

  test("update with same data") {
    master.insert("")(_.insertInto(t3, t3.HASH, t3.DATA, t3.VERSION).values("hash4", "data4", Long.box(1)))

    val result1: Int = master.update("")(_.update(t3).set(t3.DATA, "data4").where(t3.HASH.equal("hash4")))
    assert(result1 == 1)

    val result2: Int = master.update("")(_.update(t3).set(t3.DATA, "data5").where(t3.HASH.equal("hash5")))
    assert(result2 == 0)
  }

  test("multi-batch, count") {
    master.batch("")(dsl =>
      Seq(
        dsl.deleteFrom(t3),
        dsl.insertInto(t3, t3.HASH, t3.DATA, t3.VERSION).values("hash1", "data1", Long.box(1)),
        dsl.insertInto(t3, t3.HASH, t3.DATA, t3.VERSION).values("hash2", "data2", Long.box(1)),
        dsl.insertInto(t3, t3.HASH, t3.DATA, t3.VERSION).values("hash3", "data3", Long.box(1)),
        dsl.update(t3).set(t3.DATA, "data3_2").where(t3.HASH.equal("hash3"))
      )
    )

    val data3 =
      master.query("")(_.select(t3.DATA).from(t3).where(t3.HASH.equal("hash3"))).map(_.getValue(0)).head.toString
    assert(data3 == "data3_2")

    val count: Long = master.count("")(_.selectCount().from(t3))
    assert(count == 3)

    val count2 = master.query("")(_.select(DSL.count(t3.ID)).from(t3)).headOption.map(x => x.getValue(0)).getOrElse(0)
    assert(count2 == 3)
  }
}
