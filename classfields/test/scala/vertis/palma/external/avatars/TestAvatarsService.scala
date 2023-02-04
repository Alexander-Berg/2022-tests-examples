package vertis.palma.external.avatars

import common.clients.avatars.model.AvatarsCoordinates
import ru.yandex.vertis.palma.images.images.Image
import zio.Task

import scala.concurrent.duration.Duration

/** @author kusaeva
  */
class TestAvatarsService extends AvatarsService {
  override def putFile(namespace: String, content: Array[Byte], expire: Option[Duration]): Task[Image] = ???

  override def putUrl(namespace: String, url: String, expire: Option[Duration]): Task[Image] = ???

  override def delete(coordinates: AvatarsCoordinates): Task[Unit] = ???

  override def getSignedPath(
      coordinates: AvatarsCoordinates,
      watermark: String,
      ttl: Option[Duration],
      alias: String): Task[String] = ???

  override def urlForAlias(coordinates: AvatarsCoordinates, alias: String): String =
    s"localhost:80/get-${coordinates.namespace}/${coordinates.groupId}/${coordinates.name}/$alias"

  override def defaultNamespace: String = ???
}
