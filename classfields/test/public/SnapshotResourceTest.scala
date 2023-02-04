package ru.yandex.vertis.general.bonsai.logic.test.public

import java.util.concurrent.TimeUnit._

import common.zio.clients.s3.S3Client
import common.zio.clients.s3.testkit.TestS3
import general.bonsai.category_model.Category
import ru.yandex.vertis.general.bonsai.logic.public.InnerBonsaiSnapshot
import ru.yandex.vertis.general.bonsai.model.{EntityRef, EntityRefWithVersion, Latest}
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshotResource
import zio.blocking.Blocking
import zio.duration.Duration
import zio.test.Assertion._
import zio.test._
import zio.{ZIO, ZLayer}

object SnapshotResourceTest extends DefaultRunnableSpec {

  private val S3Bucket = "test"
  private val S3Key = "snapshot"
  private val CategoryId = "id"

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SnapshotResource")(
      testM("load and parse SNAPSHOT resource") {
        for {
          snapshotResource <- ZIO.succeed(
            new BonsaiSnapshotResource(S3Bucket, S3Key, Duration(10, SECONDS), InnerBonsaiSnapshot.createSnapshot)
          )
          snapshot <- snapshotResource.load
        } yield {
          val categoriesIds = snapshot
            .selectCategories(Set(EntityRefWithVersion(EntityRef("category", CategoryId), Latest)))
            .values
            .map(_.id)
          assert(categoriesIds)(contains(CategoryId))
        }
      }
    ).provideCustomLayerShared {
      val testS3 = (Blocking.live ++ TestS3.mocked) >>> ZLayer.fromEffect {
        for {
          s3 <- ZIO.service[S3Client.Service]
          _ <- s3.createBucket(S3Bucket)
          data <- PublicLogicTestUtils.writeDelimitedToStream(Seq(Category(id = "id")), Seq.empty)
          collected <- data.runCollect
          _ <- s3.uploadContent(S3Bucket, S3Key, collected.length, "application/protobuf", data)
        } yield s3
      }.orDie
      testS3 ++ Blocking.live
    }
  }
}
