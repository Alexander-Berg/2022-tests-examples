package ru.yandex.vertis.safe_deal.spec

import cats.implicits.catsSyntaxOptionId
import zio.ZIO
import zio.test.Assertion.isSome
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object DummyTest extends DefaultRunnableSpec {

  def spec: ZSpec[TestEnvironment, Any] =
    suite("SendToBankStage")(
      testM("process with tinkoff") {
        val actual = ZIO.succeed(1.some)
        assertM(actual)(isSome)
      }
    )
}
