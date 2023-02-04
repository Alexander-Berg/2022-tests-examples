package ru.yandex.vertis.curator.recipes.discovery

import org.apache.curator.x.discovery.details.JsonInstanceSerializer
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.curator.ZooKeeperAware
import ru.yandex.vertis.curator.recipes.discovery.CacheChangeAwaiter.usingPool
import ru.yandex.vertis.curator.recipes.discovery.TestData.Deploys._
import ru.yandex.vertis.curator.util.CuratorUtils

/**
 * Specs on [[DeployAwareServiceDiscoveryPool]]
 */
class DeployAwareServiceDiscoveryPoolSpec
  extends WordSpec
  with Matchers
  with ZooKeeperAware {

  val service1 = "service-1"
  val service2 = "service-2"
  val service3 = "service-3"

  val curator = CuratorUtils.fixNamespace(curatorBase, "DeployAwareServiceDiscoveryPoolSpec")

  val discovery = new DeployAwareServiceDiscoveryPool[String](
    curator,
    "/test",
    Iterable(service1, service2, service3),
    new JsonInstanceSerializer[String](classOf[String]))

  override protected def beforeAll() {
    super.beforeAll()
    discovery.start()
  }

  override protected def afterAll() {
    discovery.close()
    super.afterAll()
  }

  "DeployAwareServiceDiscovery" should {

    "provide empty service instances" in {
      discovery.serviceInstances(service1) should be('empty)
      discovery.cachedServiceInstances(service1) should be('empty)
    }

    "not register alien service instance" in {
      an[IllegalArgumentException] should be thrownBy {
        discovery.register(
          csbo1ft,
          TestData.serviceInstance(
            id = "foo",
            serviceName = "alien-service",
            payload = "bar"))
      }
    }

    "provide added service instance" in {
      val instance = service1instance("foo", "bar")
      val dsi = DeployedServiceInstance(csbo1ft, instance)

      usingPool(discovery)(_.register(dsi))

      discovery.serviceInstances(service1).toSet should be(Set(dsi))
      discovery.cachedServiceInstances(service1).toSet should be(Set(dsi))
      discovery.plainServiceInstances(service1).toSet should be(Set(instance))
      discovery.cachedPlainServiceInstances(service1).toSet should be(Set(instance))

      discovery.serviceInstances(service2) should be('empty)
      discovery.cachedServiceInstances(service2) should be('empty)

      discovery.serviceInstances(service3) should be('empty)
      discovery.cachedServiceInstances(service3) should be('empty)
    }

    "not provide removed service instance" in {
      val instance = service1instance("foo", "bar")
      val dsi = DeployedServiceInstance(csbo1ft, instance)

      usingPool(discovery)(_.unregister(dsi))

      discovery.serviceInstances(service1) should be('empty)
      discovery.cachedServiceInstances(service1) should be('empty)
      discovery.plainServiceInstances(service1) should be('empty)
      discovery.cachedPlainServiceInstances(service1) should be('empty)
    }

    "silently re-register service instance" in {
      val instance = service1instance("foo", "payload")
      discovery.unregister(csbo1ft, instance)
      try {
        discovery.unregister(csbo1ft, instance)
      } catch {
        case e: Exception => fail(e)
      }
    }

    "silently unregister non-existed service instance" in {
      val instance = service1instance("not-existed", "payload")
      try {
        discovery.unregister(csbo1ft, instance)
        discovery.register(csbo1ft, instance)
      } catch {
        case e: Exception => fail(e)
      }
    }
  }

  private def service1instance(id: String, payload: String) =
    TestData.serviceInstance(id, payload, service1)
}
