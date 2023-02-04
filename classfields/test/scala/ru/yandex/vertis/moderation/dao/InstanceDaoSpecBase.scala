package ru.yandex.vertis.moderation.dao

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import cats.effect.IO
import org.scalacheck.Gen
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Minutes, Span}
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.context.Context
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.index.{UpsertIndexRequest, VinEntity}
import ru.yandex.vertis.moderation.model.instance.{EssentialsPatch, ExpiredInstance, ExternalId, Instance}
import ru.yandex.vertis.moderation.model.meta.{Metadata, MetadataSet}
import ru.yandex.vertis.moderation.model.signal.{SignalSet, Tombstone}
import ru.yandex.vertis.moderation.model.{DetailedReason, InstanceId, MaybeExpiredInstance}
import ru.yandex.vertis.moderation.proto.Model.Visibility
import ru.yandex.vertis.moderation.util.DateTimeUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Base specs on [[InstanceDao]]
  *
  * @author semkagtn
  */
trait InstanceDaoSpecBase extends SpecBase {
  import InstanceDaoSpecBase._

  def instanceDao: InstanceDao[Future]

  protected val InstanceGen: Gen[Instance] =
    CoreGenerators.InstanceGen.map(
      _.copy(
        metadata = MetadataSet.Empty
      )
    )

  "upsert" should {
    "correctly insert instance without context, signals, and metadata" in {
      val payload = CoreGenerators.InstancePayloadGen.next

      instanceDao.upsert(payload).futureValue

      val actualResult = instanceDao.getOpt(payload.id, allowExpired = false).futureValue
      val expectedResult = payload.toInstance(SignalSet.Empty, Context.Default, MetadataSet.Empty)

      actualResult shouldBe Some(expectedResult)
    }

    "correctly update instance" in {
      val instance = InstanceGen.next
      val updatedPayload =
        EssentialsPatch(
          instance.id,
          RealtyEssentialsGen.next,
          instance.createTime,
          instance.essentialsUpdateTime,
          Seq.empty
        )
      testUpsert(instance)()

      val previousResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue

      instanceDao.upsert(updatedPayload).futureValue

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue

      previousResult.map(_.signals) shouldBe actualResult.map(_.signals)
      previousResult.map(_.metadata) shouldBe actualResult.map(_.metadata)
      previousResult.map(_.context) shouldBe actualResult.map(_.context)
      previousResult.map(_.createTime) shouldBe actualResult.map(_.createTime)
      previousResult.map(_.essentialsUpdateTime) shouldBe actualResult.map(_.essentialsUpdateTime)
      actualResult shouldBe Some(updatedPayload.toInstance(instance.signals, instance.context, instance.metadata))
    }

    "correctly insert instance with too much precised double" in {
      val precisedPrice = 0.3 - 0.1
      val price = PriceInfoGen.next.copy(value = Some(precisedPrice))
      val realtyEssentials = RealtyEssentialsGen.next.copy(price = Some(price))
      val payload = CoreGenerators.InstancePayloadGen.next.copy(essentials = realtyEssentials)

      instanceDao.upsert(payload).futureValue

      val actualResult = instanceDao.getOpt(payload.id, allowExpired = false).futureValue
      val expectedResult = payload.toInstance(SignalSet.Empty, Context.Default, MetadataSet.Empty)

      actualResult shouldBe Some(expectedResult)
    }

    "correctly insert vin indexes" in {
      val essentials = AutoruEssentialsGen.next.copy(vin = Some("test_vin"))
      val payload = CoreGenerators.InstancePayloadGen.next.copy(essentials = essentials)
      val eId = ExternalId.fromInstanceId(payload.id)
      val withIndex =
        payload.copy(indexUpdates =
          Seq(
            UpsertIndexRequest(VinEntity("test_vin"), eId)
          )
        )

      instanceDao.upsert(withIndex).futureValue

      val indexedIds = instanceDao.index.getIds(VinEntity("test_vin")).futureValue

      indexedIds should contain(eId)
    }
  }

  "upsertWithContext" should {
    "correctly insert instance with context, but signals and metadata should remain empty" in {
      val blockedContext = Context(Visibility.BLOCKED, None, Some(DateTimeUtil.now()))
      val payload = CoreGenerators.InstancePayloadGen.next

      instanceDao.upsertWithContext(payload, blockedContext).futureValue

      val actualResult = instanceDao.getOpt(payload.id, allowExpired = false).futureValue
      val expectedResult = payload.toInstance(SignalSet.Empty, blockedContext, MetadataSet.Empty)

      actualResult shouldBe Some(expectedResult)
    }

    "correctly update instance with context" in {
      val blockedContext = Context(Visibility.BLOCKED, None, Some(DateTimeUtil.now()))
      val payload = CoreGenerators.InstancePayloadGen.next

      val newEssentials = RealtyEssentialsGen.next

      instanceDao.upsertWithContext(payload, blockedContext).futureValue

      val insertResult = instanceDao.getOpt(payload.id, allowExpired = false).futureValue

      instanceDao
        .upsertWithContext(payload.copy(essentials = newEssentials), Context.Default)
        .futureValue

      val actualResult = instanceDao.getOpt(payload.id, allowExpired = false).futureValue

      val expectedResult =
        payload
          .toInstance(SignalSet.Empty, Context.Default, MetadataSet.Empty)
          .copy(essentials = newEssentials)

      insertResult.map(_.context) shouldBe Some(blockedContext)
      actualResult shouldBe Some(expectedResult)
    }
  }

  "updateContext" should {
    "correctly update context" in {
      val context = ContextGen.next
      val instance = InstanceGen.next.copy(context = context)
      testUpsert(instance)()

      val newContext = ContextGen.suchThat(c => c != context).next
      instanceDao.updateContext(instance.id, newContext).futureValue

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue
      val expectedResult = Some(instance.copy(context = newContext))
      actualResult should smartEqual(expectedResult)
    }

    "correctly update with default" in {
      val context = ContextGen.suchThat(c => c != Context.Default).next
      val instance = InstanceGen.next.copy(context = context)
      testUpsert(instance)()

      instanceDao.updateContext(instance.id, Context.Default).futureValue
      val actualResult1 = instanceDao.getOpt(instance.id, allowExpired = false).futureValue
      val expectedResult1 = Some(instance.copy(context = Context.Default))
      actualResult1 should smartEqual(expectedResult1)

      instanceDao.updateContext(instance.id, context).futureValue
      val actualResult2 = instanceDao.getOpt(instance.id, allowExpired = false).futureValue
      val expectedResult2 = Some(instance)
      actualResult2 should smartEqual(expectedResult2)
    }

    "correctly update context with non-ascii tag" in {
      val context = ContextGen.next
      val instance = InstanceGen.next.copy(context = context)
      testUpsert(instance)()

      val newContext =
        ContextGen.suchThat(c => c.visibility != context.visibility).next.copy(tag = Some("Архххх \n\b\t ℌ¼カ"))
      instanceDao.updateContext(instance.id, newContext).futureValue

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue
      val expectedResult = Some(instance.copy(context = newContext))
      actualResult should smartEqual(expectedResult)
    }

    "set context in expired instance" in {
      val instance = CoreGenerators.InstanceGen.next
      instanceDao.updateContext(instance.id, instance.context).futureValue

      val expectedResult =
        ExpiredInstance(
          id = instance.id,
          signals = SignalSet.Empty,
          createTime = None,
          context = Some(instance.context),
          essentialsUpdateTime = None,
          essentials = None,
          metadata = MetadataSet.Empty
        )

      instanceDao.getMaybeExpiredInstance(instance.id).futureValue shouldBe Right(expectedResult)
    }
  }

  "changeSignalsAndSwitchOffs" should {
    "correctly add signals" in {
      val oldSignal = BanSignalGen.next
      val instance = InstanceGen.next.copy(signals = SignalSet(oldSignal))
      val newSignal1 = UnbanSignalGen.next.withSwitchOff(None)
      val newSignal2 = BanSignalGen.next.withSwitchOff(None)
      val signals =
        Map(
          newSignal1.key -> newSignal1,
          newSignal2.key -> newSignal2
        )

      testUpsert(instance)()

      instanceDao
        .changeSignalsAndSwitchOffs(instance.id, signals.mapValues(Right(_)), Map.empty, instance.signals)
        .futureValue

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue
      val expectedResult = instance.copy(signals = SignalSet(oldSignal, newSignal1, newSignal2))

      actualResult shouldBe Some(expectedResult)
    }

    "correctly update signal" in {
      val oldSignal = IndexErrorSignalGen.next.copy(detailedReasons = Set(DetailedReason.Clone))
      val instance = InstanceGen.next.copy(signals = SignalSet(oldSignal))

      testUpsert(instance)()

      val newSignal = oldSignal.copy(detailedReasons = Set(DetailedReason.Clone, DetailedReason.Commercial))
      instanceDao
        .changeSignalsAndSwitchOffs(instance.id, Map(newSignal.key -> Right(newSignal)), Map.empty, instance.signals)
        .futureValue

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue
      val expectedResult = instance.copy(signals = SignalSet(newSignal))

      actualResult shouldBe Some(expectedResult)
    }

    "correctly delete signal" in {
      val updateTime = DateTimeGen.next
      val signal = BanSignalGen.withoutSwitchOff.next.copy(timestamp = updateTime)
      val signalToDelete = UnbanSignalGen.withoutSwitchOff.next.copy(timestamp = updateTime)

      val instance =
        InstanceGen.next.copy(
          signals = SignalSet(signal, signalToDelete),
          essentialsUpdateTime = updateTime
        )

      testUpsert(instance)()

      val newUpdateTime = updateTime.plusDays(1)
      val signalsToDeleteMap = Map(signalToDelete.key -> Tombstone(newUpdateTime, None))

      instanceDao
        .changeSignalsAndSwitchOffs(instance.id, signalsToDeleteMap.mapValues(Left(_)), Map.empty, instance.signals)
        .futureValue

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue
      val expectedResult = instance.copy(signals = SignalSet(signal))
      actualResult shouldBe Some(expectedResult)

      val actualSignalsUpdateTime = actualResult.flatMap(_.signals.updateTime)
      val expectedSignalsUpdateTime = Some(newUpdateTime)
      actualSignalsUpdateTime shouldBe expectedSignalsUpdateTime
    }

    "correctly delete multiple signals" in {
      val updateTime = DateTimeGen.next
      val signal = BanSignalGen.withoutSwitchOff.next.copy(timestamp = updateTime)
      val signalToDelete1 = UnbanSignalGen.withoutSwitchOff.next.copy(timestamp = updateTime)
      val signalToDelete2 = UnbanSignalGen.withoutSwitchOff.next.copy(timestamp = updateTime)

      val instance =
        InstanceGen.next.copy(
          signals = SignalSet(signal, signalToDelete1, signalToDelete2),
          essentialsUpdateTime = updateTime
        )

      testUpsert(instance)()

      val newUpdateTime = updateTime.plusDays(1)
      val signalsToDeleteMap =
        Map(
          signalToDelete1.key -> Tombstone(newUpdateTime, None),
          signalToDelete2.key -> Tombstone(newUpdateTime, None)
        )

      instanceDao
        .changeSignalsAndSwitchOffs(instance.id, signalsToDeleteMap.mapValues(Left(_)), Map.empty, instance.signals)
        .futureValue

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue
      val expectedResult = instance.copy(signals = SignalSet(signal))

      actualResult shouldBe Some(expectedResult)

      val actualSignalsUpdateTime = actualResult.flatMap(_.signals.updateTime)
      val expectedSignalsUpdateTime = Some(newUpdateTime)
      actualSignalsUpdateTime shouldBe expectedSignalsUpdateTime
    }

    "do nothing if signal doesn't exist" in {
      val signal = BanSignalGen.next
      val nonexistentSignal = UnbanSignalGen.next
      val instance = InstanceGen.next.copy(signals = SignalSet(signal))
      testUpsert(instance)()

      val signalsToDeleteMap =
        Map(
          nonexistentSignal.key -> Tombstone(DateTimeGen.next, None)
        )
      instanceDao
        .changeSignalsAndSwitchOffs(instance.id, signalsToDeleteMap.mapValues(Left(_)), Map.empty, instance.signals)
        .futureValue

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue
      val expectedResult = instance.copy(signals = SignalSet(signal))

      actualResult shouldBe Some(expectedResult)
    }

    "correctly adds several switch offs" in {
      val firstSignal = BanSignalGen.withoutSwitchOff.next
      val secondSignal = WarnSignalGen.withoutSwitchOff.next
      val thirdSignal = TagSignalGen.withoutSwitchOff.next
      val signals = SignalSet(firstSignal, secondSignal, thirdSignal)
      val instance = InstanceGen.next.copy(signals = signals)

      testUpsert(instance)()

      val firstSwitchOff = SignalSwitchOffGen.next
      val secondSwitchOff = SignalSwitchOffGen.next
      val switchOffsMap = Map(firstSignal.key -> firstSwitchOff, secondSignal.key -> secondSwitchOff)

      instanceDao
        .changeSignalsAndSwitchOffs(instance.id, Map.empty, switchOffsMap.mapValues(Right(_)), instance.signals)
        .futureValue

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue

      val expectedSignals =
        SignalSet(
          firstSignal.withSwitchOff(Some(firstSwitchOff)),
          secondSignal.withSwitchOff(Some(secondSwitchOff)),
          thirdSignal
        )
      val expectedResult = instance.copy(signals = expectedSignals)

      actualResult shouldBe Some(expectedResult)
    }

    "correctly deletes switch offs" in {
      val updateTime = DateTimeGen.next
      val firstSignal =
        BanSignalGen.next
          .withSwitchOff(Some(SignalSwitchOffGen.next.copy(timestamp = updateTime)))
          .copy(timestamp = updateTime)
      val secondSignal =
        WarnSignalGen.next
          .withSwitchOff(Some(SignalSwitchOffGen.next.copy(timestamp = updateTime)))
          .copy(timestamp = updateTime)
      val thirdSignal =
        TagSignalGen.next
          .withSwitchOff(None)
          .copy(timestamp = updateTime)
      val signals = SignalSet(firstSignal, secondSignal, thirdSignal)

      val instance =
        InstanceGen.next.copy(
          signals = signals,
          essentialsUpdateTime = updateTime
        )

      testUpsert(instance)()

      val newUpdateTime = updateTime.plusDays(1)
      val signalsToDeleteMap =
        Map(
          firstSignal.key -> Tombstone(newUpdateTime, None),
          thirdSignal.key -> Tombstone(newUpdateTime, None)
        )

      instanceDao
        .changeSignalsAndSwitchOffs(instance.id, Map.empty, signalsToDeleteMap.mapValues(Left(_)), instance.signals)
        .futureValue

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue
      val expectedSignals =
        SignalSet(
          firstSignal.withSwitchOff(None),
          secondSignal,
          thirdSignal
        )
      val expectedResult = instance.copy(signals = expectedSignals)

      actualResult shouldBe Some(expectedResult)

      val actualSignalsUpdateTime = actualResult.flatMap(_.signals.updateTime)
      val expectedSignalsUpdateTime = Some(newUpdateTime)
      actualSignalsUpdateTime shouldBe expectedSignalsUpdateTime
    }

    "do nothing" in {
      val instance = InstanceGen.next
      testUpsert(instance)()
      instanceDao.changeSignalsAndSwitchOffs(instance.id, Map.empty, Map.empty, instance.signals).futureValue

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).futureValue
      val expectedResult = Some(instance)

      actualResult shouldBe expectedResult
    }

    "change signals in expired instance" in {
      val instance = CoreGenerators.InstanceGen.next
      instanceDao
        .changeSignalsAndSwitchOffs(
          instance.id,
          instance.signals.signalMap,
          instance.signals.switchOffMap,
          SignalSet.Empty
        )
        .futureValue

      val expectedResult =
        ExpiredInstance(
          id = instance.id,
          signals = instance.signals,
          createTime = None,
          context = None,
          essentialsUpdateTime = None,
          essentials = None,
          metadata = MetadataSet.Empty
        )
      instanceDao.getMaybeExpiredInstance(instance.id).futureValue shouldBe Right(expectedResult)
    }
  }

  "get by instance id" should {

    "return existent instances" in {
      val instance = InstanceGen.next
      testUpsert(instance)()

      val actualInstance = instanceDao.getOpt(instance.id, allowExpired = false).futureValue

      actualInstance shouldBe Some(instance)
    }

    "not return nonexistent instance" in {
      val nonexistentId = InstanceIdGen.next
      val none = instanceDao.getOpt(nonexistentId, allowExpired = false).futureValue

      none shouldBe None
    }
  }

  "get by external id" should {

    "return existent instance" in {
      val externalId = ExternalIdGen.next
      val instance = instanceGen(externalId).next

      testUpsert(instance)()

      val actualInstance = instanceDao.getOpt(externalId, allowExpired = false).futureValue

      actualInstance shouldBe Some(instance)
    }

    "not return nonexistent instance" in {
      val nonexistentExternalId = ExternalIdGen.next

      val none = instanceDao.getOpt(nonexistentExternalId, allowExpired = false).futureValue
      none should be(None)
    }
  }

  "getMaybeExpiredInstance" should {
    "consider instance without essentials as expired" in {
      val instance = CoreGenerators.InstanceGen.next
      instanceDao.updateContext(instance.id, instance.context).futureValue
      instanceDao
        .changeSignalsAndSwitchOffs(
          instance.id,
          instance.signals.signalMap,
          instance.signals.switchOffMap,
          SignalSet.Empty
        )
        .futureValue
      recursiveMetadataUpsert(instance.id, instance.metadata.toList).futureValue

      val expectedResult =
        ExpiredInstance(
          id = instance.id,
          signals = instance.signals,
          createTime = None,
          context = Some(instance.context),
          essentialsUpdateTime = None,
          essentials = None,
          metadata = instance.metadata
        )
      instanceDao.getMaybeExpiredInstance(instance.id).futureValue shouldBe Right(expectedResult)
    }

    "not be expired having only essentials and essentialsUpdateTime" in {
      val instance = CoreGenerators.InstanceGen.next
      instanceDao.upsert(EssentialsPatch.fromInstance(instance)).futureValue
      instanceDao.getMaybeExpiredInstance(instance.id).futureValue.isRight shouldBe false
    }
  }

  "getAll" should {
    "return all current instances" in {
      val instanceOne = InstanceGen.next
      val instanceTwo = InstanceGen.next

      testUpsert(instanceOne)()
      testUpsert(instanceTwo)()

      val actualResult =
        instanceDao
          .getAll()
          .compile
          .fold(Set.empty[Instance])(_ + _)
          .unsafeToFuture()
          .futureValue

      actualResult.map(_.id) shouldBe Set(instanceOne, instanceTwo).map(_.id)
      actualResult should contain theSameElementsAs Set(instanceOne, instanceTwo)
    }

    "get multipage all instances" in {
      val current =
        instanceDao
          .getAll()
          .compile
          .toList
          .unsafeToFuture()
          .futureValue(Timeout(Span(5, Minutes)))

      val instances = testBatchUpsert(1100)

      val actualResult =
        instanceDao
          .getAll()
          .compile
          .toList
          .unsafeToFuture()
          .futureValue(Timeout(Span(5, Minutes)))

      val expected = current ++ instances

      actualResult.size shouldBe expected.size
      actualResult.map(_.id).sorted shouldBe expected.map(_.id).sorted
      actualResult should contain theSameElementsAs expected
    }

    "get multi page instances batch" in {
      val instances = testBatchUpsert(1100)

      val nonExistingIds = Gen.listOfN(1, ExternalIdGen).next.filterNot(id => instances.exists(_.externalId == id))

      val actualResult =
        instanceDao
          .streamMaybeExpiredInstances(instances.map(_.externalId).toList ++ nonExistingIds)
          .flatMapConcat(Source.apply)
          .runWith(Sink.seq[MaybeExpiredInstance])(Materializer.matFromSystem(actorSystem))
          .futureValue
          .flatMap(_.left.toOption)

      actualResult.size shouldBe instances.size
      actualResult.map(_.id).sorted shouldBe instances.map(_.id).toSeq.sorted
      actualResult should contain theSameElementsAs instances
    }
  }

  "upsertMetadata" should {

    "correctly add new metadata" in {
      val instance = InstanceGen.next
      testUpsert(instance)()

      val metadataSet = MetadataSetGen.next
      recursiveMetadataUpsert(instance.id, metadataSet.toList).futureValue

      val actualMeta = instanceDao.getOpt(instance.id, allowExpired = false).futureValue.get.metadata
      actualMeta shouldBe metadataSet
    }

    "correctly update metadata with specified type" in {
      val instance = InstanceGen.next
      testUpsert(instance)()

      val metadata = AutoruPhotoLicensePlateMetadataGen.next
      instanceDao.upsertMetadata(instance.id, metadata, instance.metadata).futureValue

      val newInstance = instanceDao.getOpt(instance.id, allowExpired = false).futureValue.get
      val newMetadata = AutoruPhotoLicensePlateMetadataGen.next
      instanceDao.upsertMetadata(instance.id, newMetadata, newInstance.metadata).futureValue

      val actualMeta = instanceDao.getOpt(instance.id, allowExpired = false).futureValue.get.metadata
      actualMeta shouldBe MetadataSet(newMetadata)
    }

    "do nothing if instance doesn't exist" in {
      val metadata = MetadataGen.next
      val nonexistentInstanceId = InstanceIdGen.next
      instanceDao.upsertMetadata(nonexistentInstanceId, metadata, MetadataSet.Empty).futureValue
      val instance = instanceDao.getOpt(nonexistentInstanceId, allowExpired = false).futureValue
      instance shouldBe None
    }

    "upsert metadata to expired instance" in {
      val instance = CoreGenerators.InstanceGen.next
      recursiveMetadataUpsert(instance.id, instance.metadata.toList).futureValue

      val expectedResult =
        ExpiredInstance(
          id = instance.id,
          signals = SignalSet.Empty,
          createTime = None,
          context = None,
          essentialsUpdateTime = None,
          essentials = None,
          metadata = instance.metadata
        )
      instanceDao.getMaybeExpiredInstance(instance.id).futureValue shouldBe Right(expectedResult)
    }
  }

  "deleteMetadata" should {

    "correctly delete metadata with one entry in metadata set" in {
      val metadata = OffersStatisticsMetadataGen.next
      val instance = InstanceGen.next.copy(metadata = MetadataSet(metadata))
      testUpsert(instance)()
      instanceDao.deleteMetadata(instance.id, metadata.`type`, instance.metadata).futureValue

      val actualResult = instanceDao.getOpt(instance.externalId, allowExpired = false).futureValue
      val expectedResult = Some(instance.copy(metadata = MetadataSet.Empty))
      actualResult shouldBe expectedResult
    }

    "correctly delete metadata with many entries in metadata set" in {
      val metadata = OffersStatisticsMetadataGen.next
      val metadata1 = SignalsStatisticsMetadataGen.next
      val instance = InstanceGen.next.copy(metadata = MetadataSet(metadata, metadata1))
      testUpsert(instance)()
      instanceDao.deleteMetadata(instance.id, metadata.`type`, instance.metadata).futureValue

      val actualResult = instanceDao.getOpt(instance.externalId, allowExpired = false).futureValue
      val expectedResult = Some(instance.copy(metadata = MetadataSet(metadata1)))
      actualResult shouldBe expectedResult
    }

    "correctly delete non-existent metadata" in {
      val metadata = OffersStatisticsMetadataGen.next
      val instance = InstanceGen.next.copy(metadata = MetadataSet.Empty)
      testUpsert(instance)()
      instanceDao.deleteMetadata(instance.id, metadata.`type`, instance.metadata).futureValue

      val actualResult = instanceDao.getOpt(instance.externalId, allowExpired = false).futureValue
      val expectedResult = Some(instance)
      actualResult shouldBe expectedResult
    }

    "delete metadata in expired instance" in {
      val instance = CoreGenerators.InstanceGen.next
      recursiveMetadataUpsert(instance.id, instance.metadata.toList).futureValue

      instanceDao.deleteMetadata(instance.id, instance.metadata.head.`type`, instance.metadata).futureValue

      val expectedResult =
        ExpiredInstance(
          id = instance.id,
          signals = SignalSet.Empty,
          createTime = None,
          context = None,
          essentialsUpdateTime = None,
          essentials = None,
          metadata = MetadataSet(instance.metadata.tail)
        )
      instanceDao.getMaybeExpiredInstance(instance.id).futureValue shouldBe Right(expectedResult)
    }
  }

  "getObjectIds" should {

    "return current instance ids" in {
      val user = UserGen.next
      val objectId1 = ObjectIdGen.next
      val objectId2 = ObjectIdGen.suchThat(_ != objectId1).next

      val externalId1 = ExternalId(user, objectId1)
      val externalId2 = ExternalId(user, objectId2)

      val instances1 = instanceGen(externalId1).next(2).toList
      val instance2 = instanceGen(externalId2).next
      val instances = instances1 :+ instance2
      for (instance <- instances) testUpsert(instance)()

      val actualResult = instanceDao.getObjectIds(user).futureValue
      val expectedResult = Set(objectId1, objectId2)

      actualResult shouldBe expectedResult
    }
  }

  protected def recursiveMetadataUpsert(instanceId: InstanceId, l: List[Metadata]): Future[Unit] =
    if (l.isEmpty)
      Future.unit
    else
      instanceDao
        .getMaybeExpiredInstance(instanceId)
        .flatMap(i => instanceDao.upsertMetadata(instanceId, l.head, i.fold(_.metadata, _.metadata)))
        .flatMap(_ => recursiveMetadataUpsert(instanceId, l.tail))

  final protected def testUpsert(newInstance: Instance,
                                 instanceDao: InstanceDao[Future] = InstanceDaoSpecBase.this.instanceDao
                                )(oldSignals: SignalSet = SignalSet.Empty): Instance =
    testUpsertF(newInstance, instanceDao)(oldSignals).futureValue

  protected def testUpsertF(newInstance: Instance,
                            instanceDao: InstanceDao[Future] = InstanceDaoSpecBase.this.instanceDao
                           )(oldSignals: SignalSet = SignalSet.Empty): Future[Instance] = {
    for {
      _ <- instanceDao.upsert(EssentialsPatch.fromInstance(newInstance))
      _ <- instanceDao.updateContext(newInstance.id, newInstance.context)
      _ <-
        instanceDao.changeSignalsAndSwitchOffs(
          newInstance.id,
          newInstance.signals.signalMap,
          newInstance.signals.switchOffMap,
          oldSignals
        )
      _ <- recursiveMetadataUpsert(newInstance.id, newInstance.metadata.toList)
    } yield newInstance
  }

  protected def testBatchUpsert(count: Int): Set[Instance] = {
    val instances =
      1.to(count)
        .map(_ => InstanceGen.next)
        .groupBy(_.externalId)
        .values
        .map(_.last)
        .toList

    Source(instances)
      .mapAsync(40) { instance =>
        testUpsertF(instance)()
      }
      .runWith(Sink.seq[Instance])(Materializer.matFromSystem(actorSystem))
      .futureValue(Timeout(Span(5, Minutes)))
      .toSet
  }
}

object InstanceDaoSpecBase {
  implicit protected class RichInstancePayload(val payload: EssentialsPatch) extends AnyVal {
    def toInstance(signals: SignalSet, context: Context, metadata: MetadataSet): Instance =
      Instance(
        id = payload.id,
        essentials = payload.essentials,
        signals = signals,
        createTime = payload.createTime,
        essentialsUpdateTime = payload.essentialsUpdateTime,
        context = context,
        metadata = metadata
      )
  }
}
