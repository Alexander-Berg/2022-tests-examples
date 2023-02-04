package ru.yandex.vertis.caching.plain.support

import ru.yandex.vertis.caching.support.{CacheControl, SyncCacheSupport}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
  * @author korvit
  */
class SyncPlainCacheSupportAdapter[K, V](syncCacheSupport: SyncCacheSupport[K, V])
  extends PlainCacheSupport[K, V] {

  def withCacheControl(key: K,
                       expire: FiniteDuration,
                       cacheControl: CacheControl)
                      (loader: => V): V =
    syncCacheSupport.withCacheControl(key, expire, cacheControl)(Try(loader)).get

  def withCacheControl(keys: Set[K],
                       expire: FiniteDuration,
                       cacheControl: CacheControl)
                      (loader: Set[K] => Map[K, V]): Map[K, V] =
    syncCacheSupport.withCacheControl(keys, expire, cacheControl)(keys => Try(loader(keys))).get

  def withCache(key: K,
                expire: FiniteDuration)
               (loader: => V): V =
    syncCacheSupport.withCache(key, expire)(Try(loader)).get

  def withCache(keys: Set[K],
                expire: FiniteDuration)
               (loader: Set[K] => Map[K, V]): Map[K, V] =
    syncCacheSupport.withCache(keys, expire)(keys => Try(loader(keys))).get

  def withCacheRefresh(key: K,
                       expire: FiniteDuration)
                      (loader: => V): V =
    syncCacheSupport.withCacheRefresh(key, expire)(Try(loader)).get

  def withCacheRefresh(keys: Set[K],
                       expire: FiniteDuration)
                      (loader: Set[K] => Map[K, V]): Map[K, V] =
    syncCacheSupport.withCacheRefresh(keys, expire)(keys => Try(loader(keys))).get
}
