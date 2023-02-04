package ru.yandex.vertis.moisha.test

import org.scalamock.scalatest.MockFactory
import org.scalatest.{LoneElement, Matchers, OneInstancePerTest, OptionValues, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.{PropertyChecks, TableDrivenPropertyChecks}
import org.scalatest.time.{Second, Span}
import ru.yandex.vertis.scalatest.{BetterEitherValues, BetterTryValues}
import org.scalacheck.ShrinkLowPriority

trait BaseSpec
  extends Matchers
  with WordSpecLike
  with BetterTryValues
  with BetterEitherValues
  with OptionValues
  with ScalaFutures
  with PropertyChecks
  with TableDrivenPropertyChecks
  with ShrinkLowPriority
  with MockFactory
  with OneInstancePerTest
  with ScalamockCallHandlers
  with UnitSpec
  with JsonConverters
  with LoneElement {

  // Default patience config timeout in scalatest is 150 millis. Sometimes we
  // experience GC pauses when run tests on TC agents, which cause tests to
  // fail. So, we increase the timeout to avoid such failures.
  implicit abstract override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(1, Second)))
}
