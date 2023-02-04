package ru.yandex.vertis.clustering.utils

import java.io.File

import org.apache.commons.io.{FileUtils => ApacheFileUtils}
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec

@RunWith(classOf[JUnitRunner])
class FileUtilsSpec extends BaseSpec {

  "FileUtils" should {
    val systemTempDir = ApacheFileUtils.getTempDirectory

    "createSubDirectory" in {
      val dirName = "test_dir_" + Gen.alphaNumStr.filter(_.nonEmpty).sample.get
      FileUtils.createSubDirectory(systemTempDir, dirName)
      val check = new File(s"$systemTempDir/$dirName")
      check.exists && check.isDirectory shouldBe true
      check.delete
    }

    "newTempDirectory" in {
      val prefix = "test_prefix_" + Gen.alphaNumStr.filter(_.nonEmpty).sample.get
      FileUtils.newTempDirectory(prefix)

      systemTempDir.listFiles.exists(_.getName.startsWith(prefix)) shouldBe true
      systemTempDir.listFiles.filter(_.getName.startsWith(prefix)).foreach(_.delete)
    }
  }
}
