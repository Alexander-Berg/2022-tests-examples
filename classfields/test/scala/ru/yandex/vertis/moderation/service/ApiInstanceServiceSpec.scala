package ru.yandex.vertis.moderation.service

import org.mockito.ArgumentMatchers.{any => argAny, eq => meq}
import ru.yandex.vertis.moderation.dao._
import ru.yandex.vertis.moderation.feature.EmptyFeatureRegistry
import ru.yandex.vertis.moderation.handler.ModerationRequestHandler
import ru.yandex.vertis.moderation.instance.{
  EssentialsPatchCalculatorImpl,
  InstancePatchCalculatorImpl,
  InstancePatcherImpl
}
import ru.yandex.vertis.moderation.kafka.KafkaProducer
import ru.yandex.vertis.moderation.model.ModerationRequest
import ru.yandex.vertis.moderation.model.ModerationRequest.InitialDepth
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{ExternalId, Instance, UpdateJournalRecord}
import ru.yandex.vertis.moderation.model.signal.HoboSignalSource
import ru.yandex.vertis.moderation.opinion.OpinionCalculator
import ru.yandex.vertis.moderation.{RequestContext, SpecBase}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author mpoplavkov
  */
class ApiInstanceServiceSpec extends SpecBase {

  private val instanceDao = mock[InstanceDao[Future]]
  private val requestHandler = mock[ModerationRequestHandler]
  private val userUpdatesCache = mock[UserUpdatesCache]
  private val apiInstanceService =
    new ApiInstanceServiceImpl(
      requestHandler,
      instanceDao,
      mock[SearchInstanceDao],
      new InstancePatchCalculatorImpl(EssentialsPatchCalculatorImpl),
      InstancePatcherImpl,
      userUpdatesCache,
      EmptyFeatureRegistry,
      mock[InstanceArchiveDao],
      mock[OpinionCalculator],
      mock[KafkaProducer[_, UpdateJournalRecord]],
      mock[MetadataService],
      StubPhoneRedirectSignalEnrichmentService
    )
  private val service = ServiceGen.next

  private val instance = InstanceGen.next
  private val externalId = instance.externalId
  private val expiredInstance = ExpiredInstanceGen.next
  private val expiredExternalId = expiredInstance.externalId
  private val signals = Seq(SignalSourceGen.suchThat(!_.isInstanceOf[HoboSignalSource]).next)
  private val hoboSignal = HoboSignalSourceGen.next
  private val requestTime = DateTimeGen.next
  implicit private val requestContext: RequestContext = RequestContext(requestTime, DateTimeGen.next, InitialDepth)

  "ApiInstanceService" should {
    "get instance by external id" in {
      doReturn(Future.successful(instance)).when(instanceDao).get(externalId, allowExpired = false)
      val actual = apiInstanceService.get(externalId, allowExpired = false).futureValue
      actual shouldBe instance
    }

    "get expired instance by external id" in {
      val expired = expiredInstance.toInstance(service, DateTimeGen.next)
      doReturn(Future.successful(expired)).when(instanceDao).get(expiredExternalId, allowExpired = true)
      val actual = apiInstanceService.get(expiredExternalId, allowExpired = true).futureValue
      actual shouldBe expired
    }

    "not get expired instance by external id if expired are not allowed" in {
      val expired = expiredInstance.toInstance(service, DateTimeGen.next)
      doReturn(Future.successful(expired)).when(instanceDao).get(expiredExternalId, allowExpired = true)
      doReturn(Future.failed(new IllegalArgumentException)).when(instanceDao).get(argAny[ExternalId], meq(false))

      val actualExpired = apiInstanceService.get(expiredExternalId, allowExpired = true).futureValue
      actualExpired shouldBe expired
      apiInstanceService
        .get(expiredExternalId, allowExpired = false)
        .shouldCompleteWithException[IllegalArgumentException]
    }

    "append signals to the instance" in {
      doReturn(Future.successful(instance)).when(instanceDao).get(externalId, allowExpired = false)
      doReturn(Future.unit)
        .when(requestHandler)
        .handle(ModerationRequest.AppendSignals.withInitialDepth(externalId, signals, requestTime))
      doReturn(Future.unit)
        .when(userUpdatesCache)
        .set(argAny[ExternalId], argAny[Instance], argAny[Duration])(argAny[ExecutionContext])

      val actual =
        apiInstanceService
          .appendSignals(externalId, signals, allowExpired = false, forceJournalUpdate = false)
          .futureValue
      there.was(
        one(requestHandler).handle(ModerationRequest.AppendSignals.withInitialDepth(externalId, signals, requestTime))
      )
      val actualSignals = actual.signals.toMap.keySet
      signals.map(_.getKey).toSet.subsetOf(actualSignals) shouldBe true
    }

    "append signals to the expired instance" in {
      val expired = expiredInstance.toInstance(service, DateTimeGen.next)
      doReturn(Future.successful(expired)).when(instanceDao).get(expiredExternalId, allowExpired = true)
      doReturn(Future.unit)
        .when(requestHandler)
        .handle(ModerationRequest.AppendSignals.withInitialDepth(expiredExternalId, signals, requestTime))
      doReturn(Future.unit)
        .when(userUpdatesCache)
        .set(argAny[ExternalId], argAny[Instance], argAny[Duration])(argAny[ExecutionContext])

      val actual =
        apiInstanceService
          .appendSignals(expiredExternalId, signals, allowExpired = true, forceJournalUpdate = false)
          .futureValue
      there.was(
        one(requestHandler).handle(
          ModerationRequest.AppendSignals.withInitialDepth(expiredExternalId, signals, requestTime)
        )
      )
      val actualSignals = actual.signals.toMap.keySet
      signals.map(_.getKey).toSet.subsetOf(actualSignals) shouldBe true
    }

    "append signals with force update journal" in {
      val expired = expiredInstance.toInstance(service, DateTimeGen.next)
      doReturn(Future.successful(expired)).when(instanceDao).get(expiredExternalId, allowExpired = true)
      doReturn(Future.unit)
        .when(requestHandler)
        .handle(ModerationRequest.AppendSignals.withInitialDepth(expiredExternalId, signals, requestTime, true))
      doReturn(Future.unit)
        .when(userUpdatesCache)
        .set(argAny[ExternalId], argAny[Instance], argAny[Duration])(argAny[ExecutionContext])

      val actual =
        apiInstanceService
          .appendSignals(expiredExternalId, signals, allowExpired = true, forceJournalUpdate = true)
          .futureValue
      there.was(
        one(requestHandler).handle(
          ModerationRequest.AppendSignals.withInitialDepth(expiredExternalId, signals, requestTime)
        )
      )
      val actualSignals = actual.signals.toMap.keySet
      signals.map(_.getKey).toSet.subsetOf(actualSignals) shouldBe true
    }

    "return error on append hobo signal with allow expired" in {
      apiInstanceService
        .appendSignals(expiredExternalId, Seq(hoboSignal), allowExpired = true, forceJournalUpdate = false)
        .shouldCompleteWithException[IllegalArgumentException]
    }

    "not append signals to the expired instance if expired are not allowed" in {
      doReturn(Future.failed(new IllegalArgumentException))
        .when(instanceDao)
        .get(expiredExternalId, allowExpired = false)

      apiInstanceService
        .appendSignals(expiredExternalId, signals, allowExpired = false, forceJournalUpdate = false)
        .shouldCompleteWithException[IllegalArgumentException]
    }
  }
}
