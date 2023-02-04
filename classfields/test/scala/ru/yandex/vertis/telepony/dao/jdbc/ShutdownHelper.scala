package ru.yandex.vertis.telepony.dao.jdbc

import java.util.concurrent.ConcurrentHashMap

import ru.yandex.vertis.telepony.logging.SimpleLogging
import scala.jdk.CollectionConverters._

/**
  * Helps to register shutdown actions.
  *
  * @author dimas
  */
object ShutdownHelper extends SimpleLogging {

  type Action = () => Unit

  private val shutdownActions = new ConcurrentHashMap[Action, Unit]()

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {

    def run(): Unit = {
      log.info("Shutting down...")
      for ((action, _) <- shutdownActions.asScala) {
        try {
          action()
        } catch {
          case e: Exception =>
            log.error("Error while execute shutdown action", e)
        }
      }
    }
  }, s"shutdown-helper"))

  def register(action: Action): Unit = {
    shutdownActions.put(action, ())
  }
}
