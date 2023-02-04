package ru.yandex.vertis.billing.banker.service.effect.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.AsyncSpecBase

import scala.concurrent.Future
import scala.util.Failure

trait EffectRefundHelperServiceBaseSpec
  extends RefundHelperServiceMockProvider
  with Matchers
  with AnyWordSpecLike
  with AsyncSpecBase {

  type T <: RefundHelperServiceMock with TestingEffectExecutionContextAware

  protected def createInstance: T

  private def asTestCase(name: String, check: => Unit): Unit = {
    name in {
      check
    }
  }

  protected def checkSuccess(name: String, check: (T => Future[Unit]) => Unit): Unit = {
    asTestCase(s"$name (on processRefund method call)", check(_.mockProcessRefundAndCall))
    asTestCase(s"$name (on processRefundRequest method call)", check(_.mockProcessRefundRequestAndCall))
  }

  protected def checkFail(name: String, check: (T => Future[Unit]) => Unit): Unit = {
    asTestCase(s"$name (on processRefund method call)", check(_.mockProcessRefundFailAndCall))
    asTestCase(s"$name (on processRefundRequest method call)", check(_.mockProcessRefundRequestFailAndCall))
  }

  protected def checkEffectNotCalled(instance: T): Unit

  private def nonEffectWhenMainActionFailCheck(prepare: T => Future[Unit]): Unit = {
    val instance = createInstance
    val action = prepare(instance)

    checkEffectNotCalled(instance)

    action.toTry match {
      case Failure(`ValidationException`) =>
        ()
      case other =>
        fail(s"Unexpected $other")
    }
  }

  private def nonEffectWhenMainActionFail(): Unit = {
    "not call effect" when {
      checkFail("main action fail", nonEffectWhenMainActionFailCheck)
    }
  }

  "EffectRefundHelperService" should {
    behave.like(nonEffectWhenMainActionFail())
  }

}
