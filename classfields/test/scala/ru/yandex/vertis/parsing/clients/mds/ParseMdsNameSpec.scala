package ru.yandex.vertis.parsing.clients.mds

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.clients.mds.MdsImage.parseMdsName

@RunWith(classOf[JUnitRunner])
class ParseMdsNameSpec extends FlatSpec with Matchers {

  behavior.of("MdsImage.parseMdsName")

  it should "parse correct mds names" in {
    parseMdsName("111-bbb") shouldBe (111, "bbb")
  }

  it should "throw when group id is not a number" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      parseMdsName("1a1-bbb")
    }
  }

  it should "throw when there is no -" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      parseMdsName("111aaa")
    }
  }

  it should "throw when group id is missing" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      parseMdsName("-bbb")
    }
  }

  it should "throw when image name is missing" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      parseMdsName("111-")
    }
  }

}
