package ru.auto.salesman.test

import org.scalacheck.ShrinkLowPriority
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Span}
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.scalatest.{BetterEitherValues, BetterTryValues}

trait BaseSpec
    extends Matchers
    with WordSpecLike
    with ZIOValues
    with BetterTryValues
    with BetterEitherValues
    with OptionValues
    with ScalaFutures
    with ScalaCheckPropertyChecks
    with ShrinkLowPriority
    with MockFactory
    with OneInstancePerTest
    with ScalamockCallHandlers
    with TestHttpServer
    with CollectingInstances
    with UnitSpec
    with JsonConverters
    with ReadResource
    with LoneElement
    with CustomMatchers
    with TestExecutionContext {

  // Default patience config timeout in scalatest is 150 millis. Sometimes we
  // experience GC pauses when run tests on TC agents, which cause tests to
  // fail. So, we increase the timeout to avoid such failures.
  implicit abstract override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(1, Second)))
}
