package ru.auto.salesman.test.model.gens.user

import cats.data.NonEmptySet
import com.github.nscala_time.time.OrderingImplicits._
import org.joda.time._
import org.scalacheck.Gen
import ru.auto.salesman.model.ScheduleInstance
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.schedule.AllowMultipleRescheduleUpsert.SameOrTrue
import ru.auto.salesman.model.user.schedule.{
  IsVisible,
  ProductSchedule,
  ScheduleParameters,
  ScheduleSource
}
import ru.auto.salesman.test.model.gens.ScheduleInstanceGenerators._
import ru.auto.salesman.test.model.gens._
import ru.yandex.vertis.util.time.JodaLocalTimeInterval

import scala.collection.JavaConverters.asScalaSetConverter

trait ProductScheduleModelGenerators extends UserModelGenerators {

  val WeekdaysGen: Gen[Set[Int]] = list(1, 14, Gen.chooseNum(1, 7)).map(_.toSet)

  // mysql truncates millis in schedule parameters fields
  val jodaLocalTimeTruncatedMillisGen: Gen[LocalTime] =
    jodaLocalTimeGen.map(_.withMillisOfSecond(0))

  val jodaLocalTimeIntervalTruncatedMillisGen: Gen[JodaLocalTimeInterval] =
    jodaLocalTimeIntervalGen.map { interval =>
      interval.copy(
        from = interval.from.withMillisOfSecond(0),
        to = interval.to.withMillisOfSecond(0)
      )
    }

  val TimeZoneGen: Gen[DateTimeZone] = Gen
    .oneOf(DateTimeZone.getAvailableIDs.asScala.toSeq)
    .map(timezoneAsOffset)

  private def timezoneAsOffset(timezone: String): DateTimeZone = {
    val offsetMillis =
      DateTimeZone.forID(timezone).getOffset(DateTimeUtils.currentTimeMillis())
    DateTimeZone.forOffsetMillis(offsetMillis)
  }

  val ScheduleOnceAtTimeGen: Gen[ScheduleParameters.OnceAtTime] =
    for {
      weekdays <- WeekdaysGen
      time <- jodaLocalTimeTruncatedMillisGen
      tz <- TimeZoneGen
    } yield ScheduleParameters.OnceAtTime(weekdays, time, tz)

  def scheduleOnceAtDatesGen(
      datesGen: Gen[NonEmptySet[LocalDate]] = nonEmptySetGen(
        localDateInFutureGen
      )
  ): Gen[ScheduleParameters.OnceAtDates] =
    for {
      dates <- datesGen
      time <- jodaLocalTimeTruncatedMillisGen
      tz <- TimeZoneGen
    } yield ScheduleParameters.OnceAtDates(dates, time, tz)

  val ScheduleParametersGen: Gen[ScheduleParameters] =
    Gen.oneOf(ScheduleOnceAtTimeGen, scheduleOnceAtDatesGen())

  val isVisibleGen: Gen[IsVisible] = bool.map(IsVisible)

  def productScheduleGen(
      isVisibleGen: Gen[IsVisible] = isVisibleGen,
      isDeletedGen: Gen[Boolean] = bool,
      expireDateGen: Gen[Option[DateTime]] = Gen.option(dateTimeInFuture()),
      parametersGen: Gen[ScheduleParameters] = ScheduleParametersGen,
      offerIdGen: Gen[AutoruOfferId] = AutoruOfferIdGen,
      allowMultipleRescheduleGen: Gen[Boolean] = bool
  ): Gen[ProductSchedule] =
    for {
      id <- posNum[Int] // because the DB table's column has type INT
      offerId <- offerIdGen
      user <- AutoruUserGen
      product <- OfferProductGen
      updatedAt <- dateTimeInPast()
      parameters <- parametersGen
      isDeleted <- isDeletedGen
      epoch <- dateTimeInPast
      isVisible <- isVisibleGen
      expireDate <- expireDateGen
      customPrice <- optFundsGen
      allowMultipleReschedule <- allowMultipleRescheduleGen
    } yield
      ProductSchedule(
        id,
        offerId,
        user,
        product,
        updatedAt,
        parameters,
        isDeleted,
        epoch,
        isVisible,
        expireDate,
        customPrice,
        allowMultipleReschedule = allowMultipleReschedule,
        prevScheduleId = None
      )

  val ProductScheduleGen: Gen[ProductSchedule] = productScheduleGen()

  def scheduleSourceGen(
      isVisibleGen: Gen[IsVisible] = isVisibleGen
  ): Gen[ScheduleSource] =
    for {
      offerId <- AutoruOfferIdGen
      user <- AutoruUserGen
      product <- OfferProductGen
      parameters <- ScheduleParametersGen
      isVisible <- isVisibleGen
      expireDate <- Gen.option(dateTimeInFuture())
      customPrice <- optFundsGen
    } yield
      ScheduleSource(
        offerId,
        user,
        product,
        parameters,
        isVisible,
        expireDate,
        customPrice,
        allowMultipleReschedule = SameOrTrue,
        prevScheduleId = None
      )

  val ScheduleSourceGen: Gen[ScheduleSource] = scheduleSourceGen()

  val visibleScheduleSourceGen: Gen[ScheduleSource] =
    scheduleSourceGen(IsVisible(true))

  val ProductWithScheduleGen: Gen[(ProductSchedule, ScheduleInstance)] = for {
    id <- Gen.posNum[Long]
    schedule <- ProductScheduleGen
    instance <- ScheduleInstanceGen
  } yield
    (
      schedule.copy(id = id, isDeleted = false),
      instance.copy(scheduleId = id, scheduleUpdateTime = schedule.updatedAt)
    )

}
