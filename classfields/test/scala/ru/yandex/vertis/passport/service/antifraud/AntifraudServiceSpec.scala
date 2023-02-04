package ru.yandex.vertis.passport.service.antifraud

import org.scalacheck.Gen
import org.scalatest.WordSpec
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.passport.integration.hydra.impl.InMemoryHydraClient
import ru.yandex.vertis.passport.model.LoginAntifraudDecision.Allow
import ru.yandex.vertis.passport.model.{ApiPayload, ClientInfo, Identity, RequestContext}
import ru.yandex.vertis.passport.service.antifraud.AntifraudService.{TooManyConfirmationRequestsException, TooManyFailedAttemptsException}
import ru.yandex.vertis.passport.test.ModelGenerators.{identity, ipv4Address, ipv6Address}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

/**
  *
  * @author zvez
  */
class AntifraudServiceSpec extends WordSpec with SpecBase with PropertyChecks {

  import scala.concurrent.ExecutionContext.Implicits.global

  val config = AntifraudLimitsConfig()

  class Context {
    val hydra = new InMemoryHydraClient
    val service = new AntifraudServiceImpl(hydra, new SingleAntifraudLimitsProvider(config))
  }

  def withCtx(f: Context => Unit): Unit = f(new Context)

  "AntifraudService.reportFailedLogin" should {
    "increment counters on failed login" in {
      forAll(identity, ipv4Address) { (cred, ip) =>
        withCtx { ctx =>
          ctx.service.reportFailedLogin(cred, Some("12"))(requestContext(Some(ip))).futureValue

          ctx.hydra.clicker(AntifraudServiceImpl.BadLoginClickerName).get(cred.login).futureValue shouldBe 1
          ctx.hydra.counter(AntifraudServiceImpl.BadIpCounterName).get(ip).futureValue shouldBe 1
        }
      }
    }

    "not count ipv6 addresses" in {
      forAll(identity, ipv6Address) { (cred, ip) =>
        withCtx { ctx =>
          ctx.service.reportFailedLogin(cred, Some("42"))(requestContext(Some(ip))).futureValue

          ctx.hydra.clicker(AntifraudServiceImpl.BadLoginClickerName).get(cred.login).futureValue shouldBe 1
          ctx.hydra.counter(AntifraudServiceImpl.BadIpCounterName).get(ip).futureValue shouldBe 0
        }
      }
    }

    "increment ip counter twice if login is wrong" in {
      forAll(identity, ipv4Address) { (cred, ip) =>
        withCtx { ctx =>
          ctx.service.reportFailedLogin(cred, None)(requestContext(Some(ip))).futureValue

          ctx.hydra.clicker(AntifraudServiceImpl.BadLoginClickerName).get(cred.login).futureValue shouldBe 1
          ctx.hydra.counter(AntifraudServiceImpl.BadIpCounterName).get(ip).futureValue shouldBe 2
        }
      }
    }
  }

  "AntifraudService.allowLogin" should {
    "block by login" in withCtx { ctx =>
      val cred = identity.next
      val loginResults = (1 to (config.loginAuthLimit + 5)).map { _ =>
        val userId = ModelGenerators.userId.next
        val ip = Gen.option(ModelGenerators.ipAddress).next
        ctx.service.reportFailedLogin(cred, Some(userId))(requestContext(ip)).futureValue
        ctx.service.allowLogin(cred, userId)(requestContext(ip)).futureValue == Allow
      }
      val (ok, failed) = loginResults.splitAt(config.loginAuthLimit)
      ok.forall(_ == true) shouldBe true
      failed.forall(_ == false) shouldBe true
    }

    "block by ip" in withCtx { ctx =>
      val ip = ModelGenerators.ipv4Address.next
      val loginResults = (1 to (config.ipAuthLimit + 5)).map { _ =>
        val userId = ModelGenerators.userId.next
        val cred = identity.next
        ctx.service.reportFailedLogin(cred, Some(userId))(requestContext(Some(ip))).futureValue
        ctx.service.allowLogin(cred, userId)(requestContext(Some(ip))).futureValue == Allow
      }
      val (ok, failed) = loginResults.splitAt(config.ipAuthLimit)
      ok.forall(_ == true) shouldBe true
      failed.forall(_ == false) shouldBe true
    }
  }

  "AntifraudService.confirmationRequestAttempt" should {
    "limit requests count for identity" in withCtx { ctx =>
      val identity = ModelGenerators.identity.next
      (1 until config.confirmRequestsLimit).foreach { _ =>
        ctx.service.confirmationRequestAttempt(identity).futureValue
      }

      ctx.service
        .confirmationRequestAttempt(identity)
        .failed
        .futureValue shouldBe a[TooManyConfirmationRequestsException]

      ctx.service.confirmationRequestAttempt(ModelGenerators.identity.next).futureValue
    }

    "limit confirmation requests count for ip" in withCtx { ctx =>
      val ip = ModelGenerators.ipv4Address.next
      var identity: Identity = null
      (1 until config.ipConfirmRequestsLimit).foreach { _ =>
        identity = ModelGenerators.identity.next
        ctx.service.confirmationRequestAttempt(identity)(requestContext(Some(ip))).futureValue
      }

      ctx.service
        .confirmationRequestAttempt(identity)(requestContext(Some(ip)))
        .failed
        .futureValue shouldBe a[TooManyConfirmationRequestsException]

      ctx.service.confirmationRequestAttempt(ModelGenerators.identity.next).futureValue
    }
  }

  "AntifraudService.confirmationCodeUsageAttempt" should {
    "not limit success code usage" in withCtx { ctx =>
      val identity = ModelGenerators.identity.next
      (1 to 200).foreach { _ =>
        ctx.service.confirmationCodeUsageAttempt(identity).futureValue
      }
    }

    "limit failed code usage attempts" in withCtx { ctx =>
      val identity = ModelGenerators.identity.next
      (1 to (config.failedConfirmLimit + 1)).foreach { _ =>
        ctx.service.confirmationCodeUsageAttempt(identity).futureValue
        ctx.service.reportFailedConfirmation(identity, "123").futureValue
      }

      ctx.service.confirmationCodeUsageAttempt(identity).failed.futureValue shouldBe a[TooManyFailedAttemptsException]

      ctx.service.confirmationCodeUsageAttempt(ModelGenerators.identity.next).futureValue
    }
  }

  private def requestContext(ip: Option[String] = None) =
    wrap(ApiPayload("test", ClientInfo(ip = ip)))
}
