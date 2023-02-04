package ru.yandex.realty.traffic.service.dao

import com.google.protobuf.Int32Value
import org.junit.runner.RunWith
import ru.yandex.realty.traffic.model.s3.S3Path.S3DirPath
import ru.yandex.realty.traffic.service.dao.VersionedDao.VersionedDao
import ru.yandex.realty.traffic.service.dao.live.S3VersionedDao
import ru.yandex.realty.traffic.utils.StoredEntriesFile.StoredFormat
import zio._
import zio.test._
import zio.test.junit._
import zio.test.Assertion._
import eu.timepit.refined.auto._
import ru.yandex.realty.traffic.model.s3.S3Path
import ru.yandex.realty.traffic.service.s3.S3
import ru.yandex.realty.traffic.service.s3.S3.S3
import ru.yandex.realty.traffic.testkit.InMemoryS3
import ru.yandex.realty.traffic.testkit.InMemoryS3.InMemoryS3Exception.NoFileFound
import ru.yandex.realty.traffic.utils.{FilesService, StoredEntriesFile}
import ru.yandex.realty.traffic.utils.FilesService.{FilesService, FilesServiceConfig}
import zio.blocking.Blocking
import zio.stream.ZStream

import java.nio.file.Files
import scala.tools.nsc.interpreter.{InputStream, OutputStream}

@RunWith(classOf[ZTestJUnitRunner])
class VersionedDaoSpec extends JUnitRunnableSpec {
  import VersionedDaoSpec._

  type S3SpecEnv = VersionedDao[Int] with S3 with FilesService with Blocking

  private def s3SpecLayer: Layer[Any, S3SpecEnv] = {
    val blocking = Blocking.live
    val fs = Task
      .effect(FilesServiceConfig(Files.createTempDirectory("VersionedDaoSpec")))
      .toLayer >>> FilesService.live

    val s3 = blocking >>> InMemoryS3.live >>> S3.logged
    val dao = blocking ++ fs ++ ZLayer.succeed(Config) ++ s3 >>> VersionedDao.s3Live[Int]

    dao ++ s3 ++ fs ++ blocking
  }

  private def s3DaoSpec(f: => RIO[S3SpecEnv, TestResult]): IO[Any, TestResult] =
    (for {
      _ <- S3.createBucket(S3Dir.bucket)
      _ <- S3.list(S3Dir)
      result <- f
      _ <- FilesService.freeAllTemporary()
    } yield result).provideLayer(s3SpecLayer)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("VersionedDao")(
      s3Suite
    )

  private def s3Suite =
    suite("when it's above s3")(
      testM("should fail on current version when no versions") {
        s3DaoSpec(assertM(VersionedDao.currentVersion[Int]().run)(fails(isSubtype[NoFileFound](anything))))
      },
      testM("should correctly set and get version") {
        s3DaoSpec {
          VersionedDao.setCurrentVersion[Int](1) *>
            VersionedDao.currentVersion[Int]().map(v => assertTrue(v == 1))
        }
      },
      testM("should correctly upload and load versions") {
        s3DaoSpec {
          val version = 1L
          val versionDir =
            S3Dir.child(S3Path.makePathPart(version))

          val elements: Seq[Int] = 1 to 100

          for {
            _ <- VersionedDao.uploadVersion[Int](version, stubEntriesFiles(elements))
            partitionsCount <- S3.listFiles(versionDir).map(_.size)
            loaded <- VersionedDao.loadVersion[Int](version)
            actualElements <- loaded.read.runCollect
          } yield assertTrue(partitionsCount == Partitions) && assert(actualElements)(hasSameElements(elements))
        }
      },
      testM("should correctly return list") {
        s3DaoSpec {
          val versions: Seq[Long] = 1L to 5
          for {
            _ <- ZIO.foreach_(versions)(VersionedDao.uploadVersion[Int](_, stubEntriesFiles(1 to 100)))
            existing <- VersionedDao.listVersions[Int]()
          } yield assert(existing)(hasSameElements(versions))
        }
      },
      testM("should correctly delete version") {
        s3DaoSpec {
          val versions: Seq[Long] = 1L to 5
          for {
            _ <- ZIO.foreach_(versions)(VersionedDao.uploadVersion[Int](_, stubEntriesFiles(1 to 100)))
            beforeDelete <- VersionedDao.listVersions[Int]()
            _ <- VersionedDao.deleteVersion[Int](3)
            _ <- VersionedDao.deleteVersion[Int](3)
            _ <- VersionedDao.deleteVersion[Int](2)
            afterDelete <- VersionedDao.listVersions[Int]()
          } yield assert(beforeDelete)(hasSameElements(versions)) && assert(afterDelete)(
            hasSameElements(Seq(1L, 4L, 5L))
          )
        }
      }
    )
}

object VersionedDaoSpec {

  private val S3Dir = S3DirPath(
    "bucket",
    Seq("some", "dir")
  )

  private def stubEntriesFiles(values: Seq[Int]): StoredEntriesFile[Int] =
    new StoredEntriesFile[Int] {
      override def read: ZStream[Blocking, Throwable, Int] = ZStream.fromIterable(values)
    }

  private val Partitions = 5

  private val Config = S3VersionedDao.Config(
    S3Dir,
    1,
    1,
    Partitions
  )

  implicit val format: StoredFormat[Int] = new StoredFormat[Int] {
    override def write(e: Int, os: OutputStream): Unit =
      Int32Value.of(e).writeDelimitedTo(os)

    override def read(is: InputStream): Option[Int] =
      Option(Int32Value.parseDelimitedFrom(is)).map(_.getValue)
  }

}
