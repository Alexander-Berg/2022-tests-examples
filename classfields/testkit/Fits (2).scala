package auto.dealers.calltracking.model.testkit

import auto.dealers.calltracking.model.{Call, Filters}
import ru.auto.calltracking.proto.filters_model.CallResultGroup
import ru.auto.calltracking.proto.model.Call.CallResult

trait Fits[F, E] {
  def fits(filters: F, value: E): Boolean
}

object Fits {
  def apply[F, E](implicit fits: Fits[F, E]): Fits[F, E] = fits
  def apply[F, E](filter: F, value: E)(implicit fits: Fits[F, E]): Boolean = fits.fits(filter, value)

  implicit def elemFitsEqFilter[T]: Fits[Filters.EqFilter[T], T] =
    (filters: Filters.EqFilter[T], v: T) => {
      filters match {
        case Filters.EmptyEqFilter => true
        case Filters.EqValue(value) => value == v
      }
    }

  implicit def optElemFitsEqFilter[T]: Fits[Filters.EqFilter[T], Option[T]] =
    (filters: Filters.EqFilter[T], vOpt: Option[T]) => {
      (filters, vOpt) match {
        case (Filters.EqValue(value), Some(v)) => value == v
        case _ => true
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

  implicit def elemFitsCallResult[T <: CallResult]: Fits[CallResultGroup, T] =
    (filter: CallResultGroup, value: T) => {
      filter match {
        case CallResultGroup.ALL_RESULT_GROUP => !value.isBlocked
        case CallResultGroup.ANSWERED_GROUP => value.isSuccess
        case CallResultGroup.MISSED_GROUP =>
          val missedGroup = List(
            CallResult.NO_ANSWER,
            CallResult.STOP_CALLER,
            CallResult.BUSY_CALLEE,
            CallResult.UNAVAILABLE_CALLEE,
            CallResult.INVALID_CALLEE,
            CallResult.NO_REDIRECT,
            CallResult.NO_CONFIRMATION,
            CallResult.CALLBACK_MISSING,
            CallResult.ERROR,
            CallResult.UNKNOWN_RESULT
          )
          missedGroup.contains(value)
        case CallResultGroup.Unrecognized(_) => false

      }
    }

  implicit val callFitFilters: Fits[Filters, Call] =
    (filters: Filters, call: Call) =>
      Seq(
        Fits(filters.sourcePhone, call.sourcePhone),
        Fits(filters.targetPhone, call.targetPhone),
        filters.tags.isEmpty || (call.tags & filters.tags) == filters.tags,
        Fits(filters.callTime, call.callTime),
        Fits(filters.callResultGroup, call.callResult)
        // TODO
      ).reduce(_ && _)
}
