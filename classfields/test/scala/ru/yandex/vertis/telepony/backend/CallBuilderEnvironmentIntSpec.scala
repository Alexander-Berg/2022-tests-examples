package ru.yandex.vertis.telepony.backend

import java.util.concurrent.Executors

import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.application.runtime.VertisRuntime
import ru.yandex.vertis.telepony.Specbase
import ru.yandex.vertis.telepony.operational.Operational
import ru.yandex.vertis.telepony.server.env.ConfigHelper
import ru.yandex.vertis.telepony.settings.CallBuilderSettings

import scala.concurrent.ExecutionContext

/**
  * @author neron
  */
class CallBuilderEnvironmentIntSpec extends Specbase {

  val config = ConfigHelper.load(Seq("application-test.conf"))

  implicit val e = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))

  "CallBuilderEnvironment" should {
    "create settings successfully" in {
      CallBuilderSettings(config)
    }

    "start and stop" in {
      val settings = CallBuilderSettings(config)
      val operational = Operational.default(VertisRuntime)
      val app = new CallBuilderEnvironment(settings, operational, Environments.Local)
      app.start()
      app.stop()
    }
  }
}
