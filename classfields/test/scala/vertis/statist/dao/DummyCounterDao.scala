package vertis.statist.dao

import org.joda.time.LocalDate
import vertis.statist.dao.counter.ChCounterDao.{makeCounterValues, makeMultipleCounterValues}
import vertis.statist.dao.counter.CounterDao
import vertis.statist.model._

import scala.concurrent.ExecutionContext.Implicits.global
import zio.Task

/** @author kusaeva
  */
class DummyCounterDao(default: Int, loadMillis: Long = 0L, failing: Boolean = false) extends CounterDao {

  override def getPlainByDayWithFilter(
      component: Component,
      period: DatesPeriod,
      filters: Seq[FieldFilter]): Task[ObjectDailyValues] = ???

  override def get(component: Component, id: Id, period: DatesPeriod): Task[Int] = ???

  override def getMultiple(component: Component, ids: Set[Id], period: DatesPeriod): Task[CounterValues] =
    if (failing) {
      Task.fail(new RuntimeException("loading failed"))
    } else {
      Task {
        Thread.sleep(loadMillis)
        val values = ids.map(_ -> default).toMap
        makeCounterValues(ids, values)
      }
    }

  override def getMultipleComponents(
      components: Set[Component],
      ids: Set[Id],
      period: DatesPeriod): Task[MultipleCountersValues] =
    if (failing) {
      Task.fail(new RuntimeException("loading failed"))
    } else {
      Task {
        Thread.sleep(loadMillis)
        val values =
          ids.toSeq.sorted.map { id =>
            id -> ObjectCounterValues(
              components.toSeq.sorted
                .map(c => c -> default)
                .toMap
            )
          }.toMap
        makeMultipleCounterValues(ids, components, values)
      }
    }

  override def getMultipleComponentsWithFilter(
      components: Set[Component],
      ids: Set[Id],
      period: DatesPeriod,
      filters: Seq[FieldFilter]): Task[MultipleCountersValues] = ???

  override def getMultipleByDay(component: Component, ids: Set[Id], period: DatesPeriod): Task[DailyCounterValues] =
    ???

  override def getMultipleComponentsByDay(
      components: Set[Component],
      ids: Set[Id],
      period: DatesPeriod): Task[MultipleDailyValues] =
    if (failing) {
      Task.fail(new RuntimeException("loading failed"))
    } else {
      Task {
        Thread.sleep(loadMillis)
        val byObject =
          ids.map { id =>
            id -> ObjectDailyValues(
              Map(
                LocalDate.now -> ObjectCounterValues(components.map(_ -> default).toMap)
              )
            )
          }.toMap

        MultipleDailyValues(byObject)
      }
    }

  override def getMultipleComponentsByDayWithFilter(
      components: Set[Component],
      ids: Set[Id],
      period: DatesPeriod,
      filters: Seq[FieldFilter]): Task[MultipleDailyValues] = ???

  override def getMultipleByDayWithFilter(
      component: Component,
      ids: Set[Id],
      period: DatesPeriod,
      filters: Seq[FieldFilter]): Task[DailyCounterValues] = ???
}
