package ru.yandex.vertis.billing.banker.service.log

import ch.qos.logback.classic.Level
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.service.effect.util.{
  RefundHelperServiceMockProvider,
  TestingEffectExecutionContextAware
}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import ru.yandex.vertis.banker.util.LogFetchingProvider
import ru.yandex.vertis.banker.util.LogFetchingProvider.ResultWithLoggingEvents
import LoggedEffectRefundHelperServiceSpec.LoggedEffectRefundHelperServiceWrapProvider
import ru.yandex.vertis.billing.banker.service.effect.EffectRefundHelperService

class LoggedEffectRefundHelperServiceSpec
  extends RefundHelperServiceMockProvider
  with LogFetchingProvider
  with Matchers
  with AnyWordSpecLike
  with AsyncSpecBase {

  def safeRunWithLogFetching[T](future: Future[T]): ResultWithLoggingEvents[Try[T]] = {
    withLogFetching[EffectRefundHelperService, Try[T]] {
      val impl = new LoggedEffectRefundHelperServiceWrapProvider(future)
      impl.unwrap
    }
  }

  "LoggedEffectRefundHelperService" should {
    "log error" in {
      val resultWrapper = safeRunWithLogFetching(Future.failed(ValidationException))
      resultWrapper.result match {
        case Failure(`ValidationException`) =>
          ()
        case other =>
          fail(s"Unexpected result $other")
      }
      resultWrapper.logEvents.size shouldBe 1
      val event = resultWrapper.logEvents.head
      event.getLevel shouldBe Level.ERROR

      val wrapper = event.getThrowableProxy
      wrapper.getClassName shouldBe classOf[IllegalArgumentException].getName
      wrapper.getMessage shouldBe ValidationException.getMessage
    }
    "not log error" in {
      val resultWrapper = safeRunWithLogFetching(Future(()))
      resultWrapper.result match {
        case Success(()) =>
          ()
        case other =>
          fail(s"Unexpected result $other")
      }
      resultWrapper.logEvents.isEmpty shouldBe true
    }
  }

}

object LoggedEffectRefundHelperServiceSpec extends RefundHelperServiceMockProvider with AsyncSpecBase {

  class LoggedEffectRefundHelperServiceWrapProvider[T](future: Future[T])
    extends RefundHelperServiceMock
    with TestingEffectExecutionContextAware
    with LoggedEffectRefundHelperService {

    def unwrap: Try[T] = {
      future.withErrorLogging.toTry
    }

  }

}
