package ru.yandex.vos2.autoru.utils.testforms

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.{Availability, Section}
import ru.auto.api.TrucksModel.TruckCategory
import ru.yandex.vos2.util.RandomUtil

case class TestFormParams[T <: FormInfo](isDealer: Boolean = false,
                                         optOwnerId: Option[Long] = None,
                                         sameGeobaseId: Boolean = true,
                                         optGeobaseId: Option[Long] = None,
                                         section: Section = Section.USED,
                                         availability: Availability = Availability.IN_STOCK,
                                         optCard: Option[T#CardType] = None,
                                         hidden: Boolean = false,
                                         now: DateTime = DateTime.now(),
                                         wheelLeft: Option[Boolean] = None,
                                         gearType: String = "",
                                         optCreateDate: Option[DateTime] = None,
                                         excludeUserRefs: Seq[String] = Seq.empty,
                                         isYandexVideo: Boolean = false,
                                         specialTruck: Boolean = false,
                                         excludeTruckCategories: Option[Seq[TruckCategory]] = None,
                                         generateBadges: Boolean = false,
                                         customAddress: Boolean = false)
// ToDo Вернуть вероятность использования Яндекс видео, когда придумаем как его размещать

object TestFormParams {
  val CarDefault = TestFormParams[CarFormInfo]()
  val CarPrivateDefault = TestFormParams[CarFormInfo](isDealer = false)
  val CarDealerDefault = TestFormParams[CarFormInfo](isDealer = true)

  val TruckDefault = TestFormParams[TruckFormInfo]()
  val TruckPrivateDefault = TestFormParams[TruckFormInfo](isDealer = false)
  val TruckDealerDefault = TestFormParams[TruckFormInfo](isDealer = true)

  val MotoDefault = TestFormParams[MotoFormInfo]()
  val MotoPrivateDefault = TestFormParams[MotoFormInfo](isDealer = false)
  val MotoDealerDefault = TestFormParams[MotoFormInfo](isDealer = true)
}
