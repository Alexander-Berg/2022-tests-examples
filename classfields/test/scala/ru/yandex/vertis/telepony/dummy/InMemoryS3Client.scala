package ru.yandex.vertis.telepony.dummy

import java.net.URL
import java.util.concurrent.ConcurrentHashMap

import ru.yandex.vertis.telepony.generator.Generator.ShortStr
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.Url
import ru.yandex.vertis.telepony.service.S3Client

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class InMemoryS3Client extends S3Client {

  import ru.yandex.vertis.telepony.util.Threads.lightWeightTasksEc

  private val map = new ConcurrentHashMap[String, Array[Byte]]()

  override def put(key: String, bytes: Array[Byte]): Future[Url] =
    Future.successful {
      map.put(key, bytes)
      ShortStr.next
    }

  override def delete(key: String): Future[Unit] =
    Future.successful {
      map.remove(key)
    }

  override def get(key: String): Future[Array[Byte]] =
    Future {
      Option(map.get(key)).get
    }

  override def getUrl(key: String, ttl: FiniteDuration): Future[URL] =
    Future.successful {
      new URL("https://example.com")
    }
}
