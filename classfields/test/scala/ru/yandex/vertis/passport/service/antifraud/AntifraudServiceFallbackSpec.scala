package ru.yandex.vertis.passport.service.antifraud

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.WordSpec
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.passport.AkkaSupport
import ru.yandex.vertis.passport.integration.hydra.HydraConfig
import ru.yandex.vertis.passport.integration.hydra.impl.HydraClientImpl
import ru.yandex.vertis.passport.model.{LoginAntifraudDecision, RequestContext}
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.http.HttpClientMock

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
  *
  * @author zvez
  */
class AntifraudServiceFallbackSpec
  extends WordSpec
  with HttpClientMock
  with AkkaSupport
  with SpecBase
  with PropertyChecks { root =>

  import scala.concurrent.ExecutionContext.Implicits.global

  val config = AntifraudLimitsConfig()

  class Context {
    val hydra = new HydraClientImpl(http, HydraConfig("hydra.ya.ru", "passport", "RU-ru"))

    val service =
      new AntifraudServiceImpl(hydra, new SingleAntifraudLimitsProvider(config))
        with AntifraudServiceWithTimeout
        with FallbackAntifraudService {

        implicit override protected def actorSystem: ActorSystem = root.actorSystem
        override protected def timeout: FiniteDuration = 50.millis
      }
  }

  def withCtx(f: Context => Unit): Unit = f(new Context)

  "AntifraudService.confirmationRequestAttempt" should {
    "recover after timeout exceeded" in new Context { ctx =>
      val userId = ModelGenerators.userId.next
      val identity = ModelGenerators.identity.next
      val credentials = ModelGenerators.userCredentials.next
      onRequest { _ =>
        Thread.sleep(100) // should lead to service timeout
        throw new AssertionError("should not be returned")
      }
      ctx.service.allowLogin(identity, userId).futureValue shouldBe LoginAntifraudDecision.Allow
      ctx.service.confirmationRequestAttempt(identity).futureValue shouldBe AntifraudCounters(0)
      ctx.service.confirmationRequestAttempt(identity).futureValue shouldBe AntifraudCounters(0)
    }
  }
}
