package ru.yandex.auto.extdata.service.fetcher

import com.typesafe.config.Config
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.auto.extdata.service.Main

class AbstractFetcherSpec extends WordSpec with Matchers {
  System.setProperty("module.name", "auto2-ext-data-service")
  System.setProperty("host.name", "localhost")
  System.setProperty("service.name", "autoru")

  val mainContext = Main
  implicit val cfg: Config = mainContext.config
}
