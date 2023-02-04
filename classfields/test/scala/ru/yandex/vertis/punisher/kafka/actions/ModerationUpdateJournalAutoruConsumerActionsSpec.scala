package ru.yandex.vertis.punisher.kafka.actions

import cats.effect.{IO, Sync, Timer}
import fs2.kafka.ConsumerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.SellerType
import ru.yandex.vertis.moderation.proto.Model.Diff.Autoru.Value.{ADDRESS, CONTEXT_VISIBILITY, VIN_HISTORY_METADATA}
import ru.yandex.vertis.moderation.proto.Model.Metadata.VinHistoryMetadata
import ru.yandex.vertis.moderation.proto.Model._
import ru.yandex.vertis.moderation.proto.RealtyLight.RealtyEssentials
import ru.yandex.vertis.punisher.builder.AutoruOfferStateBuilder
import ru.yandex.vertis.punisher.dao.AutoruOffersStateDao
import ru.yandex.vertis.punisher.model.TaskId
import ru.yandex.vertis.punisher.services.impl.PunisherRequestProducer
import ru.yandex.vertis.punisher.{BaseSpec, ModerationProtoSpec}
import ru.yandex.vertis.quality.cats_utils.Awaitable._

import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class ModerationUpdateJournalAutoruConsumerActionsSpec extends BaseSpec with ModerationProtoSpec {

  implicit val timer: Timer[F] = IO.timer(SameThreadExecutionContext)

  val dao = mock[AutoruOffersStateDao[F]]
  when(dao.upsert(any(), any())).thenReturn(Sync[F].unit)

  val punisherRequestProducer = mock[PunisherRequestProducer[F]]
  val recordMetadata = mock[RecordMetadata]
  when(punisherRequestProducer.append(any())).thenReturn(Sync[F].pure(recordMetadata))

  val setConsumeWithRequest = Set(VIN_HISTORY_METADATA)
  val setConsumeWithoutRequest = Set(CONTEXT_VISIBILITY)
  val setNotOk = Set(ADDRESS)

  val diffAutoruConsumeWithRequest = Diff.Autoru.newBuilder().setVersion(1).addAllValues(setConsumeWithRequest.asJava)

  val diffAutoruConsumeWithoutRequest =
    Diff.Autoru.newBuilder().setVersion(1).addAllValues(setConsumeWithoutRequest.asJava)
  val diffAutoruNotAccept = Diff.Autoru.newBuilder().setVersion(1).addAllValues(setNotOk.asJava)

  val diffConsumeWithRequest = Diff.newBuilder().setVersion(1).setAutoru(diffAutoruConsumeWithRequest)
  val diffConsumeWithoutRequest = Diff.newBuilder().setVersion(1).setAutoru(diffAutoruConsumeWithoutRequest)
  val diffNotOk = Diff.newBuilder().setVersion(1).setAutoru(diffAutoruNotAccept)

  val privateUser = User.newBuilder().setVersion(1).setAutoruUser(UserId)
  val realtyUser = User.newBuilder().setVersion(1).setYandexUser(YandexUid)

  val externalIdPrivate = buildExternalId(privateUser, AutoruObjectId)
  val externalIdRealty = buildExternalId(realtyUser, RealtyObjectId)

  val autoruPrivateEssentials = buildAutoruEssentials()
  val autoruComercialEssentials = buildAutoruEssentials(sellerType = SellerType.COMMERCIAL)
  val privateEssentials = Essentials.newBuilder().setVersion(1).setAutoru(autoruPrivateEssentials)
  val comercialEssentials = Essentials.newBuilder().setVersion(1).setAutoru(autoruComercialEssentials)
  val realtyEssentials = Essentials.newBuilder().setVersion(1).setRealty(RealtyEssentials.newBuilder().setVersion(1))

  val autoruDomain = Domain.newBuilder().setVersion(1).setAutoru(Domain.Autoru.DEFAULT_AUTORU)
  val realtyDomain = Domain.newBuilder().setVersion(1).setRealty(Domain.Realty.DEFAULT_REALTY)

  val okOpinion = buildOpinion(Opinion.Type.OK, Seq.empty)

  val autoruOkEntry = buildEntry(autoruDomain, okOpinion)
  val realtyOkEntry = buildEntry(realtyDomain, okOpinion)

  val autoruOkOpinions = buildOpinions(Seq(autoruOkEntry))
  val realtyOkOpinions = buildOpinions(Seq(realtyOkEntry))

  val visibleContext = Context.newBuilder.setVersion(1).setVisibility(Visibility.VISIBLE)

  val metadata =
    List(
      Metadata.newBuilder().setVinHistoryMetadata(VinHistoryMetadata.newBuilder().setPreviousVin(PreviousVin)).build()
    )

  val instanceAutoruPrivateOk =
    buildInstance(externalIdPrivate, privateEssentials, autoruOkOpinions, visibleContext, metadata)

  val instanceAutoruCommercialOk =
    buildInstance(externalIdPrivate, comercialEssentials, autoruOkOpinions, visibleContext, metadata)

  val instanceRealty = buildInstance(externalIdRealty, realtyEssentials, realtyOkOpinions, visibleContext)

  val recordAutoruWithVinReplacement =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruPrivateOk),
      diff = Some(diffConsumeWithRequest),
      ts = Some(Timestamp)
    )

  val recordAutoruConsumeWithRequest =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruCommercialOk),
      diff = Some(diffConsumeWithRequest),
      ts = Some(Timestamp)
    )

  val recordAutoruConsumeWithoutRequest =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruCommercialOk),
      diff = Some(diffConsumeWithoutRequest),
      ts = Some(Timestamp)
    )

  val recordAutoruNotOk =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruPrivateOk),
      diff = Some(diffNotOk),
      ts = Some(Timestamp)
    )

  val recordRealtyOk =
    buildUpdateJournalRecord(
      instance = Some(instanceRealty),
      diff = Some(diffConsumeWithoutRequest),
      ts = Some(Timestamp)
    )

  val consumerActions =
    new ModerationUpdateJournalAutoruConsumerActions(AutoruOfferStateBuilder, dao, punisherRequestProducer)

  featureRegistryF.updateFeature(consumerActions.saveOffersToDatabaseFeature.name, true).await

  before {
    clearInvocations(dao)
    clearInvocations(punisherRequestProducer)
  }

  "ModerationUpdateJournalAutoruConsumerActions" should {
    val consumerRecord = mock[ConsumerRecord[TaskId, UpdateJournalRecord]]

    "accept UpdateJournalRecord with suitable diff" in {
      when(consumerRecord.value).thenReturn(recordAutoruConsumeWithRequest)
      consumerActions.accept(consumerRecord) shouldBe true
    }

    "not accept UpdateJournalRecord without suitable diff" in {
      when(consumerRecord.value).thenReturn(recordAutoruNotOk)
      consumerActions.accept(consumerRecord) shouldBe false
    }

    "consume and do request for UpdateJournalRecord with VIN_HISTORY_METADATA diff(sellerType=commercial)" in {
      when(consumerRecord.value).thenReturn(recordAutoruConsumeWithRequest)
      consumerActions.consume(consumerRecord).await
      verify(dao).upsert(any(), any())
      verify(punisherRequestProducer, times(1)).append(any())
    }
    "consume and do request for UpdateJournalRecord with VIN_HISTORY_METADATA diff(sellerType=private)" in {
      when(consumerRecord.value).thenReturn(recordAutoruWithVinReplacement)
      consumerActions.consume(consumerRecord).await
      verify(dao).upsert(any(), any())
      verify(punisherRequestProducer, times(2)).append(any())
    }

    "consume but don't do request for UpdateJournalRecord with MARK diff" in {
      when(consumerRecord.value).thenReturn(recordAutoruConsumeWithoutRequest)
      consumerActions.consume(consumerRecord).await
      verify(dao).upsert(any(), any())
      verifyNoInteractions(punisherRequestProducer)
    }

    "not consume UpdateJournalRecord without any suitable diffs" in {
      when(consumerRecord.value).thenReturn(recordRealtyOk)
      consumerActions.consume(consumerRecord).await
      verifyNoInteractions(dao)
      verifyNoInteractions(punisherRequestProducer)
    }
  }
}
