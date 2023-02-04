package ru.yandex.vertis.general.category_matcher.model.testkit

import ru.yandex.vertis.general.category_matcher.model.{CategoryId, Match}
import zio.test.magnolia.DeriveGen

object MatchGen {
  implicit val anyCategoryId: DeriveGen[CategoryId] = DeriveGen.instance(CategoryIdGen.any)

  val anyKey = DeriveGen[Match.Key]
  val any = DeriveGen[Match]
}
