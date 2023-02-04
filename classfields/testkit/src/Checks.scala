package common.zio.testkit

import zio.ZIO
import zio.test.{checkNM, Gen, TestConfig, TestResult}

object Checks {

  case class CheckMn(n: Int) extends AnyVal {

    def apply[R <: TestConfig, R1 <: R, E, A, B, C, D, F, G, H](
        rv1: Gen[R, A],
        rv2: Gen[R, B],
        rv3: Gen[R, C],
        rv4: Gen[R, D],
        rv5: Gen[R, F],
        rv6: Gen[R, G],
        rv7: Gen[R, H]
      )(test: (A, B, C, D, F, G, H) => ZIO[R1, E, TestResult]): ZIO[R1, E, TestResult] =
      checkNM(n)(rv1 <*> rv2 <*> rv3 <*> rv4 <*> rv5 <*> rv6 <*> rv7)(reassociate(test))
  }

  private def reassociate[A, B, C, D, E, F, G, H](
      fn: (A, B, C, D, E, F, G) => H): (((((((A, B), C), D), E), F), G)) => H = {
    case ((((((a, b), c), d), e), f), g) => fn(a, b, c, d, e, f, g)
  }

}
