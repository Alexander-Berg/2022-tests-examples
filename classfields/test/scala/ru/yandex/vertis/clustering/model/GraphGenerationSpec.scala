package ru.yandex.vertis.clustering.model

import java.io.File

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class GraphGenerationSpec extends BaseSpec {

  val tarGzFile = new File(ClassLoader.getSystemResource("generation.data").toURI)

  "PackedGraphGeneration" should {
    "unpack to FilesystemGraphGeneration" in {
      val gen = PackedGraphGeneration(randomGraphGenerationDateTime, tarGzFile)
      gen.unpack(tarGzFile.getParentFile)(FilesystemGraphGeneration.apply)
    }
  }
}
