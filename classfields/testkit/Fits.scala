package ru.auto.comeback.model.testkit

import ru.auto.comeback.model.Comeback.{CarInfo, Comeback}
import ru.auto.comeback.model.Filters.CatalogFilter
import ru.auto.comeback.model.Filters

/** Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 20/01/2020
  */
trait Fits[F, E] {
  def fits(filters: F, value: E): Boolean
}

object Fits {
  def apply[F, E](implicit fits: Fits[F, E]): Fits[F, E] = fits
  def apply[F, E](filter: F, value: E)(implicit fits: Fits[F, E]): Boolean = fits.fits(filter, value)

  implicit val carFitsCatalogFilter: Fits[CatalogFilter, CarInfo] =
    (filters: CatalogFilter, car: CarInfo) => {
      filters match {
        case Filters.EmptyCatalogFilter => true
        case Filters.MarkFilter(mark) => car.mark == mark
        case Filters.ModelFilter(mark, model) => car.mark == mark && car.model == model
        case Filters.SuperGenFilter(mark, model, superGenId) =>
          car.mark == mark && car.model == model && car.superGenId == superGenId
      }
    }

  implicit def elemFitsEqFilter[T]: Fits[Filters.EqFilter[T], T] =
    (filters: Filters.EqFilter[T], v: T) => {
      filters match {
        case Filters.EmptyEq => true
        case Filters.EqValue(value) => value == v
      }
    }

  implicit def optElemFitsEqFilter[T]: Fits[Filters.EqFilter[T], Option[T]] =
    (filters: Filters.EqFilter[T], optV: Option[T]) => {
      filters match {
        case Filters.EmptyEq => true
        case Filters.EqValue(value) => optV.contains(value)
      }
    }

  implicit def elemFitsRange[T: Ordering]: Fits[Filters.Range[T], T] =
    (filters: Filters.Range[T], value: T) => {
      filters match {
        case Filters.EmptyRange => true
        case Filters.LeftRange(from) => Ordering[T].gteq(value, from)
        case Filters.RightRange(to) => Ordering[T].lteq(value, to)
        case Filters.FullRange(from, to) => Ordering[T].gteq(value, from) && Ordering[T].lteq(value, to)
      }
    }

  implicit val comebackFitFilters: Fits[Filters, Comeback] =
    (filters: Filters, comeback: Comeback) =>
      Seq(
        Fits(filters.catalogFilter, comeback.offer.car),
        Fits(filters.year, comeback.offer.car.year),
        Fits(filters.mileage, comeback.offer.mileage),
        Fits(filters.priceRub, comeback.offer.priceRub),
        Fits(filters.creationDate, comeback.offer.activated),
        filters.rid.isEmpty || filters.rid.contains(comeback.offer.rid),
        Fits(filters.pastSection, comeback.pastEvents.pastOffer.map(_.section)),
        !filters.onlyLastSeller || comeback.meta.sellersCountAfterPast.contains(1),
        filters.lastEventTypes.isEmpty || filters.lastEventTypes.contains(comeback.meta.lastEventType),
        filters.vins.isEmpty || filters.vins.contains(comeback.offer.car.vin)
      ).reduce(_ && _)
}
