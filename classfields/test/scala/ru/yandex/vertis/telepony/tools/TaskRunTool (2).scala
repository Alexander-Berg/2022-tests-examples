package ru.yandex.vertis.telepony.tools

import ru.yandex.vertis.telepony.util.Threads._

import scala.util.{Failure, Success}

/**
  * run task in production environment
  *
  * @author evans
  */
object TaskRunTool extends Tool {

  val component = environment.serviceComponents.find(_.domain == "autoru_def").get

  val task = component.statusOutdatedTask

  log.info(s"Starting task $task")

  val f = task.payload()
  f.onComplete {
    case Success(_) =>
      System.err.print(s"Result: All is ok")
    case Failure(e) =>
      System.err.print(s"Result: $e")
      e.printStackTrace()
      sys.exit(0)
  }
}
