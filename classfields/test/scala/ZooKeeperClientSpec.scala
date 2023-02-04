import org.apache.zookeeper.KeeperException.NotEmptyException
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.application.ng.curator.{
  BoundedExponentialBackoffRetryConfig,
  CuratorConfig,
  CuratorConfigProvider,
  DefaultCuratorProvider
}
import ru.yandex.realty.application.ng.zookeeper.ZooKeeperClient
import ru.yandex.realty.AsyncSpecBase

/**
  * Created by Viacheslav Kukushkin <vykukushkin@yandex-team.ru> on 08.12.18
  */
// Though this test is located at realty-archive-scheduler module,
// really it tests ZooKeeper client from realty-common.
// Don't moved to realty-common because ZKClient wants config details to connect to real zk instance
@RunWith(classOf[JUnitRunner])
class ZooKeeperClientSpec extends AsyncSpecBase with Logging with PropertyChecks {
  private lazy val defaultCuratorConfig = CuratorConfig(
    "zookeeper-legacy-01-myt.test.vertis.yandex.net:2181,zookeeper-legacy-01-vla.test.vertis.yandex.net:2181,zookeeper-legacy-01-sas.test.vertis.yandex.net:2181",
    30000,
    5000,
    BoundedExponentialBackoffRetryConfig(1000, 600000, 29),
    ""
  )
  val testNamespace = "realty/dev/zookeeper-client-test"

  def currentMillis: String = {
    DateTime.now.getMillis.toString
  }

  def testZkClient(namespace: String): ZooKeeperClient = {
    val curatorProvider = new CuratorConfigProvider with DefaultCuratorProvider {
      override def curatorConfig: CuratorConfig = defaultCuratorConfig.copy(namespace = namespace)
    }

    new ZooKeeperClient(curatorProvider.curator)
  }

  "ZooKeeper Client" should {
    "make a connection to existing namespace" in {
      val baseClient = testZkClient(testNamespace)
      baseClient.framework.close()
      println()
    }

    "set a string to existing node and get it back" in {
      val baseClient = testZkClient(testNamespace)
      val testValue = currentMillis
      val testExistingPath = "/existing-node"
      baseClient.setString(testExistingPath, testValue)

      baseClient.getString(testExistingPath) shouldEqual Some(testValue)
      baseClient.framework.close()
      println()
    }

    "get a None from non-existing node (get)" in {
      val baseClient = testZkClient(testNamespace)
      val testValue = currentMillis
      val testExistingPath = "/existing-node-" ++ testValue

      baseClient.getString(testExistingPath) shouldEqual None
      baseClient.framework.close()
      println()
    }

    "remove an existing node" in {
      val baseClient = testZkClient(testNamespace)
      val testValue = currentMillis
      val testExistingPath = "/existing-node-" ++ testValue
      baseClient.setString(testExistingPath, testValue)

      baseClient.deletePath(testExistingPath)
      baseClient.framework.close()
      println()
    }

    "gracefully remove a non-existing node" in {
      val baseClient = testZkClient(testNamespace)
      val testValue = currentMillis
      val testExistingPath = "/existing-node-" ++ testValue

      baseClient.deletePath(testExistingPath)
      baseClient.framework.close()
      println()
    }

    "set a string to non-existing node" in {
      val baseClient = testZkClient(testNamespace)
      val testValue = currentMillis
      val testExistingPath = "/existing-node-" ++ testValue
      baseClient.setString(testExistingPath, testValue)

      baseClient.getString(testExistingPath) shouldEqual Some(testValue)

      baseClient.deletePath(testExistingPath)
      baseClient.framework.close()
      println()
    }

    "get a default value from non-existing node (getOrCreate)" in {
      val baseClient = testZkClient(testNamespace)
      val testValue = currentMillis
      val testExistingPath = "/existing-node-" ++ testValue

      baseClient.getStringOrCreate(testExistingPath, testValue) shouldEqual testValue
      baseClient.getString(testExistingPath) shouldEqual Some(testValue)
      baseClient.deletePath(testExistingPath)
      baseClient.framework.close()
      println()
    }

    "connect to a non-existing namespace and create it" in {
      val baseClient = testZkClient(testNamespace)
      val testValue = currentMillis
      val nonExistingSubNamespace = "/non-existing-subnamespace-" ++ testValue
      val testNonExistingNamespace = testNamespace ++ nonExistingSubNamespace
      val testNonExistingPath = "/non-existing-node-" ++ testValue
      val testClient = testZkClient(testNonExistingNamespace)

      testClient.setString(testNonExistingPath, testValue)
      testClient.getString(testNonExistingPath) shouldEqual Some(testValue)
      testClient.deletePath(testNonExistingPath)
      testClient.framework.close()
      baseClient.deletePath(nonExistingSubNamespace)
      baseClient.framework.close()
      println()
    }

    "delete pathes recursively if asked" in {
      val baseClient = testZkClient(testNamespace)
      val testValue = currentMillis
      val nonExistingSubNode = "/non-existing-subnode-" ++ testValue
      val testNonExistingPath = nonExistingSubNode ++ "/non-existing-node-" ++ testValue

      baseClient.setString(nonExistingSubNode, testValue)
      baseClient.setString(testNonExistingPath, testValue)
      baseClient.getString(testNonExistingPath) shouldEqual Some(testValue)
      interceptCause[NotEmptyException] {
        baseClient.deletePath(nonExistingSubNode)
      }
      baseClient.deletePath(nonExistingSubNode, deleteRecursively = true)
      baseClient.framework.close()
      println()
    }

    "create a second-level nodes on set" in {
      val baseClient = testZkClient(testNamespace)
      val testValue = currentMillis
      val nonExistingSubNode = "/non-existing-subnode-" ++ testValue
      val testNonExistingPath = nonExistingSubNode ++ "/non-existing-node-" ++ testValue

      baseClient.setString(testNonExistingPath, testValue)
      baseClient.getString(testNonExistingPath) shouldEqual Some(testValue)
      baseClient.deletePath(nonExistingSubNode, deleteRecursively = true)
      baseClient.framework.close()
      println()
    }

    "create a second-level nodes on getOrCreate" in {
      val baseClient = testZkClient(testNamespace)
      val testValue = currentMillis
      val nonExistingSubNode = "/non-existing-subnode-" ++ testValue
      val testNonExistingPath = nonExistingSubNode ++ "/non-existing-node-" ++ testValue

      baseClient.getStringOrCreate(testNonExistingPath, testValue) shouldEqual testValue
      baseClient.getString(testNonExistingPath) shouldEqual Some(testValue)
      baseClient.deletePath(nonExistingSubNode, deleteRecursively = true)
      baseClient.framework.close()
      println()
    }

    "get None if path-to-node-parent doesn't exist" in {
      val baseClient = testZkClient(testNamespace)
      val testValue = currentMillis
      val nonExistingSubNode = "/non-existing-subnode-" ++ testValue
      val testNonExistingPath = nonExistingSubNode ++ "/non-existing-node-" ++ testValue

      baseClient.getString(testNonExistingPath) shouldEqual None
      baseClient.framework.close()
      println()
    }
  }

}
