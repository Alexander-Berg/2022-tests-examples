package auto.carfax.carfax_money.logic.testkit

import auto.carfax.carfax_money.model.Meta.Defaults
import auto.carfax.money.money_model.SelectionCriteria
import zio.random.Random
import zio.test.{Gen, Sized}

object SelectionCriteriaGen {

  def selectionCriteriaGen(cnt: Int): Gen[Random with Sized, SelectionCriteria] = for {
    mark <- Gen.oneOf(Gen.const("VAZ"), Gen.const(Defaults.EmptyStr))
    price <- Gen.oneOf(Gen.long(0L, 10000000L), Gen.const(Defaults.EmptyNum.toLong))
    quality <- Gen.int(0, 9999)
    geo <- Gen.listOf(Gen.oneOf(Gen.int(900, 1500), Gen.const(1), Gen.const(Defaults.EmptyNum)))
  } yield SelectionCriteria(mark = mark, carPrice = price, reportQuality = quality, geoIds = geo)

}
