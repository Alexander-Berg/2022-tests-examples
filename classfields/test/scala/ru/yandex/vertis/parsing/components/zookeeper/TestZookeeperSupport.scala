package ru.yandex.vertis.parsing.components.zookeeper

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.BoundedExponentialBackoffRetry
import org.apache.curator.test.TestingCluster
import ru.yandex.vertis.curator.monitoring.CuratorMonitoring
import ru.yandex.vertis.curator.util.FailSafeZookeeperFactory
import ru.yandex.vertis.parsing.components.ApplicationAware

/**
  * TODO
  *
  * @author aborunov
  */
trait TestZookeeperSupport extends ZookeeperAware with ApplicationAware {

  private val zookeeper: TestingCluster = {
    val x = new TestingCluster(1)
    x.start()
    x
  }

  override val zkCommonClient: CuratorFramework = {
    val retryPolicy = new BoundedExponentialBackoffRetry(10000, 60000, 29)
    val client = CuratorFrameworkFactory
      .builder()
      .namespace("")
      .connectString(zookeeper.getConnectString)
      .retryPolicy(retryPolicy)
      .zookeeperFactory(new FailSafeZookeeperFactory())
      .build()
    client.start()
    CuratorMonitoring.scheduleReconnect(client)
    client
  }

  val zkServiceClient: CuratorFramework = {
    zkCommonClient.usingNamespace(app.env.serviceName)
  }

  val zkComponentClient: CuratorFramework = {
    zkServiceClient.usingNamespace(zkServiceClient.getNamespace + "/" + app.env.componentName)
  }
}
