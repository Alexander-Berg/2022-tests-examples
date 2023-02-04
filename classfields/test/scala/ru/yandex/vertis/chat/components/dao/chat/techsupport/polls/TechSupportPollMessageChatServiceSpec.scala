package ru.yandex.vertis.chat.components.dao.chat.techsupport.polls

import java.util.concurrent.atomic.AtomicReference

import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfter, OptionValues}
import ru.yandex.vertis.MimeType
import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.components.dao.techsupport.polls.TechSupportPollService
import ru.yandex.vertis.chat.components.dao.techsupport.polls.messages.{PollMessages, PollMessagesService}
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.components.workersfactory.SimpleWorkersFactorySupport
import ru.yandex.vertis.chat.model.api.ApiModel.MessagePropertyType
import ru.yandex.vertis.chat.model.{MessagePayload, ModelGenerators}
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.impl.ChatServiceWrapper
import ru.yandex.vertis.chat.service.support.polls.TestPollMessagesService
import ru.yandex.vertis.chat.service.{ChatService, CreateMessageParameters, SendMessageResult, TestOperationalSupport}
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.generators.BasicGenerators.readableString
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
class TechSupportPollMessageChatServiceSpec
  extends SpecBase
  with RequestContextAware
  with MockitoSupport
  with OptionValues
  with BeforeAndAfter
  with Eventually {
  private val mockedChatService = mock[ChatService]
  private val techSupportPollService = mock[TechSupportPollService]
  private val testPollMessages = new AtomicReference[PollMessages](PollMessages.Default)

  val service: ChatService with TechSupportPollMessageChatService =
    new ChatServiceWrapper(mockedChatService)
      with SimpleWorkersFactorySupport
      with TechSupportPollMessageChatService
      with TestOperationalSupport
      with DomainAutoruSupport {
      override val techSupportPollsService: DMap[TechSupportPollService] = DMap.forAllDomains(techSupportPollService)

      override def pollMessagesService: DMap[PollMessagesService] =
        DMap.forAllDomains(new TestPollMessagesService(testPollMessages.get()))
    }

  "TechSupportPollMessageChatService" should {
    "handleUserFeedback" in {
      val user = ModelGenerators.userId.next
      val pollHash = readableString.next
      val techSupportRoomLocator = TechSupportUtils.roomLocator(user)
      val selectedPreset = "all_good"
      val messageParams: CreateMessageParameters = sendMessageParameters(techSupportRoomLocator, user).next
        .withProperties { builder =>
          builder.getTechSupportFeedbackBuilder.setHash(pollHash)
          builder.getTechSupportFeedbackBuilder.setSelectedPreset(selectedPreset)
        }
      val messageParamsWithType = messageParams.withProperties { builder =>
        builder.setType(MessagePropertyType.TECH_SUPPORT_FEEDBACK_RESPONSE)
      }
      when(mockedChatService.sendMessage(?)(?))
        .thenReturn(Future.successful(SendMessageResult(ModelGenerators.message.next, None)))
      when(techSupportPollService.setSelectedPreset(eq(pollHash), eq(selectedPreset))(?))
        .thenReturn(Future.successful(true))
      withUserContext(user) { implicit rc =>
        service.sendMessage(messageParams)
      }
      eventually {
        verify(mockedChatService).sendMessage(eq(messageParamsWithType))(?)
        verify(mockedChatService).sendMessage(
          eq(
            CreateMessageParameters(
              TechSupportUtils.roomId(user),
              TechSupportUtils.TechSupportUser,
              MessagePayload(
                MimeType.TEXT_PLAIN,
                testPollMessages.get().allPresetsMap(selectedPreset).end
              ),
              attachments = Seq.empty,
              providedId = None,
              isSilent = true,
              roomLocator = Some(TechSupportUtils.roomLocator(user))
            )
          )
        )(?)
        verify(techSupportPollService).setSelectedPreset(eq(pollHash), eq(selectedPreset))(?)
      }
    }
  }
}
