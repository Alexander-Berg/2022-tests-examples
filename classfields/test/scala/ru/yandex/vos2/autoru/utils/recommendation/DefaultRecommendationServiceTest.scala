package ru.yandex.vos2.autoru.utils.recommendation

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.calltracking.proto.Model
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.statist.model.api.ApiModel
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Offer, OfferStatusHistoryItem}
import ru.yandex.vos2.autoru.model.TestUtils.createOffer
import ru.yandex.vos2.autoru.services.dealer_pony.DealerPonyClient
import ru.yandex.vos2.autoru.utils.recommendation.Recommendation._
import ru.yandex.vos2.services.statist.StatistClient
import ru.yandex.vos2.util.Dates.instantToTimestamp

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

@RunWith(classOf[JUnitRunner])
class DefaultRecommendationServiceTest extends AnyWordSpec with Matchers with OptionValues {

  "DefaultRecommendationService" should {
    implicit val category: Category = Category.CARS
    implicit val section: Section = Section.USED

    "return no recommendations for good offer" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result shouldBe empty
    }

    "not return NoCalls when last call was 10 days ago" in new Wiring with TestData {
      val offer: Offer = offer(lastCallDaysAgo = 10)
      setupDefaultMocks(offer)
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result shouldBe empty
    }

    "return NoCalls when last call was 11 days ago" in new Wiring with TestData {
      val offer: Offer = offer(lastCallDaysAgo = 11)
      setupDefaultMocks(offer)
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result should contain(NoCalls)
    }

    "not return PhoneViewsBad when phone views are 9.9% less than average" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      setupViewStatistics(component = "phone_show", viewsForOffer = 901, averageViewsPerOffer = 1000)
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result shouldBe empty
    }

    "return PhoneViewsBad when phone views are 10.1% less than average" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      setupViewStatistics(component = "phone_show", viewsForOffer = 899, averageViewsPerOffer = 1000)
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result should contain(PhoneViewsBad)
    }

    "only take last 7 days into account when comparing to average counts" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      setupViewStatistics(
        component = "phone_show",
        viewsForOfferLastWeek = Some(899),
        viewsForOfferPreviousWeek = Some(10000),
        viewsForOffer = 10000,
        averageViewsPerOffer = 1000
      )
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result should contain(PhoneViewsBad)
    }

    "not return ViewsBad when offer views are 9.9% less than average" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      setupViewStatistics(component = "card_view", viewsForOffer = 901, averageViewsPerOffer = 1000)
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result shouldBe empty
    }

    "return ViewsBad when offer views are 10.1% less than average" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      setupViewStatistics(component = "card_view", viewsForOffer = 899, averageViewsPerOffer = 1000)
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result should contain(ViewsBad)
    }

    "not return PhoneViewsDrop when phone views dropped by 29.9% last week" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      setupViewStatistics(
        component = "phone_show",
        viewsForOfferLastWeek = Some(701),
        viewsForOfferPreviousWeek = Some(1000)
      )
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result shouldBe empty
    }

    "return PhoneViewsDrop when phone views dropped by 30.1% last week" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      setupViewStatistics(
        component = "phone_show",
        viewsForOfferLastWeek = Some(699),
        viewsForOfferPreviousWeek = Some(1000)
      )
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result should contain(PhoneViewsDrop)
    }

    "not return ViewsDrop when offer views dropped by 29.9% last week" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      setupViewStatistics(
        component = "card_view",
        viewsForOfferLastWeek = Some(701),
        viewsForOfferPreviousWeek = Some(1000)
      )
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result shouldBe empty
    }

    "return ViewsDrop when offer views dropped by 30.1% last week" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      setupViewStatistics(
        component = "card_view",
        viewsForOfferLastWeek = Some(699),
        viewsForOfferPreviousWeek = Some(1000)
      )
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result should contain(ViewsDrop)
    }

    "not return NoCalls recommendation if offer was activated only 10 days ago" in new Wiring with TestData {
      val offer: Offer = offer(lastCallDaysAgo = 15, history = activatedHistory(daysAgo = 10))
      setupDefaultMocks(offer)
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result shouldBe empty
    }

    "not return NoCalls recommendation if call tracking is disabled" in new Wiring with TestData {
      val offer: Offer = offer(lastCallDaysAgo = 15)
      setupDefaultMocks(offer)
      setupDealerPonyMock(calltrackingEnabled = false)
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result shouldBe empty
    }

    "return empty response for unknown category" in new Wiring with TestData {
      val offer: Offer = offer(lastCallDaysAgo = 15)(Category.CATEGORY_UNKNOWN, Section.USED)
      setupDefaultMocks(offer)
      setupVeryBadStatistics()
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result shouldBe empty
    }

    "return empty response for unknown section" in new Wiring with TestData {
      val offer: Offer = offer(lastCallDaysAgo = 15)(Category.CARS, Section.SECTION_UNKNOWN)
      setupDefaultMocks(offer)
      setupVeryBadStatistics()
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result shouldBe empty
    }

    "fail for offer with non-dealer userRef" in new Wiring with TestData {
      val offer: Offer = offer(dealer = false)
      setupDefaultMocks(offer)
      val result: Try[Seq[Recommendation]] = service.getRecommendations(offer)
      result.isFailure shouldBe true
    }

    "fail for not activated offer" in new Wiring with TestData {
      val offer: Offer = offer(history = neverActivatedHistory)
      setupDefaultMocks(offer)
      val result: Try[Seq[Recommendation]] = service.getRecommendations(offer)
      result.isFailure shouldBe true
    }

    "fail when dealerPony responds with error" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      setupDealerPonyError()
      val result: Try[Seq[Recommendation]] = service.getRecommendations(offer)
      result.isFailure shouldBe true
    }

    "fail when statist responds with error" in new Wiring with TestData {
      val offer: Offer = offer()
      setupDefaultMocks(offer)
      setupStatistError()
      val result: Try[Seq[Recommendation]] = service.getRecommendations(offer)
      result.isFailure shouldBe true
    }

    "ignore technical deactivations" in new Wiring with TestData {
      val offer: Offer = offer(lastCallDaysAgo = 15, history = historyWithTechnicalDeactivation(daysAgo = 4))
      setupDefaultMocks(offer)
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result should contain(NoCalls)
    }

    "ignore days with zero offer count" in new Wiring with TestData {
      val offer: Offer = offer(lastCallDaysAgo = 15)
      setupDefaultMocks(offer)
      setupVeryBadStatistics()
      setupOfferStatisticsWithZeroes(offer)
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result.toSet shouldBe Set(NoCalls, PhoneViewsBad, PhoneViewsDrop, ViewsBad, ViewsDrop)
    }

    "return no recommendations if offer was activated today" in new Wiring with TestData {
      val offer: Offer = offer(lastCallDaysAgo = 15, history = activatedHistory(daysAgo = 0))
      setupDefaultMocks(offer)
      setupVeryBadStatistics()
      val result: Seq[Recommendation] = service.getRecommendations(offer).get
      result shouldBe empty
    }
  }

  "DefaultRecommendationService" when {

    "offer is for a used car" should {
      implicit val category: Category = Category.CARS
      implicit val section: Section = Section.USED

      "return all recommendations for very bad offer" in new Wiring with TestData {
        val offer: Offer = offer(lastCallDaysAgo = 15)
        setupDefaultMocks(offer)
        setupVeryBadStatistics()
        val result: Seq[Recommendation] = service.getRecommendations(offer).get
        result.toSet shouldBe Set(NoCalls, PhoneViewsBad, PhoneViewsDrop, ViewsBad, ViewsDrop)
      }

      "call statist for offer count only once" in new Wiring with TestData {
        val offer: Offer = offer(lastCallDaysAgo = 15)
        setupDefaultMocks(offer)
        setupVeryBadStatistics()
        service.getRecommendations(offer).get

        verify(statistClient, times(1)).getCounterPlainByDayValues(
          eq("autoru_vsdealers"),
          eq("category_per_offer_by_day"),
          ?,
          ?,
          ?,
          ?
        )(?)
      }

      "call statist twice for every event_type: for offer and for average counts" in new Wiring with TestData {
        val offer: Offer = offer(lastCallDaysAgo = 15)
        setupDefaultMocks(offer)
        setupVeryBadStatistics()
        service.getRecommendations(offer).get

        Seq("card_view", "phone_show").foreach { eventType =>
          verify(statistClient, times(2)).getCounterPlainByDayValues(
            ?,
            ?,
            eq(eventType),
            ?,
            ?,
            ?
          )(?)
        }
      }
    }

    "offer is for a new car" should {
      implicit val category: Category = Category.CARS
      implicit val section: Section = Section.NEW

      "return only (NoCalls, PhoneViewsBad, PhoneViewsDrop) for very bad offer" in new Wiring with TestData {
        val offer: Offer = offer(lastCallDaysAgo = 15)
        setupDefaultMocks(offer)
        setupVeryBadStatistics()
        val result: Seq[Recommendation] = service.getRecommendations(offer).get
        result.toSet shouldBe Set(NoCalls, PhoneViewsBad, PhoneViewsDrop)
      }

      "call statist for phone_show but not card_view event count" in new Wiring with TestData {
        val offer: Offer = offer(lastCallDaysAgo = 15)
        setupDefaultMocks(offer)
        setupVeryBadStatistics()
        service.getRecommendations(offer).get

        verify(statistClient, atLeastOnce).getCounterPlainByDayValues(
          ?,
          ?,
          eq("phone_show"),
          ?,
          ?,
          ?
        )(?)

        verify(statistClient, never).getCounterPlainByDayValues(
          ?,
          ?,
          eq("card_view"),
          ?,
          ?,
          ?
        )(?)
      }
    }

    "offer is for a moto bike" should {
      implicit val category: Category = Category.MOTO
      implicit val section: Section = Section.USED

      "return only (PhoneViewsDrop, ViewsDrop) for very bad offer" in new Wiring with TestData {
        val offer: Offer = offer(lastCallDaysAgo = 15)
        setupDefaultMocks(offer)
        setupVeryBadStatistics()
        val result: Seq[Recommendation] = service.getRecommendations(offer).get
        result.toSet shouldBe Set(PhoneViewsDrop, ViewsDrop)
      }

      "call statist for offer counts but not average counts" in new Wiring with TestData {
        val offer: Offer = offer(lastCallDaysAgo = 15)
        setupDefaultMocks(offer)
        setupVeryBadStatistics()
        service.getRecommendations(offer).get

        verify(statistClient, atLeastOnce).getCounterPlainByDayValues(
          eq("autoru_public"),
          eq("event_type_per_dealer_card_category_section_by_day"),
          ?,
          ?,
          ?,
          ?
        )(?)

        verify(statistClient, never).getCounterPlainByDayValues(
          eq("autoru_vsdealers"),
          eq("category_per_offer_by_day"),
          ?,
          ?,
          ?,
          ?
        )(?)

        verify(statistClient, never).getCounterPlainByDayValues(
          eq("autoru_public"),
          eq("event_type_per_mark_model_generation_region_by_day"),
          ?,
          ?,
          ?,
          ?
        )(?)
      }
    }

    "offer is for a truck" should {
      implicit val category: Category = Category.TRUCKS
      implicit val section: Section = Section.USED

      "return only (PhoneViewsDrop, ViewsDrop) for very bad offer" in new Wiring with TestData {
        val offer: Offer = offer(lastCallDaysAgo = 15)
        setupDefaultMocks(offer)
        setupVeryBadStatistics()
        val result: Seq[Recommendation] = service.getRecommendations(offer).get
        result.toSet shouldBe Set(PhoneViewsDrop, ViewsDrop)
      }

      "do not call dealerPony because it is needed only for NoCalls recommendation" in new Wiring with TestData {
        val offer: Offer = offer(lastCallDaysAgo = 15)
        setupDefaultMocks(offer)
        setupVeryBadStatistics()
        service.getRecommendations(offer).get

        verifyNoMoreInteractions(dealerPonyClient)
      }

      "call statist for offer counts but not average counts" in new Wiring with TestData {
        val offer: Offer = offer(lastCallDaysAgo = 15)
        setupDefaultMocks(offer)
        setupVeryBadStatistics()
        service.getRecommendations(offer).get

        verify(statistClient, atLeastOnce).getCounterPlainByDayValues(
          eq("autoru_public"),
          eq("event_type_per_dealer_card_category_section_by_day"),
          ?,
          ?,
          ?,
          ?
        )(?)

        verify(statistClient, never).getCounterPlainByDayValues(
          eq("autoru_vsdealers"),
          eq("category_per_offer_by_day"),
          ?,
          ?,
          ?,
          ?
        )(?)

        verify(statistClient, never).getCounterPlainByDayValues(
          eq("autoru_public"),
          eq("event_type_per_mark_model_generation_region_by_day"),
          ?,
          ?,
          ?,
          ?
        )(?)
      }
    }
  }

  trait Wiring extends MockitoSupport { self: TestData =>
    val dealerPonyClient: DealerPonyClient = mock[DealerPonyClient]
    val statistClient: StatistClient = mock[StatistClient]

    val service: DefaultRecommendationService =
      new DefaultRecommendationService(dealerPonyClient, statistClient)

    implicit val trace: Traced = Traced.empty

    private val OffersPerDay = 500

    def setupDefaultMocks(offer: Offer): Unit = {
      setupDealerPonyMock()
      setupOfferStatistics(offer)
      setupViewStatistics("card_view")
      setupViewStatistics("phone_show")
    }

    def setupVeryBadStatistics(): Unit = {
      setupViewStatistics(
        component = "phone_show",
        viewsForOfferLastWeek = Some(10),
        viewsForOfferPreviousWeek = Some(100),
        viewsForOffer = 200,
        averageViewsPerOffer = 1000
      )
      setupViewStatistics(
        component = "card_view",
        viewsForOfferLastWeek = Some(10),
        viewsForOfferPreviousWeek = Some(100),
        viewsForOffer = 200,
        averageViewsPerOffer = 1000
      )
    }

    def setupDealerPonyMock(calltrackingEnabled: Boolean = true): Unit =
      when(dealerPonyClient.getCalltrackingSettings(?)(?)).thenReturn {
        Success(calltrackingSettings(calltrackingEnabled))
      }

    def setupOfferStatistics(offer: Offer): Unit =
      setupStatistCall(
        domain = "autoru_vsdealers",
        counter = "category_per_offer_by_day",
        offer.getOfferAutoru.getCategory.name().toLowerCase,
        OffersPerDay
      )

    def setupOfferStatisticsWithZeroes(offer: Offer): Unit =
      setupStatistCall(
        domain = "autoru_vsdealers",
        counter = "category_per_offer_by_day",
        offer.getOfferAutoru.getCategory.name().toLowerCase,
        size =>
          Stream
            .range(0, size)
            .map {
              case i if i % 5 == 0 => 0
              case _ => OffersPerDay
            }
            .reverse
            .toList
      )

    def setupViewStatistics(
        component: String,
        averageViewsPerOffer: Int = 700,
        viewsForOffer: Int = 700,
        viewsForOfferLastWeek: Option[Int] = None,
        viewsForOfferPreviousWeek: Option[Int] = None
    ): Unit = {
      setupStatistCall(
        domain = "autoru_public",
        counter = "event_type_per_mark_model_generation_region_by_day",
        component,
        averageViewsPerOffer * OffersPerDay
      )

      setupStatistCall(
        domain = "autoru_public",
        counter = "event_type_per_dealer_card_category_section_by_day",
        component,
        size =>
          Stream
            .concat(
              Stream.fill(7)(viewsForOfferLastWeek.getOrElse(viewsForOffer)),
              Stream.fill(7)(viewsForOfferPreviousWeek.getOrElse(viewsForOffer)),
              Stream.fill(size)(viewsForOffer)
            )
            .take(size)
            .reverse
            .toList
      )
    }

    private def setupStatistCall(domain: String, counter: String, component: String, value: Int): Unit =
      setupStatistCall(
        domain,
        counter,
        component,
        size => Stream.fill(size)(value).toList
      )

    private def setupStatistCall(domain: String, counter: String, component: String, valueFun: Int => List[Int]): Unit =
      when(
        statistClient.getCounterPlainByDayValues(eq(domain), eq(counter), eq(component), ?, ?, ?)(?)
      ).thenAnswer { invocation =>
        val from = invocation.getArgument[LocalDate](4)
        val until = invocation.getArgument[Option[LocalDate]](5).value
        val values = valueFun((until.toEpochDay - from.toEpochDay).toInt)
        dailyValues(from, until, component, day => values((day.toEpochDay - from.toEpochDay).toInt))
      }

    def setupDealerPonyError(): Unit =
      when(dealerPonyClient.getCalltrackingSettings(?)(?)).thenReturn {
        Failure(new RuntimeException("DealerPony responds with an error in test"))
      }

    def setupStatistError(): Unit =
      when(statistClient.getCounterPlainByDayValues(?, ?, ?, ?, ?, ?)(?)).thenThrow {
        new RuntimeException("Statist responds with an error in test")
      }

  }

  trait TestData {

    private val HistoryOffsetDays: Int = 30
    private val dayZero: ZonedDateTime = ZonedDateTime.now().minusDays(HistoryOffsetDays)

    def offer(
        dealer: Boolean = true,
        lastCallDaysAgo: Int = 1,
        history: Seq[OfferStatusHistoryItem] = regularStatusHistory
    )(implicit category: Category, section: Section): Offer = {
      val offer = createOffer(
        dealer = dealer,
        category = category,
        now = dayZero.toInstant.toEpochMilli
      )

      offer.getOfferAutoruBuilder
        .setSection(section)
        .setLastRelevantCall(ZonedDateTime.now().minusDays(lastCallDaysAgo).toInstant)

      offer
        .clearStatusHistory()
        .addAllStatusHistory(history.asJava)
        .build()
    }

    def regularStatusHistory: Seq[OfferStatusHistoryItem] = historyOf(
      CompositeStatus.CS_DRAFT -> 0,
      CompositeStatus.CS_NEED_ACTIVATION -> 1,
      CompositeStatus.CS_ACTIVE -> 2
    )

    def activatedHistory(daysAgo: Int): Seq[OfferStatusHistoryItem] = historyOf(
      CompositeStatus.CS_DRAFT -> 0,
      CompositeStatus.CS_NEED_ACTIVATION -> 1,
      CompositeStatus.CS_ACTIVE -> (HistoryOffsetDays - daysAgo)
    )

    def neverActivatedHistory: Seq[OfferStatusHistoryItem] = historyOf(
      CompositeStatus.CS_DRAFT -> 0,
      CompositeStatus.CS_NEED_ACTIVATION -> 1
    )

    def historyWithTechnicalDeactivation(daysAgo: Int): Seq[OfferStatusHistoryItem] = historyOf(
      CompositeStatus.CS_DRAFT -> 0,
      CompositeStatus.CS_NEED_ACTIVATION -> 1,
      CompositeStatus.CS_ACTIVE -> 2,
      CompositeStatus.CS_INACTIVE -> (HistoryOffsetDays - daysAgo),
      CompositeStatus.CS_ACTIVE -> (HistoryOffsetDays - daysAgo + 1)
    )

    private def historyOf(items: (CompositeStatus, Int)*): Seq[OfferStatusHistoryItem] =
      items.map {
        case (status, dayNumber) =>
          OfferStatusHistoryItem
            .newBuilder()
            .setOfferStatus(status)
            .setTimestamp(dayZero.plusDays(dayNumber).toInstant.toEpochMilli)
            .build()
      }

    def calltrackingSettings(calltrackingEnabled: Boolean): Model.Settings =
      Model.Settings
        .newBuilder()
        .setOffersStatEnabled(calltrackingEnabled)
        .build()

    def dailyValues(
        from: LocalDate,
        to: LocalDate,
        component: String,
        valueFun: LocalDate => Int
    ): ApiModel.ObjectDailyValues = {
      val days = Stream
        .iterate(from)(_.plusDays(1))
        .takeWhile(_.isBefore(to))
        .map { day =>
          ApiModel.ObjectDayValues
            .newBuilder()
            .setDay(day.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .clearComponents()
            .putComponents(component, valueFun(day))
            .build()
        }
        .asJava
      ApiModel.ObjectDailyValues
        .newBuilder()
        .clearDays()
        .addAllDays(days)
        .build()
    }
  }
}
