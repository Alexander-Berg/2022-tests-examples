package ru.yandex.vertis.caching.redis

import ru.yandex.vertis.caching.base.ContainerIntSpec

/**
  * @author korvit
  */
trait RedisCacheSpecBase
  extends ContainerIntSpec {

  override protected def containerName = "redis:latest"
}
