package ru.yandex.vertis.caching.plain.base

import ru.yandex.vertis.caching.base.SyncCache

import scala.concurrent.duration.Duration

/**
  * @author korvit
  */
class SyncPlainCacheAdapter[K, V](val syncCache: SyncCache[K, V])
  extends PlainCache[K, V] {

  override def get(key: K): Option[V] =
    syncCache.get(key).get

  override def multiGet(keys: Set[K]): Map[K, V] =
    syncCache.multiGet(keys).get

  override def set(key: K, value: V, expire: Duration): Unit =
    syncCache.set(key, value, expire).get

  override def multiSet(entries: Map[K, V], expire: Duration): Unit =
    syncCache.multiSet(entries, expire).get

  override def delete(key: K): Unit =
    syncCache.delete(key).get

  override def multiDelete(keys: Set[K]): Unit =
    syncCache.multiDelete(keys).get
}
