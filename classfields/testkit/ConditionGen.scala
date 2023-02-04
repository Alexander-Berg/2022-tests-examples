package ru.yandex.vertis.general.gost.model.testkit

import general.bonsai.category_model.Category
import ru.yandex.vertis.general.gost.model.Offer
import ru.yandex.vertis.general.gost.model.Offer.Condition
import zio.random.Random
import zio.test.Gen

object ConditionGen {

  val anyCondition: Gen[Random, Option[Condition]] = Gen.option(Gen.elements(Offer.New, Offer.Used))

  def ofCategory(category: Category): Gen[Random, Option[Condition]] =
    if (category.ignoreCondition) Gen.const(None) else Gen.elements(Some(Offer.New), Some(Offer.Used))
}
