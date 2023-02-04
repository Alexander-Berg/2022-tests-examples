package ru.yandex.vertis.telepony.api

import java.util.concurrent.Executors

import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.application.runtime.VertisRuntime
import ru.yandex.vertis.telepony.backend.PhoneUnifierApiEnvironment
import ru.yandex.vertis.telepony.operational.Operational
import ru.yandex.vertis.telepony.server.env.ConfigHelper
import ru.yandex.vertis.telepony.settings.PhoneUnifierApiSettings

import scala.concurrent.ExecutionContext

/**
  * @author evans
  */
class PhoneUnifierApiBackendIntSpec extends AnyWordSpec {

  val config = ConfigHelper.load(Seq("application.test.conf"))

  implicit val e = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))

  "Api Backend" should {
    "creates settings successfully" in {
      PhoneUnifierApiSettings(config)
    }

    "creates backend successfully" in {
      val settings = PhoneUnifierApiSettings(config)
      val operational = Operational.default(VertisRuntime)
      new PhoneUnifierApiEnvironment(settings, operational, Environments.Local).apiBackend
    }
  }
}
