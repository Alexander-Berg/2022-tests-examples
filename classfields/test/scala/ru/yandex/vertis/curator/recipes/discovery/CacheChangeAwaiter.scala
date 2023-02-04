package ru.yandex.vertis.curator.recipes.discovery

import java.util.concurrent.{TimeUnit, CountDownLatch}

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.state.ConnectionState
import org.apache.curator.x.discovery.ServiceInstance
import org.apache.curator.x.discovery.details.ServiceCacheListener
import ru.yandex.vertis.curator.recipes.map.{Event, EventListener}

/**
 * Helps to wait changes of service discovery cache.
 *
 * @author dimas
 */
class CacheChangeAwaiter[A](eventsCount: Int = 1)
  extends EventListener[Event[String, ServiceInstance[A]]] {

  private val cdl = new CountDownLatch(eventsCount)

  override def onEvent(event: Event[String, ServiceInstance[A]]): Unit = {
    cdl.countDown()
  }

  def cacheChanged(): Unit = cdl.countDown()

  def await() = {
    if (!cdl.await(10, TimeUnit.SECONDS))
      throw new AssertionError("Cache change expected")
  }
}

object CacheChangeAwaiter {
  /**
   * Uses [[DeployAwareServiceDiscovery]] by performing given action
   * and waits until cache will be changed.
   */
  def using[A, B](discovery: DeployAwareServiceDiscovery[A], eventsCount: Int = 1)
                 (action: DeployAwareServiceDiscovery[A] => B): B = {
    val awaiter = new CacheChangeAwaiter[A](eventsCount)
    discovery.usedMap.subscribe(awaiter)
    try {
      val result = action(discovery)
      awaiter.await()
      result
    }
    finally awaiter.await()
  }

  /**
   * Uses [[DeployAwareServiceDiscoveryPool]] by performing given action
   * and waits until some underlying cache will be changed.
   */
  def usingPool[A, B](pool: DeployAwareServiceDiscoveryPool[A], eventsCount: Int = 1)
                 (action: DeployAwareServiceDiscoveryPool[A] => B): B = {
    val awaiter = new CacheChangeAwaiter[A](eventsCount)
    pool.discoveries.foreach(_.usedMap.subscribe(awaiter))
    try {
      val result = action(pool)
      awaiter.await()
      result
    }
    finally awaiter.await()
  }
}
