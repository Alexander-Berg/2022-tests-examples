package ru.yandex.vertis.moderation

import akka.actor.{ActorSystem, Scheduler}
import com.google.common.base.Charsets
import com.typesafe.config.ConfigFactory
import org.mockito.InOrder
import org.scalacheck.Shrink
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.specs2.matcher.ThrownExpectationsCreation
import org.specs2.mock.Mockito
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.scalatest.matcher.SmartEqualMatcher

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * @author sunlight
  */
trait SpecBase
    extends WordSpec
    with BeforeAndAfter
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures
    with Mockito
    with ThrownExpectationsCreation {

  private val DefaultPatienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(50, Millis))

  lazy val actorSystem: ActorSystem = ActorSystem("test", ConfigFactory.empty())

  implicit lazy val prometheusRegistry: PrometheusRegistry = StubPrometheusRegistry

  implicit lazy val scheduler: Scheduler = actorSystem.scheduler

  implicit override def patienceConfig: PatienceConfig = DefaultPatienceConfig

  implicit class TestFuture[T](val future: Future[T]) {

    def shouldCompleteWithException[A <: Throwable: ClassTag]: Assertion =
      whenReady(future.failed) { e =>
        e shouldBe a[A]
      }
  }

  implicit def noShrink[A]: Shrink[A] = Shrink(_ => Stream.empty)

  def smartEqual[A](right: A): SmartEqualMatcher[A] = SmartEqualMatcher.smartEqual(right)

  protected def readResource(path: String): String =
    scala.io.Source
      .fromInputStream(
        getClass.getResourceAsStream(path),
        Charsets.UTF_8.name()
      )
      .mkString

  def anyTimes[T <: AnyRef](mock: T)(implicit anOrder: Option[InOrder] = inOrder()): T = atLeast(0)(mock)
}
