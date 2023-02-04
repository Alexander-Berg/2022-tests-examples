package auto.dealers.calltracking.model.testkit

import cats.data.NonEmptyList
import common.zio.testkit.CommonGen._
import auto.dealers.calltracking.model.Filters
import auto.dealers.calltracking.model.Filters._
import ru.auto.calltracking.proto.filters_model.{CallResultGroup, CallbackGroup, TargetGroup, UniqueGroup}
import zio.random.Random
import zio.test.{Gen, Sized}

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
      Gen.const(EmptyEqFilter),
      valueGen.map(EqValue(_))
    )

  def anyMultiFilter[R <: Random with Sized, T](valueGen: Gen[R, T]): Gen[R, MultiFilter[T]] =
    Gen.listOf(valueGen).map {
      case head :: tail => AnyOf(NonEmptyList.apply(head, tail))
      case Nil => EmptyMultiFilter
    }

  private val filterPatches = List(
    anyEqFilter(CallGen.anyPhoneNumber).map(v => (_: Filters).copy(sourcePhone = v)),
    anyEqFilter(CallGen.anyPhoneNumber).map(v => (_: Filters).copy(targetPhone = v)),
    anyRange(anyInstant).map(v => (_: Filters).copy(callTime = v)),
    Gen.fromIterable(TargetGroup.values).map(v => (_: Filters).copy(targetGroup = v)),
    Gen.fromIterable(CallResultGroup.values).map(v => (_: Filters).copy(callResultGroup = v)),
    Gen.fromIterable(CallbackGroup.values).map(v => (_: Filters).copy(callbackGroup = v)),
    Gen.fromIterable(UniqueGroup.values).map(v => (_: Filters).copy(uniqueGroup = v)),
    Gen.listOf(anyString).map(v => (_: Filters).copy(tags = v.toSet)),
    Gen.listOf(anyCatalogFilter).map(v => (_: Filters).copy(catalogFilter = v)),
    anyMultiFilter(CallGen.anyCategory).map(v => (_: Filters).copy(category = v)),
    anyMultiFilter(CallGen.anySection).map(v => (_: Filters).copy(section = v)),
    anyMultiFilter(CallGen.anyOfferId).map(v => (_: Filters).copy(offerId = v)),
    anyMultiFilter(CallGen.anyVinCode).map(v => (_: Filters).copy(vinCode = v)),
    anyRange(CallGen.anyYear).map(v => (_: Filters).copy(year = v)),
    anyRange(CallGen.anyPrice).map(v => (_: Filters).copy(price = v)),
    anyMultiFilter(CallGen.anyBodyType).map(v => (_: Filters).copy(bodyType = v)),
    anyMultiFilter(CallGen.anyTransmission).map(v => (_: Filters).copy(transmission = v))
  )

  def filters(size: Int): Gen[Random with Sized, Filters] = {
    collectAllShuffled(filterPatches)
      .map(
        _.take(size)
          .foldLeft(Filters.empty)((filters, patch) => patch.apply(filters))
      )
  }

  val anyFilters: Gen[Sized with Random, Filters] = Gen.small(filters)
}
