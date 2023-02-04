package ru.yandex.realty.model.serialization.relevance

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.offer.{RelevanceValue, RelevanceValues}

@RunWith(classOf[JUnitRunner])
class RelevanceValuesTest extends FlatSpec with Matchers {

  "RelevanceValues" should "correctly add values" in {
    val value = new RelevanceValue("test", relevance = 0f)

    val relevanceValues = new RelevanceValues(List()).addValue(value)

    RelevanceValues(List(value)) shouldEqual relevanceValues
  }
}
