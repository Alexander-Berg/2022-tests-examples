package ru.yandex.vertis.clustering

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext

/**
  * @author devreggs
  */
trait BaseSpec extends WordSpec with Matchers with ScalaFutures {

  implicit override def patienceConfig: PatienceConfig = DefaultPatienceConfig

  private val DefaultPatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(50, Milliseconds))

  implicit protected val SameThreadExecutionContext: ExecutionContext = new ExecutionContext {
    override def reportFailure(cause: Throwable): Unit = {}

    override def execute(runnable: Runnable): Unit = runnable.run()
  }
}
