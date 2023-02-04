package common.zio.resources.test

import java.util.concurrent.TimeUnit._

import common.zio.clients.s3.S3Client
import common.zio.clients.s3.testkit.TestS3
import common.zio.resources.defaults.RegionsResource
import zio.blocking.Blocking
import zio.duration.Duration
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._
import zio.{RIO, ZIO, ZLayer}

object RegionsResourceTest extends DefaultRunnableSpec {

  private val S3Bucket = "test"
  private val MoscowRegionId = 213
  private val RegionsResourceName = "regions"

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Resource")(
      testM("Load and parse REGIONS resource") {
        for {
          regionsResource <- ZIO.succeed(new RegionsResource(S3Bucket, "regions", Duration(10, SECONDS)))
          regionTree <- regionsResource.load
          pathToRootFromMoscow = regionTree.pathToRoot(MoscowRegionId)
        } yield assert(pathToRootFromMoscow)(hasSize(equalTo(6)))
      }
    ).provideCustomLayerShared {
      val testS3 = (Blocking.live ++ TestS3.mocked) >>> ZLayer.fromEffect {
        for {
          s3 <- ZIO.service[S3Client.Service]
          _ <- s3.createBucket(S3Bucket).orDie
          _ <- uploadResource(s3, RegionsResourceName).orDie
        } yield s3
      }
      testS3 ++ Blocking.live
    }

  private def uploadResource(s3Client: S3Client.Service, name: String): RIO[Blocking, Unit] =
    for {
      stream <- ZIO.effect(RegionsResourceTest.getClass.getResourceAsStream(s"/$name"))
      content <- ZStream.fromInputStream(stream).runCollect
      _ <- s3Client.uploadContent[Blocking](S3Bucket, name, content.length, "text/plain", ZStream.fromIterable(content))
    } yield ()
}
