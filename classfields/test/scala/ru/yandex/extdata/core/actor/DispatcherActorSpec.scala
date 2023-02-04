package ru.yandex.extdata.core.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.extdata.core.TaskId
import ru.yandex.extdata.core.actor.DispatcherActor.Command.Dispatch
import ru.yandex.extdata.core.actor.DispatcherActor.Event.{Complete, Start, Submit}
import ru.yandex.extdata.core.actor.DispatcherActor._
import ru.yandex.extdata.core.gens.Producer._
import ru.yandex.extdata.core.util.actor.PausableTickActor

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author evans
  */
class DispatcherActorSpec(_system: ActorSystem)
  extends TestKit(_system)
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender {

  import system.dispatcher

  def this() = this(ActorSystem("DispatcherSpec"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  def receiveEvents(tasks: Int, maxDuration: FiniteDuration = 7.seconds): Seq[Event] =
    receiveWhile(maxDuration, maxDuration, tasks * 3) {
      case e: Submit => e
      case e: Start => e
      case e: Complete => e
      case s => throw new IllegalStateException(s"Unexpected message $s")
    }

  def checkInvariants(tasks: Seq[Dispatch], maxWeight: Int, maxConcurrent: Int, msgs: Seq[Event]): Unit = {
    val taskMap = tasks.map(t => t.id -> t).toMap

    def checkConcurrentTasks(msgs: Seq[Event]) = {
      msgs.foldLeft(0) {
        case (num, s: Start) =>
          if (num + 1 > maxConcurrent)
            throw new IllegalStateException(s"Too many concurrent tasks: ${s.id}")
          num + 1
        case (num, _: Complete) =>
          num - 1
        case (num, _) => num
      }
    }

    def checkWeight(msgs: Seq[Event]) = {
      msgs.foldLeft(0) {
        case (num, s: Start) =>
          if (num + taskMap(s.id).weight > maxWeight)
            throw new IllegalStateException(s"Too many concurrent tasks: ${s.id}")
          num + taskMap(s.id).weight
        case (num, s: Complete) =>
          num - taskMap(s.id).weight
        case (num, _) => num
      }
    }

    //not works for multiple tasks with same taskId
    def checkTaskOrdering(taskId: TaskId, msgs: Seq[Event]) =
      msgs.reduceLeft[Event] {
        case (x: Submit, y: Start) => y
        case (x: Start, y: Complete) => y
        case (x: Complete, y: Submit) => y
        case (x, y) => throw new IllegalStateException(s"$taskId: $x -> $y. Unexpected transition.")
      }

    def checkConcurrencyOfTasks(msgs: Seq[Event]) = {
      msgs.foldLeft(Set.empty[TaskId]) {
        case (set, x: Start) =>
          if (set.contains(x.id)) {
            throw new IllegalStateException(s"${x.id}: concurrent running tasks with equal task id")
          } else {
            set.+(x.id)
          }
        case (set, x: Complete) =>
          set.-(x.id)
        case (set, _) =>
          set
      }
    }

    checkConcurrencyOfTasks(msgs)
//    msgs.groupBy(_.id).foreach {
//      case (taskId, ms) =>
//        checkTaskOrdering(taskId, ms)
//    }
    checkConcurrentTasks(msgs)
    checkWeight(msgs)
  }

  "Dispatcher" should {
    "process one task" in {
      val maxWeight = 100
      val maxConcurrent = 10
      val actor = system.actorOf(DispatcherActor.props(maxWeight, maxConcurrent))
      val task = Dispatch("test", 1, () => Future.successful(1))
      actor ! Dispatch("test", 1, () => Future.successful(1))
      val msgs = receiveEvents(1)
      checkInvariants(Seq(task), 100, 10, msgs)
      expectNoMessage(100.millis)
    }

    "no process task if not started" in {
      val actor = system.actorOf(DispatcherActor.props(100, 10))
      actor ! PausableTickActor.Stop
      actor ! Dispatch("test", 1, () => Future.successful(1))
      expectMsgPF() {
        case Submit("test", _) =>
      }
      expectNoMessage(100.millis)
      actor ! PausableTickActor.Start
      expectMsgPF() {
        case e: Start =>
      }
      expectMsgPF() {
        case e: Complete =>
      }
      expectNoMessage(100.millis)
    }

    "not parallel execute tasks with same id" in {
      val actor = system.actorOf(DispatcherActor.props(100, 10))
      val task = Dispatch(
        "test",
        1,
        () =>
          Future {
            Thread.sleep(1000)
          }
      )
      actor ! task
      Thread.sleep(500)
      actor ! task
      val msgs = receiveEvents(2)
      expectNoMessage(100.millis)
      checkInvariants(Seq(task, task), 100, 10, msgs)
    }

    "process random tasks" in {
      val maxWeight = 100
      val maxConcurrent = 10
      val actor = system.actorOf(DispatcherActor.props(maxWeight, maxConcurrent))
      val cnt = 20
      val tasks = (1 to cnt).map { id =>
        taskGen(id, maxWeight, 50).next
      }
      tasks.foreach(actor ! _)
      val msgs = receiveEvents(cnt)
      expectNoMessage(100.millis)
      checkInvariants(tasks, maxWeight, maxConcurrent, msgs)
    }

    "dispatch one task many time" in {
      val maxWeight = 100
      val maxConcurrent = 10
      val actor = system.actorOf(DispatcherActor.props(maxWeight, maxConcurrent))
      val cnt = 15
      val task = taskGen(1, maxWeight, 50).next
      val tasks = Seq.fill(cnt)(task)
      tasks.foreach(actor ! _)
      val msgs = receiveEvents(cnt)
      expectNoMessage(100.millis)
      msgs.collect { case e: Complete => 1 }.size shouldEqual cnt
    }

    "fail on too weighty task" in {
      val maxWeight = 10
      val maxConcurrent = 10
      val actor = system.actorOf(DispatcherActor.props(maxWeight, maxConcurrent))
      val task = taskGen(2, maxWeight, 50).next.copy(weight = 100)
      actor ! task
      val msgs = receiveEvents(1)
      expectNoMessage(100.millis)
      msgs.collect { case e: Complete if e.data.isFailure => 1 }.size shouldEqual 1
    }
  }

  def taskGen(id: Int, maxWeight: Int, maxSleep: Int) =
    for {
      weight <- Gen.choose(0, maxWeight)
      sleep <- Gen.choose(0, maxSleep)
    } yield Dispatch(
      id.toString,
      weight,
      () =>
        Future {
          Thread.sleep(sleep)
        }
    )
}
