package ru.yandex.vertis.passport.service.confirmation

import org.mockito.ArgumentMatchers.{anyString, argThat}
import org.mockito.Mockito.verify
import org.scalatest.WordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.ConfirmationDao
import ru.yandex.vertis.passport.model.IdentityOrToken.RealIdentity
import ru.yandex.vertis.passport.model.IdentityTypes.Email
import ru.yandex.vertis.passport.model.{Confirmation, Identity, RequestContext}
import ru.yandex.vertis.passport.service.confirmation.ConfirmationService2.ConfirmationOptions
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.util.concurrent.Threads

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class ConfirmationService2SecondSpec extends WordSpec with SpecBase with MockitoSupport {

  trait Fixture {
    val confirmationDao = mock[ConfirmationDao]
    val emailProducer = mock[IdentityConfirmationProducer[Identity.Email]]
    val phoneProducer = mock[IdentityConfirmationProducer[Identity.Phone]]
    val email = ModelGenerators.emailAddress.next

    val emailWithSubAddress =
      email.takeWhile(c => c != '@') + "+" + ModelGenerators.readableString.next + email.dropWhile(c => c != '@')
    val payload = ModelGenerators.confirmationPayload.next
  }

  "save code for email without subaddress and send notify to subaddress" in new Fixture {
    implicit val ec = Threads.SameThreadEc

    val confirmationService: ConfirmationService2 =
      new ConfirmationService2(confirmationDao, emailProducer, phoneProducer)(ec)
    when(emailProducer.generateCode(None)).thenReturn(new String("12345678"))
    when(
      emailProducer.emit(
        Identity.Email(emailWithSubAddress),
        "12345678",
        payload,
        ConfirmationOptions()
      )
    ).thenReturn(Future.unit)
    when(emailProducer.ttl).thenReturn(Duration.Inf)
    when(confirmationDao.upsert(?, ?)(?)).thenReturn(Future.unit)

    val code =
      confirmationService.requestIdentityConfirmation(Identity.Email(emailWithSubAddress), payload).futureValue

    verify(confirmationDao).upsert(argThat[Confirmation] { confirmation =>
      confirmation.code.identity.contains(RealIdentity(Identity.Email(email)))
    }, ?)(?)

    verify(emailProducer).emit(argThat[Identity.Email](email => email.email == emailWithSubAddress), ?, ?, ?)(?)
  }
}
