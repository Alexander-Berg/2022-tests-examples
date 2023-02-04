package vs.parser

import vsql.query.Value.Pointer
import zio.test.Assertion.*
import zio.test.*

object ElementaryParserSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment, Any] = {
    suite("ElementaryParserSpec")(
      suite("float")(
        test("float") {
          assertTrue(
            fastparse
              .parse("0.7 + 0.8", DeclarationParser.arithmetics(_))
              .get
              .value == "0.7+0.8",
          )
        },
      ),
      suite("time")(
        test("time") {
          assertTrue(
            fastparse
              .parse("'2021-11-13 14:01:15'", ElementaryParser.timestamp(_))
              .get
              .value
              .toString == "2021-11-13T14:01:15Z",
          )
        },
      ),
      suite("string")(
        test("russian string") {
          assertTrue(
            fastparse
              .parse(
                """"Шекспир \"Cон в летнюю ночь\""""",
                DeclarationParser.arithmetics(_),
              )
              .get
              .value == "Шекспир \"Cон в летнюю ночь\"",
          )
        },
      ),
      suite("pointer")(
        test("valid") {
          assertTrue(
            fastparse.parse("$abc", ElementaryParser.pointer(_)).get.value ==
              Pointer("abc"),
          )
        },
        test("no $") {
          assert(fastparse.parse("abc", ElementaryParser.pointer(_)).isSuccess)(
            equalTo(false),
          )
        },
      ),
      suite("null")(
        test("valid") {
          assertTrue(
            fastparse.parse("null", ElementaryParser.nullConst(_)).isSuccess,
          )
        },
      ),
    )
  }

}
