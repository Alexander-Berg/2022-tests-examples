package ru.auto.comeback.model.testkit

import common.zio.testkit.CommonGen._
import ru.auto.comeback.model.Filters
import ru.auto.comeback.model.Filters._
import zio.random.Random
import zio.test.{Gen, Sized}

/** Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 21/01/2020
  */
object FiltersGen {

  val anyCatalogFilter: Gen[Random with Sized, CatalogFilter] = Gen.oneOf(
    Gen.const(EmptyCatalogFilter),
    anyString.map(MarkFilter),
    anyString.zip(anyString).map(ModelFilter.tupled),
    for {
      mark <- anyString
      model <- anyString
      superGenId <- Gen.int(0, Int.MaxValue - 1)
    } yield SuperGenFilter(mark, model, superGenId)
  )

  def anyRange[R <: Random, T: Ordering](valueGen: Gen[R, T]): Gen[R, Range[T]] =
    Gen.oneOf(
      Gen.const(EmptyRange),
      valueGen.map(LeftRange(_)),
      valueGen.map(RightRange(_)),
      valueGen.zipWith(valueGen)((a, b) => FullRange(Ordering[T].min(a, b), Ordering[T].max(a, b)))
    )

  def anyEqFilter[R <: Random, T](valueGen: Gen[R, T]): Gen[R, EqFilter[T]] =
    Gen.oneOf(
      Gen.const(EmptyEq),
      valueGen.map(EqValue(_))
    )

  private val filterPatches = List(
    anyCatalogFilter.map(v => (_: Filters).copy(catalogFilter = v)),
    anyRange(Gen.int(1950, 2020)).map(v => (_: Filters).copy(year = v)),
    anyRange(Gen.int(0, 1000000)).map(v => (_: Filters).copy(mileage = v)),
    anyRange(Gen.int(0, Int.MaxValue - 1)).map(v => (_: Filters).copy(priceRub = v)),
    anyRange(CommonGen.anyInstant).map(v => (_: Filters).copy(creationDate = v)),
    Gen.listOf(Gen.anyInt).map(v => (_: Filters).copy(rid = v.toSet)),
    anyEqFilter(CommonGen.anySection).map(v => (_: Filters).copy(pastSection = v)),
    Gen.boolean.map(v => (_: Filters).copy(onlyLastSeller = v)),
    Gen.listOf(CommonGen.anyEventType).map(v => (_: Filters).copy(lastEventTypes = v.toSet)),
    Gen.listOf(CommonGen.anyVinCode).map(v => (_: Filters).copy(vins = v.toSet))
  )

  def filters(size: Int): Gen[Random with Sized, Filters] = {
    collectAllShuffled(filterPatches).map(
      _.take(size)
        .foldLeft(Filters.Empty)((filters, patch) => patch.apply(filters))
    )
  }

  val anyFilters: Gen[Random with Sized, Filters] =
    Gen.small(filters)
}
