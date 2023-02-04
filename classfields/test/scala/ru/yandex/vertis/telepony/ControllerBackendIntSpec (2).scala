package ru.yandex.vertis.telepony

import org.scalatest.Ignore
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.application.runtime.VertisRuntime
import ru.yandex.vertis.telepony.backend.ControllerEnvironment
import ru.yandex.vertis.telepony.operational.Operational
import ru.yandex.vertis.telepony.server.env.ConfigHelper
import ru.yandex.vertis.telepony.settings.ControllerSettings

/**
  * @author evans
  */
@Ignore
class ControllerBackendIntSpec extends AnyWordSpec {

  val config = ConfigHelper.load(Seq("application-test.conf"))

  "Controller Backend" should {
    "creates settings successfully" in {
      ControllerSettings(config)
    }

    "creates backend successfully" in {
      val settings = ControllerSettings(config)
      val operational = Operational.default(VertisRuntime)
      new ControllerEnvironment(settings, VertisRuntime, operational).serviceComponents
    }
  }
}
