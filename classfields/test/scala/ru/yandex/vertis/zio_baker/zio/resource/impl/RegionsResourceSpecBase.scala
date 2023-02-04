package ru.yandex.vertis.zio_baker.zio.resource.impl

import com.softwaremill.tagging._
import ru.yandex.vertis.zio_baker.geobase.Tree
import ru.yandex.vertis.zio_baker.model.{GeobaseId, Tag}
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import ru.yandex.vertis.zio_baker.zio.resource.impl.RegionsResource.Config
import ru.yandex.vertis.zio_baker.zio.resource.impl.S3Resource.Env
import common.zio.clients.s3.S3Client
import common.zio.clients.s3.S3Client.S3Client
import common.zio.clients.s3.testkit.TestS3
import zio.blocking.Blocking
import zio.stream.ZStream
import zio.{Has, RIO, ULayer, ZIO, ZLayer}

import scala.concurrent.duration._

object RegionsResourceSpecBase {
  val RussiaRegionId: GeobaseId = 225L.taggedWith[Tag.GeobaseId]

  val MoscowRegionId: GeobaseId = 213L.taggedWith[Tag.GeobaseId]
  val SpbRegionId: GeobaseId = 2L.taggedWith[Tag.GeobaseId]

  val CrimeaRegionId: GeobaseId = 977L.taggedWith[Tag.GeobaseId]
  val YaltaRegionId: GeobaseId = 11470L.taggedWith[Tag.GeobaseId]

  val ChechenRegionId: GeobaseId = 11024L.taggedWith[Tag.GeobaseId]
  val GroznyRegionId: GeobaseId = 1106L.taggedWith[Tag.GeobaseId]

  val KarachayCherkessRegionId: GeobaseId = 11020L.taggedWith[Tag.GeobaseId]
  val CherkesskRegionId: GeobaseId = 1104L.taggedWith[Tag.GeobaseId]

  val RegionsResourceConfig: Config = Config("test", "regions", 10.seconds)

  lazy val s3ClientLayer: ULayer[S3Client] = Blocking.live ++ TestS3.mocked >>> ZLayer.fromEffect {
    for {
      s3Service <- ZIO.service[S3Client.Service]
      _ <- s3Service.createBucket(RegionsResourceConfig.bucket).orDie
      _ <- uploadResource(s3Service, RegionsResourceConfig.key).orDie
    } yield s3Service
  }

  lazy val regionsResourceLayer: ULayer[Has[Resource[Env, Tree]]] =
    ZLayer.succeed(RegionsResourceConfig) >>> RegionsResource.live

  private def uploadResource(s3Client: S3Client.Service, name: String): RIO[Blocking, Unit] =
    for {
      stream <- ZIO.effect(this.getClass.getResourceAsStream(s"/$name.xml"))
      content <- ZStream.fromInputStream(stream).runCollect
      _ <- s3Client.uploadContent[Blocking](
        RegionsResourceConfig.bucket,
        name,
        content.length.toLong,
        "text/plain",
        ZStream.fromIterable(content)
      )
    } yield ()
}
