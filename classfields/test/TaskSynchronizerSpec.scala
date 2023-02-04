package vertis.zio.tasks.synchronized

import java.util.concurrent.atomic.AtomicBoolean
import org.scalatest.{Assertion, ParallelTestExecution}
import vertis.zio.tasks.supervised.{RunningTask, SupervisedTask, TaskRegistry}
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase
import common.zio.logging.Logging
import zio._
import zio.clock.Clock
import zio.duration._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class TaskSynchronizerSpec extends ZioSpecBase with ParallelTestExecution {

  "TaskSynchronizer" should {
    "add tasks" in ioTest {
      for {
        q <- Queue.sliding[Map[String, EventConf]](1)
        _ <- sychronizer(q).use { supervisor =>
          for {
            _ <- q.offer(Map("key1" -> InfEvent(1, "inf event for key 1")))
            _ <- checkTaskSet(supervisor)("task added", Set("key1"))
            _ <- q.offer(Map("key2" -> InfEvent(1, "inf event for key 2")))
            _ <- checkTaskSet(supervisor)("task set replaced", Set("key2"))
            _ <- q.offer(Map.empty)
            //            _ <- checkTaskSet(supervisor)("task removed", Set.empty)
          } yield ()
        }
      } yield ()
    }
    "replace tasks with newer version" in ioTest {
      for {
        q <- Queue.sliding[Map[String, EventConf]](1)
        _ <- sychronizer(q).use { supervisor =>
          for {
            _ <- q.offer(Map("key" -> InfEvent(1)))
            addedTask <- getTask(supervisor, "key")
            _ <- check("task added")(addedTask.version shouldBe 1)

            _ <- q.offer(Map("key" -> InfEvent(2)))
            _ <- addedTask.await
            _ <- Logging.info("im done")
            replacedTask <- getTask(supervisor, "key")
            _ <- check("task replaced")(replacedTask.version shouldBe 2)

            _ <- q.offer(Map("key" -> InfEvent(1)))
            // todo await for what
            replacedTask <- getTask(supervisor, "key")
            _ <- check("task not replaced with older version")(replacedTask.version shouldBe 2)
          } yield ()
        }
      } yield ()
    }

    "restart finished tasks" in ioTest {
      for {
        q <- Queue.sliding[Map[String, EventConf]](1)
        _ <- sychronizer(q).use { supervisor =>
          for {
            _ <- q.offer(Map("key1" -> FiniteEvent(1, 10)))
            originalTask <- getTask(supervisor, "key1")
            _ <- Logging.info("task started")
            _ <- originalTask.await
            restartedTask <- getTask(supervisor, "key1")
            _ <- check("task still running")(originalTask.version shouldBe restartedTask.version)
            _ <- check("task is new")(originalTask.description should not be restartedTask.description)
          } yield ()
        }
      } yield ()
    }

    "fail on broken event" in ioTest {
      for {
        q <- Queue.sliding[Map[String, EventConf]](1)
        _ <- sychronizer(q).use { supervisor =>
          for {
            _ <- q.offer(Map("key1" -> BrokenEvent))
            _ <- getTask(supervisor, "key1", Schedule.forever)
          } yield ()
        }
      } yield ()
    }
  }

  type Patience = Schedule[Clock, Any, Any]

  protected val defaultPatience: Patience =
    Schedule.recurs(20) && Schedule.spaced(30.millis)

  protected val shortPatience: Patience =
    Schedule.recurs(3) && Schedule.spaced(10.millis)

  val any: Any => UIO[Boolean] = _ => UIO.succeed(true)

  private def checkTaskSet(registry: TaskRegistry[_, _])(clue: String, expected: Set[String]): RIO[BaseEnv, Assertion] =
    checkEventually(clue)(
      registry.listRunning >>= (tasks =>
        Logging.info(s"Checking if tasks ${expected.mkString(", ")} are in ${tasks.mkString(", ")}") *>
          Task(tasks should contain theSameElementsAs expected)
      )
    )(defaultPatience)

  private def getTask[A](
      registry: TaskRegistry[_, A],
      key: String,
      patience: Patience = defaultPatience): RIO[Clock, RunningTask[A]] =
    (registry.getRunningTask(key) >>= {
      case Some(task) => UIO.succeed(task)
      case None => Task.fail(new RuntimeException(s"No task for key $key"))
    }).retry(patience)

  private def sychronizer(
      eventQ: Queue[Map[String, EventConf]],
      resyncAwait: Duration = 0.nanos): RManaged[BaseEnv, TaskRegistry[BaseEnv, Unit]] =
    TaskRegistry.make[BaseEnv, Unit]("supervisor") >>= (s =>
      TaskSynchronizer
        .make[BaseEnv, Unit, EventConf](eventQ, s, (_, conf) => conf.toTask(), resyncAwait)
        .as(s)
    )

  trait EventConf {
    def toTask(): SupervisedTask[Clock with Logging.Logging, Unit]
  }

  case class InfEvent(version: Int, description: String = "int task") extends EventConf {

    def toTask(): SupervisedTask[Clock with Logging.Logging, Unit] = {
      val desc = s"Running $description v.$version"
      SupervisedTask(
        version,
        desc,
        Logging
          .info(desc)
          .interruptible
          .repeat(Schedule.spaced(10.millis))
          .unit
          .toManaged_
      )
    }
  }

  case class FiniteEvent(version: Int, n: Int) extends EventConf {

    def toTask(): SupervisedTask[Clock with Logging.Logging, Unit] = {
      val desc = s"task $version started at ${System.currentTimeMillis()}"
      SupervisedTask(
        version,
        desc,
        Logging
          .info(s"Running $desc")
          .repeat(Schedule.spaced(20.millis) && Schedule.recurs(n))
          .unit
          .interruptible
          .toManaged_
      )
    }
  }

  case class ShortEvent(version: Int, ref: Ref[Int]) extends EventConf {

    def toTask(): SupervisedTask[Clock with Logging.Logging, Unit] = {
      SupervisedTask(
        version,
        "short event",
        (Logging.info(s"Running") <* ref.update(_ + 1)).toManaged_
      )
    }
  }

  /** Fails on task creation, which is illegal,
    * succeeds the second time
    */
  case object BrokenEvent extends EventConf {
    private val isFirstRun = new AtomicBoolean(true)

    private val failOnFirstRun = UIO {
      val wasFirstRun = isFirstRun.compareAndExchange(true, false)
      if (wasFirstRun) throw new IllegalStateException("fail on first run")
    }

    def toTask(): SupervisedTask[Clock with Logging.Logging, Unit] =
      SupervisedTask(
        1,
        "",
        (failOnFirstRun *>
          infLog("running", 20.millis)).interruptible.toManaged_
      )
  }

  private def infLog(string: String, period: Duration): URIO[Clock with Logging.Logging, Unit] =
    Logging.info(string).repeat(Schedule.spaced(period)).unit
}
