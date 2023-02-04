package ru.yandex.vertis.vsquality.utils.cats_utils

import cats.effect.IO
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.ScalaFutures

class FutureUtilSpec extends AnyWordSpec with Matchers with ScalaFutures {

  "FutureUtil" should {
    "convert good IO to the good Future" in {
      val action =
        IO[Int] {
          val x = 10
          val y = 100
          x + y
        }
      val future = FutureUtil.fromF(action)
      future.futureValue shouldBe 110
    }

    "convert failed IO to the failed Future with the same error" in {
      val error = new IllegalArgumentException
      val action = IO.raiseError(error)
      val future = FutureUtil.fromF(action)
      future.failed.futureValue shouldBe error
    }
  }
}
