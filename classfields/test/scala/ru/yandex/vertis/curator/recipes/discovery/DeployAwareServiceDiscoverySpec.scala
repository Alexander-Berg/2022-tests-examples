package ru.yandex.vertis.curator.recipes.discovery

import org.apache.curator.x.discovery.details.JsonInstanceSerializer
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.curator.ZooKeeperAware
import ru.yandex.vertis.curator.recipes.discovery.CacheChangeAwaiter.using
import ru.yandex.vertis.curator.recipes.discovery.TestData.Deploys._
import ru.yandex.vertis.curator.recipes.discovery.TestData._
import ru.yandex.vertis.curator.util.CuratorUtils

/**
 * Specs on [[DeployAwareServiceDiscovery]]
  */
class DeployAwareServiceDiscoverySpec
  extends WordSpec
  with Matchers
  with ZooKeeperAware {

  val testServiceName = "test"
  val curator = CuratorUtils.fixNamespace(curatorBase, "DeployAwareServiceDiscoverySpec")

  val discovery = new DeployAwareServiceDiscovery[String](
    curator,
    "/test",
    testServiceName,
    new JsonInstanceSerializer[String](classOf[String]))

  override protected def beforeAll() {
    super.beforeAll()
    discovery.start()
  }

  "DeployAwareServiceDiscovery" should {
    "provide empty service instances" in {
      discovery.serviceInstances should be('empty)
      discovery.cachedServiceInstances should be('empty)
    }

    "not register alien service instance" in {
      an [IllegalArgumentException] should be thrownBy {
        discovery.register(
          csbo1ft,
          TestData.serviceInstance(
            id = "foo",
            serviceName = "alien-service",
            payload = "bar"))
      }
    }

    "provide added service instance" in {
      val instance = serviceInstance("foo", "bar")
      val dsi = DeployedServiceInstance(csbo1ft, instance)

      using(discovery)(_.register(dsi))

      discovery.serviceInstances.toSet should be(Set(dsi))
      discovery.cachedServiceInstances.toSet should be(Set(dsi))
      discovery.plainServiceInstances.toSet should be(Set(instance))
      discovery.cachedPlainServiceInstances.toSet should be(Set(instance))
    }

    "not provide removed service instance" in {
      val instance = serviceInstance("foo", "bar")
      val dsi = DeployedServiceInstance(csbo1ft, instance)

      using(discovery)(_.unregister(dsi))

      discovery.serviceInstances should be('empty)
      discovery.cachedServiceInstances should be('empty)
      discovery.plainServiceInstances should be('empty)
      discovery.cachedPlainServiceInstances should be('empty)
    }

    "provide correct list of deploys" in {
      val instanceFoo = serviceInstance("foo", "payload1")
      val instanceBar = serviceInstance("bar", "payload2")

      using(discovery, eventsCount = 4) {
        d => d.register(csbo1ft, instanceFoo)
          d.register(csbo1ft, instanceBar)
          d.register(csbo2ft, instanceFoo)
          d.register(csbo1gt, instanceFoo)
      }

      val allDeploys = Set(csbo1ft, csbo2ft, csbo1gt)
      val allDataCenters = allDeploys.map(_.dataCenter)

      discovery.serviceInstances.deploys should be(allDeploys)
      discovery.serviceInstances.dataCenters should be(allDataCenters)

      using(discovery)(_.unregister(csbo1ft, instanceBar))
      discovery.serviceInstances.deploys should be(allDeploys)
      discovery.serviceInstances.dataCenters should be(allDataCenters)

      using(discovery)(_.unregister(csbo1ft, instanceFoo))
      discovery.serviceInstances.deploys should be(allDeploys - csbo1ft)
      discovery.serviceInstances.dataCenters should be(allDataCenters)

      using(discovery)(_.unregister(csbo2ft, instanceFoo))
      discovery.serviceInstances.deploys should be(allDeploys - csbo1ft - csbo2ft)
      discovery.serviceInstances.dataCenters should be(Set(DataCenters.Fol))
    }

    "silently re-register service instance" in {
      val instance = serviceInstance("foo", "payload")
      discovery.unregister(csbo1ft, instance)
      try {
        discovery.unregister(csbo1ft, instance)
      } catch {
        case e: Exception => fail(e)
      }
    }

    "silently unregister non-existed service instance" in {
      val instance = serviceInstance("not-existed", "payload")
      try {
        discovery.unregister(csbo1ft, instance)
        discovery.register(csbo1ft, instance)
      } catch {
        case e: Exception => fail(e)
      }
    }
  }

  override protected def afterAll() {
    discovery.close()
    super.afterAll()
  }

  private def serviceInstance(id: String, payload: String) =
    TestData.serviceInstance(id, payload, testServiceName)
}
