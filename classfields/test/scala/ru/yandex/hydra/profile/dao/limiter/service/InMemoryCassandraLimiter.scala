package ru.yandex.hydra.profile.dao.limiter.service

import java.util.concurrent.TimeUnit

import com.google.common.cache.{Cache, CacheBuilder}
import ru.yandex.hydra.profile.dao.limiter.dao.CassandraLimiterDao

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

/** @author @logab
  */
class InMemoryCassandraLimiter(ttl: Int, unit: TimeUnit) extends CassandraLimiterDao {
  override protected def scope: String = "local"

  var map = Map.empty[String, Cache[Integer, Integer]]

  def cache(map: Map[String, Cache[Integer, Integer]], user: String): Cache[Integer, Integer] =
    map.getOrElse(user, CacheBuilder.newBuilder().expireAfterWrite(ttl, unit).build[Integer, Integer]())

  override def dump(
      user: String,
      timeCounter: Int,
      value: Int
    )(implicit executionContext: ExecutionContext): Future[Unit] =
    synchronized {
      val m = cache(map, user)
      m.put(timeCounter, value)
      map += user -> m
      Future.successful(())
    }

  def clear(): Unit = synchronized {
    map = Map.empty
  }

  override def get(user: String)(implicit executionContext: ExecutionContext): Future[Int] =
    synchronized {
      Future.successful(cache(map, user).asMap().values().asScala.map(_.toInt).sum)
    }

}
