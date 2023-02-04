package ru.yandex.vertis.scheduler

import java.util.UUID
import java.util.concurrent.Phaser

import com.google.common.io.Closer
import com.typesafe.config.Config
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.slf4j.{Logger, LoggerFactory}
import ru.yandex.vertis.scheduler.journal.{Journal, JvmJournal}
import ru.yandex.vertis.scheduler.model._
import ru.yandex.vertis.scheduler.producer.Producer
import ru.yandex.vertis.scheduler.tracking.TaskTrack

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Random, Try}
import SchedulerIntSpec._

/**
  * Integration specs on [[ru.yandex.vertis.scheduler.SchedulerImpl]]
  *
  * @author dimas
  */
trait SchedulerIntSpec
  extends Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  val log = LoggerFactory.getLogger(getClass)

  "Scheduler" should {
    "run one task on single instance" in {
      integrationTest(
        nrOfTasks = 1,
        maxConcurrentTask = 10,
        nrOfSchedulers = 1,
        runForSeconds = 30)
    }
    "run many tasks on single instance with bounded weight" in {
      integrationTest(
        nrOfTasks = 10,
        maxConcurrentTask = 100,
        nrOfSchedulers = 1,
        maxRunningWeights = Some(3),
        runForSeconds = 10)
    }
    "run one task on many instances" in {
      integrationTest(
        nrOfTasks = 1,
        maxConcurrentTask = 10,
        nrOfSchedulers = 3,
        runForSeconds = 30)
    }
    "run many tasks on many instances" in {
      integrationTest(
        nrOfTasks = 10,
        maxConcurrentTask = 4,
        nrOfSchedulers = 3,
        runForSeconds = 30)
    }
  }

  override protected def afterAll(): Unit = {
    closer.close()
  }

  def createSchedulers(
      caseId: String,
      nrOfInstances: Int,
      maxConcurrentTasks: Int,
      maxRunningWeights: Option[Int] = None,
      tasks: Iterable[Task],
      journal: Option[Journal]): Iterable[Scheduler]

  private val closer = Closer.create()

  def integrationTest(
      nrOfTasks: Int,
      maxConcurrentTask: Int,
      nrOfSchedulers: Int,
      runForSeconds: Int,
      maxRunningWeights: Option[Int] = None) {

    val caseId = UUID.randomUUID().toString.take(4)

    val phaser = new Phaser(1)

    val tasks = Iterator.
        continually(createNextTask(phaser)).
        take(nrOfTasks).
        toIterable

    val journal = new JvmJournal

    val schedulers = createSchedulers(
      caseId,
      nrOfSchedulers,
      maxConcurrentTask,
      maxRunningWeights,
      tasks,
      Some(journal)
    )

    schedulers foreach {
      scheduler =>
        closer.register(CloseableScheduler(scheduler))
        scheduler.start()
    }

    Thread.sleep(runForSeconds * 1000)

    schedulers foreach {
      scheduler => scheduler.shutdown(3.seconds)
    }


    log.info("Wait for tasks completeness...")
    phaser.arriveAndAwaitAdvance()
    log.info("It seems that tasks have completed.")

    val tracks = TaskTrack(journal).get
    tracks foreach {
      t =>
        val descriptor = tasks.
            find(_.descriptor.id == t.id).get.
            descriptor
        log.info(s"Check task $descriptor track is valid...")
        try {
          t.checkValid()
        }
        catch {
          case e: Exception =>
            log.error(s"Task for $descriptor is invalid", e)
            fail(e)
        }
    }
  }

  def createNextTask(implicit phaser: Phaser): Task = {
    val id = UUID.randomUUID().toString.take(5)
    val period = Math.abs(Random.nextInt(20)) + 5
    val descriptor = TaskDescriptor(id, Schedule.EverySeconds(period))
    val optConfig = Generators.OptConfigGen.next
    val payload = Random.nextInt(3) match {
      case 0 =>
        val work = SyncWork(id)
        optConfig match {
          case Some(config) =>
            Payload.Sync(config)(c => work.run(Some(c)))
          case None =>
            Payload.Sync(() => work.run(None))
        }
      case 1 =>
        val work = SyncTryWork(id)
        optConfig match {
          case Some(config) =>
            Payload.SyncTry(config)(c => work.run(Some(c)))
          case None => Payload.SyncTry(() => work.run(None))
        }
      case 2 =>
        val work = AsyncWork(id)
        optConfig match {
          case Some(config) =>
            Payload.Async(config)(c => work.run(Some(c)))
          case None =>
            Payload.Async(() => work.run(config = None))
        }
    }
    Task(descriptor, payload)
  }
}
object SchedulerIntSpec{
  case class SyncWork(id: TaskId)(implicit phaser: Phaser) {
    private val log = LoggerFactory.getLogger(getClass)

    def run(config: Option[Config]) {
      Work.progress(id, log, "synchronously", config)
    }
  }

  case class SyncTryWork(id: TaskId)(implicit phaser: Phaser) {
    private val log = LoggerFactory.getLogger(getClass)

    def run(config: Option[Config]): Try[Unit] = Try {
      Work.progress(id, log, "synchronously with Try", config)
    }
  }

  case class AsyncWork(id: TaskId)(implicit phaser: Phaser) {
    private val log = LoggerFactory.getLogger(getClass)

    import scala.concurrent.ExecutionContext.Implicits.global

    def run(config: Option[Config]): Future[Unit] = Future {
      Work.progress(id, log, "asynchronously", config)
    }
  }

  object Work {
    def progress(id: String,
                 log: Logger,
                 kind: String,
                 config: Option[Config])
                (implicit phaser: Phaser) = {

      phaser.register()
      try doWork()
      finally phaser.arriveAndDeregister()

      def doWork() = {
        val timeToWork = Math.abs(Random.nextInt(5)) + 3
        log.info(s"Task $id start $kind with config $config. " +
          s"It is going to work for $timeToWork seconds.")

        Thread.sleep(timeToWork * 1000)

        if (Random.nextBoolean())
          throw new Exception(s"Task $id artificial failure.")

        log.info(s"Task $id stop.")
      }
    }

  }

}
