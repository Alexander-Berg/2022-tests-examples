package auto.c2b.common.postgresql.test

import auto.c2b.common.postgresql.FilterRange
import auto.c2b.common.postgresql.FilterRange.EmptyRange
import zio.test.Assertion._
import zio.test.TestAspect.shrinks
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.magnolia.DeriveGen

object FilterRangeTest extends DefaultRunnableSpec {

  private val anyRange = DeriveGen[FilterRange.Range[Int]]

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("FilterRange")(
      test("EmptyRange should be empty")(
        assertTrue(EmptyRange.isEmpty)
      ),
      test("EmptyRange should fold as empty")(
        assertTrue(EmptyRange.fold(0)(_ => 1, _ => 2, (_, _) => 3) == 0)
      ),
      test("EmptyRange should foldOption as None")(
        assert(EmptyRange.foldOption(_ => 1, _ => 2, (_, _) => 3))(isNone)
      ),
      testM("Range should be not empty")(
        checkN(10)(anyRange) { range =>
          val excepted = getExceptedValue(range).isEmpty
          assertTrue(range.isEmpty == excepted)
        }
      ),
      testM("Range should fold")(
        checkN(10)(anyRange) { range =>
          val excepted = getExceptedValue(range).getOrElse(0)
          assertTrue(range.fold(0)(_ => 1, _ => 2, (_, _) => 3) == excepted)
        }
      ),
      testM("Range should foldOption as Some")(
        checkN(10)(anyRange) { range =>
          val excepted = getExceptedValue(range)
          assertTrue(range.foldOption(_ => 1, _ => 2, (_, _) => 3) == excepted)
        }
      )
    ) @@ shrinks(0)
  }

  private def getExceptedValue(range: FilterRange.Range[Int]): Option[Int] = {
    val both = range.from.zip(range.to).map(_ => 3)
    val right = range.to.map(_ => 2)
    val left = range.from.map(_ => 1)
    both.orElse(left).orElse(right)
  }
}
