package amogus.model

import amogus.model.AmogusConfig.AmogusServiceConfig
import ru.yandex.vertis.amogus.model.AmoEventType
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import pureconfig.ConfigSource

class AmogusServiceConfigSpec extends AnyWordSpecLike with Matchers with EitherValues {

  "AmogusServiceConfig" should {
    "load empty webhooks" in {
      val config = ConfigSource.string("""
          |{
          |  service-id = "00000000-0000-0000-0000-000000000000"
          |  service-name = "autorutesting"
          |  host = {host = "autorutesting.amocrm.ru", port = 443, schema = "https"}
          |  webhooks = ""
          |  topic = "amogus-autorutesting"
          |  credentials = []
          |}
          |""".stripMargin)
      val amogusConfigResult = config.load[AmogusServiceConfig]
      amogusConfigResult should be(Symbol("right"))
      val amogusConfig = amogusConfigResult.toOption.get

      amogusConfig.webhooks shouldBe empty
    }

    "load some webhooks" in {
      val config = ConfigSource.string("""
          |{
          |  service-id = "00000000-0000-0000-0000-000000000000"
          |  service-name = "autorutesting"
          |  host = {host = "autorutesting.amocrm.ru", port = 443, schema = "https"}
          |  webhooks = "NOTE_CONTACT,delete_task, adD_TAsk"
          |  topic = "amogus-autorutesting"
          |  credentials = []
          |}
          |""".stripMargin)
      val amogusConfigResult = config.load[AmogusServiceConfig]
      amogusConfigResult should be(Symbol("right"))
      val amogusConfig = amogusConfigResult.toOption.get

      (amogusConfig.webhooks should contain).theSameElementsAs(
        Seq(AmoEventType.NOTE_CONTACT, AmoEventType.DELETE_TASK, AmoEventType.ADD_TASK)
      )
    }

    "not load missing webhooks" in {
      val config = ConfigSource.string("""
          |{
          |  service-id = "00000000-0000-0000-0000-000000000000"
          |  service-name = "autorutesting"
          |  host = {host = "autorutesting.amocrm.ru", port = 443, schema = "https"}
          |  topic = "amogus-autorutesting"
          |  credentials = []
          |}
          |""".stripMargin)
      val amogusConfigResult = config.load[AmogusServiceConfig]
      amogusConfigResult should be(Symbol("left"))
    }
  }
}
