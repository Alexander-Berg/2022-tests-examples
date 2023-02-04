package ru.yandex.vertis.moderation.proven.owner.post

import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.extdata.core.gens.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.impl.inmemory.{InMemoryInstanceDao, InMemoryStorageImpl}
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, ModerationRequest}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  instanceGen,
  AutoruEssentialsGen,
  ContextGen,
  ProvenOwnerMetadataGen,
  UpdateJournalRecordGen
}
import ru.yandex.vertis.moderation.model.instance.EssentialsPatch
import ru.yandex.vertis.moderation.model.meta.MetadataSet
import ru.yandex.vertis.moderation.model.signal.{AutomaticSource, SignalInfoSet, WarnSignalSource}
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.{BodyType, EngineType, GearType, SteeringWheel}
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Metadata.ProvenOwnerMetadata
import ru.yandex.vertis.moderation.proto.Model.{Service, Visibility}

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class PostProvenOwnerDeciderSpec extends SpecBase {

  implicit private val materializer: Materializer =
    ActorMaterializer(
      ActorMaterializerSettings(actorSystem)
        .withSupervisionStrategy(_ => Supervision.Resume)
    )(actorSystem)

  private val service = Service.AUTORU
  private val inMemoryStorageImpl = new InMemoryStorageImpl
  private val instanceDao = new InMemoryInstanceDao(service, inMemoryStorageImpl)
  private val decider = new PostProvenOwnerDeciderImpl(service, instanceDao)

  "PostProvenOwnerDecider" should {
    val vin = "VIN"
    val mark = "mark"
    val year = 1990
    val colorName = "Золотистый"
    val model = "model"
    val bodyType = BodyType.PICKUP
    val superGen = "generation"
    val engineType = EngineType.DIESEL
    val engineVolume = 11795
    val horsePower = 400
    val steeringWheel = SteeringWheel.LEFT
    val gearType = GearType.ALL

    "decide to append signal if current instance differs from reference with proven owner ok" in {
      val referenceEssentials =
        AutoruEssentialsGen.next.copy(
          vin = Some(vin),
          mark = Some(mark),
          year = Some(year),
          colorName = Some(colorName),
          model = Some(model),
          bodyType = Some(bodyType),
          superGen = Some(superGen),
          engineType = Some(engineType),
          engineVolume = Some(engineVolume),
          horsePower = Some(horsePower),
          steeringWheel = Some(steeringWheel),
          gearType = Some(gearType)
        )
      val referenceProvenOwnerMetadata =
        ProvenOwnerMetadataGen.next.copy(
          vin = Some(vin),
          verdict = ProvenOwnerMetadata.Verdict.PROVEN_OWNER_OK
        )
      val referenceInstance =
        instanceGen(Service.AUTORU).next.copy(
          essentials = referenceEssentials,
          context = ContextGen.next.copy(visibility = Visibility.INACTIVE),
          metadata = MetadataSet(referenceProvenOwnerMetadata)
        )
      val currentEssentials =
        AutoruEssentialsGen.next.copy(
          vin = Some(vin),
          mark = Some(mark),
          year = Some(year),
          colorName = Some(colorName),
          model = Some(model),
          bodyType = Some(bodyType),
          superGen = Some(superGen),
          engineType = Some(EngineType.GASOLINE),
          engineVolume = Some(engineVolume),
          horsePower = Some(horsePower),
          steeringWheel = Some(steeringWheel),
          gearType = Some(gearType)
        )
      val currentInstance =
        instanceGen(Service.AUTORU).next.copy(
          essentials = currentEssentials,
          context = ContextGen.next.copy(visibility = Visibility.VISIBLE),
          metadata = MetadataSet.Empty
        )
      val record =
        UpdateJournalRecordGen.next.copy(
          instance = currentInstance,
          prev = None
        )

      instanceDao
        .upsertWithContext(EssentialsPatch.fromInstance(referenceInstance), referenceInstance.context)
        .futureValue
      instanceDao.upsertMetadata(referenceInstance.id, referenceProvenOwnerMetadata, MetadataSet.Empty).futureValue

      val expectedSignalSource =
        WarnSignalSource(
          domain = Domain.default(service),
          source = AutomaticSource(Application.MODERATION, tag = Some("tag")),
          info = Some(s"Reference: ${referenceInstance.externalId}, diffs: ENGINE_TYPE"),
          detailedReason = DetailedReason.PostProvenOwnerMismatch,
          weight = 1.0,
          ttl = None,
          timestamp = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val expected =
        ModerationRequest.AppendSignals(
          record.instance.externalId,
          signalSources = Seq(expectedSignalSource),
          timestamp = record.timestamp,
          depth = record.depth
        )

      val result = decider.decide(record, "tag").futureValue
      result shouldBe Some(expected)
    }

    "return None if current instance is the same as reference with proven owner ok" in {
      val referenceEssentials =
        AutoruEssentialsGen.next.copy(
          vin = Some(vin),
          mark = Some(mark),
          year = Some(year),
          colorName = Some(colorName),
          model = Some(model),
          bodyType = Some(bodyType),
          superGen = Some(superGen),
          engineType = Some(engineType),
          engineVolume = Some(engineVolume),
          horsePower = Some(horsePower),
          steeringWheel = Some(steeringWheel),
          gearType = Some(gearType)
        )
      val referenceProvenOwnerMetadata =
        ProvenOwnerMetadataGen.next.copy(
          vin = Some(vin),
          verdict = ProvenOwnerMetadata.Verdict.PROVEN_OWNER_OK
        )
      val referenceInstance =
        instanceGen(Service.AUTORU).next.copy(
          essentials = referenceEssentials,
          context = ContextGen.next.copy(visibility = Visibility.INACTIVE),
          metadata = MetadataSet(referenceProvenOwnerMetadata)
        )
      val currentEssentials = referenceEssentials
      val currentInstance =
        instanceGen(Service.AUTORU).next.copy(
          essentials = currentEssentials,
          context = ContextGen.next.copy(visibility = Visibility.VISIBLE),
          metadata = MetadataSet.Empty
        )
      val record =
        UpdateJournalRecordGen.next.copy(
          instance = currentInstance,
          prev = None
        )

      instanceDao
        .upsertWithContext(EssentialsPatch.fromInstance(referenceInstance), referenceInstance.context)
        .futureValue
      instanceDao.upsertMetadata(referenceInstance.id, referenceProvenOwnerMetadata, MetadataSet.Empty).futureValue

      val result = decider.decide(record, "tag").futureValue
      result shouldBe None
    }
  }

  "calculateNoticeableChanges" should {
    val vin = "VIN"
    val mark = "mark"
    val year = 1990
    val colorName = "Золотистый"
    val model = "model"
    val bodyType = BodyType.PICKUP
    val superGen = "generation"
    val engineType = EngineType.DIESEL
    val engineVolume = 11795
    val horsePower = 400
    val steeringWheel = SteeringWheel.LEFT
    val gearType = GearType.ALL

    "return empty set in case of same important essentials fields" in {
      val referenceEssentials =
        AutoruEssentialsGen.next.copy(
          vin = Some(vin),
          mark = Some(mark),
          year = Some(year),
          colorName = Some(colorName),
          model = Some(model),
          bodyType = Some(bodyType),
          superGen = Some(superGen),
          engineType = Some(engineType),
          engineVolume = Some(engineVolume),
          horsePower = Some(horsePower),
          steeringWheel = Some(steeringWheel),
          gearType = Some(gearType)
        )
      val referenceInstance =
        instanceGen(Service.AUTORU).next.copy(
          essentials = referenceEssentials,
          context = ContextGen.next.copy(visibility = Visibility.INACTIVE),
          metadata = MetadataSet.Empty
        )
      val currentEssentials =
        referenceEssentials.copy(
          description = Some("another description"),
          colorHex = Some("dd33ff")
        )
      val currentInstance =
        instanceGen(Service.AUTORU).next.copy(
          essentials = currentEssentials,
          context = ContextGen.next.copy(visibility = Visibility.VISIBLE),
          metadata = MetadataSet.Empty
        )

      val result = PostProvenOwnerDeciderImpl.calculateNoticeableChanges(currentInstance, referenceInstance, service)

      result shouldBe Set.empty[Model.Diff.Autoru.Value]
    }

    "return diffs if important fields were changed" in {
      val referenceEssentials =
        AutoruEssentialsGen.next.copy(
          vin = Some(vin),
          mark = Some(mark),
          year = Some(year),
          colorName = Some(colorName),
          model = Some(model),
          bodyType = Some(bodyType),
          superGen = Some(superGen),
          engineType = Some(engineType),
          engineVolume = Some(engineVolume),
          horsePower = Some(horsePower),
          steeringWheel = Some(steeringWheel),
          gearType = Some(gearType)
        )
      val referenceInstance =
        instanceGen(Service.AUTORU).next.copy(
          essentials = referenceEssentials,
          context = ContextGen.next.copy(visibility = Visibility.INACTIVE),
          metadata = MetadataSet.Empty
        )
      val currentEssentials =
        referenceEssentials.copy(
          vin = Some(vin),
          mark = Some("another mark"),
          year = Some(1),
          colorName = Some(colorName),
          model = Some(model),
          bodyType = Some(bodyType),
          superGen = Some(superGen),
          engineType = Some(engineType),
          engineVolume = Some(engineVolume),
          horsePower = Some(horsePower),
          steeringWheel = Some(steeringWheel),
          gearType = Some(gearType)
        )
      val currentInstance =
        instanceGen(Service.AUTORU).next.copy(
          essentials = currentEssentials,
          context = ContextGen.next.copy(visibility = Visibility.VISIBLE),
          metadata = MetadataSet.Empty
        )

      val result = PostProvenOwnerDeciderImpl.calculateNoticeableChanges(currentInstance, referenceInstance, service)
      val expected =
        Set(
          Model.Diff.Autoru.Value.YEAR,
          Model.Diff.Autoru.Value.MARK
        )

      result shouldBe expected
    }

    "return empty set if horsePower, engineVolume and colorName fields has minor changes" in {
      val referenceEssentials =
        AutoruEssentialsGen.next.copy(
          vin = Some(vin),
          mark = Some(mark),
          year = Some(year),
          colorName = Some("Желтый"),
          model = Some(model),
          bodyType = Some(bodyType),
          superGen = Some(superGen),
          engineType = Some(engineType),
          engineVolume = Some(999),
          horsePower = Some(149),
          steeringWheel = Some(steeringWheel),
          gearType = Some(gearType)
        )
      val referenceInstance =
        instanceGen(Service.AUTORU).next.copy(
          essentials = referenceEssentials,
          context = ContextGen.next.copy(visibility = Visibility.INACTIVE),
          metadata = MetadataSet.Empty
        )
      val currentEssentials =
        referenceEssentials.copy(
          vin = Some(vin),
          mark = Some(mark),
          year = Some(year),
          colorName = Some("Золотистый"),
          model = Some(model),
          bodyType = Some(bodyType),
          superGen = Some(superGen),
          engineType = Some(engineType),
          engineVolume = Some(1000),
          horsePower = Some(150),
          steeringWheel = Some(steeringWheel),
          gearType = Some(gearType)
        )
      val currentInstance =
        instanceGen(Service.AUTORU).next.copy(
          essentials = currentEssentials,
          context = ContextGen.next.copy(visibility = Visibility.VISIBLE),
          metadata = MetadataSet.Empty
        )

      val result = PostProvenOwnerDeciderImpl.calculateNoticeableChanges(currentInstance, referenceInstance, service)

      result shouldBe Set.empty[Model.Diff.Autoru.Value]
    }
  }

  "couldBeConfused" should {
    "return true if color could be confused with reference" in {
      PostProvenOwnerDeciderImpl.couldBeConfused("Бежевый", "Белый") shouldBe true
    }

    "return true if reference could be confused with color" in {
      PostProvenOwnerDeciderImpl.couldBeConfused("Серый", "Серебристый") shouldBe true
    }

    "return true if color could not be confused with reference" in {
      PostProvenOwnerDeciderImpl.couldBeConfused("Красный", "Розовый") shouldBe false
    }

    "return true if reference could not be confused with color" in {
      PostProvenOwnerDeciderImpl.couldBeConfused("Коричневый", "Черный") shouldBe false
    }
  }
}
