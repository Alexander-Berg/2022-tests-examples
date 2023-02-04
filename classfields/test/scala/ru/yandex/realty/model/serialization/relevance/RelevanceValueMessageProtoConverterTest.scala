package ru.yandex.realty.model.serialization.relevance

import ru.yandex.realty.model.serialization.RelevanceValueMessageProtoConverter
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.offer.RelevanceValue

@RunWith(classOf[JUnitRunner])
class RelevanceValueMessageProtoConverterTest extends FlatSpec with Matchers {

  "RelevanceValueMessageProtoConverter" should "correctly convert to protobuf message and back" in {
    val value = new RelevanceValue("test", relevance = 0f)
    val message = RelevanceValueMessageProtoConverter.toMessage(value)
    val newModel = RelevanceValueMessageProtoConverter.fromMessage(message)
    newModel shouldEqual value
  }
}
