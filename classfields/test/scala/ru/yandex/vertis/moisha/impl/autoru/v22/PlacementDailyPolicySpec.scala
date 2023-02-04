package ru.yandex.vertis.moisha.impl.autoru.v22

import org.joda.time.{DateTime, LocalDate}
import org.scalatest.{Matchers, OptionValues, TryValues, WordSpec}
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy.AutoRuRequest
import ru.yandex.vertis.moisha.impl.autoru.model.Categories.{New, Used}
import ru.yandex.vertis.moisha.impl.autoru.model.Products.Placement
import ru.yandex.vertis.moisha.impl.autoru.model.Transports.Cars
import ru.yandex.vertis.moisha.impl.autoru.model.{AutoRuContext, AutoRuOffer, Categories, MarkId, Transports}
import ru.yandex.vertis.moisha.impl.autoru_users.model.RegionId
import ru.yandex.vertis.moisha.model.DateTimeInterval

class PlacementDailyPolicySpec extends WordSpec with Matchers with TryValues with OptionValues {

  import PlacementDailyPolicy.calculator

  private val MassSegment = Some("FORD")
  private val PremiumSegment = Some("AUDI")
  private val LuxurySegment = Some("FERRARI")

  "Placement" should {

    "have duration = 1.day for cars:used" in {
      calculateDuration(
        Cars,
        Used,
        MassSegment,
        offerCreationTs = DateTime.parse("2019-10-28T03:00:00+03:00"),
        offerPlacementDay = None,
        today = LocalDate.parse("2019-10-28"),
        clientRegionId = 1
      ) shouldBe 1
    }

    "have duration = 60.days for not billed yet cars:new mass segment offer" in {
      calculateDuration(
        Cars,
        New,
        MassSegment,
        offerCreationTs = DateTime.parse("2019-10-28T03:00:00+03:00"),
        offerPlacementDay = Some(1),
        today = LocalDate.parse("2019-10-28"),
        clientRegionId = 1
      ) shouldBe 60
    }

    "have duration = 60.days when it's time to prolong mass segment offer" in {
      calculateDuration(
        Cars,
        New,
        MassSegment,
        offerCreationTs = DateTime.parse("2019-07-28T03:00:00+03:00"),
        offerPlacementDay = Some(100),
        today = LocalDate.parse("2019-11-30"),
        clientRegionId = 1
      ) shouldBe 60
    }

    "have duration until Oct, 1 + 60 days for cars:new mass segment offer billed before" in {
      calculateDuration(
        Cars,
        New,
        MassSegment,
        offerCreationTs = DateTime.parse("2019-09-28T03:00:00+03:00"),
        offerPlacementDay = Some(2),
        today = LocalDate.parse("2019-10-28"),
        clientRegionId = 1
      ) shouldBe 33 // from 2019-10-28 to 2019-11-30 (2019-10-01 + 60.days)
    }

    "have duration = 90.days for not billed yet cars:new premium segment offer" in {
      calculateDuration(
        Cars,
        New,
        PremiumSegment,
        offerCreationTs = DateTime.parse("2019-10-28T03:00:00+03:00"),
        offerPlacementDay = Some(1),
        today = LocalDate.parse("2019-10-28"),
        clientRegionId = 1
      ) shouldBe 90
    }

    "have duration until Oct, 1 + 90 days for cars:new premium segment offer billed before" in {
      calculateDuration(
        Cars,
        New,
        PremiumSegment,
        offerCreationTs = DateTime.parse("2019-09-28T03:00:00+03:00"),
        offerPlacementDay = Some(2),
        today = LocalDate.parse("2019-10-28"),
        clientRegionId = 1
      ) shouldBe 63 // from 2019-10-28 to 2019-12-30 (2019-10-01 + 90.days)
    }

    "have duration = 180.days for not billed yet cars:new premium segment offer" in {
      calculateDuration(
        Cars,
        New,
        LuxurySegment,
        offerCreationTs = DateTime.parse("2019-10-28T03:00:00+03:00"),
        offerPlacementDay = Some(1),
        today = LocalDate.parse("2019-10-28"),
        clientRegionId = 1
      ) shouldBe 180
    }

    "have duration until Oct, 1 + 180 days for cars:new premium segment offer billed before" in {
      calculateDuration(
        Cars,
        New,
        LuxurySegment,
        offerCreationTs = DateTime.parse("2019-09-28T03:00:00+03:00"),
        offerPlacementDay = Some(2),
        today = LocalDate.parse("2019-10-28"),
        clientRegionId = 1
      ) shouldBe 153 // from 2019-10-28 to 2020-03-29 (2019-10-01 + 180.days)
    }
  }

  private def calculateDuration(
      transport: Transports.Value,
      category: Categories.Value,
      mark: Option[MarkId],
      offerCreationTs: DateTime,
      offerPlacementDay: Option[Int],
      today: LocalDate,
      clientRegionId: RegionId) = {
    calculator(
      AutoRuRequest(
        Placement,
        AutoRuOffer(price = 0, creationTs = offerCreationTs, transport, category, mark, model = None),
        AutoRuContext(clientRegionId, clientCityId = None, offerPlacementDay, productTariff = None),
        DateTimeInterval(today.toDateTimeAtStartOfDay, today.plusDays(1).toDateTimeAtStartOfDay.minusMillis(1)),
        priceRequestId = None
      )
    ).success.value.duration
  }
}
