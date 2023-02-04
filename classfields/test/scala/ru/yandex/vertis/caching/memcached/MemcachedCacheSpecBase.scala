package ru.yandex.vertis.caching.memcached

import java.net.InetSocketAddress

import net.spy.memcached.MemcachedClient
import ru.yandex.vertis.caching.base.ContainerIntSpec

/**
  * @author korvit
  */
trait MemcachedCacheSpecBase
  extends ContainerIntSpec {

  override protected def containerName = "memcached:latest"

  val client: MemcachedClient =
    new MemcachedClient(new InetSocketAddress("localhost", container.getMappedPort(11211)))
}
