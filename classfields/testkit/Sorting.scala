package auto.dealers.calltracking.model.testkit

import ru.auto.calltracking.proto.filters_model.{Sorting => ProtoSorting}
import ru.auto.calltracking.proto.filters_model.Sorting._
import auto.dealers.calltracking.model.Call

object Sorting {

  def orderingFromSorting(sorting: ProtoSorting): Ordering[Call] =
    if (sorting.sortingType.isAscending)
      orderingForField(sorting.sortingField)
    else orderingForField(sorting.sortingField).reverse

  private def orderingForField(sortingField: SortingField): Ordering[Call] =
    Ordering.by { call =>
      sortingField match {
        case SortingField.CALL_TIME => call.callTime.toEpochMilli
        case SortingField.Unrecognized(value) => sys.error(s"Unexpected SoringField: $value")
      }
    }
}
