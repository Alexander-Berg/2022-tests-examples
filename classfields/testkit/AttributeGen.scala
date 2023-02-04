package ru.yandex.vertis.general.gost.model.testkit

import general.bonsai.category_model.Category
import ru.yandex.vertis.general.gost.model.attributes.AttributeValue.Empty
import ru.yandex.vertis.general.gost.model.attributes.{Attribute, AttributeValue, Attributes}
import zio.random.Random
import zio.test.{Gen, Sized}

object AttributeGen {

  def anyAttribute(
      id: Gen[Random with Sized, String] = Gen.anyString): Gen[Random with Sized, Attribute[AttributeValue]] =
    for {
      id <- id
    } yield Attribute[AttributeValue](id, 0L, Empty)

  val anyAttribute: Gen[Random with Sized, Attribute[AttributeValue]] = anyAttribute()

  val anyAttributes: Gen[Random with Sized, Attributes[AttributeValue]] =
    Gen.listOf(anyAttribute).map(Attributes.apply[AttributeValue])

  def ofCategory(category: Category): Gen[Random with Sized, Attributes[AttributeValue]] =
    Gen
      .setOfBounded(0, category.attributes.size) {
        Gen
          .fromIterable(category.attributes)
          .map { attribute =>
            Attribute[AttributeValue](attribute.attributeId, attribute.version, Empty)
          }
      }
      .map(_.toSeq)
      .map(Attributes.apply[AttributeValue])
}
