package ru.auto.salesman.service.schedules

import cats.data.{NonEmptyList, NonEmptySet}
import org.joda.time.{DateTime, DateTimeZone, LocalDate, LocalTime}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import ru.auto.api.ResponseModel.ResponseStatus
import ru.auto.api.billing.schedules.ScheduleModel.ScheduleResponse.{
  ScheduleParameters,
  ScheduleProducts,
  OnceAtTime => ProtobufOnceAtTime
}
import ru.auto.api.billing.schedules.ScheduleModel.{
  ScheduleRequest,
  ScheduleResponse,
  ScheduleType
}
import ru.auto.salesman.dao.impl.jdbc.user.JdbcProductScheduleDao
import ru.auto.salesman.dao.user.ProductScheduleDao.{ScheduleFilter, ScheduleUpdateFilter}
import ru.auto.salesman.dao.user.ProductScheduleDao.ScheduleFilter._
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Boost
import ru.auto.salesman.model.user.schedule
import ru.auto.salesman.model.user.schedule.AllowMultipleRescheduleUpsert.SameOrTrue
import ru.auto.salesman.model.user.schedule.ScheduleParameters.{OnceAtDates, OnceAtTime}
import ru.auto.salesman.model.user.schedule.{IsVisible, ProductSchedule, ScheduleSource}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.JodaCatsInstances._
import ru.yandex.common.monitoring.CompoundHealthCheckRegistry

class AsyncScheduleCrudServiceSpec extends BaseSpec with ScalaFutures {

  private val scheduleDaoMock = mock[JdbcProductScheduleDao]
  private val healthCheckRegistryMock = new CompoundHealthCheckRegistry

  private val serviceToTest =
    new AsyncScheduleCrudService(scheduleDaoMock, healthCheckRegistryMock)

  private val time = "10:00"
  private val timeZone = "+01:00"
  private val weekday = 1
  private val productScheduleId = 123

  private val testDate = LocalDate.now()

  private val testTime =
    LocalTime.parse(time, AsyncScheduleCrudService.HoursMinutesFormat)

  private val testZone = DateTimeZone.forID(timeZone)

  implicit private val pcfg: PatienceConfig =
    PatienceConfig(scaled(Span(1000, Millis)), scaled(Span(1, Millis)))

  private val scheduleRequest = ScheduleRequest
    .newBuilder()
    .setScheduleType(ScheduleType.ONCE_AT_TIME)
    .addWeekdays(weekday)
    .setTime(time)
    .setTimezone(timeZone)
    .build()

  private val onceAtTime =
    ProtobufOnceAtTime
      .newBuilder()
      .setTime(time)
      .addWeekdays(weekday)
      .build()

  private val scheduleParameters =
    ScheduleParameters
      .newBuilder()
      .setScheduleType(ScheduleType.ONCE_AT_TIME)
      .setTimezone(timeZone)
      .setOnceAtTime(onceAtTime)
      .build()

  private val offer = AutoruOfferId("1-1")
  private val user = AutoruUser(1)

  private val SampleGetResponse =
    ScheduleResponse
      .newBuilder()
      .putOffers(
        offer.toString,
        ScheduleProducts
          .newBuilder()
          .putProducts("all_sale_fresh", scheduleParameters)
          .build()
      )
      .build()

  "AsyncScheduleCrudService#putSchedule" should {
    "works" in {
      (scheduleDaoMock.replace _)
        .expects(
          List(
            ScheduleSource(
              offer,
              user,
              Boost,
              OnceAtTime(
                Set(weekday),
                testTime,
                testZone
              ),
              IsVisible(true),
              expireDate = None,
              customPrice = None,
              allowMultipleReschedule = SameOrTrue,
              prevScheduleId = None
            )
          )
        )
        .returningT(())
      val status = serviceToTest
        .putSchedules(user, Boost, Iterable(offer), scheduleRequest)
        .futureValue
      status.getStatus shouldBe ResponseStatus.SUCCESS
    }
  }

  "AsyncScheduleCrudService#deleteSchedule" should {
    "works" in {
      (scheduleDaoMock.delete _)
        .expects(argAssert {
          (_: Seq[ScheduleUpdateFilter]) should contain theSameElementsAs Seq(
            ForUserRef(user),
            ForOfferIds(NonEmptyList.of(offer)),
            ForProducts(NonEmptyList.of(Boost)),
            Visible,
            NonExpired
          )
        })
        .returningT(())

      val status = serviceToTest
        .deleteSchedules(
          user,
          Some(NonEmptyList.of(Boost)),
          NonEmptyList.of(offer)
        )
        .futureValue
      status.getStatus shouldBe ResponseStatus.SUCCESS
    }
  }

  "AsyncScheduleCrudService#getSchedule" should {

    val testSchedule = ProductSchedule(
      productScheduleId,
      offer,
      user,
      Boost,
      DateTime.now(),
      schedule.ScheduleParameters.OnceAtTime(Set(weekday), testTime, testZone),
      false,
      DateTime.now(),
      IsVisible(true),
      expireDate = None,
      customPrice = None,
      allowMultipleReschedule = true,
      prevScheduleId = None
    )

    "works" in {
      (scheduleDaoMock.get _)
        .expects(argAssert {
          (_: Seq[ScheduleUpdateFilter]) should contain theSameElementsAs Seq(
            ScheduleFilter.ForUserRef(user),
            ScheduleFilter.IsDeleted(false),
            ScheduleFilter.ForProducts(NonEmptyList.of(Boost)),
            ScheduleFilter.ForOfferIds(NonEmptyList.of(offer)),
            ScheduleFilter.Visible,
            ScheduleFilter.NonExpired
          )
        })
        .returningT(Iterable(testSchedule))

      val response = serviceToTest
        .getSchedules(
          user,
          Some(NonEmptyList.of(Boost)),
          Some(NonEmptyList.of(offer))
        )
        .futureValue
      response shouldBe SampleGetResponse
    }

    "works with empty filters" in {
      (scheduleDaoMock.get _)
        .expects(argAssert {
          (_: Seq[ScheduleUpdateFilter]) should contain theSameElementsAs Seq(
            ScheduleFilter.ForUserRef(user),
            ScheduleFilter.IsDeleted(false),
            ScheduleFilter.Visible,
            ScheduleFilter.NonExpired
          )
        })
        .returningT(Iterable(testSchedule))

      val response = serviceToTest
        .getSchedules(user, products = None, offerIds = None)
        .futureValue
      response shouldBe SampleGetResponse
    }

    "ignore once at dates schedule" in {
      val atSpecifiedDates = testSchedule.copy(
        offerId = AutoruOfferId("2-2"),
        scheduleParameters = OnceAtDates(NonEmptySet.one(testDate), testTime, testZone)
      )
      (scheduleDaoMock.get _)
        .expects(*)
        .returningT(List(atSpecifiedDates, testSchedule))
      val response = serviceToTest
        .getSchedules(user, products = None, offerIds = None)
        .futureValue
      response shouldBe SampleGetResponse
    }

    "ignore once at dates schedule for same offer" in {
      val atSpecifiedDates = testSchedule.copy(
        scheduleParameters = OnceAtDates(NonEmptySet.one(testDate), testTime, testZone)
      )
      (scheduleDaoMock.get _)
        .expects(*)
        .returningT(List(atSpecifiedDates, testSchedule))
      val response = serviceToTest
        .getSchedules(user, products = None, offerIds = None)
        .futureValue
      response shouldBe SampleGetResponse
    }
  }
}
