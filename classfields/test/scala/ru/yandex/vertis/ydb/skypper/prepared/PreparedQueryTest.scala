package ru.yandex.vertis.ydb.skypper.prepared

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.ydb.skypper.prepared.YdbInterpolatorUtils._

import scala.util.hashing.MurmurHash3
import ru.yandex.vertis.ydb.skypper.InitTestYdb
import ru.yandex.vertis.ydb.skypper.YdbReads
import ru.yandex.vertis.tracing.Traced

@RunWith(classOf[JUnitRunner])
class PreparedQueryTest extends AnyFunSuite with Matchers with InitTestYdb {
  implicit private val trace: Traced = Traced.empty

  test("upsert seq with empty option as null") {

    val values = (1 to 5).map { idx =>
      (idx, None, "test")
    }

    val prepQuery = ydb"upsert into table (a,b,c) values $values"
    assert(
      prepQuery.query ==
        """DECLARE $a AS Uint32;
          |DECLARE $b AS Utf8;
          |DECLARE $c AS Uint32;
          |DECLARE $d AS Utf8;
          |DECLARE $e AS Uint32;
          |DECLARE $f AS Utf8;
          |DECLARE $g AS Uint32;
          |DECLARE $h AS Utf8;
          |DECLARE $i AS Uint32;
          |DECLARE $j AS Utf8;
          |upsert into table (a,b,c) values ($a,null,$b),($c,null,$d),($e,null,$f),($g,null,$h),($i,null,$j)""".stripMargin
    )
  }
  test("upsert seq with non empty option ") {

    val values = (1 to 5).map { idx =>
      (idx, Some(1), "test")
    }

    val prepQuery = ydb"upsert into table (a,b,c) values $values"
    assert(
      prepQuery.query ==
        """DECLARE $a AS Uint32;
          |DECLARE $b AS Uint32;
          |DECLARE $c AS Utf8;
          |DECLARE $d AS Uint32;
          |DECLARE $e AS Uint32;
          |DECLARE $f AS Utf8;
          |DECLARE $g AS Uint32;
          |DECLARE $h AS Uint32;
          |DECLARE $i AS Utf8;
          |DECLARE $j AS Uint32;
          |DECLARE $k AS Uint32;
          |DECLARE $l AS Utf8;
          |DECLARE $m AS Uint32;
          |DECLARE $n AS Uint32;
          |DECLARE $o AS Utf8;
          |upsert into table (a,b,c) values ($a,$b,$c),($d,$e,$f),($g,$h,$i),($j,$k,$l),($m,$n,$o)""".stripMargin
    )
  }

  test("upsert with empty option as null") {
    val a = 1
    val b = None
    val c = "test"
    val prepQuery = ydb"upsert into table (a,b,c) values ($a,$b,$c)"
    assert(
      prepQuery.query ==
        """DECLARE $a AS Uint32;
          |DECLARE $b AS Utf8;
          |upsert into table (a,b,c) values ($a,null,$b)""".stripMargin
    )
  }

  test("upsert with Some(1)") {
    val a = 1
    val b = Some(1)
    val c = "test"
    val prepQuery = ydb"upsert into table (a,b,c) values ($a,$b,$c)"
    assert(
      prepQuery.query ==
        """DECLARE $a AS Uint32;
          |DECLARE $b AS Uint32;
          |DECLARE $c AS Utf8;
          |upsert into table (a,b,c) values ($a,$b,$c)""".stripMargin
    )
  }

  test("upsert concatenation") {
    val testShardId = 0
    val testOfferId = "123test"
    val workersSeq = Seq(
      "worker1",
      "worker2",
      "worker3",
      "worker1",
      "worker2",
      "worker3",
      "worker1",
      "worker2",
      "worker3",
      "worker1",
      "worker2",
      "worker3",
      "worker1",
      "worker2",
      "worker3",
      "worker1",
      "worker2",
      "worker3",
      "worker1",
      "worker2",
      "worker3",
      "worker1",
      "worker2",
      "worker3",
      "worker1",
      "worker2",
      "worker3"
    )
    val values = workersSeq.map(worker => (testShardId, worker, new DateTime(), testOfferId))
    val prepQuery = ydb"upsert into workers_queue (shard_id,worker, next_check, offer_id) values $values"
    assert(
      prepQuery.query ==
        """DECLARE $a AS Uint32;
          |DECLARE $b AS Utf8;
          |DECLARE $c AS Timestamp;
          |DECLARE $d AS Utf8;
          |DECLARE $e AS Uint32;
          |DECLARE $f AS Utf8;
          |DECLARE $g AS Timestamp;
          |DECLARE $h AS Utf8;
          |DECLARE $i AS Uint32;
          |DECLARE $j AS Utf8;
          |DECLARE $k AS Timestamp;
          |DECLARE $l AS Utf8;
          |DECLARE $m AS Uint32;
          |DECLARE $n AS Utf8;
          |DECLARE $o AS Timestamp;
          |DECLARE $p AS Utf8;
          |DECLARE $q AS Uint32;
          |DECLARE $r AS Utf8;
          |DECLARE $s AS Timestamp;
          |DECLARE $t AS Utf8;
          |DECLARE $u AS Uint32;
          |DECLARE $v AS Utf8;
          |DECLARE $w AS Timestamp;
          |DECLARE $x AS Utf8;
          |DECLARE $y AS Uint32;
          |DECLARE $z AS Utf8;
          |DECLARE $aa AS Timestamp;
          |DECLARE $ab AS Utf8;
          |DECLARE $ac AS Uint32;
          |DECLARE $ad AS Utf8;
          |DECLARE $ae AS Timestamp;
          |DECLARE $af AS Utf8;
          |DECLARE $ag AS Uint32;
          |DECLARE $ah AS Utf8;
          |DECLARE $ai AS Timestamp;
          |DECLARE $aj AS Utf8;
          |DECLARE $ak AS Uint32;
          |DECLARE $al AS Utf8;
          |DECLARE $am AS Timestamp;
          |DECLARE $an AS Utf8;
          |DECLARE $ao AS Uint32;
          |DECLARE $ap AS Utf8;
          |DECLARE $aq AS Timestamp;
          |DECLARE $ar AS Utf8;
          |DECLARE $as AS Uint32;
          |DECLARE $at AS Utf8;
          |DECLARE $au AS Timestamp;
          |DECLARE $av AS Utf8;
          |DECLARE $aw AS Uint32;
          |DECLARE $ax AS Utf8;
          |DECLARE $ay AS Timestamp;
          |DECLARE $az AS Utf8;
          |DECLARE $ba AS Uint32;
          |DECLARE $bb AS Utf8;
          |DECLARE $bc AS Timestamp;
          |DECLARE $bd AS Utf8;
          |DECLARE $be AS Uint32;
          |DECLARE $bf AS Utf8;
          |DECLARE $bg AS Timestamp;
          |DECLARE $bh AS Utf8;
          |DECLARE $bi AS Uint32;
          |DECLARE $bj AS Utf8;
          |DECLARE $bk AS Timestamp;
          |DECLARE $bl AS Utf8;
          |DECLARE $bm AS Uint32;
          |DECLARE $bn AS Utf8;
          |DECLARE $bo AS Timestamp;
          |DECLARE $bp AS Utf8;
          |DECLARE $bq AS Uint32;
          |DECLARE $br AS Utf8;
          |DECLARE $bs AS Timestamp;
          |DECLARE $bt AS Utf8;
          |DECLARE $bu AS Uint32;
          |DECLARE $bv AS Utf8;
          |DECLARE $bw AS Timestamp;
          |DECLARE $bx AS Utf8;
          |DECLARE $by AS Uint32;
          |DECLARE $bz AS Utf8;
          |DECLARE $ca AS Timestamp;
          |DECLARE $cb AS Utf8;
          |DECLARE $cc AS Uint32;
          |DECLARE $cd AS Utf8;
          |DECLARE $ce AS Timestamp;
          |DECLARE $cf AS Utf8;
          |DECLARE $cg AS Uint32;
          |DECLARE $ch AS Utf8;
          |DECLARE $ci AS Timestamp;
          |DECLARE $cj AS Utf8;
          |DECLARE $ck AS Uint32;
          |DECLARE $cl AS Utf8;
          |DECLARE $cm AS Timestamp;
          |DECLARE $cn AS Utf8;
          |DECLARE $co AS Uint32;
          |DECLARE $cp AS Utf8;
          |DECLARE $cq AS Timestamp;
          |DECLARE $cr AS Utf8;
          |DECLARE $cs AS Uint32;
          |DECLARE $ct AS Utf8;
          |DECLARE $cu AS Timestamp;
          |DECLARE $cv AS Utf8;
          |DECLARE $cw AS Uint32;
          |DECLARE $cx AS Utf8;
          |DECLARE $cy AS Timestamp;
          |DECLARE $cz AS Utf8;
          |DECLARE $da AS Uint32;
          |DECLARE $db AS Utf8;
          |DECLARE $dc AS Timestamp;
          |DECLARE $dd AS Utf8;
          |upsert into workers_queue (shard_id,worker, next_check, offer_id) values ($a,$b,$c,$d),($e,$f,$g,$h),($i,$j,$k,$l),($m,$n,$o,$p),($q,$r,$s,$t),($u,$v,$w,$x),($y,$z,$aa,$ab),($ac,$ad,$ae,$af),($ag,$ah,$ai,$aj),($ak,$al,$am,$an),($ao,$ap,$aq,$ar),($as,$at,$au,$av),($aw,$ax,$ay,$az),($ba,$bb,$bc,$bd),($be,$bf,$bg,$bh),($bi,$bj,$bk,$bl),($bm,$bn,$bo,$bp),($bq,$br,$bs,$bt),($bu,$bv,$bw,$bx),($by,$bz,$ca,$cb),($cc,$cd,$ce,$cf),($cg,$ch,$ci,$cj),($ck,$cl,$cm,$cn),($co,$cp,$cq,$cr),($cs,$ct,$cu,$cv),($cw,$cx,$cy,$cz),($da,$db,$dc,$dd)""".stripMargin
    )

  }

  test("concatenation") {
    val value1 = 5
    val value2 = "abc"
    val value3 = false
    val value4 = 5.5
    val value5 = new DateTime(2020, 11, 18, 0, 0, 0, 0)
    val prepQuery = ydb"update table set field1 = $value1, field2 = $value2, " +
      ydb"field3 = $value3, " +
      ydb"""|field4 = $value4,
            |field5 = $value5""".stripMargin
    assert(
      prepQuery.query ==
        """DECLARE $a AS Uint32;
          |DECLARE $b AS Utf8;
          |DECLARE $c AS Bool;
          |DECLARE $d AS Float64;
          |DECLARE $e AS Timestamp;
          |update table set field1 = $a, field2 = $b, field3 = $c, field4 = $d,
          |field5 = $e""".stripMargin
    )
  }
  test("no params") {
    val prepQuery = ydb"update table set field1 = 5, field2 = 'abc', " +
      ydb"field3 = false, " +
      ydb"""|field4 = 5.5,
            |field5 = 'pewpew'""".stripMargin
    assert(
      prepQuery.query ==
        """update table set field1 = 5, field2 = 'abc', field3 = false, field4 = 5.5,
          |field5 = 'pewpew'""".stripMargin
    )
  }

  test("concatenation with empty string") {
    val prepQuery = ydb"update table set field1 = 5, field2 = 'abc'" + ydb""
    assert(prepQuery.query == """update table set field1 = 5, field2 = 'abc'""".stripMargin)
  }

  test("concatenation without delimiter") {
    val value1 = 5
    val value2 = "abc"
    val value3 = false
    val prepQuery = ydb"update table set field1 = $value1, field2 = $value2" +
      ydb"field3 = $value3"
    assert(
      prepQuery.query ==
        """|DECLARE $a AS Uint32;
           |DECLARE $b AS Utf8;
           |DECLARE $c AS Bool;
           |update table set field1 = $a, field2 = $bfield3 = $c""".stripMargin
    )
  }

  test("concatenation without delimiter 2") {
    val value1 = 5
    val value2 = "abc"
    val value3 = false
    val prepQuery = ydb"update table set field1 = $value1, field2 = $value2" +
      ydb"$value3"
    assert(
      prepQuery.query ==
        """|DECLARE $a AS Uint32;
           |DECLARE $b AS Utf8;
           |DECLARE $c AS Bool;
           |update table set field1 = $a, field2 = $b$c""".stripMargin
    )
  }

  test("params sequence") {
    val value1 = 5
    val value2 = Seq(1, 2, 3)
    val value3 = "abc"
    val value4 = Seq("a", "b", "c")
    val value5 = Seq(true, false)
    val value6 = false

    val prepQuery =
      ydb"select * from table where field1 = $value1 and field2 in $value2, and field3 = $value3 " +
        ydb"and field4 in $value4 " +
        ydb"and field5 in $value5 and field6 = $value6"

    assert(
      prepQuery.query ==
        """|DECLARE $a AS Uint32;
           |DECLARE $b AS Uint32;
           |DECLARE $c AS Uint32;
           |DECLARE $d AS Uint32;
           |DECLARE $e AS Utf8;
           |DECLARE $f AS Utf8;
           |DECLARE $g AS Utf8;
           |DECLARE $h AS Utf8;
           |DECLARE $i AS Bool;
           |DECLARE $j AS Bool;
           |DECLARE $k AS Bool;
           |select * from table where field1 = $a and field2 in ($b, $c, $d), and field3 = $e and field4 in ($f, $g, $h) and field5 in ($i, $j) and field6 = $k""".stripMargin
    )
  }

  test("tuple with int,str") {
    def toHash(str: String) = MurmurHash3.stringHash(str)
    val tupleList =
      Seq("1085051302-6908fb90", "1088275654-387296d6", "1092772104-c4b4d4ae").map(res => (toHash(res), res))
    val prepQuery =
      ydb"select * from table where (a,b) in $tupleList"
    assert(
      prepQuery.query ==
        """|DECLARE $a AS List<Tuple<Uint32, Utf8>>;
           |select * from table where (a,b) in $a""".stripMargin
    )
  }

  test("tuple with str,str") {
    val tupleList =
      Seq("1085051302-6908fb90", "1088275654-387296d6", "1092772104-c4b4d4ae").map(res => (res, res))
    val prepQuery =
      ydb"select * from table where (a,b) in $tupleList"
    assert(
      prepQuery.query ==
        """|DECLARE $a AS List<Tuple<Utf8, Utf8>>;
           |select * from table where (a,b) in $a""".stripMargin
    )
  }

  test("tuple with str,str and str") {
    val testStr = "1a"
    val tupleList =
      Seq("1085051302-6908fb90", "1088275654-387296d6", "1092772104-c4b4d4ae").map(res => (res, res))
    val prepQuery =
      ydb"select * from table where (a,b) in $tupleList and next=$testStr"
    assert(
      prepQuery.query ==
        """|DECLARE $a AS List<Tuple<Utf8, Utf8>>;
           |DECLARE $b AS Utf8;
           |select * from table where (a,b) in $a and next=$b""".stripMargin
    )
    assert(prepQuery.params.toPb.get("$b").getValue.getTextValue == testStr)
  }

  test("interpolate sequence of primitives") {
    val in = 0.to(9).toSeq

    implicit val reads: YdbReads[Seq[Long]] = YdbReads { rs =>
      val r = rs.getColumn(0)
      0.until(r.getTupleElementsCount).map(r.getTupleElement(_).getUint32())
    }

    val prepQuery = ydb"select $in"
    assert(
      prepQuery.query ==
        """|DECLARE $a AS Uint32;
           |DECLARE $b AS Uint32;
           |DECLARE $c AS Uint32;
           |DECLARE $d AS Uint32;
           |DECLARE $e AS Uint32;
           |DECLARE $f AS Uint32;
           |DECLARE $g AS Uint32;
           |DECLARE $h AS Uint32;
           |DECLARE $i AS Uint32;
           |DECLARE $j AS Uint32;
           |select ($a, $b, $c, $d, $e, $f, $g, $h, $i, $j)""".stripMargin
    )
    val result = ydb.queryPrepared[Seq[Long]]("query")(prepQuery).toSeq.head
    result shouldBe in
  }

  test("AUTORUBACK-3419") {
    ydb.update("upsert")("""|upsert into AUTORUBACK_3419 (key, value)
         |values
         |('a', 1),
         |('b', 2),
         |('c', 3),
         |('d', 4)""".stripMargin)

    val values = Seq("a", "b", "c")
    val prepQuery = ydb"""SELECT * FROM AUTORUBACK_3419 WHERE key IN $values"""

    assert(
      prepQuery.query ==
        """|DECLARE $a AS Utf8;
           |DECLARE $b AS Utf8;
           |DECLARE $c AS Utf8;
           |SELECT * FROM AUTORUBACK_3419 WHERE key IN ($a, $b, $c)""".stripMargin
    )
    assert(prepQuery.params.toPb.get("$a").getValue.getTextValue == "a")
    assert(prepQuery.params.toPb.get("$b").getValue.getTextValue == "b")
    assert(prepQuery.params.toPb.get("$c").getValue.getTextValue == "c")

    implicit val reads: YdbReads[(String, Int)] = YdbReads { rs =>
      (rs.getColumn("key").getUtf8, rs.getColumn("value").getInt32)
    }

    val result = ydb.queryPrepared[(String, Int)]("query")(prepQuery).toSet
    result shouldBe Set(("a", 1), ("b", 2), ("c", 3))
  }
}
