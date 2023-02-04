package ru.yandex.vertis.scheduler.impl.zk

import com.google.common.io.Closer
import org.apache.curator.RetryPolicy
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.test.TestingCluster
import org.scalatest.{Suite, BeforeAndAfterAll}
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

/**
 * Introduces ZooKeeper stuff to specs
 */
trait ZooKeeperAware extends BeforeAndAfterAll {
  this: Suite =>

  private val zooLogger = LoggerFactory.getLogger(getClass)

  private val closer = Closer.create()

  /** Testing ZooKeeper Cluster  */
  val zookeeper = new TestingCluster(3)

  /** Root CuratorFramework */
  val curatorBase = CuratorFrameworkFactory.newClient(
    connectString,
    sessionTimeout.toMillis.toInt,
    connectionTimeout.toMillis.toInt,
    retryPolicy
  )

  zooLogger.info("Start ZooKeeper and Curator Client")
  zookeeper.start()
  closer.register(zookeeper)

  curatorBase.start()
  closer.register(curatorBase)

  override protected def afterAll() = {
    zooLogger.info("Stop ZooKeeper and Curator Client")

    closer.close()

    super.afterAll()
  }

  def connectString: String = zookeeper.getConnectString

  def sessionTimeout: FiniteDuration = 15.seconds

  def connectionTimeout: FiniteDuration = 10.seconds

  def retryPolicy: RetryPolicy = new ExponentialBackoffRetry(
    1000, 3
  )
}
