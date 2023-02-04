package ru.yandex.vertis.punisher

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.{Async, ContextShift, IO}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import org.slf4j.{Logger, LoggerFactory}
import org.specs2.mock.Mockito
import ru.yandex.vertis.feature.impl.InMemoryFeatureRegistry
import ru.yandex.vertis.feature.impl.BasicFeatureTypes
import ru.yandex.vertis.quality.feature_registry_utils.FeatureRegistryF

import scala.concurrent.ExecutionContext

/**
  * @author devreggs
  */
trait BaseSpec extends WordSpec with Matchers with ScalaFutures with ResourcesSpec with Mockito with BeforeAndAfter {

  type F[+T] = BaseSpec.F[T]
  protected lazy val log: Logger = LoggerFactory.getLogger("tests")

  private val featureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)
  implicit protected val featureRegistryF: FeatureRegistryF[F] = new FeatureRegistryF[F](featureRegistry)

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(60, Seconds), interval = Span(50, Milliseconds))

  implicit protected lazy val cs: ContextShift[F] = IO.contextShift(ExecutionContext.global)
  implicit protected lazy val async: Async[F] = IO.ioEffect

  implicit protected val actorSystem: ActorSystem = ActorSystem()
  implicit protected val materializer: ActorMaterializer = ActorMaterializer()
  implicit protected val SameThreadExecutionContext: ExecutionContext = actorSystem.dispatcher
}

object BaseSpec {
  type F[+T] = IO[T]
}
