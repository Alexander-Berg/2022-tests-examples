package ru.yandex.realty.traffic.testkit

import eu.timepit.refined.auto._
import org.junit.runner.RunWith
import ru.yandex.realty.traffic.model.s3.S3Path.{S3DirPath, S3FilePath}
import ru.yandex.realty.traffic.service.s3.S3
import ru.yandex.realty.traffic.utils.FilesService
import ru.yandex.realty.traffic.utils.FilesService.FilesServiceConfig
import zio._
import zio.blocking._
import zio.test.Assertion._
import zio.test._
import zio.test.junit._

import java.nio.file.Files

@RunWith(classOf[ZTestJUnitRunner])
class InMemoryS3Spec extends JUnitRunnableSpec {

  private def createTestFile(content: String) =
    for {
      testFile <- FilesService.tempFile("test-file")
      _ <- effectBlocking(Files.writeString(testFile, content))
    } yield testFile

  private def readFileContent(file: S3FilePath) =
    S3.read(file).runCollect.map(a => new String(a.toArray))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("FileSystemS3")(
      testM("Should correctly work") {

        val fs =
          ZLayer.succeed(FilesServiceConfig(Files.createTempDirectory("FileSystemS3Spec"))) >>> FilesService.live

        val specLayer = fs ++ Blocking.live >+> InMemoryS3.live >+> S3.logged

        val root = S3DirPath("test-bucket", Seq.empty)
        val root2 = S3DirPath("test-bucket2", Seq.empty)
        val dir = root.child("dir")
        val dir2 = root2.child("dir")
        val subDir = dir.child("sub-dir")
        val subDir2 = dir2.child("sub-dir")

        val file0 = root.filePath("file0")
        val file0Copy = root2.filePath("copy")
        val file1 = dir.filePath("file1")
        val file1Copy = dir2.filePath("file1")
        val file2 = subDir.filePath("file2")
        val file2Copy = subDir2.filePath("file2")

        (for {
          _ <- S3.createBucket(root.bucket)
          _ <- S3.createBucket(root2.bucket)
          _ <- createTestFile("henlo0") >>= (S3.writeFile(file0, _))
          _ <- createTestFile("henlo1") >>= (S3.writeFile(file1, _))
          _ <- createTestFile("henlo2") >>= (S3.writeFile(file2, _))
          _ <- S3.copy(file0, file0Copy)
          _ <- S3.copy(file1, file1Copy)
          _ <- S3.copy(file2, file2Copy)
          listRoot <- S3.list(root)
          listDir <- S3.list(dir)
          listSubdir <- S3.list(subDir)
          f0 <- readFileContent(file0)
          f1 <- readFileContent(file1)
          f2 <- readFileContent(file2)
          f0Copy <- readFileContent(file0Copy)
          f1Copy <- readFileContent(file1Copy)
          f2Copy <- readFileContent(file2Copy)
          _ <- S3.deleteFile(file1)
          afterFile1DeleteListDir <- S3.list(dir)
          _ <- S3.deleteFile(file0)
          _ <- S3.deleteFile(file2)
          listRootAfterDelete <- S3.list(root)
          listDirAfterDelete <- S3.list(dir)
          listSubdirAfterDelete <- S3.list(subDir)
          _ <- FilesService.freeAllTemporary()
        } yield {
          assert(listRoot)(hasSameElements(Seq(file0, dir))) &&
          assert(listDir)(hasSameElements(Seq(file1, subDir))) &&
          assert(listSubdir)(hasSameElements(Seq(file2))) &&
          assert(afterFile1DeleteListDir)(hasSameElements(Seq(subDir))) &&
          assertTrue(f0 == "henlo0") &&
          assertTrue(f1 == "henlo1") &&
          assertTrue(f2 == "henlo2") &&
          assertTrue(f0 == f0Copy) &&
          assertTrue(f1 == f1Copy) &&
          assertTrue(f2 == f2Copy) &&
          assertTrue(listDirAfterDelete.isEmpty) &&
          assertTrue(listRootAfterDelete.isEmpty) &&
          assertTrue(listSubdirAfterDelete.isEmpty)
        }).provideLayer(specLayer)
      }
    )

  sealed trait Content

  object Content {
    case class FileContent(str: String) extends Content
    case object DirectoryContent extends Content
  }
}
