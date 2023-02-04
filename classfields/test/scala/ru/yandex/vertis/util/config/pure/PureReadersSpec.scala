package ru.yandex.vertis.util.config.pure

import com.typesafe.config.{ConfigFactory, ConfigMemorySize}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import pureconfig.generic.auto._
import pureconfig.loadConfigOrThrow
import ru.yandex.vertis.util.config.pure.PureReaders._
import ru.yandex.vertis.util.config.pure.PureReadersSpec.MyApp

/**
  * @author Natalia Ratskevich (reimai@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class PureReadersSpec extends WordSpec with Matchers {

  private val config = ConfigFactory.load("unit-test.application.conf")

  "filesize reader" should {
    "parse file size" in {
      val batchSize = loadConfigOrThrow[MyApp](config.getConfig("app"))
      batchSize shouldBe MyApp(ConfigMemorySize.ofBytes(128 * 1024 * 1024))
    }
  }
}

object PureReadersSpec {
  case class MyApp(batchSize: ConfigMemorySize)
}
