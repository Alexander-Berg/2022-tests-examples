package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.Payload._
import ru.yandex.vertis.billing.model_core.PayloadSpec.{tsField, urlFieldName}
import ru.yandex.vertis.billing.util.DateTimeUtils.now

import scala.util.Success

/**
  * Specs on [[Payload]]
  *
  * @author alesavin
  */
class PayloadSpec extends AnyWordSpec with Matchers {

  "Payload" should {

    "convert timestamp to correct datetime string" in {
      val d = now()
      val sd = EventDateTimeFormatterPattern.print(d.getMillis)
      info(sd)
      assert(sd.endsWith("Z"))
      assert(sd.contains("T"))
    }

    "add timestamp when it is not present in data" in {
      val d = now()
      val p = Payload(d, "c", "u", "o", "p", Map("k" -> "v"))
      p.toEventRecord match {
        case Success(er) =>
          er.values.get(tsField) match {
            case Some(v) =>
              v should be(EventDateTimeFormatterPattern.print(d))
            case other => fail(s"Unpredicted $other")
          }
        case other => fail(s"Unpredicted $other")
      }
    }

    "not modify timestamp if it is present in data" in {
      val d = now()
      val p = Payload(d, "c", "u", "o", "p", Map(tsField -> "some_value"))
      p.toEventRecord match {
        case Success(er) =>
          er.values.get(tsField) match {
            case Some(v) =>
              v should not be EventDateTimeFormatterPattern.print(d)
            case other => fail(s"Unpredicted $other")
          }
        case other => fail(s"Unpredicted $other")
      }
    }

    "clean up url before parse it (https://st.yandex-team.ru/VSBILLING-4001)" in {
      val d = now()
      val badUrl = "https://test.ru/test-(test\u0085/test)test:opa"
      val expectedUrl = "//test.ru/test-(test/test)test:opa"
      val p = Payload(d, "c", "u", "o", "p", Map(urlFieldName -> badUrl))
      p.toEventRecord match {
        case Success(er) =>
          er.values.get(urlFieldName) match {
            case Some(`badUrl`) =>
              ()
            case other =>
              fail(s"Unpredicted $other")
          }
          er.keyParts.drop(4).head match {
            case `expectedUrl` =>
              ()
            case other =>
              fail(s"Unpredicted $other")
          }
        case other =>
          fail(s"Unpredicted $other")
      }
    }
  }
}

object PayloadSpec {
  val tsField = "timestamp"
  val urlFieldName = "offer_url"
}
