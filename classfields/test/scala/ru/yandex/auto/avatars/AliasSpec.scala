package ru.yandex.auto.avatars

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.auto.avatar._

@RunWith(classOf[JUnitRunner])
class AliasSpec extends WordSpec with Matchers {
  val url = "//avatars.mds.yandex.net/get-verba/216201/2a000001609ef2c1814bed7490951959d499/optimize"

  "ORIG" should {
    "replace alias" in {
      ORIG(url) shouldBe
        "//avatars.mds.yandex.net/get-verba/216201/2a000001609ef2c1814bed7490951959d499/orig"
    }

    "clear alias" in {
      ORIG.root(url) shouldBe "//avatars.mds.yandex.net/get-verba/216201/2a000001609ef2c1814bed7490951959d499/"
    }
  }
}
