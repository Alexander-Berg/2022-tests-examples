package ru.yandex.extdata.core.actor.util

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.extdata.core._
import ru.yandex.extdata.core.actor.DispatcherActor.Command.Dispatch
import ru.yandex.extdata.core.actor.DispatcherActor.DispatchContext
import ru.yandex.extdata.core.actor.PoolContext

import scala.concurrent.Future

/**
  * @author evans
  */
class TaskPickerSpec extends WordSpecLike with Matchers {
  def task(id: TaskId, weight: Int) = DispatchContext(Seq.empty, Dispatch(id, weight, () => Future.successful(())))
  "task picker" should {
    "take first in simple conditions" in {
      val state = PoolContext(
        maxWeight = 10,
        maxConcurrentTasks = 10,
        awaitTasks = Seq(task("1", 1)),
        workingTasks = Seq(task("2", 2))
      )
      TaskPicker.findNext(state).get.dispatch.id shouldEqual "1"
    }
    "take first in empty pool" in {
      val state = PoolContext(
        maxWeight = 10,
        maxConcurrentTasks = 10,
        awaitTasks = Seq(task("1", 1)),
        workingTasks = Seq.empty
      )
      TaskPicker.findNext(state).get.dispatch.id shouldEqual "1"
    }
    "nothing take if no waiting tasks" in {
      val state = PoolContext(
        maxWeight = 10,
        maxConcurrentTasks = 10,
        awaitTasks = Seq.empty,
        workingTasks = Seq(task("1", 1))
      )
      TaskPicker.findNext(state).isEmpty shouldEqual true
    }
    "take smallest tasks if pool is overloaded" in {
      val state = PoolContext(
        maxWeight = 10,
        maxConcurrentTasks = 10,
        awaitTasks = Seq(task("1", 9), task("2", 1)),
        workingTasks = Seq(task("3", 2))
      )
      TaskPicker.findNext(state).get.dispatch.id shouldEqual "2"
    }
    "wait if pool should wait next task" in {
      val state = PoolContext(
        maxWeight = 10,
        maxConcurrentTasks = 10,
        awaitTasks = Seq(task("1", 5), task("2", 1)),
        workingTasks = Seq(task("3", 2), task("4", 5))
      )
      TaskPicker.findNext(state).isEmpty shouldEqual true
    }
    "take smallest tasks if pool is overloaded v2" in {
      val state = PoolContext(
        maxWeight = 10,
        maxConcurrentTasks = 10,
        awaitTasks = Seq(task("1", 6), task("2", 5), task("x", 1)),
        workingTasks = Seq(task("3", 2), task("4", 5))
      )
      TaskPicker.findNext(state).get.dispatch.id shouldEqual "x"
    }
    "nothing do (reserves place for wizard_tree)" in {
      val state = PoolContext(
        maxWeight = 100,
        maxConcurrentTasks = 10,
        awaitTasks = Seq(
          task("wizard_tree", 70),
          task("region_graph", 45),
          task("agency", 45),
          task("agregate_building_info", 45)
        ),
        workingTasks = Seq(task("region_graph", 45))
      )
      TaskPicker.findNext(state).isDefined shouldEqual false
    }
  }
}
