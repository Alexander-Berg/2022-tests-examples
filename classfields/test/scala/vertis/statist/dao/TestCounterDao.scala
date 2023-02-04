package vertis.statist.dao

import org.joda.time.LocalDate
import vertis.statist.dao.TestCounterDao._
import vertis.statist.dao.counter.ChCounterDao._
import vertis.statist.dao.counter.{ChCounterDao, CounterDao}
import vertis.statist.model._

import zio.Task
import scala.util.Try

/** In-memory counter service for tests
  *
  * @author kusaeva
  */
class TestCounterDao(rows: Seq[Row]) extends CounterDao {

  override def get(component: Component, id: Id, period: DatesPeriod): Task[Int] =
    toTask {
      getSum(component, id, period)
    }

  override def getPlainByDayWithFilter(
      component: Component,
      period: DatesPeriod,
      filters: Seq[FieldFilter]): Task[ObjectDailyValues] =
    toTask {
      val values =
        getRows(Set(component), period, filters)
          .groupBy(_.day)
          .view
          .mapValues(_.map(_.value).sum)
          .toMap
      makeObjectDailyValues(component, values)
    }

  override def getMultiple(component: Component, ids: Set[Id], period: DatesPeriod): Task[CounterValues] =
    toTask {
      val values = ids.map(id => id -> getSum(component, id, period)).toMap
      makeCounterValues(ids, values)
    }

  override def getMultipleComponentsByDay(
      components: Set[Component],
      ids: Set[Id],
      period: DatesPeriod): Task[MultipleDailyValues] =
    getMultipleComponentsByDayWithFilter(components, ids, period, Nil)

  override def getMultipleComponentsByDayWithFilter(
      components: Set[Component],
      ids: Set[Id],
      period: DatesPeriod,
      filters: Seq[FieldFilter]): Task[MultipleDailyValues] =
    toTask {
      val values = getRowsByIds(components, ids, period, filters)
        .groupBy(_.id)
        .flatMap { case (id, v) =>
          v.groupBy(_.day)
            .map { case (day, rows) =>
              rows
                .groupBy(_.component)
                .map { case (c, rows) =>
                  (id, day, c, rows.map(_.value).sum)
                }
            }
        }
        .flatten

      MultipleDailyValues(
        ChCounterDao.makeDailyMultipleValuesRaw(ids, components, values).byObject.toSeq.sortBy(_._1).toMap
      )
    }

  override def getMultipleByDay(component: Component, ids: Set[Id], period: DatesPeriod): Task[DailyCounterValues] =
    getMultipleByDayWithFilter(component, ids, period, Nil)

  override def getMultipleByDayWithFilter(
      component: Component,
      ids: Set[Id],
      period: DatesPeriod,
      filters: Seq[FieldFilter]): Task[DailyCounterValues] =
    toTask {
      val values = getRowsByIds(Set(component), ids, period, filters)
        .groupBy(_.day)
        .flatMap { case (day, v) =>
          v.groupBy(_.id)
            .map { case (id, rows) =>
              (day, id, rows.map(_.value).sum)
            }
        }
      makeDailyCounterValues(ids, values)
    }

  override def getMultipleComponents(
      components: Set[Component],
      ids: Set[Id],
      period: DatesPeriod): Task[MultipleCountersValues] =
    getMultipleComponentsWithFilter(components, ids, period, Nil)

  override def getMultipleComponentsWithFilter(
      components: Set[Component],
      ids: Set[Id],
      period: DatesPeriod,
      filters: Seq[FieldFilter]): Task[MultipleCountersValues] =
    toTask {
      val values =
        ids.toSeq.sorted.map { id =>
          id -> ObjectCounterValues(
            components.toSeq.sorted
              .map(c => c -> getSum(c, id, period, filters))
              .toMap
          )
        }.toMap
      makeMultipleCounterValues(ids, components, values)
    }

  private def toTask[T](f: => T): Task[T] =
    Task.fromTry(Try(f))

  private def getRows(components: Set[Component], period: DatesPeriod, filters: Seq[FieldFilter]) =
    rows
      .filter(r => components.contains(r.component))
      .filter(r => applyPeriodFilter(period)(r.day))
      .filter(applyFieldsFilter(filters))

  private def getRowsByIds(
      components: Set[Component],
      ids: Set[Id],
      period: DatesPeriod,
      filters: Seq[FieldFilter]) =
    getRows(components, period, filters)
      .filter(r => ids.contains(r.id))

  private def getRowsById(component: Component, id: Id, period: DatesPeriod, filters: Seq[FieldFilter]) =
    getRowsByIds(Set(component), Set(id), period, filters)

  private def getSumByDay(component: Component, id: Id, period: DatesPeriod, filters: Seq[FieldFilter]) =
    getRowsById(component, id, period, filters)
      .groupBy(_.day)
      .map { case (d, rows) => d -> rows.map(_.value).sum }

  private def getSum(component: Component, id: Id, period: DatesPeriod, filters: Seq[FieldFilter] = Nil) =
    getSumByDay(component, id, period, filters).values.sum

  private def applyPeriodFilter(period: DatesPeriod)(day: LocalDate) =
    period.until.forall(until => day.isBefore(until.toLocalDate)) &&
      period.from.forall(from => !day.isBefore(from.toLocalDate))

  private def applyFieldsFilter(filters: Seq[FieldFilter])(r: Row) =
    filters.forall((f: FieldFilter) => r.fields.contains(f.fieldName -> f.fieldValue.value))
}

object TestCounterDao {

  private val FieldBar = Seq("field" -> "bar")
  private val FieldFoo = Seq("field" -> "foo")

  val CardShow: Component = "card_show"
  val PhoneShow: Component = "phone_show"

  val IdA: Id = "a_id"
  val IdB: Id = "b_id"
  val IdNone: Id = "none_id"

  val today: LocalDate = LocalDate.now()
  val yesterday: LocalDate = today.minusDays(1)

  private val data = Seq(
    // 1
    Row(CardShow, IdA, yesterday, FieldBar, 1),
    Row(CardShow, IdA, today, FieldFoo, 1),
    Row(CardShow, IdA, today, FieldBar, 1),
    // 2
    Row(CardShow, IdB, today, FieldBar, 4),
    Row(PhoneShow, IdB, today, FieldBar, 1)
  )

  val default: TestCounterDao = new TestCounterDao(data)

  case class Row(component: Component, id: Id, day: LocalDate, fields: Seq[(String, Any)], value: Int)
}
