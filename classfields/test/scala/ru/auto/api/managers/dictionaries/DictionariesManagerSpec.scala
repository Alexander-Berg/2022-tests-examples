package ru.auto.api.managers.dictionaries

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.CatalogModel.DictionaryV1
import ru.auto.api.exceptions.{DictionaryFormatNotFound, DictionaryNotFound}
import ru.auto.api.model.CategorySelector
import ru.auto.api.testkit.TestData
import ru.auto.api.util.Resources

import scala.jdk.CollectionConverters._

/**
  * Created by mcsim-gr on 23.08.17.
  */
class DictionariesManagerSpec extends BaseSpec with ScalaCheckPropertyChecks {

  val dictionariesManager = new DictionariesManager(TestData)

  "Dictionaries manager" should {
    "return cars body_types Dictionary v1" in {
      val pattern = Resources.toProto[DictionaryV1]("/dictionaries/cars/v1/body_type.json")

      val response = dictionariesManager.getDictionary("v1", CategorySelector.Cars, "body_type")

      val set = response.getDictionaryV1.getValuesList.asScala.toSet

      set.nonEmpty shouldBe true
      set shouldBe pattern.getValuesList.asScala.toSet
    }

    "throw an DictionaryFormatNotFound" in {
      intercept[DictionaryFormatNotFound] {
        dictionariesManager.getDictionary("v1000", CategorySelector.Cars, "body_types")
      }
    }

    "throw an DictionaryV1NotFound" in {
      intercept[DictionaryNotFound] {
        dictionariesManager.getDictionary("v1", CategorySelector.Cars, "non_existent_Dictionary")
      }
    }

    "obtain chat presets from eds" in {
      val messagePresets = dictionariesManager
        .getCommonDictionary("v1", "message_presets")
        .getDictionaryV1
        .getValuesList
        .asScala
      messagePresets should not be empty

      val helloMessagePresets = dictionariesManager
        .getCommonDictionary("v1", "message_hello_presets")
        .getDictionaryV1
        .getValuesList
        .asScala
      helloMessagePresets should not be empty

      val sellerMessagePresets = dictionariesManager
        .getCommonDictionary("v1", "seller_message_presets")
        .getDictionaryV1
        .getValuesList
        .asScala
      sellerMessagePresets should not be empty

      val sellerHelloMessagePresets = dictionariesManager
        .getCommonDictionary("v1", "seller_message_hello_presets")
        .getDictionaryV1
        .getValuesList
        .asScala
      sellerHelloMessagePresets should not be empty
    }
  }
}
