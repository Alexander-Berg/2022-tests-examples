package ru.yandex.vertis.general.bonsai.model.testkit

import general.bonsai.attribute_model.AttributeDefinition
import general.bonsai.category_model.{Category, CategoryState}
import zio.random.Random
import zio.test.{Gen, Sized}

object Generators {
  val idGen: Gen[Random, String] = Gen.stringN(32)(Gen.alphaNumericChar).noShrink
  val versionGen: Gen[Random, Int] = Gen.int(1, Int.MaxValue).noShrink
  val nameGen: Gen[Random, String] = Gen.stringN(32)(Gen.alphaNumericChar)

  def category(): Gen[Random, Category] =
    (for {
      id <- idGen
      version <- versionGen
      name <- nameGen
    } yield Category(id = id, version = version, name = name, state = CategoryState.DEFAULT)).noShrink

  def attribute(): Gen[Random, AttributeDefinition] =
    for {
      id <- idGen
      version <- versionGen
      name <- nameGen
    } yield AttributeDefinition(id = id, version = version, name = name)
}
