package ru.yandex.vertis.ydb.skypper.syntax

import com.yandex.ydb.table.values.ListValue
import com.yandex.ydb.table.values.PrimitiveValue
import com.yandex.ydb.table.values.Value
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.InitTestYdb
import ru.yandex.vertis.ydb.skypper.YdbReads
import ru.yandex.vertis.ydb.skypper.fragment.Fragment
import ru.yandex.vertis.ydb.skypper.fragment.Put
import ru.yandex.vertis.ydb.skypper.fragment.Put.uint32

import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class SyntaxTest extends AnyFunSuite with InitTestYdb {

  implicit private val trace: Traced = Traced.empty

  implicit private val YdbReadsValues: YdbReads[Seq[Value[_]]] = YdbReads { rs =>
    0.until(rs.getColumnCount).map(rs.getColumn(_).getValue)
  }

  test("No parameters") {
    checkSelect(ydb"SELECT 123", "SELECT 123", Map.empty, Seq(Seq(PrimitiveValue.int32(123))))
  }

  test("One parameter") {
    val x = 1
    checkSelect(
      ydb"SELECT $x",
      """|DECLARE $a AS Int32;
         |SELECT $a""".stripMargin,
      Map("$a" -> PrimitiveValue.int32(x)),
      Seq(Seq(PrimitiveValue.int32(x)))
    )
  }

  test("Multiple parameters") {
    val x = 1
    val y = ""
    checkSelect(
      ydb"SELECT $x, $y",
      """|DECLARE $a AS Int32;
         |DECLARE $b AS Utf8;
         |SELECT $a, $b""".stripMargin,
      Map("$a" -> PrimitiveValue.int32(x), "$b" -> PrimitiveValue.utf8(y)),
      Seq(Seq(PrimitiveValue.int32(x), PrimitiveValue.utf8(y)))
    )
  }

  test("Explicit type") {
    val x = 1
    checkSelect(
      ydb"SELECT ${x.as(uint32)}",
      """|DECLARE $a AS Uint32;
           |SELECT $a""".stripMargin,
      Map("$a" -> PrimitiveValue.uint32(x)),
      Seq(Seq(PrimitiveValue.uint32(x)))
    )
  }

  test("Multiple parameters with concatenation") {
    val x = 1
    val y = ""
    checkSelect(
      ydb"SELECT $x, " ++ ydb"$y",
      """|DECLARE $a AS Int32;
         |DECLARE $b AS Utf8;
         |SELECT $a, $b""".stripMargin,
      Map("$a" -> PrimitiveValue.int32(x), "$b" -> PrimitiveValue.utf8(y)),
      Seq(Seq(PrimitiveValue.int32(x), PrimitiveValue.utf8(y)))
    )
  }

  test("Multiple parameters with fragment interpolation") {
    val x = 1
    val y = ""
    val fx = ydb"$x"
    val fy = ydb"$y"
    checkSelect(
      ydb"SELECT $fx, $fy",
      """|DECLARE $a AS Int32;
         |DECLARE $b AS Utf8;
         |SELECT $a, $b""".stripMargin,
      Map("$a" -> PrimitiveValue.int32(x), "$b" -> PrimitiveValue.utf8(y)),
      Seq(Seq(PrimitiveValue.int32(x), PrimitiveValue.utf8(y)))
    )
  }

  test("'IN' expression") {
    val keys = Seq(1)
    checkSelect(
      ydb"""|SELECT value
            |FROM (VALUES (1, 'foo'u), (2, 'bar'u)) AS t(key, value)
            |WHERE key IN $keys""".stripMargin,
      """|DECLARE $a AS List<Int32>;
         |SELECT value
         |FROM (VALUES (1, 'foo'u), (2, 'bar'u)) AS t(key, value)
         |WHERE key IN $a""".stripMargin,
      Map("$a" -> ListValue.of(PrimitiveValue.int32(1))),
      Seq(Seq(PrimitiveValue.utf8("foo")))
    )
  }

  test("'VALUES' statement (1 item, 1 column)") {
    val items = Seq(1)
    checkSelect(
      Fragment.values(items),
      """|DECLARE $a AS Int32;
         |VALUES ($a)""".stripMargin,
      Map("$a" -> PrimitiveValue.int32(1)),
      Seq(Seq(PrimitiveValue.int32(1)))
    )
  }

  test("'VALUES' statement (2 items, 2 columns)") {
    val items = Seq((1, "foo"))
    checkSelect(
      Fragment.values(items),
      """|DECLARE $a AS Int32;
         |DECLARE $b AS Utf8;
         |VALUES ($a, $b)""".stripMargin,
      Map("$a" -> PrimitiveValue.int32(1), "$b" -> PrimitiveValue.utf8("foo")),
      Seq(Seq(PrimitiveValue.int32(1), PrimitiveValue.utf8("foo")))
    )
  }

  test("'VALUES' statement (explicit type)") {
    val items = Seq(1.as(uint32))
    checkSelect(
      Fragment.values(items),
      """|DECLARE $a AS Uint32;
         |VALUES ($a)""".stripMargin,
      Map("$a" -> PrimitiveValue.uint32(1)),
      Seq(Seq(PrimitiveValue.uint32(1)))
    )
  }

  test("Explicit SQL type") {
    import Put.uint32
    val x: Int = 1
    checkSelect(
      ydb"SELECT ${x.as(uint32)}",
      """|DECLARE $a AS Uint32;
         |SELECT $a""".stripMargin,
      Map("$a" -> PrimitiveValue.uint32(x)),
      Seq(Seq(PrimitiveValue.uint32(x)))
    )
  }

  private def checkSelect(fragment: Fragment,
                          expectedQuery: String,
                          expectedParams: Map[String, Value[_]],
                          expectedResults: Seq[Seq[Value[_]]]) = {
    val (query, params) = fragment.buildQuery()
    assert(query == expectedQuery)
    assert(params.values.asScala == expectedParams)
    val results = ydb.query[Seq[Value[_]]]("example")(fragment).toList
    assert(results == expectedResults)
  }
}
