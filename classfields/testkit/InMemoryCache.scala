package auto.dealers.multiposting.cache.testkit

import auto.dealers.multiposting.cache.{Cache, KeyMarshaller, ValueMarshaller}
import auto.dealers.multiposting.cache.Cache._
import zio._

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.FiniteDuration

class InMemoryCache extends Cache.Service {
  private val map = TrieMap[List[Byte], Array[Byte]]()

  override def get[K: KeyMarshaller, V: ValueMarshaller](key: K): IO[CacheError, Option[V]] =
    IO.effect(map.get(KeyMarshaller[K].marshall(key).toList))
      .flatMap {
        case Some(value) => IO.fromEither(ValueMarshaller[V].unmarshall(value)).mapBoth(MarshallerError, Some(_))
        case None => IO.none
      }
      .mapError[CacheError](CacheClientError)

  override def set[K: KeyMarshaller, V: ValueMarshaller](
      key: K,
      value: V,
      ttl: Option[FiniteDuration]): IO[Cache.CacheError, Unit] = {
    IO.effect(map.addOne((KeyMarshaller[K].marshall(key).toList, ValueMarshaller[V].marshall(value))))
      .mapError[CacheError](CacheClientError)
  }.unit

  override private[cache] def currentNamespace: String = ""

  def clean(): UIO[Unit] = UIO.effectTotal(map.clear())
}

object InMemoryCache {
  val test: ULayer[Cache] = ZLayer.succeed(new InMemoryCache)

  val clean: RIO[Has[InMemoryCache], Unit] = ZIO.accessM(_.get.clean())
}
