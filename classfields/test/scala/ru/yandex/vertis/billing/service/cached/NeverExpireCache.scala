package ru.yandex.vertis.billing.service.cached

import net.spy.memcached.CachedData
import ru.yandex.vertis.billing.service.cached.impl.{Codec, KryoCodec}
import ru.yandex.vertis.caching.base.impl.inmemory.InMemorySyncCache
import ru.yandex.vertis.caching.base.{AsyncCache, SyncCache}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author @logab
  */
class NeverExpireCache(val codec: Codec = KryoCodec) extends Cache {

  private val layout = new DefaultLayout {}

  val cache: SyncCache[String, CachedData] =
    new InMemorySyncCache[String, CachedData](layout)

  val asyncCache: AsyncCache[String, CachedData] =
    new AsyncCache[String, CachedData] {

      override def get(key: String)(implicit ec: ExecutionContext): Future[Option[CachedData]] =
        Future.fromTry(cache.get(key))

      override def multiGet(keys: Set[String])(implicit ec: ExecutionContext): Future[Map[String, CachedData]] =
        Future.fromTry(cache.multiGet(keys))

      override def set(key: String, value: CachedData, expire: Duration)(implicit ec: ExecutionContext): Future[Unit] =
        Future.fromTry(cache.set(key, value, expire))

      override def multiSet(
          entries: Map[String, CachedData],
          expire: Duration
        )(implicit ec: ExecutionContext): Future[Unit] =
        Future.fromTry(cache.multiSet(entries, expire))

      override def delete(key: String)(implicit ec: ExecutionContext): Future[Unit] =
        Future.fromTry(cache.delete(key))

      override def multiDelete(keys: Set[String])(implicit ec: ExecutionContext): Future[Unit] =
        Future.fromTry(cache.multiDelete(keys))
    }

}
