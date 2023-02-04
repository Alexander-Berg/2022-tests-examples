package vertis.sraas

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.ExecutionContext

/** @author zvez
  */
trait BaseSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalaCheckPropertyChecks with Eventually {

  implicit def ec: ExecutionContext = ExecutionContext.global

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(15, Millis))
}
