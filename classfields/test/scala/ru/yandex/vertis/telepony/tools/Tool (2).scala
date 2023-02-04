package ru.yandex.vertis.telepony.tools

import ru.yandex.vertis.telepony.backend.ControllerEnvironment
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.operational.Operational
import ru.yandex.vertis.telepony.server.env._
import ru.yandex.vertis.telepony.settings.ControllerSettings

import scala.util.control.NonFatal

/**
  * @author evans
  */
trait Tool extends App with SimpleLogging {

  def createEnvironment(): ControllerEnvironment = {
    try {
      import ru.yandex.vertis.application.runtime.VertisRuntime
      val operational = Operational.default(VertisRuntime)
      val settings = ControllerSettings(ConfigHelper.load(Seq("application-tools.conf")))
      new ControllerEnvironment(settings, VertisRuntime, operational)
    } catch {
      case NonFatal(e) =>
        //scalastyle:off
        System.err.println("Couldn't initialize application")
        e.printStackTrace(System.err)
        System.exit(-1)
        //scalastyle:on
        ???
    }
  }

  val environment: ControllerEnvironment = createEnvironment()
}
