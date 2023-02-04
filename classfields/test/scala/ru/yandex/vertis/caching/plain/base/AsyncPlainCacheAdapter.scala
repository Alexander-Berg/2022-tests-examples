package ru.yandex.vertis.caching.plain.base

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import ru.yandex.vertis.caching.base.AsyncCache

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * @author korvit
  */
class AsyncPlainCacheAdapter[K, V](val asyncCache: AsyncCache[K, V],
                                   implicit val ec: ExecutionContext,
                                   timeout: Span = Span(1000, Millis),
                                   interval: Span = Span(20, Millis))
  extends PlainCache[K, V]
    with ScalaFutures {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout, interval)

  override def get(key: K): Option[V] =
    asyncCache.get(key).futureValue

  override def multiGet(keys: Set[K]): Map[K, V] =
    asyncCache.multiGet(keys).futureValue

  override def set(key: K, value: V, expire: Duration): Unit =
    asyncCache.set(key, value, expire).futureValue

  override def multiSet(entries: Map[K, V], expire: Duration): Unit =
    asyncCache.multiSet(entries, expire).futureValue

  override def delete(key: K): Unit =
    asyncCache.delete(key).futureValue

  override def multiDelete(keys: Set[K]): Unit =
    asyncCache.multiDelete(keys).futureValue
}
