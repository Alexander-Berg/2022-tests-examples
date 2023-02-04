package common.clients.uploader.testkit

import common.clients.uploader.Uploader
import common.clients.uploader.Uploader._
import zio.{Task, ZIO, ZLayer}

import scala.concurrent.duration.FiniteDuration

class NoOpUploader extends Uploader.Service {

  override def signUrl(
      namespace: String,
      signTtl: Option[FiniteDuration] = None,
      imageTtl: Option[FiniteDuration] = None,
      imageBounds: Option[Bounds] = None): Task[String] = ZIO.succeed("")

  override def signS3(
      bucket: String,
      path: Option[String],
      ttl: Option[FiniteDuration],
      cluster: Option[Cluster],
      urlType: Option[UrlType],
      overrideContentType: Option[String],
      overrideContentDisposition: Option[String]): Task[String] = ZIO.succeed("")
}

object NoOpUploader {

  val live: ZLayer[Any, Nothing, Uploader] = ZIO.succeed(new NoOpUploader).toLayer
}
