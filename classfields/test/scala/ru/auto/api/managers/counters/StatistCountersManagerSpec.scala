package ru.auto.api.managers.counters

import java.time.{LocalDate, ZoneId}
import java.time.temporal.ChronoField

import org.mockito.Mockito
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.auto.api.managers.TestRequestWithId
import ru.auto.api.managers.counters.StatistCountersManager._
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.UserRef
import ru.auto.api.model.gen.StatistModelGenerators._
import ru.auto.api.services.statist.Domain.AutoruPublicDomain
import ru.auto.api.services.statist.StatistClient
import ru.auto.api.util.TimeUtils.RichZonedDateTime
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vertis.statist.model.api.ApiModel.{MultipleCompositeValues, MultipleDailyValues}

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext

/**
  *
  * @author zvez
  */
//noinspection TypeAnnotation
class StatistCountersManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with TestRequestWithId {

  class Test {
    val statist = mock[StatistClient]

    val manager =
      new StatistCountersManager(statist)(ExecutionContext.global)
  }

  "StatistCountersManager.getCounters" should {

    "return values from statist" in {
      forAll(OfferGen, objectCompositeValues(AllComponents)) { (offer, statistValue) =>
        new Test {
          val statistResponse =
            MultipleCompositeValues.newBuilder().putObjects(offer.id.id.toString, statistValue).build()
          val componentStatistValue = statistValue.getComponentsOrThrow(CardViewComponent)

          when(statist.getCounterMultiComponentValues(?, ?, ?, ?, ?)(?)).thenReturnF(statistResponse)

          val res = manager
            .getCounters(Seq(offer), includePhoneShows = false, useFreshDate = false, forceUseRealCreationDate = false)
            .futureValue

          res.values.head.getAll shouldBe componentStatistValue.getTotal
          res.values.head.getDaily shouldBe componentStatistValue.getToday

          Mockito
            .verify(statist)
            .getCounterMultiComponentValues(
              eqq(AutoruPublicDomain),
              eqq(EventTypePerCardByDay),
              eqq(
                Set(
                  CardViewComponent,
                  FavoriteComponent,
                  FavoriteRemoveComponent,
                  AvitoCardViewComponent,
                  DromCardViewComponent
                )
              ),
              eqq(Set(offer.id.id.toString)),
              eqq(offer.realCreationDate.withTimeAtStartOfTheDay())
            )(?)
        }
      }
    }
  }

  "StatistCountersManager.getCountersByDay" should {

    val stateGen = for {
      offerIds <- OfferIdsGen
      ids = offerIds.map(_.id.toString)
      days <- Gen.choose(1, 10)
      values <- dailyCounterMultipleValues(days, ids, AllComponents)
    } yield (offerIds, LocalDate.now.minusDays(days - 1), values)

    "transform statist model to autoru one" in {
      forAll(stateGen) {
        case (ids, from, statistValues) =>
          new Test {
            when(statist.getCounterMultiComponentValuesByDay(?, ?, eqq(AllComponents), ?, ?, ?)(?))
              .thenReturnF(statistValues)

            val result = manager.getCountersByDay(ApiOfferModel.Category.CARS, ids, from, LocalDate.now()).futureValue

            result.keySet should contain theSameElementsAs ids.toSet

            result.foreach {
              case (id, dailyValues) =>
                dailyValues.foreach { dayValue =>
                  val statistDayValue = statistDayValueValue(statistValues, dayValue.getDate, id.id.toString)
                  dayValue.getViews shouldBe statistDayValue(CardViewComponent)
                  dayValue.getPhoneViews shouldBe statistDayValue(PhoneShowComponent)

                  dayValue.getAvitoViews shouldBe statistDayValue(AvitoCardViewComponent)
                  dayValue.getAvitoPhoneViews shouldBe statistDayValue(AvitoPhoneShowComponent)

                  dayValue.getDromViews shouldBe statistDayValue(DromCardViewComponent)
                  dayValue.getDromPhoneViews shouldBe statistDayValue(DromPhoneShowComponent)
                }
            }
          }
      }
    }
  }

  "StatistCountersManager.getDealerDailyStatsV2" should {

    val dealerId = DealerUserRefGen.next
    val dealerIdStr = dealerId.clientId.toString
    val days = Gen.choose(1, 30).next
    val statistResponseGen = dailyCounterMultipleValues(days, Seq(dealerIdStr), AllComponents)
    val to = LocalDate.now()
    val from = to.minusDays(days)

    "transform statist model to autoru one" in {
      forAll(statistResponseGen) { statistResponse =>
        new Test {
          when(statist.getCounterMultiComponentValuesByDayWithFilters(?, ?, eqq(AllComponents), ?, ?, ?, ?)(?))
            .thenReturnF(statistResponse)

          val result = manager.getDealerDailyStats(dealerId, None, None, from, to).futureValue

          result.getDaysCount shouldBe days
          result.getDaysList.asScala.foreach { dayStats =>
            val dayValue = statistDayValueValue(statistResponse, dayStats.getDate, dealerIdStr)

            dayStats.getCardView shouldBe dayValue(CardViewComponent)
            dayStats.getPhoneShow shouldBe dayValue(PhoneShowComponent)

            dayStats.getAvitoCardView shouldBe dayValue(AvitoCardViewComponent)
            dayStats.getAvitoPhoneShow shouldBe dayValue(AvitoPhoneShowComponent)

            dayStats.getDromCardView shouldBe dayValue(DromCardViewComponent)
            dayStats.getDromPhoneShow shouldBe dayValue(DromPhoneShowComponent)
          }

          Mockito
            .verify(statist)
            .getCounterMultiComponentValuesByDayWithFilters(
              eqq(AutoruPublicDomain),
              eqq(DealerCounterNameV2),
              eqq(AllComponents),
              eqq(Set(dealerIdStr)),
              ?,
              eqq(from),
              eqq(Some(to.plusDays(1)))
            )(?)
          Mockito.verifyNoMoreInteractions(statist)
        }
      }
    }

  }

  "StatistCountersManager.getDealerWarehouseDailyState" should {

    val dealerId = DealerUserRefGen.next
    val dealerIdStr = dealerId.clientId.toString
    val days = Gen.choose(1, 30).next
    val statistResponseGen = dailyCounterMultipleValues(days, Seq(dealerIdStr), WarehouseComponents)
    val to = LocalDate.now()
    val from = to.minusDays(days)

    "transform statist model to autoru one" in {
      forAll(statistResponseGen) { statistResponse =>
        new Test {
          when(statist.getCounterMultiComponentValuesByDayWithFilters(?, ?, eqq(WarehouseComponents), ?, ?, ?, ?)(?))
            .thenReturnF(statistResponse)

          val result = manager.getDealerWarehouseDailyState(dealerId, None, None, from, to).futureValue

          result.getDaysCount shouldBe days
          result.getDaysList.asScala.foreach { dayStats =>
            val dayValue = statistDayValueValue(statistResponse, dayStats.getDate, dealerIdStr)

            dayStats.getAutoruActive shouldBe dayValue(AutoruActiveComponent)
            dayStats.getAutoruInactive shouldBe dayValue(AutoruInactiveComponent)
            dayStats.getAutoruRemoved shouldBe dayValue(AutoruRemovedComponent)

            dayStats.getAvitoActive shouldBe dayValue(AvitoActiveComponent)
            dayStats.getAvitoInactive shouldBe dayValue(AvitoInactiveComponent)
            dayStats.getAvitoRemoved shouldBe dayValue(AvitoRemovedComponent)

            dayStats.getDromActive shouldBe dayValue(DromActiveComponent)
            dayStats.getDromInactive shouldBe dayValue(DromInactiveComponent)
            dayStats.getDromRemoved shouldBe dayValue(DromRemovedComponent)

            dayStats.getTotalActive shouldBe dayValue(TotalActiveComponent)
            dayStats.getTotalInactive shouldBe dayValue(TotalInactiveComponent)
            dayStats.getTotalRemoved shouldBe dayValue(TotalRemovedComponent)
          }

          Mockito
            .verify(statist)
            .getCounterMultiComponentValuesByDayWithFilters(
              eqq(AutoruPublicDomain),
              eqq(DealerWarehouseCounterName),
              eqq(WarehouseComponents),
              eqq(Set(dealerIdStr)),
              ?,
              eqq(from),
              eqq(Some(to.plusDays(1)))
            )(?)
          Mockito.verifyNoMoreInteractions(statist)
        }
      }
    }

  }

  "StatistCountersManager.getDealerActiveUniqueOffersCounter" should {

    val dealerId = DealerUserRefGen.next
    val dealerIdStr = dealerId.clientId.toString
    val days = Gen.choose(1, 30).next
    val statistResponseGen = objectCompositeValues(UniqueOfferCounterComponents)
    val to = LocalDate.now()
    val from = to.minusDays(days)

    "transform statist model to autoru one" in {
      forAll(statistResponseGen) { statistValue =>
        new Test {
          val statistResponse =
            MultipleCompositeValues.newBuilder().putObjects(dealerId.clientId.toString, statistValue).build()

          when(
            statist.getCounterMultiComponentValuesWithFilters(?, ?, eqq(UniqueOfferCounterComponents), ?, ?, ?, ?)(?)
          ).thenReturnF(statistResponse)

          val result = manager.getDealerUniqueOffersCounter(dealerId, None, None, from, to).futureValue

          val autoruActiveStatistValue = statistValue.getComponentsOrThrow(AutoruUniqueActiveComponent)
          val autoruInactiveStatistValue = statistValue.getComponentsOrThrow(AutoruUniqueInactiveComponent)

          val avitoActiveStatistValue = statistValue.getComponentsOrThrow(AvitoUniqueActiveComponent)
          val avitoInactiveStatistValue = statistValue.getComponentsOrThrow(AvitoUniqueInactiveComponent)

          val dromActiveStatistValue = statistValue.getComponentsOrThrow(DromUniqueActiveComponent)
          val dromInactiveStatistValue = statistValue.getComponentsOrThrow(DromUniqueInactiveComponent)

          val totalActiveStatistValue = statistValue.getComponentsOrThrow(TotalUniqueActiveComponent)
          val totalInactiveStatistValue = statistValue.getComponentsOrThrow(TotalUniqueInactiveComponent)
          val totalRemovedStatistValue = statistValue.getComponentsOrThrow(TotalUniqueRemovedComponent)

          result.getCounter.getAutoruActive shouldBe (autoruActiveStatistValue.getTotal - autoruActiveStatistValue.getToday)
          result.getCounter.getAutoruInactive shouldBe (autoruInactiveStatistValue.getTotal - autoruInactiveStatistValue.getToday)
          result.getCounter.getAvitoActive shouldBe (avitoActiveStatistValue.getTotal - avitoActiveStatistValue.getToday)
          result.getCounter.getAvitoInactive shouldBe (avitoInactiveStatistValue.getTotal - avitoInactiveStatistValue.getToday)
          result.getCounter.getDromActive shouldBe (dromActiveStatistValue.getTotal - dromActiveStatistValue.getToday)
          result.getCounter.getDromInactive shouldBe (dromInactiveStatistValue.getTotal - dromInactiveStatistValue.getToday)
          result.getCounter.getTotalActive shouldBe (totalActiveStatistValue.getTotal - totalActiveStatistValue.getToday)
          result.getCounter.getTotalInactive shouldBe (totalInactiveStatistValue.getTotal - totalInactiveStatistValue.getToday)
          result.getCounter.getTotalRemoved shouldBe (totalRemovedStatistValue.getTotal - totalRemovedStatistValue.getToday)

          Mockito
            .verify(statist)
            .getCounterMultiComponentValuesWithFilters(
              eqq(AutoruPublicDomain),
              eqq(DealerUniqueOffersCounterName),
              eqq(UniqueOfferCounterComponents),
              eqq(Set(dealerIdStr)),
              ?,
              eqq(from),
              eqq(Some(to.plusDays(1)))
            )(?)
          Mockito.verifyNoMoreInteractions(statist)
        }
      }
    }

  }

  "StatistCountersManager.startDate" should {
    "return real counters for owner" in {
      forAll(OfferGen, instantInPast, BooleanGen) { (rawOffer, countersStartDate, useFreshDate) =>
        new Test {
          val requestUser = UserRef.user(1).toString
          val realCreationDate = countersStartDate.minusSeconds(3600)

          val offer = rawOffer.updated { b =>
            b.setUserRef(requestUser)

            b.getAdditionalInfoBuilder
              .setCountersStartDate(countersStartDate.toEpochMilli)
              .setCreationDate(realCreationDate.toEpochMilli)
          }

          val expectedDate = realCreationDate
            .atZone(ZoneId.systemDefault())
            .withTimeAtStartOfTheDay()

          manager.startDate(Seq(offer), useFreshDate) shouldBe expectedDate
        }
      }
    }

    "return real counters for other users if countersStartDate is empty" in {
      forAll(OfferGen, instantInPast, BooleanGen) { (rawOffer, realCreationDate, useFreshDate) =>
        new Test {
          val otherUser = UserRef.user(2).toString

          val offer = rawOffer.updated { b =>
            b.setUserRef(otherUser)

            b.getAdditionalInfoBuilder
              .setCreationDate(realCreationDate.toEpochMilli)
              .clearCountersStartDate()
              .clearFreshDate()
          }

          val expectedDate = realCreationDate
            .atZone(ZoneId.systemDefault())
            .withTimeAtStartOfTheDay()

          manager.startDate(Seq(offer), useFreshDate) shouldBe expectedDate
        }
      }
    }

    "return countersStartDate for other users" in {
      forAll(OfferGen, instantInPast, BooleanGen) { (rawOffer, countersStartDate, useFreshDate) =>
        new Test {
          val otherUser = UserRef.user(2).toString
          val realCreationDate = countersStartDate.minusSeconds(3600)

          val offer = rawOffer.updated { b =>
            b.setUserRef(otherUser)

            b.getAdditionalInfoBuilder
              .setCountersStartDate(countersStartDate.toEpochMilli)
              .setCreationDate(realCreationDate.toEpochMilli)
          }

          val expectedDate = countersStartDate
            .atZone(ZoneId.systemDefault())
            .`with`(ChronoField.MILLI_OF_SECOND, countersStartDate.get(ChronoField.MILLI_OF_SECOND))

          manager.startDate(Seq(offer), useFreshDate) shouldBe expectedDate
        }
      }
    }
  }

  "StatistCountersManager.getCountersStartDate" should {
    "return countersStartDate if exists" in {
      forAll(OfferGen, instantInPast, BooleanGen) { (rawOffer, date, useFreshDate) =>
        new Test {
          val timestamp = date.toEpochMilli

          val offer = rawOffer.updated { b =>
            b.userRef

            b.getAdditionalInfoBuilder
              .setCountersStartDate(date.toEpochMilli)
              .setFreshDate(date.plusSeconds(60).toEpochMilli)
          }

          manager.getCountersStartDate(offer, useFreshDate) shouldBe Some(timestamp)
        }
      }
    }

    "return freshDate if allowed and empty countersStartDate" in {
      forAll(OfferGen, instantInPast) { (rawOffer, date) =>
        new Test {
          val timestamp = date.toEpochMilli

          val offer = rawOffer.updated { b =>
            b.getAdditionalInfoBuilder
              .setFreshDate(date.toEpochMilli)
              .clearCountersStartDate()
          }

          manager.getCountersStartDate(offer, useFreshDate = false) shouldBe None
          manager.getCountersStartDate(offer, useFreshDate = true) shouldBe Some(timestamp)
        }
      }
    }

    "return none on empty countersStartDate and empty freshDate" in {
      forAll(OfferGen, BooleanGen) { (rawOffer, useFreshDate) =>
        new Test {
          val offer = rawOffer.updated { b =>
            b.getAdditionalInfoBuilder
              .clearFreshDate()
              .clearCountersStartDate()
          }

          manager.getCountersStartDate(offer, useFreshDate) shouldBe None
        }
      }
    }
  }

  def statistDayValueValue(vs: MultipleDailyValues, day: String, id: String): Map[String, Int] =
    vs.getObjectsOrThrow(id)
      .getDaysList
      .asScala
      .find(_.getDay == day)
      .get
      .getComponentsMap
      .asScala
      .toMap
      .view
      .mapValues(_.toInt)
      .toMap
}
