package ru.yandex.vertis.feature.impl

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.BoundedExponentialBackoffRetry
import org.scalatest.Ignore
import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.vertis.curator.recipes.map.{StringValueSerializer, ZooKeeperMap}
import ru.yandex.vertis.feature.model.FeatureTypes

import scala.concurrent.duration._

/**
  * Base specs for [[ZookeeperFeatureRegistry]]
  *
  * @author frenki
  */
@Ignore
class ZookeeperFeatureRegistryIntSpec
  extends TypedFeatureRegistrySpecBase {

  private val DefaultPatienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(50, Millis))

  override implicit def patienceConfig: PatienceConfig = DefaultPatienceConfig

  import ru.yandex.vertis.feature.impl.ZookeeperFeatureRegistryIntSpec.Curator

  override def syncPeriod: FiniteDuration = 2.second

  def registry(ft: FeatureTypes): TypedValueFeatureRegistry = {
    val path = "/feature-toggles-test/features"
    val zkMap = new ZooKeeperMap[String](
      client = Curator,
      basePath = s"$path",
      serializer = StringValueSerializer,
      startOnCreate = true,
      syncPeriod = Some(1.second)
    )()
    zkMap.snapshot.keys.foreach(zkMap.remove)

    new ZookeeperFeatureRegistry(
      zkPath = path,
      zkClient = Curator,
      syncPeriod = 1.second,
      ft = ft
    )
  }
}

object ZookeeperFeatureRegistryIntSpec {

  private val zkConnectString =
    "zk-02-man.test.vertis.yandex.net:2181,zk-02-myt.test.vertis.yandex.net:2181,zk-02-sas.test.vertis.yandex.net:2181"

  private val zkRetryPolicy =
    new BoundedExponentialBackoffRetry(10000,600000,29)

  val Curator: CuratorFramework = {
    val curatorFramework = CuratorFrameworkFactory.newClient(zkConnectString, zkRetryPolicy)
    curatorFramework.start()
    curatorFramework
  }
}
