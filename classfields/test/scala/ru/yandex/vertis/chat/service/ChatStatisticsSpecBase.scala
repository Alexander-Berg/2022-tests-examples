package ru.yandex.vertis.chat.service

import java.util.concurrent.atomic.AtomicInteger

import org.scalacheck.Gen
import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.components.dao.statistics.StatisticsService
import ru.yandex.vertis.chat.model.ModelGenerators.userId
import ru.yandex.vertis.chat.service.ServiceGenerators.sendMessageParameters
import ru.yandex.vertis.chat.service.ServiceGenerators.createRoomParameters
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.generators.BasicGenerators.bool
import ru.yandex.vertis.generators.ProducerProvider

trait ChatStatisticsSpecBase extends SpecBase with ChatServiceTestKit with RequestContextAware with ProducerProvider {

  def service: ChatService

  def statistics: StatisticsService

  "count spam messages" in {
    val user = userId.next
    val room = createAndCheckRoom(_.withUserId(user))
    val count = Gen.choose(0, 10).next
    val parameters = sendMessageParameters(room)
      .next(count)
      .map(_.copy(author = user, isSpam = bool.next))
    val spamSent = new AtomicInteger()
    withUserContext(user) { rc =>
      parameters.foreach(p => {
        val sendMessageResult = service.sendMessage(p)(rc).futureValue

        if (p.isSpam && !sendMessageResult.isDuplicate) {
          spamSent.incrementAndGet()
        }

        statistics.countSpam(user).futureValue shouldBe spamSent.get()
      })
    }
  }

  "count sent messages" in {
    val user = userId.next
    val room = createAndCheckRoom(_.withUserId(user))
    val count = Gen.choose(0, 10).next
    val expected = new AtomicInteger()
    withUserContext(user) { rc =>
      (1 to count).foreach(_ => {
        val parameters =
          sendMessageParameters(room).next.copy(author = user)
        val sendMessageResult = service.sendMessage(parameters)(rc).futureValue

        if (!sendMessageResult.isDuplicate) {
          expected.incrementAndGet()
        }

        statistics.countSentMessages(user).futureValue shouldBe expected.get()
      })
    }
  }

  "provide list of room timestamps" in {
    val user = userId.next
    val count = Gen.choose(0, 10).next
    (1 to count).foreach(expected => {
      val parameters = createRoomParameters.next.withUserId(user)
      withUserContext(user) { rc =>
        createAndCheckRoom(parameters)(rc)
      }
      statistics.getRoomsTsForDay(user).futureValue should have size expected
    })
  }
}
