package ru.yandex.vertis.general.globe.public.test

import common.geobase.model.RegionIds
import common.zio.clients.s3.S3Client
import common.zio.clients.s3.testkit.TestS3
import ru.yandex.vertis.general.globe.public.metro.MetroResource
import zio.{RIO, ZIO, ZLayer}
import zio.blocking.Blocking
import zio.duration.Duration
import zio.stream.ZStream
import zio.test.Assertion.{equalTo, hasSize}
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

import java.util.concurrent.TimeUnit.SECONDS

object MetroResourceTest extends DefaultRunnableSpec {

  private val S3Bucket = "test"
  private val MetroResourceName = "metro"

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Resource")(
      testM("Load and parse METRO resource") {
        for {
          metroResource <- ZIO.succeed(new MetroResource(S3Bucket, MetroResourceName, Duration(10, SECONDS)))
          metro <- metroResource.load
          nevskyStation = for {
            city <- metro.cities
            if city.id == RegionIds.SaintPetersburg.id
            line <- city.lines
            if line.id == "2_2"
            station <- line.stations
            if station.name.ru.startsWith("Невский")
          } yield station
        } yield assert(nevskyStation)(hasSize(equalTo(1)))
      }
    ).provideCustomLayerShared {
      val testS3 = (Blocking.live ++ TestS3.mocked) >>> ZLayer.fromEffect {
        for {
          s3 <- ZIO.service[S3Client.Service]
          _ <- s3.createBucket(S3Bucket).orDie
          _ <- uploadResource(s3, MetroResourceName).orDie
        } yield s3
      }
      testS3 ++ Blocking.live
    }

  private def uploadResource(s3Client: S3Client.Service, name: String): RIO[Blocking, Unit] =
    for {
      stream <- ZIO.effect(MetroResourceTest.getClass.getResourceAsStream(s"/$name"))
      content <- ZStream.fromInputStream(stream).runCollect
      _ <- s3Client.uploadContent[Blocking](S3Bucket, name, content.length, "text/plain", ZStream.fromIterable(content))
    } yield ()
}
