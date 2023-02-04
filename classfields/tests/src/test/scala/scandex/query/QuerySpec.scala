package scandex.query

import scandex.schema.field.RangeFieldPrototype
import zio.test._
import zio.test.Assertion.{nothing => _, _}
import zio.test.ZIOSpecDefault

object QuerySpec extends ZIOSpecDefault {

  case object TestField extends RangeFieldPrototype[Int]

  val simpleEqualsQuery = TestField === 0
  val simpleGTQuery     = TestField > 0
  val simpleGTEQuery    = TestField >= 0
  val simpleLTQuery     = TestField < 0
  val simpleLTEQuery    = TestField <= 0
  val simpleNullQuery   = TestField.isNull

  def spec =
    suite("Queries")(
      suite("Logical optimizations")(
        test(
          s"$simpleEqualsQuery AND $everything should be $simpleEqualsQuery",
        ) {
          assert(simpleEqualsQuery & everything)(equalTo(simpleEqualsQuery))
        },
        test(s"$simpleEqualsQuery AND $nothing should be $nothing") {
          assert(simpleEqualsQuery & nothing)(equalTo(nothing))
        },
        test(s"$simpleEqualsQuery OR $nothing should be $simpleEqualsQuery") {
          assert(simpleEqualsQuery | nothing)(equalTo(simpleEqualsQuery))
        },
        test(s"$simpleEqualsQuery OR $everything should be $everything") {
          assert(simpleEqualsQuery | everything)(equalTo(everything))
        },
        test(s"$simpleEqualsQuery AND $empty should be $simpleEqualsQuery") {
          assert(simpleEqualsQuery & empty)(equalTo(simpleEqualsQuery))
        },
        test(s"$empty OR $simpleEqualsQuery should be $simpleEqualsQuery") {
          assert(simpleEqualsQuery | empty)(equalTo(simpleEqualsQuery))
        },
        test(s"NOT (${!simpleEqualsQuery}) should be $simpleEqualsQuery") {
          assert(!(!simpleEqualsQuery))(equalTo(simpleEqualsQuery))
        },
        test(
          s"$simpleEqualsQuery OR ${!simpleEqualsQuery} should be $everything",
        ) {
          assert(simpleEqualsQuery | !simpleEqualsQuery)(equalTo(everything))
        },
        test(
          s"$simpleEqualsQuery AND ${!simpleEqualsQuery} should be $nothing",
        ) {
          assert(simpleEqualsQuery & !simpleEqualsQuery)(equalTo(nothing))
        },
        test(s"NOT ($empty) should be $empty") {
          assert(!empty)(equalTo(empty))
        },
        test(s"$empty AND $simpleEqualsQuery should be $simpleEqualsQuery") {
          assert(simpleEqualsQuery & empty)(equalTo(simpleEqualsQuery))
        },
        test(s"NOT ($simpleGTQuery) should be $simpleLTEQuery") {
          assert(!simpleGTQuery)(equalTo(simpleLTEQuery))
        },
        test(s"NOT ($simpleGTEQuery) should be $simpleLTQuery") {
          assert(!simpleGTEQuery)(equalTo(simpleLTQuery))
        },
        test(s"NOT ($simpleLTQuery) should be $simpleGTEQuery") {
          assert(!simpleLTQuery)(equalTo(simpleGTEQuery))
        },
        test(s"NOT ($simpleLTEQuery) should be $simpleGTQuery") {
          assert(!simpleLTEQuery)(equalTo(simpleGTQuery))
        },
      ),
      suite("Null ")(
        test(s"$simpleNullQuery AND $everything should be $simpleNullQuery") {
          assert(simpleNullQuery & everything)(equalTo(simpleNullQuery))
        },
        test(s"NOT (${!simpleNullQuery}) should be $simpleNullQuery") {
          assert(!(!simpleNullQuery))(equalTo(simpleNullQuery))
        },
      ),
    )

}
