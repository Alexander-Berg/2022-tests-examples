package ru.yandex.realty.seller.convertors

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.proto.WeekDay
import ru.yandex.realty.proto.seller.ProductTypes
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.schedule.{
  ManualSchedulePatch,
  OfferScheduleUpdate,
  ProductSchedulePatch,
  ScheduleEnabledPatch,
  ScheduleMultiPatch,
  ScheduleVisitTimePatch,
  ProductScheduleItem => ModelScheduleItem
}
import ru.yandex.realty.seller.proto.api.schedules.UpdateProductScheduleRequest.{ScheduleOncePolicyUpdate, StatusUpdate}
import ru.yandex.realty.seller.proto.api.schedules.{
  ProductScheduleItem,
  ProductScheduleItems,
  ProductScheduleStatus,
  UpdateOfferSchedulesRequest,
  UpdateOffersSchedulesRequest,
  UpdateProductScheduleRequest
}
import ru.yandex.realty.seller.proto.convertors.ProductScheduleConverter
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.realty.seller.model.product.{ProductTypes => ModelProductType}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ProductScheduleConverterSpec extends SpecBase with SellerModelGenerators {

  private def updateOffer(offerId: String, schedule: Map[DateTime, Seq[WeekDay]]) = {
    val items = schedule.map {
      case (time, schedules) =>
        ProductScheduleItem
          .newBuilder()
          .setStartTime(DateTimeFormat.write(time))
          .addAllDaysOfWeek(schedules.asJava)
          .build()
    }

    UpdateOfferSchedulesRequest
      .newBuilder()
      .setOfferId(offerId)
      .addProductUpdates(
        UpdateProductScheduleRequest
          .newBuilder()
          .setProduct(ProductTypes.PRODUCT_TYPE_RAISING)
          .setStatusUpdate(StatusUpdate.newBuilder().setStatus(ProductScheduleStatus.ENABLED))
          .setScheduleOncePolicyUpdate(
            ScheduleOncePolicyUpdate
              .newBuilder()
              .setManual(ProductScheduleItems.newBuilder().addAllItems(items.asJava))
          )
      )
      .build()
  }

  def expectedUpdate(schedule: Map[DateTime, Seq[WeekDay]], visitTime: DateTime): Seq[ProductSchedulePatch] = {
    Seq(
      ScheduleEnabledPatch(true),
      ManualSchedulePatch(
        schedule.map {
          case (time, days) =>
            ModelScheduleItem(
              time,
              days
            )
        }.toSeq
      ),
      ScheduleVisitTimePatch(Some(visitTime))
    )

  }

  private val AllDays: Seq[WeekDay] = Seq(
    WeekDay.MONDAY,
    WeekDay.TUESDAY,
    WeekDay.WEDNESDAY,
    WeekDay.THURSDAY,
    WeekDay.FRIDAY,
    WeekDay.SATURDAY,
    WeekDay.SUNDAY
  )

  private val nowPlusHour =
    DateTimeUtil
      .now()
      .plusHours(1)
      .withMillisOfSecond(0)
      .withSecondOfMinute(0)
  private val nowPlus2Hours =
    DateTimeUtil
      .now()
      .plusHours(2)
      .withMillisOfSecond(0)
      .withSecondOfMinute(0)
  private val nowMinus2Hours1minute =
    DateTimeUtil
      .now()
      .minusHours(2)
      .minusMinutes(1)
      .withMillisOfSecond(0)
      .withSecondOfMinute(0)
  private val nowMinusHour =
    DateTimeUtil
      .now()
      .minusHours(1)
      .withMillisOfSecond(0)
      .withSecondOfMinute(0)

  private val offerId1 = readableString.next
  private val offerId2 = readableString.next
  private val offerId3 = readableString.next
  private val offerId4 = readableString.next

  private val allDayFromNowPlus1h = Map(nowPlusHour -> AllDays)
  private val todayFromNowPlus2h = Map(nowPlus2Hours -> Seq(AllDays(nowPlus2Hours.getDayOfWeek - 1))) // today
  private val todayFromNowMinus2h1m = Map(nowMinus2Hours1minute -> Seq(AllDays(nowMinus2Hours1minute.getDayOfWeek - 1)))
  private val allDaysFromNowMinus1h = Map(nowMinusHour -> AllDays)

  private val updateRequest = UpdateOffersSchedulesRequest
    .newBuilder()
    .addAllOffersUpdates(
      Iterable(
        updateOffer(offerId1, allDayFromNowPlus1h),
        updateOffer(offerId2, todayFromNowPlus2h),
        updateOffer(offerId3, todayFromNowMinus2h1m),
        updateOffer(offerId4, allDaysFromNowMinus1h)
      ).asJava
    )
    .build()

  "ProductScheduleConverter" must {
    "correctly build offers update" in {

      val update = ProductScheduleConverter.buildOffersUpdates(updateRequest)

      val offer1Update = update.find(_.offerId == offerId1).get
      val offer2Update = update.find(_.offerId == offerId2).get
      val offer3Update = update.find(_.offerId == offerId3).get
      val offer4Update = update.find(_.offerId == offerId4).get

      def extractItems(upd: OfferScheduleUpdate) =
        upd.patches(ModelProductType.Raising).asInstanceOf[ScheduleMultiPatch].patches

      extractItems(offer1Update) should contain theSameElementsAs expectedUpdate(
        allDayFromNowPlus1h,
        nowPlusHour
      )

      extractItems(offer2Update) should contain theSameElementsAs expectedUpdate(
        todayFromNowPlus2h,
        nowPlus2Hours
      )

      extractItems(offer3Update) should contain theSameElementsAs expectedUpdate(
        todayFromNowMinus2h1m,
        nowMinus2Hours1minute.plusWeeks(1)
      )

      extractItems(offer4Update) should contain theSameElementsAs expectedUpdate(
        allDaysFromNowMinus1h,
        nowMinusHour
      )
    }
  }
}
