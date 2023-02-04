package ru.yandex.vertis.caching.plain.support

import ru.yandex.vertis.caching.support.CacheControl

import scala.concurrent.duration.FiniteDuration

/**
  * @author korvit
  */
trait PlainCacheSupport[K, V] {

  def withCacheControl(key: K,
                       expire: FiniteDuration,
                       cacheControl: CacheControl)
                      (loader: => V): V

  def withCacheControl(keys: Set[K],
                       expire: FiniteDuration,
                       cacheControl: CacheControl)
                      (loader: Set[K] => Map[K, V]): Map[K, V]

  def withCache(key: K,
                expire: FiniteDuration)
               (loader: => V): V

  def withCache(keys: Set[K],
                expire: FiniteDuration)
               (loader: Set[K] => Map[K, V]): Map[K, V]

  def withCacheRefresh(key: K,
                       expire: FiniteDuration)
                      (loader: => V): V

  def withCacheRefresh(keys: Set[K],
                       expire: FiniteDuration)
                      (loader: Set[K] => Map[K, V]): Map[K, V]
}
