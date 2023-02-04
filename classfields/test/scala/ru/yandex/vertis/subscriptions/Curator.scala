package ru.yandex.vertis.subscriptions

import ru.yandex.vertis.subscriptions.util._

import com.google.common.io.Closer
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.test.TestingCluster

import scala.concurrent.duration._

/** Testing ZooKeeper and Curator stuff
  */
object Curator extends Logging {

  private val closer = Closer.create()

  /** Testing ZooKeeper Cluster
    */
  lazy val zookeeperCluster = {
    log.info("Start testing ZooKeeper cluster with 3 nodes")
    val cluster = new TestingCluster(3)
    cluster.start()
    closer.register(cluster)
  }

  def newClient(
      connectString: String,
      sessionTimeout: FiniteDuration = 15.seconds,
      connectionTimeout: FiniteDuration = 5.seconds,
      maxRetries: Int = 3) = {
    val _sessionTimeout = sessionTimeout.toMillis.toInt
    val _connectionTimeout = connectionTimeout.toMillis.toInt
    val baseSleepTime = 1.second.toMillis.toInt
    val retryPolicy = new ExponentialBackoffRetry(baseSleepTime, maxRetries)

    CuratorFrameworkFactory.newClient(
      connectString,
      _sessionTimeout,
      _connectionTimeout,
      retryPolicy
    )
  }

  /** Testing CuratorFramework instance
    */
  lazy val testingClient = {
    log.info("Start testing CuratorFramework")
    val client = newClient(zookeeperCluster.getConnectString)
    client.start()
    closer.register(client)
  }

  closeOnShutdown(closer) { closer =>
    log.info("Close testing ZooKeeper and Curator environment")
    closer.close()
  }
}
