package ru.yandex.vertis.chat.service

import org.scalatest.OptionValues
import org.scalatest.concurrent.Eventually
import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.components.dao.security.spam.ImageSpamDetectionService
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.model._
import ru.yandex.vertis.chat.model.api.ApiModel
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.util.test.RequestContextAware

trait ImageSpamDetectionServiceBaseSpec
  extends SpecBase
  with ChatServiceTestKit
  with RequestContextAware
  with Eventually
  with OptionValues {

  val imageSpamDetectionService: ImageSpamDetectionService

  "ImageSpamDetectionService" should {
    "return true on sending message with attachments to an empty chat" in {
      val room = createAndCheckRoom()
      val parameters =
        sendMessageParameters(room).next.copy(attachments = Seq(ApiModel.Attachment.newBuilder().build()))

      val res = imageSpamDetectionService.isSpam(parameters).futureValue

      res shouldBe true
    }

    "return false on sending message with attachments to a non empty chat" in {
      val room = createAndCheckRoom()
      val parameters1 =
        sendMessageParameters(room).next
      service.sendMessage(parameters1).futureValue

      val parameters2 =
        sendMessageParameters(room).next.copy(
          author = room.users.find(_ != parameters1.author).get,
          attachments = Seq(ApiModel.Attachment.newBuilder().build())
        )

      imageSpamDetectionService.isSpam(parameters2).futureValue shouldBe false
    }

  }

}
