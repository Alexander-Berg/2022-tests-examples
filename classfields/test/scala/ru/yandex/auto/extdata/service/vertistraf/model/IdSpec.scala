package ru.yandex.auto.traffic.model

import org.scalatest.{FlatSpec, Matchers}
import zio.prelude.Validation

class IdSpec extends FlatSpec with Matchers {

  it should "return valid code from the correct id" in {
    Id("autoru-1097540491").code shouldBe Validation.succeed(1097540491L)
  }
}
