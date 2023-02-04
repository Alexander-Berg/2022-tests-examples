package ru.yandex.vertis.passport.scheduler.tasks.moderation

import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.Consumer.NoopControl
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.mockito.Mockito.verify
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.kafka.instrumented.OffsetMetrics
import ru.yandex.vertis.kafka.model.CommittableEvent
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.moderation.client.ModerationClient
import ru.yandex.vertis.passport.dao.FullUserDao
import ru.yandex.vertis.passport.model._
import ru.yandex.vertis.passport.model.proto.{Event => ProtoEvent}
import ru.yandex.vertis.passport.proto.EventsProtoFormats
import ru.yandex.vertis.passport.scheduler.AkkaSupport
import ru.yandex.vertis.passport.service.ban.ModerationService
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class PushToModerationTaskSpec extends WordSpec with Matchers with ScalaFutures with MockitoSupport with AkkaSupport {

  import ru.yandex.vertis.passport.test.Producer.generatorAsProducer

  implicit val ec = actorSystem.dispatcher
  implicit val mat = ActorMaterializer()

  trait Test extends EventsProtoFormats {

    val userDao = mock[FullUserDao]
    val moderationService = mock[ModerationService]
    val moderationClient = mock[ModerationClient]
    val offsetMetrics = mock[OffsetMetrics]
    val tracingSupport = LocalTracingSupport(EndpointConfig.Empty)
    val offset = mock[CommittableOffset]

    def runTask(event: Event) = {
      val payload = event.toProto
      val protoEvent = ProtoEvent.newBuilder().setPayload(payload).build()
      val task = new PushToModerationTask(
        userDao,
        moderationService,
        moderationClient,
        Source
          .fromIterator(() => Seq(CommittableEvent(offset, protoEvent)).toIterator)
          .mapMaterializedValue(_ => NoopControl),
        offsetMetrics,
        tracingSupport
      )
      Await.ready(task.run(), Duration.Inf)
    }

  }

  "send user event to moderation" in new Test {

    val fullUser = ModelGenerators.fullUserWithAllCredentials.next

    when(moderationService.getUserModerationStatus(?)(?)).thenReturn(Future.successful(EnrichedUserModerationStatus()))
    when(userDao.get(?)(?)).thenReturn(Future.successful(fullUser))
    when(moderationClient.push(?, ?)).thenReturn(Future.unit)

    val event = SocializeUserResult(
      userId = "123",
      provider = SocialProviders.Gosuslugi,
      source = SocialUserSource("123"),
      trusted = true
    )

    runTask(event)

    verify(moderationClient, VerificationModeFactory.only()).push(?, ?)
  }

  "skip irrelevant event" in new Test {

    val fullUser = ModelGenerators.fullUserWithAllCredentials.next

    when(moderationService.getUserModerationStatus(?)(?)).thenReturn(Future.successful(EnrichedUserModerationStatus()))
    when(userDao.get(?)(?)).thenReturn(Future.successful(fullUser))
    when(moderationClient.push(?, ?)).thenReturn(Future.unit)

    val event = EmailSent(None, "some@address.ru", "", Map.empty)

    runTask(event)

    verify(moderationService, VerificationModeFactory.atMost(0)).getUserModerationStatus(?)(?)
    verify(userDao, VerificationModeFactory.atMost(0)).get(?)(?)
    verify(moderationClient, VerificationModeFactory.atMost(0)).push(?, ?)
  }

}
