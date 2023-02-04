package ru.yandex.realty.parser

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.context.ExtDataLoaders
import ru.yandex.realty.model.message.ExtDataSchema.{TagAlgorithm, TagMessage}

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class TagVerbaParserTestSpec extends FlatSpec with MockFactory with Matchers {

  "TagVerbaParser" should "parses correct dictionary from Verba " in {

    val inputStream = getClass.getResource("/TagDictionary.gz").openStream()
    val verbaStorage = ExtDataLoaders.createVerbaStorage(inputStream)

    val parser = new TagVerbaParser(verbaStorage)
    val result = parser.parse()

    inputStream.close()

    result.seq.size should be(1)
    val message: TagMessage = result.head
    message.getId should be(1688912L)
    message.getTitle should be("в стиле лофт")
    message.getPriority should be(1)
    message.getSuggestsCount should be(2)
    message.getSuggestsList should contain("в стиле лофт")
    message.getSuggestsList should contain("лофт")
    message.getValues.getAliassesCount should be(2)
    message.getValues.getAliassesList should contain("в стиле лофт")
    message.getValues.getAliassesList should contain("лофт")
    message.getCategoryCount should be(3)
    message.getCategoryList should contain(1)
    message.getCategoryList should contain(2)
    message.getCategoryList should contain(3)
    message.getTypeCount should be(0)
    message.getAlgorithm should be(TagAlgorithm.DESCRIPTION_CONTAINS_VALUES)
  }

}
