package vertis.pica.service.avatars

import common.clients.avatars.AvatarsClient
import common.clients.avatars.model.{AvatarsCoordinates, AvatarsError, ImageInfo, ImageMeta}
import zio.IO

import scala.concurrent.duration.Duration

class TestAvatarsClient extends AvatarsClient.Service {
  override def get(coordinates: AvatarsCoordinates, size: String): IO[AvatarsError, Array[Byte]] = ???

  override def getOriginal(coordinates: AvatarsCoordinates): IO[AvatarsError, Array[Byte]] = ???

  override def getMeta(coordinates: AvatarsCoordinates): IO[AvatarsError, ImageMeta] = ???

  override def deleteMeta(coordinates: AvatarsCoordinates): IO[AvatarsError, String] = ???

  override def delete(coordinates: AvatarsCoordinates): IO[AvatarsError, String] = ???

  override def getImageInfo(coordinates: AvatarsCoordinates): IO[AvatarsError, ImageInfo] = ???

  override def put(
      image: Array[Byte],
      namespace: String,
      imageName: Option[String],
      ttl: Option[Duration]): IO[AvatarsError, ImageInfo] = ???

  override def put(
      url: String,
      namespace: String,
      imageName: Option[String],
      ttl: Option[Duration]): IO[AvatarsError, ImageInfo] = ???

  override def getSignedPath(
      coordinates: AvatarsCoordinates,
      watermark: String,
      ttl: Option[Duration],
      alias: String): IO[AvatarsError, String] = ???
}
