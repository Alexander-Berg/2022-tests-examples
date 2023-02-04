package ru.yandex.vertis.telepony.util

import scala.concurrent.ExecutionContext

/**
  * @author evans
  */
object CallingExecutionContext extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = runnable.run()

  override def reportFailure(cause: Throwable): Unit = throw cause
}

object CallingExecutionContextImplicit {
  implicit val ec = CallingExecutionContext
}
