package ru.yandex.vertis.caching.plain.support

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import ru.yandex.vertis.caching.support.{AsyncCacheSupport, CacheControl}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

/**
  * @author korvit
  */
class AsyncPlainCacheSupportAdapter[K, V](asyncCacheSupport: AsyncCacheSupport[K, V],
                                          implicit val ec: ExecutionContext,
                                          timeout: Span = Span(150, Millis),
                                          interval: Span = Span(15, Millis))
  extends PlainCacheSupport[K, V]
    with ScalaFutures {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout, interval)

  def withCacheControl(key: K,
                       expire: FiniteDuration,
                       cacheControl: CacheControl)
                      (loader: => V): V =
    asyncCacheSupport.withCacheControl(key, expire, cacheControl)(Future(loader)).futureValue

  def withCacheControl(keys: Set[K],
                       expire: FiniteDuration,
                       cacheControl: CacheControl)
                      (loader: Set[K] => Map[K, V]): Map[K, V] =
    asyncCacheSupport.withCacheControl(keys, expire, cacheControl)(keys => Future(loader(keys))).futureValue

  def withCache(key: K,
                expire: FiniteDuration)
               (loader: => V): V =
    asyncCacheSupport.withCache(key, expire)(Future(loader)).futureValue

  def withCache(keys: Set[K],
                expire: FiniteDuration)
               (loader: Set[K] => Map[K, V]): Map[K, V] =
    asyncCacheSupport.withCache(keys, expire)(keys => Future(loader(keys))).futureValue

  def withCacheRefresh(key: K,
                       expire: FiniteDuration)
                      (loader: => V): V =
    asyncCacheSupport.withCacheRefresh(key, expire)(Future(loader)).futureValue

  def withCacheRefresh(keys: Set[K],
                       expire: FiniteDuration)
                      (loader: Set[K] => Map[K, V]): Map[K, V] =
    asyncCacheSupport.withCacheRefresh(keys, expire)(keys => Future(loader(keys))).futureValue
}
