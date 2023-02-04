package ru.yandex.vertis.parsing.auto.clients.scrapinghub

import java.io.{File, InputStream}
import java.nio.file.{Files, Paths, StandardCopyOption}

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.clients.bucket.ScrapingHubImportProcessor
import ru.yandex.vertis.parsing.util.IO
import ru.yandex.vertis.parsing.common.Site

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ScrapingHubImportProcessorTest extends FunSuite {

  test("process") {
    val io = new IO(new File("."))
    val tmpFile = io.newTempFile("scrappingHubImportProcessorTest", "")
    val in: InputStream = this.getClass.getResourceAsStream("/2019-03-07-10-31-58-fresh-drom.json")
    Files.copy(in, Paths.get(tmpFile.getAbsolutePath), StandardCopyOption.REPLACE_EXISTING)

    val processor = new ScrapingHubImportProcessor(tmpFile)

    val rows = processor.process { it =>
      it.toList
    }

    assert(rows.length == 10)
    rows.foreach(row => {
      assert(Site.fromUrl(row.rawUrl) == Site.Drom)
    })
  }
}
