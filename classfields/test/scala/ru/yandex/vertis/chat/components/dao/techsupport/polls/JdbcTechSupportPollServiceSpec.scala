package ru.yandex.vertis.chat.components.dao.techsupport.polls

import org.joda.time.DateTime
import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.components.time.DefaultTimeServiceImpl
import ru.yandex.vertis.chat.model.{MessagePayload, UserId}
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.model.api.ApiModel.MessageProperties
import ru.yandex.vertis.chat.service.{ChatServiceTestKit, CreateMessageParameters}
import ru.yandex.vertis.chat.service.ServiceGenerators.sendMessageParameters
import ru.yandex.vertis.chat.service.impl.jdbc.{JdbcChatService, JdbcSpec}
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.chat.util.uuid.{RandomIdGenerator, TimeIdGenerator}
import ru.yandex.vertis.chat.util.versionchecker.{Platform, Version}
import ru.yandex.vertis.generators.BasicGenerators._
import ru.yandex.vertis.generators.ProducerProvider

/**
  * TODO
  *
  * @author aborunov
  */
class JdbcTechSupportPollServiceSpec
  extends SpecBase
  with JdbcSpec
  with RequestContextAware
  with ChatServiceTestKit
  with ProducerProvider {
  private val defaultDate: DateTime = new DateTime(0)

  val service =
    new JdbcChatService(database, RandomIdGenerator, new TimeIdGenerator("test"), new DefaultTimeServiceImpl)
      with SameThreadExecutionContextSupport

  private val techSupportPollService = new JdbcTechSupportPollService(database) {
    override def acceptedVersions: Map[Platform, Version] = Map.empty
  }

  "Tech Support Poll Service" should {
    "add poll" in {
      val room = createAndCheckRoom()
      val parameters = sendMessageParameters(room).next
      val sendMessageResult = service.sendMessage(parameters).futureValue
      val mId = sendMessageResult.message.id
      val hash = readableString.next
      val uId = userId.next
      val opId = operatorId.next
      techSupportPollService.addPoll(mId, hash, uId, opId).futureValue

      techSupportPollService.getUser(hash).futureValue shouldBe Some(uId)
      techSupportPollService.getPolls(Set(hash)).futureValue should have size 1
      withDefaultDates(techSupportPollService.getPolls(Set(hash)).futureValue.head) shouldBe
        PollData(hash, uId, None, opId, defaultDate, None, None)
    }

    "set rating" in {
      val room = createAndCheckRoom()
      val parameters = sendMessageParameters(room).next
      val sendMessageResult = service.sendMessage(parameters).futureValue
      val mId = sendMessageResult.message.id
      val hash = readableString.next
      val uId = userId.next
      val opId = operatorId.next
      techSupportPollService.addPoll(mId, hash, uId, opId).futureValue

      val res1 = techSupportPollService.setRating(hash, 3).futureValue
      res1._1 shouldBe true
      res1._2.userId shouldBe uId
      res1._2.operatorId shouldBe opId
      res1._2.hash shouldBe hash
      res1._2.rating shouldBe Some(3)
      withDefaultDates(techSupportPollService.getPolls(Set(hash)).futureValue.head) shouldBe
        PollData(hash, uId, Some(3), opId, defaultDate, None, None)

      val res2 = techSupportPollService.setRating(hash, 1).futureValue
      res2._1 shouldBe false
      res1._2.userId shouldBe uId
      res1._2.operatorId shouldBe opId
      res1._2.hash shouldBe hash
      res1._2.rating shouldBe Some(3)
      withDefaultDates(techSupportPollService.getPolls(Set(hash)).futureValue.head) shouldBe
        PollData(hash, uId, Some(3), opId, defaultDate, None, None)
    }

    "set preset message id" in {
      val room = createAndCheckRoom()
      val parameters = sendMessageParameters(room).next
      val sendMessageResult = service.sendMessage(parameters).futureValue
      val mId = sendMessageResult.message.id
      val hash = readableString.next
      val uId = userId.next
      val opId = operatorId.next
      techSupportPollService.addPoll(mId, hash, uId, opId).futureValue

      val presetMessageId = messageId.next

      techSupportPollService.setPresetMessageId(hash, presetMessageId).futureValue shouldBe true
      techSupportPollService.setPresetMessageId(hash, "other").futureValue shouldBe false
    }

    "set selected preset" in {
      val room = createAndCheckRoom()
      val parameters = sendMessageParameters(room).next
      val sendMessageResult = service.sendMessage(parameters).futureValue
      val mId = sendMessageResult.message.id
      val hash = readableString.next
      val uId = userId.next
      val opId = operatorId.next
      techSupportPollService.addPoll(mId, hash, uId, opId).futureValue

      techSupportPollService.setSelectedPreset(hash, "didnt_help").futureValue shouldBe true
      techSupportPollService.setSelectedPreset(hash, "other").futureValue shouldBe false
    }

    "return empty Seq from getPolls for empty hashes" in {
      techSupportPollService.getPolls(Set()).futureValue should have size 0
    }

    "get last user app version" in {
      val user = userId.next
      createAndCheckRoom(TechSupportUtils.roomLocator(user).asSource.get.parameters)(requestContext)
      service
        .sendMessage(
          supportMessageToUser(
            user,
            "Добрый день, меня зовут Валерия! Пока что такой опции нет, но такую идею разработчикам уже передавали. " +
              "Остается ждать, когда смогут внедрить.",
            "rela-varela@yandex-team.ru"
          )
        )
        .futureValue
      service.sendMessage(userMessageToSupport(user, "Спасибо!", "ANDROID 6.5.3")).futureValue
      service
        .sendMessage(
          supportMessageToUser(
            user,
            "Надеемся, мы вам помогли!\nБудет здорово, если вы поставите " +
              "нам оценку",
            ""
          )
        )
        .futureValue
      service.sendMessage(supportMessageToUser(user, "Что понравилось?", "")).futureValue
      service.sendMessage(userMessageToSupport(user, "Вежливое общение", "")).futureValue
      service.sendMessage(supportMessageToUser(user, "Рады стараться!", "")).futureValue
      techSupportPollService.getLastUserAppVersion(user).futureValue shouldBe (Platform.Android, Version(6, 5, 3, 0))
    }

    "not get last user app version" in {
      val user = userId.next
      createAndCheckRoom(TechSupportUtils.roomLocator(user).asSource.get.parameters)(requestContext)
      service
        .sendMessage(
          supportMessageToUser(user, "Покупатели снова видят ваше объявление о продаже Subaru Impreza на сайте.", "")
        )
        .futureValue
      techSupportPollService.getLastUserAppVersion(user).futureValue shouldBe (Platform.Unexpected, Version.Undefined)
    }
  }

  private def userMessageToSupport(user: UserId, text: String, userAppVersion: String) = {
    CreateMessageParameters(
      TechSupportUtils.roomId(user),
      user,
      MessagePayload.fromText(text),
      Seq.empty,
      None,
      properties = MessageProperties.newBuilder().setUserAppVersion(userAppVersion).build()
    )
  }

  private def supportMessageToUser(user: UserId, text: String, operator: String) = {
    CreateMessageParameters(
      TechSupportUtils.roomId(user),
      TechSupportUtils.TechSupportUser,
      MessagePayload.fromText(text),
      Seq.empty,
      None,
      properties = MessageProperties.newBuilder().setTechSupportOperatorId(operator).build()
    )
  }

  private def withDefaultDates(pollData: PollData): PollData = {
    pollData.copy(created = defaultDate, updated = None)
  }
}
