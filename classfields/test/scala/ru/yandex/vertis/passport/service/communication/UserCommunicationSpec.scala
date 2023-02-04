package ru.yandex.vertis.passport.service.communication

import org.scalatest.FreeSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.FullUserDao
import ru.yandex.vertis.passport.integration.chat.ChatClient
import ru.yandex.vertis.passport.integration.email.EmailSender
import ru.yandex.vertis.passport.integration.sms.SmsSender
import ru.yandex.vertis.passport.loc.AutoruStringResources
import ru.yandex.vertis.passport.model.{Identity, RequestContext, UserPhone}
import ru.yandex.vertis.passport.model.api.NotifyUserParameters
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

import scala.concurrent.Future

class UserCommunicationSpec extends FreeSpec with SpecBase with MockitoSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val ctx = RequestContext("test")

  val smsSender = mock[SmsSender]
  val chatClient = mock[ChatClient]
  val emailSender = mock[EmailSender]
  val userDao = mock[FullUserDao]

  val communicationService =
    new UserCommunicationServiceImpl(smsSender, emailSender, chatClient, userDao, AutoruStringResources)

  val phones = ModelGenerators.phoneNumber.next(2).toList
  val template = "plain"
  val phone = phones.head

  val user = {
    val u = ModelGenerators.fullUser.next
    val userPhones = phones.map(p => UserPhone(p, None))
    u.copy(phones = userPhones)
  }
  val userId: String = user.id

  "UserCommunicationService" - {

    "notify via sms by phone" in {
      val text = ModelGenerators.readableString.next
      when(smsSender.send(phone, text, template)).thenReturn(Future.successful(text))
      communicationService
        .notifyUser(user, NotifyUserParameters.PlainSms(Some(phone), text))
        .futureValue
    }

    "notify via sms by phone and by userId" in {
      val text = ModelGenerators.readableString.next
      when(smsSender.send(phone, text, template)).thenReturn(Future.successful(text))
      when(userDao.get(userId)).thenReturn(Future.successful(user))
      communicationService
        .notifyUserByUserId(userId, NotifyUserParameters.PlainSms(Some(phone), text))
        .futureValue
    }

    "notify via sms without specifying phone" in {
      val text = ModelGenerators.readableString.next
      when(smsSender.send(phones.last, text, template)).thenReturn(Future.successful(text))
      communicationService
        .notifyUser(user, NotifyUserParameters.PlainSms(None, text))
        .futureValue
    }
  }
}
