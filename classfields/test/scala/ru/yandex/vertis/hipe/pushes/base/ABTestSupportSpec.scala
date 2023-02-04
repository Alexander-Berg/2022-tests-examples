package ru.yandex.vertis.hipe.pushes.base

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.hipe.clients.BaseSpec

@RunWith(classOf[JUnitRunner])
class ABTestSupportSpec extends BaseSpec {

  case class PushInfo(uuid: String) extends ABTestSupport {
    def test: String = choice("A", "B")
  }

  "ABTest" should {
    "give A" in {
      PushInfo("10").test shouldBe "A"
      PushInfo("00019d976e23302711baf454973d390b").test shouldBe "A"
    }

    "give B" in {
      PushInfo("15").test shouldBe "B"
      PushInfo("0003e4203a7d0c173e611b49a2d29dbc").test shouldBe "B"
    }
  }
}
