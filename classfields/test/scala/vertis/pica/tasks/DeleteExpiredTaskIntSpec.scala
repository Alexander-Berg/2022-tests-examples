package vertis.pica.tasks

import java.time.Instant
import vertis.pica.Gens.{asProducer, ImageRecordGen}
import vertis.pica.dao.PicaYdbSpecBase
import vertis.pica.model.{ImageId, Url}
import vertis.pica.service.{ImageService, TestBrokerQueueService}
import vertis.ydb.YEnv
import vertis.zio.test.ZioSpecBase
import zio.ZIO
import zio.duration._

/** @author ruslansd
  */
class DeleteExpiredTaskIntSpec extends ZioSpecBase with PicaYdbSpecBase {

  private val brokerQueueServiceMock = TestBrokerQueueService.apply(namespaceConf.namespace)
  private lazy val imageService = new ImageService(storage, brokerQueueServiceMock)

  private lazy val task = new DeleteExpiredTask(imageService, namespaceConf.partitioning)

  private val expiration = 3

  "DeleteExpiredTask" should {

    "do nothing on empty base" in ydbTest {
      task.task
    }

    "delete all expired images" in ydbTest {
      val expiredRecords =
        ImageRecordGen.next(5).map(_.copy(expireTs = Some(Instant.now().minusSeconds(expiration.day.toSeconds))))
      for {
        _ <- ZIO.foreach(expiredRecords)(imageService.upsert)
        ids <- ZIO.collectAll(expiredRecords.map(r => Url.create(r.originalUrl).map(ImageId.apply)))
        _ <- allImagesExists(ids).checkResult { existsAll =>
          existsAll shouldBe true
        }
        _ <- task.task
        _ <- allImagesNotExists(ids).checkResult { notExistsAll =>
          notExistsAll shouldBe true
        }
      } yield ()
    }

    "delete only expired images" in ydbTest {
      val expiredRecords =
        ImageRecordGen.next(5).map(_.copy(expireTs = Some(Instant.now().minusSeconds(expiration.day.toSeconds))))
      val nonExpiredImages =
        ImageRecordGen.next(5).map(_.copy(expireTs = Some(Instant.now().plusSeconds(1.day.toSeconds))))
      for {
        _ <- ZIO.foreach(expiredRecords ++ nonExpiredImages)(imageService.upsert)
        expireIds <- ZIO.collectAll(expiredRecords.map(r => Url.create(r.originalUrl).map(ImageId.apply)))
        nonExpiredIds <- ZIO.collectAll(nonExpiredImages.map(r => Url.create(r.originalUrl).map(ImageId.apply)))
        _ <- allImagesExists(nonExpiredIds ++ expireIds).checkResult { existsAll =>
          existsAll shouldBe true
        }
        _ <- task.task
        _ <- allImagesExists(nonExpiredIds).checkResult { nonExpiredExists =>
          nonExpiredExists shouldBe true
        }
        _ <- allImagesNotExists(expireIds).checkResult { expiredNonExists =>
          expiredNonExists shouldBe true
        }
      } yield ()
    }
  }

  private def allImagesExists(ids: Iterable[ImageId]): ZIO[YEnv, Throwable, Boolean] =
    ZIO.collectAll(ids.map(id => imageService.get(id).map(_.isDefined))).map(_.forall(e => e))

  private def allImagesNotExists(ids: Iterable[ImageId]): ZIO[YEnv, Throwable, Boolean] =
    ZIO.collectAll(ids.map(id => imageService.get(id).map(_.isDefined))).map(_.forall(e => !e))
}
