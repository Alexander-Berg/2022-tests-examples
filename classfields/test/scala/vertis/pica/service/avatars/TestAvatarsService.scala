package vertis.pica.service.avatars

import common.clients.avatars.model.{AvatarsCoordinates, ImageMeta}
import ru.yandex.vertis.pica.model.model.Image
import vertis.zio.BTask

import scala.concurrent.duration.Duration

/** @author kusaeva
  */
class TestAvatarsService extends AvatarsService {

  override def put(url: String, imageName: String, expire: Option[Duration]): BTask[Image] = ???

  override def getMeta(coordinates: AvatarsCoordinates): BTask[ImageMeta] = ???

  override def getImageInfo(coordinates: AvatarsCoordinates): BTask[Image] = ???

  override def deleteImage(coordinates: AvatarsCoordinates): BTask[Unit] = ???

  override def deleteMeta(coordinates: AvatarsCoordinates): BTask[Unit] = ???
}
