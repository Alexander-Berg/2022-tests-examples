package ru.auto.api.util

import java.io.{ByteArrayInputStream, Closeable, File, FileInputStream}
import java.util.zip.GZIPInputStream

import org.mockito.Mockito._
import ru.auto.api.BaseSpec
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 16.02.17
  */
class IOSpec extends BaseSpec with MockitoSupport {

  private def currentTempFileSet: Set[File] = IO.getTempDirectory.listFiles().toSet

  "IO" should {
    "close java.io.Closeable in `using`" in {
      val closeable = mock[Closeable]
      doNothing().when(closeable).close()
      IO.using(closeable)(ref => ref shouldBe closeable)
      verify(closeable).close()
    }

    "close java.ui.Closeable in `using` after exception" in {
      val closeable = mock[Closeable]
      doThrow(classOf[RuntimeException]).when(closeable).close()

      intercept[RuntimeException] {
        IO.using(closeable)(ref => ref shouldBe closeable)
      }
      verify(closeable).close()
    }

    "help in reading lines from InputStream" in {
      val data =
        """line1
          |line2
        """.stripMargin

      val it = IO.readLines(new ByteArrayInputStream(data.getBytes("UTF-8"))).toVector
      it shouldBe data.split("\n").toVector
    }

    "help in creating temporary files" in {
      val atStart = currentTempFileSet
      val file = IO.newTempFile("prefix0", "suffix0")
      file should exist
      file.getName should include("prefix0")
      file.getName should endWith("suffix0")
      file.getParentFile shouldBe IO.getTempDirectory
      (currentTempFileSet -- atStart) shouldBe Set(file)
      file.delete()
    }

    "help in writing temporary files" in {
      val atStart = currentTempFileSet
      val file = IO.usingTmpWriter("prefix") { writer =>
        writer.println("line1")
        writer.println("line2")
      }
      IO.readLines(new FileInputStream(file)).toVector shouldBe Seq("line1", "line2")
      (currentTempFileSet -- atStart) shouldBe Set(file)
      file.delete()
    }

    "help in writing compressed temporary files" in {
      val atStart = currentTempFileSet
      val file = IO.usingTmpWriter("prefix", gzip = true) { writer =>
        writer.println("line1")
        writer.println("line2")
      }
      val lines = IO.readLines(new GZIPInputStream(new FileInputStream(file))).toVector
      lines shouldBe Seq("line1", "line2")

      file.getName should endWith(".gz")
      (currentTempFileSet -- atStart) shouldBe Set(file)
      file.delete()
    }

    "delete temp file on exception" in {
      val atStart = currentTempFileSet
      intercept[RuntimeException] {
        IO.usingTmpWriter("prefix")(_ => sys.error("test error"))
      }
      (currentTempFileSet -- atStart) shouldBe Set()
    }
  }
}
