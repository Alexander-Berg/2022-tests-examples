package common.zio.redis.sentinel.testkit

import common.zio.redis.sentinel.RedisSentinelClient
import zio.stm.TMap
import zio.{Has, Task, UIO, URIO, ZIO}

import scala.concurrent.duration.FiniteDuration

class InMemoryRedisSentinelClient(storage: TMap[String, Array[Byte]]) extends RedisSentinelClient.Service {

  override def get(key: Array[Byte]): Task[Option[Array[Byte]]] =
    storage.get(key.map(_.toChar).mkString).commit

  override def set(key: Array[Byte], value: Array[Byte], ttl: Option[FiniteDuration]): Task[Unit] =
    storage.put(key.map(_.toChar).mkString, value).commit

  def clear: UIO[Unit] = storage.removeIf((_, _) => true).commit

  override def mget(keys: Seq[Array[Byte]]): Task[Seq[Array[Byte]]] =
    ZIO.foreach(keys)(get).map(_.map(_.orNull))

  override def mset(keysvalues: Seq[(Array[Byte], Array[Byte])], ttl: Option[FiniteDuration]): Task[Unit] =
    ZIO.foreach_(keysvalues)(v => set(v._1, v._2, None))

  override def delete(key: Array[Byte]): Task[Unit] = storage.delete(key.map(_.toChar).mkString).commit

  override def mdelete(keys: Seq[Array[Byte]]): Task[Unit] = ZIO.foreach_(keys)(delete)
}

object InMemoryRedisSentinelClient {
  def clear: URIO[InMemoryRedisSentinelClient, Unit] = ZIO.accessM[InMemoryRedisSentinelClient](_.clear)

  val test = TMap
    .empty[String, Array[Byte]]
    .map { tmap =>
      val client = new InMemoryRedisSentinelClient(tmap)
      Has.allOf[RedisSentinelClient.Service, InMemoryRedisSentinelClient](client, client)
    }
    .commit
    .toLayerMany
}
