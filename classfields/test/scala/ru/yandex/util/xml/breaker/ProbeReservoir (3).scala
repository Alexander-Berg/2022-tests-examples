package ru.yandex.util.xml.breaker

import com.codahale.metrics.health.HealthCheck.Result
import org.jetbrains.annotations.NotNull
import ru.yandex.common.monitoring.error.ErrorReservoir

/**
  * @author Ilya Gerasimov (747mmhg@yandex-team.ru)
  */
class ProbeReservoir(delegate: ErrorReservoir) extends ErrorReservoir {

  var errorCount = 0
  var okCount = 0

  override def error(): Unit = {
    errorCount += 1
    delegate.error()
  }

  override def error(msg: String): Unit = {
    errorCount += 1
    delegate.error(msg)
  }

  override def ok(): Unit = {
    okCount += 1
    delegate.ok()
  }

  @NotNull
  override def toResult: Result = {
    delegate.toResult
  }
}
