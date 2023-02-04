package ru.yandex.vertis.personal.couchbase

import org.scalacheck.Gen
import org.scalacheck.Prop._
import org.scalatest.prop.Checkers._
import ru.yandex.vertis.personal.model.UserProperty
import ru.yandex.vertis.personal.util.BaseSpec

import scala.concurrent.duration.DurationInt

/**
  * Base specs on [[DocumentLayout]]
  *
  * @author dimas
  */
trait DocumentLayoutSpec[P <: UserProperty[_]] extends BaseSpec {

  private val DummyTtl = 1.minute

  def generator: Gen[P]

  def layout: DocumentLayout[P]

  "DocumentLayout" should {
    "round trip property" in {
      check(
        forAll(generator) { x =>
          x == roundTrip(x)
        }
      )
    }
  }

  private def roundTrip(property: P) = {
    val document = layout.propertyToDocument(property, DummyTtl)
    layout.documentToProperty(property.user, Some(document))
  }

}
