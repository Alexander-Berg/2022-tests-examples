package ru.yandex.realty.model.serialization.relevance

import ru.yandex.realty.model.serialization._
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.offer.{RelevanceValue, RelevanceValues}

import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[JUnitRunner])
class RelevanceMessageProtoConverterTest extends FlatSpec with Matchers {

  "RelevanceMessageProtoConverter" should "correctly convert to protobuf message and back" in {
    val values = RelevanceValues(List(RelevanceValue("test", relevance = 0f)))

    val message = RelevanceMessageProtoConverter.toMessage(values)
    val newModel = RelevanceMessageProtoConverter.fromMessage(message)
    newModel shouldEqual values
  }
}
