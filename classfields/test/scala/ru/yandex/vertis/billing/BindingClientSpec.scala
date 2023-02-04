package ru.yandex.vertis.billing

import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.microcore_model.Dsl

import scala.collection.JavaConversions._
import scala.util.Success

/**
  * Base specs on [[BindingClient]].
  *
  * @author dimas
  * @author alesavin
  */
trait BindingClientSpec extends AnyWordSpec with Matchers {

  def bindingClient: BindingClient

  "BindingClient" should {

    "successfully update bindings" in {
      val rq = Dsl.compactBindingRequest(
        Iterable(Dsl.partnerOfferId("bar", "baz")),
        "foo",
        Model.BindingSource.FEED,
        false
      )
      bindingClient.update(rq) match {
        case Success(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
  }
}
