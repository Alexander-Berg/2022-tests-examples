package ru.yandex.vertis.punisher.kafka

import cats.data.OptionT
import cats.effect.{ConcurrentEffect, IO, Sync, Timer}
import fs2.kafka.ConsumerRecord
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.generators.NetGenerators.asProducer
import ru.yandex.vertis.moderation.proto.Model.UpdateJournalRecord
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.Generators.{AutoruCategoryGen, AutoruOfferStateGen, PunisherRequestGen, UserCheckGen}
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model._
import ru.yandex.vertis.punisher.services.ModerationService.{AutoruSignal, SuccessSendResult}
import ru.yandex.vertis.punisher.services.{AutoruOffersStateService, ModerationService}
import ru.yandex.vertis.punisher.stages.Clusterizer
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class VinReplacementConsumerTest extends BaseSpec {
  implicit val timer: Timer[F] = IO.timer(SameThreadExecutionContext)
  implicit val context: TaskContext.Stream =
    TaskContext.Stream(
      taskDomain = TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers),
      triggerEventDateTime = DateTimeUtils.now.toInstant
    )
  val conString = "mocked connectionString"
  val kafkaConfig = Map("kafkaMockKey" -> "kafkaMockKeyValue")
  val offerStateServiceMock: AutoruOffersStateService[F] = mock[AutoruOffersStateService[F]]
  val moderationServiceMock: ModerationService[F] = mock[ModerationService[F]]
  val clusteringMock: Clusterizer[F] = mock[Clusterizer[F]]
  val updateJournalRecordMock: UpdateJournalRecord = mock[UpdateJournalRecord]
  val consumerRecordMock: ConsumerRecord[String, PunisherRequest] = mock[ConsumerRecord[String, PunisherRequest]]
  val vinReplacementConsumer =
    new VinReplacementConsumer[F](
      conString,
      kafkaConfig,
      offerStateServiceMock,
      moderationServiceMock,
      clusteringMock,
      TaskDomainImpl(Domain.DOMAIN_AUTO, TaskDomain.Labels.VinReplacement)
    )

  "consumer" should {

    "consume properly and return success" in {
      resetMocks()
      val punisherRequest = PunisherRequestGen.next
      val returnValue = OptionT.pure(createCurrentState())
      val localCluster = new UserIdCluster(Set(new UserId("2")), "1")

      when(moderationServiceMock.send(any[AutoruSignal]())).thenReturn(Sync[F].pure(SuccessSendResult))
      when(consumerRecordMock.value).thenReturn(
        punisherRequest.copy(check = UserCheckGen.next.copy(offerId = Some("1"), userId = "1"))
      )
      when(offerStateServiceMock.getById(any(), any())).thenReturn(returnValue)
      when(clusteringMock.clusterize(any(), any())(any())).thenReturn(Sync[F].pure(localCluster))
      when(offerStateServiceMock.getUserVinCluster(any(), any(), any()))
        .thenReturn(Sync[F].pure(constructListOfOffersForTest(1, 3, active = true)))

      vinReplacementConsumer.consume(consumerRecordMock).await
      verify(moderationServiceMock, times(1)).send(signal = any())

    }

    "consume properly, with dealers 3 ofers, 1 current, 2 dealers" in {
      resetMocks()
      val punisherRequest = PunisherRequestGen.next
      val returnValue = OptionT.pure(createCurrentState())
      val localCluster = new UserIdCluster(Set(new UserId("2")), "1")
      val offerStatesForCheck = constructListOfOffersForTest(2, 3, active = true, isAuthorDealer = true)
      when(clusteringMock.clusterize(any(), any())(any())).thenReturn(Sync[F].pure(localCluster))
      when(moderationServiceMock.send(any[AutoruSignal]())).thenReturn(Sync[F].pure(SuccessSendResult))
      when(consumerRecordMock.value).thenReturn(
        punisherRequest.copy(check = UserCheckGen.next.copy(offerId = Some("1"), userId = "1"))
      )
      when(offerStateServiceMock.getById(any(), any())).thenReturn(returnValue)

      when(offerStateServiceMock.getUserVinCluster(any(), any(), any()))
        .thenReturn(Sync[F].pure(offerStatesForCheck))

      vinReplacementConsumer.consume(consumerRecordMock).await
      verify(moderationServiceMock, times(1)).send(signal = any())

    }
  }

  "processOfferStatesCluster" should {

    "return single verdict" in {

      val userIdCluster = UserIdCluster(Set(new UserId("4")), "4")
      val currentOffer = createCurrentState()
      val offerStates = constructListOfOffersForTest(2, 4, active = true)

      when(clusteringMock.clusterize(any(), any())(any())).thenReturn(Sync[F].pure(userIdCluster))

      val result = vinReplacementConsumer.processOfferStatesCluster(offerStates, currentOffer).await
      println(result)
      result.isDefined should be(true)
    }

    "return verdict with earliest triggerEventDate" in {
      val userIdCluster = UserIdCluster(Set(new UserId("4")), "4")
      val currentOffer = createCurrentState(Instant.now().minus(30, ChronoUnit.DAYS))
      val offerStates = constructListOfOffersForTest(2, 4, active = true)
      when(clusteringMock.clusterize(any(), any())(any())).thenReturn(Sync[F].pure(userIdCluster))
      val result = vinReplacementConsumer.processOfferStatesCluster(offerStates, currentOffer).await
      result.isDefined should be(true)
      result.get.externalId.userId shouldNot be("1")

    }

    "return no verdicts cause of clustering" in {
      val userIdCluster = UserIdCluster(Set(new UserId("4"), new UserId("3"), new UserId("2")), "1")
      val state = createCurrentState()
      val offerStates = constructListOfOffersForTest(2, 4, active = true)
      when(clusteringMock.clusterize(any(), any())(any())).thenReturn(ConcurrentEffect[F].pure(userIdCluster))
      val result = vinReplacementConsumer.processOfferStatesCluster(offerStates, state).await
      result.isDefined should be(false)
    }

  }

  private def createCurrentState(creationDate: Instant = Instant.now(), userId: UserId = "1") = {
    AutoruOfferStateGen.next.copy(
      userId = userId,
      offerId = "1",
      category = Some(AutoruCategoryGen.next),
      vin = Some("vin"),
      isActive = true,
      isCallCenter = Some(false),
      isAuthorDealer = false,
      creationDate = Some(creationDate),
      triggerEventDatetime = Some(Instant.now().minus(Random.between(10, 20), ChronoUnit.DAYS))
    )
  }

  private def constructListOfOffersForTest(i: Int, n: Int, active: Boolean, isAuthorDealer: Boolean = false) = {
    (i to n).toList.map(id =>
      AutoruOfferStateGen.next.copy(
        userId = id.toString,
        isActive = active,
        isCallCenter = Some(false),
        isAuthorDealer = isAuthorDealer,
        vin = Some(i.toString),
        category = Some(AutoruCategoryGen.next),
        creationDate = Some(Instant.now().minus(Random.between(10, 20), ChronoUnit.DAYS)),
        triggerEventDatetime = Some(Instant.now().minus(Random.between(10, 20), ChronoUnit.DAYS))
      )
    )
  }

  private def resetMocks(): Unit = {
    reset(offerStateServiceMock)
    reset(updateJournalRecordMock)
    reset(clusteringMock)
    reset(consumerRecordMock)
    reset(moderationServiceMock)
  }
}
