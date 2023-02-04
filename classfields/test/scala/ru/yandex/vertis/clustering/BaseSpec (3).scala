package ru.yandex.vertis.clustering

import java.time.ZonedDateTime

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import ru.yandex.vertis.clustering.utils.DateTimeUtils

import scala.concurrent.ExecutionContext
import scala.util.Random

/**
  * @author devreggs
  */
trait BaseSpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  implicit override def patienceConfig: PatienceConfig = DefaultPatienceConfig

  private val DefaultPatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(50, Milliseconds))

  implicit protected val SameThreadExecutionContext: ExecutionContext = new ExecutionContext {
    override def reportFailure(cause: Throwable): Unit = {}

    override def execute(runnable: Runnable): Unit = runnable.run()
  }

  private val generationBoundSeconds = 10000

  def randomGraphGenerationDateTime: ZonedDateTime =
    DateTimeUtils.now.minusSeconds(Random.nextInt(generationBoundSeconds))
}
