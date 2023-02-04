package ru.yandex.vertis.billing.backend

import org.joda.time.LocalDate
import ru.yandex.vertis.billing.model_core.DiscountPolicy.{AmountDiscountPolicy, LoyaltyDiscountPolicy}
import ru.yandex.vertis.billing.model_core.{CostTypes, DiscountPolicy, DiscountType, GoodTypes, PercentDiscount, Target}
import ru.yandex.vertis.billing.util.DateTimeInterval

/**
  * @author ruslansd
  */
object TestDiscountPolicy
  extends DiscountPolicy
  with DiscountPolicy.AmountDiscountPolicy
  with DiscountPolicy.LoyaltyDiscountPolicy {

  def amount: Option[AmountDiscountPolicy] = Some(this)

  def loyalty: Option[LoyaltyDiscountPolicy] = Some(this)

  val LoyaltyDaysWindow = 180
  val OfflineBizTarget = Target.ForProductType(GoodTypes.Placement, CostTypes.CostPerDay)

  /**
    * https://beta.wiki.yandex-team.ru/tourism/201415/product/tz/partnerinterfacespecifications/partner-travel-discount-specification/#skidkanakol-vopokazyvaemyxorganizacijj
    */
  override def amount(eventsNumber: Long): DiscountType = {
    val value = eventsNumber match {
      case _ if eventsNumber >= 3 && eventsNumber < 10 => 3
      case _ if eventsNumber >= 10 && eventsNumber < 50 => 5
      case _ if eventsNumber >= 50 && eventsNumber < 100 => 10
      case _ if eventsNumber >= 100 && eventsNumber < 200 => 15
      case _ if eventsNumber >= 200 && eventsNumber < 500 => 20
      case _ if eventsNumber >= 500 => 30
      case _ => 0
    }
    PercentDiscount(value * 1000)
  }

  /**
    * https://beta.wiki.yandex-team.ru/tourism/201415/product/tz/partnerinterfacespecifications/partner-travel-discount-specification/#nakopitelnajasistemaskidok
    */
  override def loyalty(dates: Iterable[LocalDate], interval: DateTimeInterval): DiscountType = {
    val small = DateTimeInterval(interval.from.plusDays(150), interval.to)
    val (lastMonthDates, lastFiveMothsDates) =
      dates
        .filter(d =>
          !d.isBefore(interval.from.toLocalDate) &&
            !d.isAfter(interval.to.toLocalDate)
        )
        .partition(d => !small.from.toLocalDate.isAfter(d))
    val lastMonth = lastMonthDates.size / 3
    val lastFiveMonths = lastFiveMothsDates.size / 15
    PercentDiscount((lastFiveMonths + lastMonth) * 1000)
  }

  override def loyaltyDaysWindow = LoyaltyDaysWindow
  override def amountTarget = OfflineBizTarget
  override def loyaltyTarget = OfflineBizTarget
}
