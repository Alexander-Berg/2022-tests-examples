package vertis.zio.tasks

import common.zio.logging.Logging
import org.scalatest.{Assertion, EitherValues}
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.tasks.supervised.{CloseableTask, SupervisedTask, TaskRegistry}
import vertis.zio.util.ZioExit._
import vertis.zio.{BaseEnv, ServerEnv}
import vertis.zio.stream.StreamsTest
import vertis.zio.test.ZioSpecBase
import zio.clock.Clock
import zio.duration._
import zio.{Ref, _}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class TaskRegistrySpec extends ZioSpecBase with EitherValues {

  "TaskSupervisor" should {

    "run task" in taskTest { case TaskTest(out, supervisor) =>
      val task = infTask(out)("a" -> 0)
      for {
        taskAdded <- supervisor.run("a", task)
        _ <- check("task registered")(taskAdded.map(_.version) shouldBe Some(task.version))
        _ <- checkRunning(out, "a", task)
      } yield ()
    }

    "not add task if already running" in taskTest { case TaskTest(out, supervisor) =>
      val taskA1 = infTask(out)("a" -> 1)
      val taskA2 = infTask(out)("a" -> 2)
      for {
        a1Added <- supervisor.run("a", taskA1)
        _ <- check("task a1 registered")(a1Added should not be empty)
        a2Added <- supervisor.run("a", taskA2)
        _ <- check("task a2 not registered")(a2Added shouldBe empty)
        _ <- checkRunning(out, "a", taskA1)
        _ <- checkClosed(out, "a", taskA2)
      } yield ()
    }

    "replace task" in taskTest { case TaskTest(out, supervisor) =>
      val taskA1 = infTask(out)("a" -> 1)
      val taskA2 = infTask(out)("a" -> 2)
      for {
        a1Added <- supervisor.run("a", taskA1)
        _ <- check("task a1 registered")(a1Added.map(_.version) shouldBe Some(taskA1.version))
        a2Added <- supervisor.runOrReplace("a", taskA2)
        _ <- check("task a2 registered")(a2Added.map(_.version) shouldBe Some(taskA2.version))
        _ <- checkClosed(out, "a", taskA1)
        _ <- checkRunning(out, "a", taskA2)
      } yield ()
    }

    "remove task" in taskTest { case TaskTest(out, supervisor) =>
      val taskA1 = infTask(out)("a" -> 1)
      for {
        taskAdded <- supervisor.run("a", taskA1)
        _ <- check("task registered")(taskAdded should not be empty)
        _ <- checkRunning(out, "a", taskA1)
        taskRemoved <- supervisor.stop("a")
        _ <- check("task removed")(taskRemoved shouldBe true)
        _ <- checkM("task finished")(taskAdded.get.isDone.map(_ shouldBe true))
        _ <- checkClosed(out, "a", taskA1)
      } yield ()
    }

    "not fail removing a missing task" in taskTest { case TaskTest(_, supervisor) =>
      for {
        taskRemoved <- supervisor.stop("a")
        _ <- check("task removed")(taskRemoved shouldBe false)
      } yield ()
    }

    s"stop blocking task" in taskTest { case TaskTest(out, supervisor) =>
      val blockingTask = new SupervisedTask[Clock with Logging.Logging, Long](
        0,
        s"blocking task",
        Queue.unbounded[Long].map { q =>
          CloseableTask(
            withOutput(out, s"blocking_task") *>
              (Logging.info("taking...") *> q.take).toManaged_,
            Logging.info(s"Shutting down q") *> q.shutdown
          )
        }
      )

      for {
        added <- supervisor.run("blocking_task", blockingTask)
        _ <- check(s"task ${blockingTask.version} added")(added should not be empty)
      } yield ()
    }

    "remove finished task" ignore taskTest { case TaskTest(out, supervisor) =>
      val task = finiteTask(out)("finite_task" -> 0)
      for {
        taskAdded <- supervisor.run("a", task)
        _ <- check("task added")(taskAdded should not be empty)
        _ <- taskAdded.get.await
        _ <- checkM("task removed") {
          supervisor.listRunning.map { runningTasks =>
            runningTasks should not contain task.version
          }
        }
      } yield ()
    }

    "run stream" in taskTest { case TaskTest(_, supervisor) =>
      for {
        q <- Queue.unbounded[Long]
        _ <- (Logging.info("offering") *> q.offer(5)).repeat(Schedule.recurs(9))
        task = SupervisedTask(
          0,
          "stream",
          UIO {
            CloseableTask(
              StreamsTest
                .stream[Long](q, 0, _ + _)
                .map(_.sum)
                .toManaged_,
              q.shutdown
            )
          }
        )
        taskAdded <- supervisor.run("a", task)
        _ <- check("task added")(taskAdded should not be empty)
        res <- taskAdded.get.await
        // could be folded in different ways, just check it got the head of the stream
        _ <- check("stream done")(res.toOption.get should be > 0L)
      } yield ()
    }
  }
  private val taskPeriod = 100.millis

  private def taskTest(test: TaskTest => TestBody): Unit = {
    ioTest {
      for {
        out <- Ref.make(Map[String, Long]())
        _ <- TaskRegistry
          .make[BaseEnv, Long]("my-tasks")
          .use { supervisor =>
            test(TaskTest(out, supervisor))
          }
        _ <- checkAllClosed(out)
      } yield ()
    }
  }

  private def checkAllClosed[R](out: Ref[Map[String, Long]]): RIO[R, Assertion] =
    checkM(s"all tasks are closed")(out.get.map(_ shouldBe empty))

  implicit private val patience: Schedule[Clock, Any, (Long, Long)] =
    Schedule.recurs(3) && Schedule.spaced(taskPeriod)

  /** @return a task that increments it's value in the out map and removes it on close
    */
  private def infTask(out: Ref[Map[String, Long]])(taskId: (String, Int)): SupervisedTask[BaseEnv, Long] =
    createTask[Long](out, taskId, Schedule.spaced(taskPeriod))

  /** @return a task that increments it's value in the out map and removes it on close
    */
  private def finiteTask(out: Ref[Map[String, Long]])(taskId: (String, Int)): SupervisedTask[BaseEnv, Long] =
    createTask[Long](out, taskId, (Schedule.spaced(taskPeriod) && Schedule.recurs(3)).map(_._1))

  /** @return a task that increments it's value in the out map and removes it on close
    */
  private def createTask[A](
      out: Ref[Map[String, Long]],
      taskId: (String, Int),
      schedule: Schedule[Clock, Unit, A],
      continue: Option[UIO[A]] = None): SupervisedTask[Clock with Logging.Logging, A] = {
    val (taskName, taskVersion) = taskId
    val taskIdStr = s"${taskName}_$taskVersion"

    val loop = Logging.info(s"executing task $taskVersion") *>
      out.update(_.updatedWith(taskIdStr)(count => Some(count.getOrElse(0L) + 1)))

    val managedTask =
      ZManaged.makeExit(UIO.unit) { case (_, exit) =>
        Logging.info(s"Properly closing task $taskIdStr after ${exit.prettyPrint}") *>
          out.update(_.removed(taskIdStr))
      } *> {
        ZManaged.fromEffect(
          loop.interruptible.repeat(schedule) >>=
            (res => continue.getOrElse(UIO.succeed(res)))
        )
      }

    SupervisedTask[Clock with Logging.Logging, A](
      taskVersion,
      s"task $taskVersion",
      managedTask
    )
  }

  private def withOutput(out: Ref[Map[String, Long]], taskId: String): ZManaged[Logging.Logging, Nothing, Unit] =
    ZManaged
      .fromEffect(
        Logging.info(s"Starting task $taskId") *>
          out.update(_.updatedWith(taskId)(count => Some(count.getOrElse(0L) + 1)))
      )
      .onExit(exit =>
        Logging.info(s"Cleaning task $taskId after ${exit.prettyPrint}") *>
          out.update(_.removed(taskId))
      )

  private def checkRunning[R](
      out: Ref[Map[String, Long]],
      taskKey: String,
      task: SupervisedTask[R, _]): RIO[ServerEnv, Assertion] =
    checkEventually(s"task ${task.version} is running")(
      out.get >>= (map => Task((map should contain).key(s"${taskKey}_${task.version}")))
    )(patience).orDie

  private def checkClosed[R](
      out: Ref[Map[String, Long]],
      taskKey: String,
      task: SupervisedTask[R, _]): RIO[ServerEnv, Assertion] =
    checkEventually(s"task ${task.version} is closed")(
      out.get >>= (map => Task(map should not contain key(s"${taskKey}_${task.version}")))
    )(patience).orDie

  private case class TaskTest(out: Ref[Map[String, Long]], registry: TaskRegistry[BaseEnv, Long])
}
