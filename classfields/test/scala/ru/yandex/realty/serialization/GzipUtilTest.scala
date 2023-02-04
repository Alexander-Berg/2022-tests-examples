package ru.yandex.realty.serialization

import java.io.File
import java.nio.file.Files

import org.apache.commons.io.FileUtils
import org.junit.runner.RunWith
import org.scalatest.TryValues._
import org.scalatest._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.generator.FileGenerators
import ru.yandex.realty.generator.FileGenerators.DirWithFiles

@RunWith(classOf[JUnitRunner])
class GzipUtilTest extends FlatSpec with Matchers with PropertyChecks with FileGenerators with BeforeAndAfterAll {

  "GzipUtil" should "serialize a nonempty directory" in {
    forAll(dirWithFilesGen(baseDir, 1, 30, 0, 10000)) {
      case DirWithFiles(dir, files) =>
        forAll(dirGen(baseDir), filesGen(baseDir)) { (unarchiveDir, archiveFile) =>
          val archived = GzipUtil.shallowDirectoryGzip(dir, archiveFile).get
          val resultDir = GzipUtil.shallowDirectoryGunzip(archived, unarchiveDir).get

          val unarchivedFiles = resultDir.listFiles()
          unarchivedFiles.length shouldEqual files.length

          files.foreach { f =>
            val originalBytes = Files.readAllBytes(f.toPath)
            val unarchivedBytes = Files.readAllBytes(resultDir.toPath.resolve(f.getName))
            unarchivedBytes shouldEqual originalBytes
          }
        }
    }
  }

  it should "fail when is given a file instead of a directory" in {
    val file = new File(baseDir + File.separator + "qwer")
    file.createNewFile()
    GzipUtil.shallowDirectoryGzip(file, new File("asdf")).failure.exception shouldBe a[IllegalArgumentException]
  }

  it should "serialize empty directory" in {
    forAll(dirGen(baseDir), dirGen(baseDir), filesGen(baseDir)) { (emptyDir, unarchiveDir, archiveFile) =>
      val archived: File = GzipUtil.shallowDirectoryGzip(emptyDir, archiveFile).get
      val resultDir: File = GzipUtil.shallowDirectoryGunzip(archived, unarchiveDir).get

      resultDir.isDirectory shouldEqual true
      resultDir.listFiles().isEmpty shouldEqual true
    }
  }

  private val baseDir: File = {
    val dir = new File("./tmp")
    dir.mkdir()
    dir
  }

  override protected def afterAll(): Unit = {
    FileUtils.deleteDirectory(baseDir)
  }
}
