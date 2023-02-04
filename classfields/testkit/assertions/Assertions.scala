package ru.yandex.vertis.test_utils.assertions

import com.softwaremill.tagging.@@
import zio.test.AssertionM.Render.param
import zio.test.diff.Diff.DiffOps
import zio.test.diff.{Diff, DiffResult}
import zio.test.magnolia.diff.DeriveDiff
import zio.test.{AssertResult, Assertion, AssertionValue, BoolAlgebra}

object Assertions {

  /**
    * В большинстве случаях, при использовании этой функции достаточно импортировать magnolia.diff.gen
    * пример: import zio.test.magnolia.diff.gen
    */
  def noDiff[T: Diff](expected: T): Assertion[T] = {
    lazy val assertion: Assertion[T] = Assertion.assertionDirect[T]("noDiff")(param(expected)) { actual =>
      lazy val diff = actual.diffed(expected)
      lazy val result: AssertResult =
        if (diff.noDiff) BoolAlgebra.success(AssertionValue(assertion, actual, result))
        else BoolAlgebra.failure(AssertionValue(assertion.label(s"\n${diff.render}"), actual, result))
      result
    }
    assertion
  }

  trait DiffSupport extends DeriveDiff {

    def noDiff[T: Diff](expected: T): Assertion[T] = Assertions.noDiff(expected)

    implicit def diffFromTagged[T, U]: Diff[T @@ U] = new Diff[T @@ U] {

      override def diff(x: T @@ U, y: T @@ U): DiffResult =
        if (x == y) DiffResult.Identical(x)
        else DiffResult.Different(x, y)
    }
  }
}
