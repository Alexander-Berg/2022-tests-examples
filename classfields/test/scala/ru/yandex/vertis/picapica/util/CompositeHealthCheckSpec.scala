package ru.yandex.vertis.picapica.util

import com.codahale.metrics.health.HealthCheck.Result
import org.jetbrains.annotations.NotNull
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.common.monitoring.WarningHealthCheck
import ru.yandex.common.monitoring.error.ErrorReservoir

/**
  * @author evans
  */
@RunWith(classOf[JUnitRunner])
class CompositeHealthCheckSpec extends WordSpecLike with Matchers {
  "Composite health check" should {
    "return no errors with no health checks" in {
      val composite = new CompositeHealthCheck
      composite.check().isHealthy shouldBe true
    }
    "return no errors with many healthy reservoirs" in {
      val composite = new CompositeHealthCheck
      composite.register("a", new ConstErrorReservoir(Result.healthy()))
      composite.register("b", new ConstErrorReservoir(Result.healthy()))
      composite.register("c", new ConstErrorReservoir(Result.healthy()))
      composite.check().isHealthy shouldBe true
    }
    "return warning if exists warning in reservoirs" in {
      val composite = new CompositeHealthCheck
      composite.register("d", new ConstErrorReservoir(Result.healthy()))
      composite.register("e", new ConstErrorReservoir(WarningHealthCheck.warning()))
      composite.register("f", new ConstErrorReservoir(Result.healthy()))
      WarningHealthCheck.isWarning(composite.check()) shouldBe true
    }
    "return error if exist errors in reservoirs" in {
      val composite = new CompositeHealthCheck
      composite.register("g", new ConstErrorReservoir(Result.healthy()))
      composite.register("h", new ConstErrorReservoir(Result.unhealthy("")))
      composite.register("k", new ConstErrorReservoir(Result.unhealthy("")))
      WarningHealthCheck.isUnhealthy(composite.check()) shouldBe true
    }
    "return error if exist errors and warnings in reservoirs" in {
      val composite = new CompositeHealthCheck
      composite.register("a", new ConstErrorReservoir(Result.healthy()))
      composite.register("b", new ConstErrorReservoir(WarningHealthCheck.warning))
      composite.register("c", new ConstErrorReservoir(Result.unhealthy("")))
      WarningHealthCheck.isUnhealthy(composite.check()) shouldBe true
    }
  }
}

class ConstErrorReservoir(result: Result) extends ErrorReservoir {
  override def error(): Unit = {}

  override def error(msg: String): Unit = {}

  override def ok(): Unit = {}

  @NotNull
  override def toResult: Result = result
}
