package ru.yandex.vertis.clustering

import java.io.File

import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.utils.TarGzUtils

/**
  * @author devreggs
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class TarSpec extends BaseSpec {

  "TarUtils" should {
    "tar and untar" in {
      TarGzUtils.createTarGzOfDirectory(new File("./assembly"), new File("./cache/assembly.tar"))
      TarGzUtils.unTarGz(new File("./cache/assembly.tar"), new File("./cache/"))
    }
  }
}
