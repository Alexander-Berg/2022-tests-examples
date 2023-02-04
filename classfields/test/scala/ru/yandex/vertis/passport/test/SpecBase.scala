package ru.yandex.vertis.passport.test

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.model.{RequestContext, RequestPayload}
import ru.yandex.vertis.tracing.Traced

import scala.language.implicitConversions

trait SpecBase extends Matchers with ScalaFutures with OptionValues {

  implicit override def patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(20, Seconds))

  implicit val requestContext: RequestContext = RequestContext("1")

  def wrap(payload: RequestPayload): RequestContext =
    RequestContext(payload, Traced.empty)

  /**
    * Used to provide higher-priority implicit RequestContext to a test
    * in place of default SpecBase#requestContext.
    */
  trait WithCustomRequestContext {
    implicit def requestContext: RequestContext
  }
}
