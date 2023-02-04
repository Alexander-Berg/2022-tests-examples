package ru.yandex.vertis.passport.service.confirmation

import org.scalatest.WordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.impl.memory.InMemoryConfirmationDao
import ru.yandex.vertis.passport.integration.hydra.impl.InMemoryHydraClient
import ru.yandex.vertis.passport.model.{ConfirmOneTimeLogin, ConfirmationCode, RequestContext}
import ru.yandex.vertis.passport.service.antifraud.AntifraudService.{TooManyConfirmationRequestsException, TooManyFailedAttemptsException}
import ru.yandex.vertis.passport.service.antifraud._
import ru.yandex.vertis.passport.service.communication.UserCommunicationService
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.ConfirmationCodeNotFoundException

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class AntifraudConfirmationServiceSpec extends WordSpec with SpecBase with MockitoSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val antifraudConfig = AntifraudLimitsConfig()
  val confirmationDao = new InMemoryConfirmationDao
  val hydra = new InMemoryHydraClient

  val confirmationService: ConfirmationService2 =
    new ConfirmationService2(confirmationDao, DummyConfirmationProducer, DummyConfirmationProducer)
      with AntifraudConfirmationService2 {

      override val antifraudService =
        new AntifraudServiceImpl(hydra, new SingleAntifraudLimitsProvider(antifraudConfig))(ec)
    }

  implicit val reqCtx = RequestContext("123")

  "AntifraudConfirmationService" should {
    "limit confirmation requests count" in {
      val identity = ModelGenerators.identity.next
      (1 until antifraudConfig.confirmRequestsLimit).foreach { _ =>
        confirmationService.requestIdentityConfirmation(identity, ConfirmOneTimeLogin("1")).futureValue
      }
      confirmationService
        .requestIdentityConfirmation(identity, ConfirmOneTimeLogin("1"))
        .failed
        .futureValue shouldBe a[TooManyConfirmationRequestsException]
    }

    "use 8-digit SMS codes after second attempt" in {
      val identity = ModelGenerators.phoneIdentity.next

      val userCommunicationService = mock[UserCommunicationService]
      when(userCommunicationService.sendSms(?, ?, ?)(?)).thenReturn(Future.unit)

      val mockedAntifraudService = mock[AntifraudService]
      when(mockedAntifraudService.confirmationRequestAttempt(identity))
        .thenReturn(Future.successful(AntifraudCounters(2)))

      val confirmationService: ConfirmationService2 =
        new ConfirmationService2(
          confirmationDao,
          DummyConfirmationProducer,
          phoneProducer = new PhoneConfirmationProducer(userCommunicationService, () => true)
        ) with AntifraudConfirmationService2 {
          override val antifraudService = mockedAntifraudService
        }

      confirmationService
        .requestIdentityConfirmation(identity, ConfirmOneTimeLogin("1"))
        .futureValue
        .length shouldBe 8
    }

    "limit code usage attempts" in {
      val identity = ModelGenerators.identity.next
      val code = confirmationService.requestIdentityConfirmation(identity, ConfirmOneTimeLogin("2")).futureValue
      val wrongCode = ConfirmationCode(identity, "wrong code")
      for (_ <- 1 to (antifraudConfig.failedConfirmLimit + 1)) {
        confirmationService
          .useCode(wrongCode) {
            case _ => Future.unit
          }
          .failed
          .futureValue shouldBe a[ConfirmationCodeNotFoundException]
      }
      confirmationService
        .useCode(ConfirmationCode(identity, code)) {
          case _ => Future.unit
        }
        .failed
        .futureValue shouldBe a[TooManyFailedAttemptsException]
    }
  }

}
