package ru.yandex.vertis.chat.processor.components.workers.unread.enricher

import com.google.common.util.concurrent.{MoreExecutors, RateLimiter}
import org.mockito.Mockito._
import org.scalatest.{Matchers, WordSpec}
import ru.auto.api.ApiOfferModel
import ru.yandex.vertis.chat.components.clients.searcher.AutoruApiClient
import ru.yandex.vertis.chat.components.tracing.{TraceCreator, TracedUtils}
import ru.yandex.vertis.chat.model.ModelGenerators.{CategoryGen, OfferIDGen}
import ru.yandex.vertis.chat.processor.common.model.rooms.Room
import ru.yandex.vertis.chat.processor.common.model.users.User
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}

import scala.concurrent.{ExecutionContext, Future}
import ru.yandex.vertis.chat.{Domain, Domains}

class ChatOnlyAutoruRoomsEnricherSpec extends WordSpec with Matchers with MockitoSupport with ProducerProvider {
  implicit private val domain: Domain = Domains.Auto

  trait Fixture {
    val mockedTraceCreator = mock[TraceCreator]
    val mockedAutoruApiClient = mock[AutoruApiClient]

    val enricher = new DefaultAutoruRoomsEnricher with ChatOnlyAutoruRoomsEnricher {
      override val rateLimiter: RateLimiter = RateLimiter.create(50)

      override val autoruApiClient: DMap[AutoruApiClient] = DMap.forAllDomains {
        mockedAutoruApiClient
      }

      override val traceCreator: TraceCreator = {
        mockedTraceCreator
      }

      implicit override val ec: ExecutionContext = {
        ExecutionContext.fromExecutor(MoreExecutors.directExecutor())
      }
    }
  }

  "ChatOnlyAutoruRoomsEnricher" should {
    "enrich rooms with chatOnly from offer" in new Fixture {
      when(mockedTraceCreator.trace).thenReturn(TracedUtils.empty)
      val offer = ApiOfferModel.Offer.newBuilder()
      offer.getAdditionalInfoBuilder.setChatOnly(true)
      when(mockedAutoruApiClient.getOffer(?, ?)(?)).thenReturn(Future.successful(Some(offer.build())))

      private val category = "cars"
      private val offerId = OfferIDGen.next
      val result = enricher.enrichRooms(
        Seq(
          Room(
            "roomId",
            "objectId",
            offerId,
            category,
            User("user1", unread = false, Seq()),
            User("user2", unread = false, Seq()),
            None,
            Seq(),
            chatOnly = false
          )
        )
      )

      result.head.chatOnly shouldBe true
    }

    "use offerId in requests to autoruApiClient" in new Fixture {
      when(mockedTraceCreator.trace).thenReturn(TracedUtils.empty)
      when(mockedAutoruApiClient.getOffer(?, ?)(?)).thenReturn(Future.successful(None))

      private val category = CategoryGen.next.toString
      private val offerId = OfferIDGen.next
      enricher.enrichRooms(
        Seq(
          Room(
            "roomId",
            "objectId",
            offerId,
            category,
            User("user1", unread = false, Seq()),
            User("user2", unread = false, Seq()),
            None,
            Seq(),
            chatOnly = false
          )
        )
      )

      verify(mockedAutoruApiClient).getOffer(category, offerId)(TracedUtils.empty)
    }
  }
}
