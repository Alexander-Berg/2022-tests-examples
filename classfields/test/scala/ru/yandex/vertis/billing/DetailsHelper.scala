package ru.yandex.vertis.billing

import ru.yandex.vertis.billing.model_core.EventStat.{
  DetailsConversionException,
  ItemDetails,
  RawEventDetails,
  RevenueDetails
}

trait DetailsHelper {

  protected def toRevenueDetails(rawEventDetails: RawEventDetails): RevenueDetails = {
    rawEventDetails.toOldDetails match {
      case Some(r: RevenueDetails) => r
      case other =>
        throw DetailsConversionException(s"Expect RevenueDetails but got $other")
    }
  }

  protected def toItemDetails(rawEventDetails: RawEventDetails): ItemDetails = {
    rawEventDetails.toOldDetails match {
      case Some(i: ItemDetails) => i
      case other =>
        throw DetailsConversionException(s"Expect ItemDetails but got $other")
    }
  }

}
