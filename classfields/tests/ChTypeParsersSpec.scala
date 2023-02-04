package vertis.clickhouse.tests

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import vertis.clickhouse.model.ChType

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class ChTypeParsersSpec extends AnyWordSpec with Matchers {

  "ChTypeParsers" should {
    "parse primitives" in {
      parserTest("String", ChType.String)
      parserTest("Float64", ChType.Float64)
      parserTest("DateTime64(9, 'Europe/Moscow')", ChType.DateTimeNanos)
      parserTest("DateTime", ChType.DateTime)
    }

    "parse primitive arrays" in {
      parserTest("Array(Int32)", ChType.Array(ChType.Int32))
    }

    "parse nested arrays" in {
      parserTest("Array(Array(Int32))", ChType.Array(ChType.Array(ChType.Int32)))
    }

    "parse nullable" in {
      parserTest("Nullable(Int32)", ChType.Nullable(ChType.Int32))
    }

    "parse arrays of nullable" in {
      parserTest("Array(Nullable(UInt32))", ChType.Array(ChType.Nullable(ChType.UInt32)))
    }

    "not parse nullable of arrays" in {
      intercept[IllegalArgumentException] {
        ChType.withName("Nullable(Array(UInt32))")
      }
    }
  }

  private def parserTest(input: String, expected: ChType) =
    ChType.withName(input) shouldBe expected
}
