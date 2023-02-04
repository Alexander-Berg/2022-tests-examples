package ru.yandex.vertis.telepony.api

import org.scalatest.Ignore
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.application.runtime.VertisRuntime
import ru.yandex.vertis.telepony.backend.ReactiveApiEnvironment
import ru.yandex.vertis.telepony.operational.Operational
import ru.yandex.vertis.telepony.server.env.ConfigHelper
import ru.yandex.vertis.telepony.settings.ReactiveApiSettings

/**
  * @author evans
  */
@Ignore
class ReactiveApiBackendIntSpec extends AnyWordSpec {

  val config = ConfigHelper.load(Seq("application.test.conf"))

  "Api Backend" should {
    "creates settings successfully" in {
      ReactiveApiSettings(config)
    }

    "creates backend successfully" in {
      val settings = ReactiveApiSettings(config)
      val runtime = VertisRuntime
      val operational = Operational.default(runtime)
      new ReactiveApiEnvironment(settings, operational, runtime).apiBackend
    }
  }
}
