package vertis.statist

import org.joda.time
import org.joda.time.{DateTime, LocalDate, LocalTime}
import org.scalacheck.Gen
import vertis.statist.model._
import ru.yandex.vertis.generators.ProducerProvider._

/** @author zvez
  */
object Generators {

  val LocalDate: Gen[LocalDate] = Gen.choose(-365, 10).map(time.LocalDate.now().plusDays)
  val LocalDateInPast: Gen[LocalDate] = Gen.choose(-365, -1).map(time.LocalDate.now().plusDays)
  val LocalTime: Gen[LocalTime] = Gen.choose(0, 24 * 60 * 60).map(time.LocalTime.MIDNIGHT.plusSeconds)

  val DateTime: Gen[DateTime] = for {
    date <- LocalDate
    time <- LocalTime
  } yield date.toDateTime(time)

  val DateTimeInPast: Gen[DateTime] = for {
    date <- LocalDateInPast
    time <- LocalTime
  } yield date.toDateTime(time)

  val LocalDateOpt: Gen[Option[LocalDate]] = Gen.option(LocalDate)

  val Period: Gen[DatesPeriod] = for {
    from <- Gen.option(DateTime)
    until <- Gen.option(DateTime)
  } yield DatesPeriod(from, until)

  val ReadableString: Gen[String] =
    Gen.choose(5, 10).flatMap(l => Gen.alphaNumChar.next(l).mkString).suchThat(_.nonEmpty)

  val Component: Gen[String] = ReadableString

  val components: Gen[Set[String]] = for {
    n <- Gen.choose(1, 5)
    xs <- Gen.listOfN(n, Component)
  } yield xs.toSet

  val Id: Gen[Id] = ReadableString

  val Ids: Gen[Set[Id]] =
    Gen.choose(1, 10).flatMap(cnt => Gen.listOfN(cnt, Id).map(_.toSet)).suchThat(_.nonEmpty)

  def objectCounterValues(components: Set[Component]): Gen[ObjectCounterValues] =
    Gen
      .listOfN(components.size, Gen.chooseNum(0, 10000))
      .map(xs => ObjectCounterValues(components.zip(xs).toMap))

  def multipleCountersValues(components: Set[Component], ids: Set[Id]): Gen[MultipleCountersValues] =
    Gen
      .listOfN(ids.size, objectCounterValues(components))
      .map(xs => MultipleCountersValues(ids.zip(xs).toMap))

  def objectCompositeValues(components: Set[Component]): Gen[ObjectCompositeValues] =
    for {
      past <- objectCounterValues(components)
      today <- objectCounterValues(components)
    } yield ObjectCompositeValues.build(past, today)

  def multipleCompositeValues(components: Set[Component], ids: Set[Id]): Gen[MultipleCompositeValues] =
    for {
      past <- multipleCountersValues(components, ids)
      today <- multipleCountersValues(components, ids)
    } yield MultipleCompositeValues.build(past, today)

  def multipleDailyValues(components: Set[Component]): Gen[MultipleDailyValues] =
    for {
      ids <- Ids
      values <- Gen.listOfN(ids.size, objectDailyValues(components))
    } yield MultipleDailyValues(ids.zip(values).toMap)

  def objectDailyValues(components: Set[Component]): Gen[ObjectDailyValues] =
    for {
      days <- Gen.listOf(LocalDateInPast)
      values <- Gen.listOfN(days.size, objectCounterValues(components))
    } yield ObjectDailyValues(days.zip(values).toMap)

  def dailyCounterValues(ids: Set[Id]): Gen[DailyCounterValues] = {
    for {
      days <- Gen.listOf(LocalDateInPast)
      values <- Gen.listOfN(days.size, counterValues(ids))
    } yield DailyCounterValues(days.zip(values).toMap)
  }

  def counterValues(ids: Set[Id]): Gen[CounterValues] =
    for {
      xs <- Gen.listOfN(ids.size, Gen.choose(0, 10000))
    } yield CounterValues(ids.zip(xs).toMap)
}
