package ru.yandex.vertis.caching.base

import ru.yandex.vertis.caching.plain.base.{PlainCache, SyncPlainCacheAdapter}

/**
  * @author korvit
  */
trait SyncCacheSpec
  extends PlainCacheSpec {

  protected val syncCache: SyncCache[String, Int]

  override protected lazy val cache: PlainCache[String, Int] =
    new SyncPlainCacheAdapter[String, Int](syncCache)
}
