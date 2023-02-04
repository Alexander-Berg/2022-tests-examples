package ru.yandex.vertis.passport.service.confirmation

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.dao.impl.memory.InMemoryConfirmationDao
import ru.yandex.vertis.passport.model.IdentityTypes.Email
import ru.yandex.vertis.passport.model.{ConfirmationCode, Identity, RequestContext}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class ConfirmationService2Spec extends WordSpec with SpecBase {

  import scala.concurrent.ExecutionContext.Implicits.global

  val confirmationDao = new InMemoryConfirmationDao

  val confirmationService: ConfirmationService2 =
    new ConfirmationService2(confirmationDao, DummyConfirmationProducer, DummyConfirmationProducer)

  implicit val reqCtx = RequestContext("test")

  val identity = ModelGenerators.identity.next
  val payload = ModelGenerators.confirmationPayload.next

  var confirmationCode: ConfirmationCode = _

  "ConfirmationService" should {

    "send request" in {
      val code = confirmationService
        .requestIdentityConfirmation(identity, payload)
        .futureValue
      confirmationCode = ConfirmationCode(identity, code)
    }

    "use confirmation code" in {
      confirmationService
        .useCode(confirmationCode) {
          case pl =>
            pl shouldBe payload
            Future.unit
        }
        .futureValue
    }

    "not allow to use same code again" in {
      val res = confirmationService.useCode(confirmationCode) {
        case _ =>
          Future.unit
      }
      res.failed.futureValue shouldBe a[NoSuchElementException]
    }
  }

}
