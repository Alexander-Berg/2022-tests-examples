package ru.yandex.vertis.chat.service.cache

import java.io.IOException

import org.mockito.Mockito._
import ru.yandex.vertis.chat.components.cache.CacheService
import ru.yandex.vertis.chat.components.cache.local.JvmCache
import ru.yandex.vertis.chat.components.cache.metrics.NopCacheMetricsImpl
import ru.yandex.vertis.chat.components.dao.chat.cached.CachedChatService
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.model.ModelGenerators.{anyParticipant, roomId}
import ru.yandex.vertis.chat.model.{ModelGenerators, Room}
import ru.yandex.vertis.chat.service.ServiceGenerators.{createRoomParameters, sendMessageParameters}
import ru.yandex.vertis.chat.service._
import ru.yandex.vertis.chat.service.impl.ChatServiceWrapper
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.{CacheControl, RequestContext, SlagGenerators}
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  * Checks whether [[CacheService]] doesn't violates
  * [[ChatService]] behaviour.
  *
  * @author dimas
  */
class CachedChatServiceSpec extends ChatServiceSpecBase with MockitoSupport {

  private val state: JvmChatState = JvmChatState.empty()

  private val jvmChatService = new JvmChatService(state)

  private val cacheService: DMap[JvmCache] = DMap.forDomainAware(d => {
    new JvmCache with NopCacheMetricsImpl with d.DomainAwareImpl {
      override val persistentChatService: DMap[ChatService] = DMap.forAllDomains(jvmChatService)
    }
  })

  val service: ChatService =
    new ChatServiceWrapper(jvmChatService)
      with CachedChatService
      with LoggingChatService
      with TestOperationalSupport
      with DomainAutoruSupport {
      override protected def loggerClass: Class[_] = classOf[ChatService]

      override def localCacheService: DMap[JvmCache] = cacheService

      override def userLocalityCacheService: DMap[CacheService] = cacheService
    }

  "CachingChatService" should {
    val m = mock[ChatService]
    val cache = DMap.forDomainAware(d => {
      new JvmCache with NopCacheMetricsImpl with d.DomainAwareImpl {
        override val persistentChatService: DMap[ChatService] = DMap.forAllDomains(m)
      }
    })
    val cached = new ChatServiceWrapper(m) with CachedChatService with DomainAutoruSupport {
      override def localCacheService: DMap[JvmCache] = cache

      override def userLocalityCacheService: DMap[CacheService] = cache
    }

    val ArtificialFailure =
      Future.failed(new IOException("Artificial"))

    "sendMessage to roomLocator" in {
      val id = roomId.next
      val newRoomParameters = createRoomParameters(Some(id)).next
      val roomLocator = RoomLocator.Source(newRoomParameters)
      val author = anyParticipant(newRoomParameters.participants).next
      val parameters = sendMessageParameters(roomLocator, author).next
      parameters.room shouldBe ""
      val room = ModelGenerators.room.next
      when(m.getRoom(?)(?)).thenReturn(Future.successful(room))
      val message = ModelGenerators.message.next
      val sendMessageResult = SendMessageResult(message, None)
      when(m.sendMessage(?)(?)).thenReturn(Future.successful(sendMessageResult))

      cached.sendMessage(parameters).futureValue
      verify(m).getRoom(eq(sendMessageResult.roomId))(any())
      clearInvocations(m)
    }

    "not cache getRoom() failures" in {
      val room = ModelGenerators.room.next

      when(m.getRoom(?)(?))
        .thenReturn(ArtificialFailure)

      intercept[IOException] {
        withExplicitAllowCache(times = 1) { rc =>
          cause(
            cached
              .getRoom(room.id)(rc)
              .futureValue
          )
        }
      }

      when(m.getRoom(?)(?))
        .thenReturn(Future.successful(room))

      withExplicitAllowCache(times = 3) { rc =>
        cached
          .getRoom(room.id)(rc)
          .futureValue should be(room)
      }

      verify(m, times(2)).getRoom(?)(?)
    }

    "not cache getRooms() failures" in {
      val user = ModelGenerators.userId.next
      val rooms = ModelGenerators.room.next(10).toSeq.map(_.withUser(user)).sorted(Room.ordering(None))

      when(m.getAllRooms(?, ?, ?, ?, ?)(?))
        .thenReturn(ArtificialFailure)

      intercept[IOException] {
        withExplicitAllowCache(1) { rc =>
          cause(
            cached
              .getRooms(user)(rc)
              .futureValue
          )
        }
      }

      when(m.getAllRooms(?, ?, ?, ?, ?)(?))
        .thenReturn(Future.successful(rooms))

      withExplicitAllowCache(3) { rc =>
        cached
          .getRooms(user)(rc)
          .futureValue should be(rooms)
      }

      verify(m, times(2)).getAllRooms(?, ?, ?, ?, ?)(?)
    }

    "not cache hasUnread() failures" in {
      val user = ModelGenerators.userId.next

      when(m.hasUnread(?)(?))
        .thenReturn(ArtificialFailure)

      intercept[IOException] {
        withExplicitAllowCache(1) { rc =>
          cause(
            cached
              .hasUnread(user)(rc)
              .futureValue
          )
        }
      }

      when(m.hasUnread(?)(?))
        .thenReturn(Future.successful(true))

      withExplicitAllowCache(3) { rc =>
        cached
          .hasUnread(user)(rc)
          .futureValue should be(true)
      }

      verify(m, times(2)).hasUnread(?)(?)
    }
  }

  private def withExplicitAllowCache(times: Int)(action: RequestContext => Unit): Unit = {
    val rc = SlagGenerators.requestContext.next
      .withCacheControl(CacheControl.Allow)
    (1 to times)
      .foreach { _ =>
        action(rc)
      }
  }
}
