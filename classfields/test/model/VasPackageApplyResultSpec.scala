package auto.dealers.multiposting.clients.avito.test.model

import auto.dealers.multiposting.clients.avito.model.VasPackageApplyResult
import io.circe.parser._
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, _}

object VasPackageApplyResultSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("VasPackageApplyResult")(
    decodeProductApply
  )

  val decodeProductApply =
    test("parse VasPackageApplyResult") {
      val json =
        """
        | {
        |   "amount": 1.0
        | }
        |""".stripMargin

      val decoded = decode[VasPackageApplyResult](json)

      val expectedVasApplyResult = VasPackageApplyResult(
        amount = 1.0
      )

      assert(decoded)(equalTo(Right(expectedVasApplyResult)))
    }
}
