package ru.yandex.vertis.ydb.syntax

import com.yandex.ydb.table.values._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.ydb.syntax.PreparedStatement._

import scala.jdk.CollectionConverters._

class PreparedStatementSpec extends AnyWordSpec with Matchers {

  "PreparedStatement" should {
    "PreparedStatement0" in {
      val prep = prepare(yql"SELECT 0;")
      val orig = "SELECT 0;"

      prep.queryString shouldBe orig
      prep.queryParams.values().asScala.toMap shouldBe Map.empty[String, Value[_]]
    }
    "PreparedStatement1" in {
      val prep = prepare((one: YInt32) => yql"SELECT $one;")
      val res = prep(1)
      val orig =
        s"""DECLARE $$_1 AS Int32;
           |SELECT $$_1;""".stripMargin

      res.queryString shouldBe orig
      res.queryParams.values().asScala.toMap shouldBe Map("$_1" -> PrimitiveValue.int32(1))
    }
    "PreparedStatement2" in {
      val arg0: Int = 1
      val arg1: String = "key"
      val prep = prepare { (one: YInt32, two: YUtf8) =>
        yql"SELECT $one WHERE pk = $two;"
      }
      val res = prep(arg0, arg1)
      val orig =
        s"""DECLARE $$_1 AS Int32;
           |DECLARE $$_2 AS Utf8;
           |SELECT $$_1 WHERE pk = $$_2;""".stripMargin

      res.queryString shouldBe orig
      res.queryParams.values().asScala.toMap shouldBe Map(
        "$_1" -> PrimitiveValue.int32(arg0),
        "$_2" -> PrimitiveValue.utf8(arg1)
      )
    }
    "PreparedStatement2 repeated" in {
      val arg0: Int = 1
      val arg1: String = "key"
      val prep = prepare { (one: YInt32, two: YUtf8) =>
        yql"SELECT $one, $two WHERE pk = $two;"
      }
      val res = prep(arg0, arg1)
      val orig =
        s"""DECLARE $$_1 AS Int32;
           |DECLARE $$_2 AS Utf8;
           |SELECT $$_1, $$_2 WHERE pk = $$_2;""".stripMargin

      res.queryString shouldBe orig
      res.queryParams.values().asScala.toMap shouldBe Map(
        "$_1" -> PrimitiveValue.int32(arg0),
        "$_2" -> PrimitiveValue.utf8(arg1)
      )
    }
    "primitive" in {
      val prep = prepare((one: Arg[Int]) => yql"SELECT $one;")
      val res = prep(1)
      val orig =
        s"""DECLARE $$_1 AS Int32;
           |SELECT $$_1;""".stripMargin

      res.queryString shouldBe orig
      res.queryParams.values().asScala.toMap shouldBe Map("$_1" -> PrimitiveValue.int32(1))
    }
    "unsigned" in {
      val prep = prepare((one: YUint64) => yql"SELECT $one;")
      val res = prep(1)
      val orig =
        s"""DECLARE $$_1 AS Uint64;
           |SELECT $$_1;""".stripMargin

      res.queryString shouldBe orig
      res.queryParams.values().asScala.toMap shouldBe Map("$_1" -> PrimitiveValue.uint64(1))
    }
    "option" in {
      val prep = prepare((one: Arg[Option[String]]) => yql"SELECT $one;")
      val res = prep(Some("123"))
      val orig =
        s"""DECLARE $$_1 AS Utf8?;
           |SELECT $$_1;""".stripMargin

      res.queryString shouldBe orig
      res.queryParams.values().asScala.toMap shouldBe Map("$_1" -> PrimitiveValue.utf8("123").makeOptional())
    }
    "list" in {
      val prep = prepare((one: Arg[List[String]]) => yql"SELECT $one;")
      val res = prep(List("123"))
      val orig =
        s"""DECLARE $$_1 AS List<Utf8>;
           |SELECT $$_1;""".stripMargin

      res.queryString shouldBe orig
      res.queryParams.values().asScala.toMap shouldBe Map("$_1" -> ListValue.of(PrimitiveValue.utf8("123")))
    }
    "tuples" in {
      val prep = prepare((one: Arg[(String, Int, Boolean)]) => yql"SELECT $one;")
      val res = prep(("123", 2, false))
      val orig =
        s"""DECLARE $$_1 AS Tuple<Utf8, Int32, Bool>;
           |SELECT $$_1;""".stripMargin

      res.queryString shouldBe orig
      res.queryParams.values().asScala.toMap shouldBe Map(
        "$_1" -> TupleValue.of(
          PrimitiveValue.utf8("123"),
          PrimitiveValue.int32(2),
          PrimitiveValue.bool(false)
        )
      )
    }
  }
}
