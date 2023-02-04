package ru.yandex.vertis.chat.components.dao.security.limits

import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.components.cache.metrics.NopCacheMetricsImpl
import ru.yandex.vertis.chat.components.dao.chat.limits.LimitedChatService
import ru.yandex.vertis.chat.components.dao.chat.storage.{ChatStorage, JvmStorage}
import ru.yandex.vertis.chat.components.dao.statistics.{InStorageStatisticsService, StatisticsService}
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.model.ModelGenerators.userId
import ru.yandex.vertis.chat.model.{Room, UserId}
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.exceptions.LimitExceededException
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}
import ru.yandex.vertis.chat.service.impl.{ChatServiceWrapper, TestDomainAware}
import ru.yandex.vertis.chat.service.{ChatService, ChatServiceTestKit}
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.generators.ProducerProvider

import scala.concurrent.duration._

class LimitedChatServiceSpec extends SpecBase with ChatServiceTestKit with RequestContextAware with ProducerProvider {

  private val state: JvmChatState = JvmChatState.empty()
  private val storage = JvmStorage(state)
  private val jvmChatService = new JvmChatService(state)

  private val effectiveStatisticsService =
    new InStorageStatisticsService with NopCacheMetricsImpl with DomainAutoruSupport {
      override val chatStorage: DMap[ChatStorage] = DMap.forAllDomains(storage)
    }

  private def maxChatsAtMinute: Int = 5

  private def maxFirstMessagesAtMinute: Int = 2

  private val limits: ChatLimits =
    ChatLimits(1.minute -> maxChatsAtMinute, 1.minute -> maxFirstMessagesAtMinute)

  override val service: ChatService =
    new ChatServiceWrapper(jvmChatService) with LimitedChatService with TestDomainAware {
      override val statisticsService: DMap[StatisticsService] = DMap.forAllDomains(effectiveStatisticsService)

      override def chatLimits: ChatLimits = limits
    }

  "LimitedChatService" should {
    "prohibit creation of too many rooms" in {
      val user = userId.next
      withUserContext(user) { rc =>
        (1 to maxChatsAtMinute).foreach(_ => {
          val parameters = createRoomParameters.next.withUserId(user)
          createAndCheckRoom(parameters)(rc)
        })
        a[LimitExceededException] should be thrownBy {
          val parameters = createRoomParameters.next.withUserId(user)
          cause(createAndCheckRoom(parameters)(rc))
        }
      }
    }

    "prohibit sending of too many first messages" in {
      val user = userId.next
      (1 to maxFirstMessagesAtMinute).foreach(_ => {
        createRoomAndSend(user)
      })
      a[LimitExceededException] should be thrownBy {
        cause(createRoomAndSend(user))
      }
    }

    "ignore not first messages" in {
      val user = userId.next
      val rooms = (1 until maxFirstMessagesAtMinute).map(_ => {
        createRoomAndSend(user)
      })
      val msg = sendMessageParameters(rooms.head.id, user).next
      withUserContext(user) { rc =>
        service.sendMessage(msg)(rc).futureValue
      }
    }

    "ignore tech support room and tech support user" in {
      val user = TechSupportUtils.TechSupportUser
      withUserContext(user) { rc =>
        (1 to maxChatsAtMinute).foreach(_ => {
          val parameters = createRoomParameters.next.withUserId(user)
          createAndCheckRoom(parameters)(rc)
        })
        val parameters = createRoomParameters.next.withUserId(user)
        cause(createAndCheckRoom(parameters)(rc))
      }
      (1 to maxFirstMessagesAtMinute).foreach(_ => {
        createRoomAndSend(user)
      })
      createRoomAndSend(user)
    }
  }

  private def createRoomAndSend(user: UserId): Room = {
    withUserContext(user) { rc =>
      val parameters = createRoomParameters.next.withUserId(user)
      val room = createAndCheckRoom(parameters)(rc)
      val msg = sendMessageParameters(room.id, user).next
      service.sendMessage(msg)(rc).futureValue
      room
    }
  }
}
