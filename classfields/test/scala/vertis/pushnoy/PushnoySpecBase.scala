package vertis.pushnoy

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

/** @author kusaeva
  */
trait PushnoySpecBase extends AnyWordSpec with Matchers with ScalaFutures with MockedCtx with PatienceConfiguration {

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(1, Seconds)), interval = scaled(Span(15, Millis)))
}
