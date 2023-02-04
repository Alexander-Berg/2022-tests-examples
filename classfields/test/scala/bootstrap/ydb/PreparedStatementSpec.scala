package bootstrap.ydb

import bootstrap.ydb.syntax.*
import com.yandex.ydb.table.values.*
import strict.{Int32, Uint32, Utf8}
import zio.test.*

import scala.jdk.CollectionConverters.MapHasAsScala

case object PreparedStatementSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment, Any] =
    suite("PreparedStatement")(
      test("PreparedStatement0") {
        val prep = declare(txRoOnline)(yql"SELECT 0;")
        val orig =
          """--!syntax_v1
            |SELECT 0;""".stripMargin

        assertTrue(prep.queryString == orig)
        assertTrue(
          prep.queryParams.values().asScala.toMap == Map.empty[String, Value[?]],
        )
      },
      test("PreparedStatement1 case class") {
        case class Dummy(string: Utf8, i: Int32)

        val prep =
          declare(txRoOnline) { (one: "label1" as Dummy) =>
            yql"SELECT $one;"
          }
        val res = prep(Dummy(Utf8(""), Int32(2)))
        val orig =
          s"""--!syntax_v1
             |DECLARE $$label1 AS Struct<'i': Int32, 'string': Utf8>;
             |SELECT $$label1;""".stripMargin

        assertTrue(res.queryString == orig) &&
        assertTrue(
          res.queryParams.values().asScala.toMap ==
            Map(
              "$label1" ->
                StructValue.of(
                  "string",
                  PrimitiveValue.utf8(""),
                  "i",
                  PrimitiveValue.int32(2),
                ),
            ),
        )
      },
      test("PreparedStatement1 Iterable[case class]") {
        case class Dummy(string: Utf8, i: Int32)

        val prep =
          declare(txRoOnline) { (one: "labels" as Iterable[Dummy]) =>
            yql"SELECT $one;"
          }
        val res = prep(
          List(Dummy(Utf8("1"), Int32(1)), Dummy(Utf8("2"), Int32(2))),
        )
        val orig =
          s"""--!syntax_v1
             |DECLARE $$labels AS List<Struct<'i': Int32, 'string': Utf8>>;
             |SELECT $$labels;""".stripMargin

        assertTrue(res.queryString == orig) &&
        assertTrue(
          res.queryParams.values().asScala.toMap ==
            Map(
              "$labels" ->
                ListValue.of(
                  StructValue.of(
                    "string",
                    PrimitiveValue.utf8("1"),
                    "i",
                    PrimitiveValue.int32(1),
                  ),
                  StructValue.of(
                    "string",
                    PrimitiveValue.utf8("2"),
                    "i",
                    PrimitiveValue.int32(2),
                  ),
                ),
            ),
        )
      },
      test("PreparedStatement1 tuple3") {
        val prep =
          declare(txRoOnline) { (one: "$label1" as (Utf8, Int32, Uint32)) =>
            yql"SELECT $one;"
          }
        val res = prep((Utf8(""), Int32(2), Uint32(4)))
        val orig =
          s"""--!syntax_v1
             |DECLARE $$label1 AS Tuple<Utf8, Int32, Uint32>;
             |SELECT $$label1;""".stripMargin

        assertTrue(res.queryString == orig) &&
        assertTrue(
          res.queryParams.values().asScala.toMap ==
            Map(
              "$label1" ->
                TupleValue.of(
                  PrimitiveValue.utf8(""),
                  PrimitiveValue.int32(2),
                  PrimitiveValue.uint32(4),
                ),
            ),
        )
      },
      test("PreparedStatement2") {
        val arg0: Int    = 1
        val arg1: String = "key"
        val prep =
          declare(txRoOnline) { (one: "_1" as Int32, two: "_2" as Utf8) =>
            yql"SELECT $one WHERE pk = $two;"
          }
        val res = prep(arg0, Utf8(arg1))
        val orig =
          s"""--!syntax_v1
             |DECLARE $$_1 AS Int32;
             |DECLARE $$_2 AS Utf8;
             |SELECT $$_1 WHERE pk = $$_2;""".stripMargin

        assertTrue(res.queryString == orig) &&
        assertTrue(
          res.queryParams.values().asScala.toMap ==
            Map(
              "$_1" -> PrimitiveValue.int32(arg0),
              "$_2" -> PrimitiveValue.utf8(arg1),
            ),
        )
      },
      test("PreparedStatement2 repeated") {
        val arg0: Int  = 1
        val arg1: Utf8 = "key"
        val prep =
          declare(txRoOnline) { (one: "_1" as Int32, two: "_2" as Utf8) =>
            yql"SELECT $one, $two WHERE pk = $two;"
          }
        val res = prep(arg0, arg1)
        val orig =
          s"""--!syntax_v1
             |DECLARE $$_1 AS Int32;
             |DECLARE $$_2 AS Utf8;
             |SELECT $$_1, $$_2 WHERE pk = $$_2;""".stripMargin

        assertTrue(res.queryString == orig) &&
        assertTrue(
          res.queryParams.values().asScala.toMap ==
            Map(
              "$_1" -> PrimitiveValue.int32(arg0),
              "$_2" -> PrimitiveValue.utf8(arg1.value),
            ),
        )
      },
    )

}
