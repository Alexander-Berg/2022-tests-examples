package ru.yandex.vertis.telepony.api

import org.scalatest.Ignore
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.application.runtime.VertisRuntime
import ru.yandex.vertis.telepony.backend.ApiEnvironment
import ru.yandex.vertis.telepony.operational.Operational
import ru.yandex.vertis.telepony.server.env.ConfigHelper
import ru.yandex.vertis.telepony.settings.ApiSettings

/**
  * @author evans
  */
@Ignore
class ApiBackendIntSpec extends AnyWordSpec {
//  System.setProperty("config.resource", "application.test.conf")

  val config = ConfigHelper.load(Seq("application.test.conf"))

  "Api Backend" should {
    "creates settings successfully" in {
      ApiSettings(config)
    }

    "creates backend successfully" in {
      val settings = ApiSettings(config)
      val operational = Operational.default(VertisRuntime)
      new ApiEnvironment(settings, VertisRuntime, operational).apiBackend
    }
  }
}
