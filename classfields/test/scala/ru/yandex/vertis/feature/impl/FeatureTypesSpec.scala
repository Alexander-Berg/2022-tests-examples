package ru.yandex.vertis.feature.impl

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.feature.model.FeatureTypes

/**
  * Specs on [[FeatureTypes]]
  *
  * @author alesavin
  */
class FeatureTypesSpec
  extends WordSpec
  with Matchers {

  val FeatureTypes: FeatureTypes =
    new CompositeFeatureTypes(Iterable(
      BasicFeatureTypes,
      CustomFeatureTypes))

  "DefaultFeatureTypes" should {
    "return basics" in {
      FeatureTypes.withKey("boolean")
      FeatureTypes.withKey("byte")
      FeatureTypes.withKey("short")
      FeatureTypes.withKey("integer")
      FeatureTypes.withKey("long")
      FeatureTypes.withKey("float")
      FeatureTypes.withKey("double")
      FeatureTypes.withKey("string")
    }
    "return custom type" in {
      FeatureTypes.withKey("custom_ComplexType")
    }
    "fail for unknown type" in {
      intercept[NoSuchElementException] {
        FeatureTypes.withKey("__")
      }
    }
  }
}
