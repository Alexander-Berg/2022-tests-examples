package ru.yandex.vertis.scheduler.impl.zk

import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean

import org.apache.curator.RetryPolicy
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.vertis.scheduler._
import ru.yandex.vertis.scheduler.logging.LoggingLockManagerMixin
import ru.yandex.vertis.scheduler.model.SchedulerInstance

import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Success

/**
 * Specs on [[ru.yandex.vertis.scheduler.LockManager]] with broken ZooKeeper instance.
 *
 * @author alesavin
 */
class ZkSpecificLockManagerIntSpec
  extends Matchers
  with WordSpecLike
  with ZooKeeperAware
  with BeforeAndAfterAll {

  import TestData._

  val numLockers = 2
  val executor = Executors.newFixedThreadPool(numLockers + 1)
  val brokenCuratorBase = CuratorFrameworkFactory.newClient(
    "127.0.0.1:80",
    sessionTimeout.toMillis.toInt,
    connectionTimeout.toMillis.toInt,
    retryPolicy
  )
  val manageable = ManageableCuratorFramework(curatorBase, brokenCuratorBase)
  val onRelease = new AtomicBoolean(false)
  var locked = Set.empty[Int]

  override def connectionTimeout: FiniteDuration = 1.seconds
  brokenCuratorBase.start()

  override def retryPolicy: RetryPolicy =
    new ExponentialBackoffRetry(100, 2)

  def getRunnable(lockManager: ZkLockManager,
                  instance: SchedulerInstance,
                  id: TaskId,
                  number: Int) = new Runnable {
    override def run(): Unit = {
      while (true) {
        lockManager.acquire(instance, id) match {
          case Success(true) =>
            locked = Set.empty[Int]
            info(s"Acquire $number: RUN")
            onRelease.set(true)
            lockManager.release(instance, id)
            onRelease.set(false)
            info(s"Release $number")
          case Success(false) =>
            info(s"Acquire $number: SKIP")
            locked += number
          case other =>
            fail(s"Unexpected $other")
        }
        Thread.sleep(sessionTimeout.toMillis)
      }
    }
  }

  override def sessionTimeout: FiniteDuration = 5.seconds

  "ZkLockManager" should {
    "run acquire/release loop" in {
      val closed = new AtomicBoolean(false)
      val lockManagers = for (i <- 1 to numLockers) yield {
        val lockManager = new ZkLockManager(manageable, "/path/to/locks")
          with LoggingLockManagerMixin
        executor.execute(getRunnable(lockManager, instance1, "1", i))
        lockManager
      }
      executor.execute(new Runnable {
        override def run(): Unit = {
          while (!closed.get()) {
            if (onRelease.get()) {
              manageable.brokeGetData = true
              Thread.sleep(connectionTimeout.toMillis)
              onRelease.set(false)
              manageable.brokeGetData = false
            }
          }
        }
      })

      executor.shutdown()
      Thread.sleep(30000)
      closed.set(true)
      executor.shutdownNow()
      lockManagers.foreach(_.shutdown())

      locked.size should be < numLockers
    }
  }

  override protected def afterAll(): Unit = {
    brokenCuratorBase.close()
    super.afterAll()
  }
}
