package ru.yandex.vertis.dao

import cats.effect.{ContextShift, IO}
import cats.implicits.toTraverseOps
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.dao.CoreGenerators.{BanSignalGen, ContextGen, DateTimeGen, ExternalIdGen, GeneralEssentialsGen, IndexErrorSignalGen, InstanceIdGen, MetadataGen, MetadataSetGen, ObjectIdGen, OffersStatisticsMetadataGen, RichSignalGen, ServiceGen, SignalSwitchOffGen, TagSignalGen, UnbanSignalGen, UserGen, WarnSignalGen, instanceGen, instanceIdGen}
import ru.yandex.vertis.generators.NetGenerators.asProducer
import ru.yandex.vertis.model.instance.ExternalId
import ru.yandex.vertis.model.{Context, DetailedReason, Instance}
import ru.yandex.vertis.model.meta.MetadataSet
import ru.yandex.vertis.model.signal.{SignalSet, Tombstone}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.test_utils.ydb.YdbWrapperContainer
import ru.yandex.vertis.quality.ydb_utils.DefaultYdbWrapper
import ru.yandex.vertis.scalatest.matcher.SmartEqualMatcher

import scala.concurrent.ExecutionContext
import scala.io.{Codec, Source}
import scala.util.Try

trait InstanceDaoSpecBase extends AnyWordSpec with Matchers with ForAllTestContainer with BeforeAndAfter {
  type F[X] = IO[X]
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  def instanceDao: InstanceDao[IO]
  def schemaFileName: String
  override lazy val container: YdbWrapperContainer[F] = YdbWrapperContainer.stable

  def smartEqual[A](right: A): SmartEqualMatcher[A] = SmartEqualMatcher.smartEqual(right)

  lazy val ydbWrapper: DefaultYdbWrapper[F] = {
    val database = container.ydb
    val stream = getClass.getResourceAsStream(schemaFileName)
    val schema = Source.fromInputStream(stream)(Codec.UTF8).mkString
    database.executeSchema(schema).await
    database
  }

  before {
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM instances;")).await)
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM instances_main;")).await)
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM instances_kv;")).await)
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM instances_signals;")).await)
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM instances_signal_switch_offs;")).await)
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM instances_metadata;")).await)
  }


  protected val InstanceGen: Gen[Instance] =
    CoreGenerators.InstanceGen.map(
      _.copy(
        metadata = MetadataSet.Empty
      )
    )

  "upsert" should {

    "correctly insert instance" in {
      val instance = InstanceGen.next
      instanceDao.upsert(instance).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult = Some(instance)

      actualResult should smartEqual(expectedResult)
    }

    "correctly insert instance without signals" in {
      val instance = InstanceGen.next.copy(signals = SignalSet.Empty)
      instanceDao.upsert(instance).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult = Some(instance)

      actualResult should be(expectedResult)
    }

    "correctly update instance" in {
      val instance = InstanceGen.next
      val updatedInstance = instance.copy(essentials = GeneralEssentialsGen.next)
      instanceDao.upsert(instance).await
      instanceDao.upsert(updatedInstance).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult = Some(updatedInstance)

      actualResult should be(expectedResult)
    }

    "correctly add signals" in {
      val oldSignal = BanSignalGen.next
      val instance =
        InstanceGen.next.copy(
          signals = SignalSet(oldSignal)
        )
      val newSignal = UnbanSignalGen.next
      val updatedInstance =
        instance.copy(
          signals = SignalSet(newSignal)
        )
      instanceDao.upsert(instance).await
      instanceDao.upsert(updatedInstance).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult =
        Some(
          instance.copy(
            signals = SignalSet(oldSignal, newSignal)
          )
        )

      actualResult should be(expectedResult)
    }

    "correctly update signal" in {
      val oldSignal = IndexErrorSignalGen.next.copy(detailedReasons = Set.empty)
      val newSignal = oldSignal.copy(detailedReasons = Set(DetailedReason.AreaError))
      val instance =
        InstanceGen.next.copy(
          signals = SignalSet(oldSignal)
        )
      val updatedInstance =
        instance.copy(
          signals = SignalSet(newSignal)
        )
      instanceDao.upsert(instance).await
      instanceDao.upsert(updatedInstance).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult = Some(updatedInstance)

      actualResult should be(expectedResult)
    }

    "correctly update context" in {
      val oldContext = ContextGen.next
      val newContext = ContextGen.suchThat(c => c != oldContext).next
      val instance = InstanceGen.next.copy(context = oldContext)
      val updatedInstance = instance.copy(context = newContext)
      instanceDao.upsert(instance).await
      instanceDao.upsert(updatedInstance).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult = Some(updatedInstance)

      actualResult should be(expectedResult)
    }
  }

  "appendSignals" should {

    "correctly add signals" in {
      val oldSignal = BanSignalGen.next
      val instance =
        InstanceGen.next.copy(
          signals = SignalSet(oldSignal)
        )
      val newSignal1 = UnbanSignalGen.next
      val newSignal2 = BanSignalGen.next
      testUpsert(instance)

      instanceDao.appendSignals(instance.id, SignalSet(newSignal1, newSignal2), null).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult =
        Some(
          instance.copy(
            signals = SignalSet(oldSignal, newSignal1, newSignal2)
          )
        )

      actualResult should be(expectedResult)
    }

    "correctly add 0 signals" in {
      val instance = InstanceGen.next
      testUpsert(instance)

      instanceDao.appendSignals(instance.id, SignalSet.Empty, null).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult = Some(instance)

      actualResult should be(expectedResult)
    }

    "correctly update signal" in {
      val oldSignal = IndexErrorSignalGen.next.copy(detailedReasons = Set(DetailedReason.Clone))
      val instance =
        InstanceGen.next.copy(
          signals = SignalSet(oldSignal)
        )
      val newSignal = oldSignal.copy(detailedReasons = Set(DetailedReason.Clone, DetailedReason.Commercial))
      testUpsert(instance)

      instanceDao.appendSignals(instance.id, SignalSet(newSignal), null).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult =
        Some(
          instance.copy(
            signals = SignalSet(newSignal)
          )
        )

      actualResult should be(expectedResult)
    }
  }

  "deleteSignals" should {

    "do nothing if no signals specified for deletion" in {
      val updateTime = DateTimeGen.next
      val signal = BanSignalGen.withoutSwitchOff.next.copy(timestamp = updateTime)
      val instance =
        InstanceGen.next.copy(
          signals = SignalSet(signal),
          essentialsUpdateTime = updateTime
        )
      testUpsert(instance)

      instanceDao.deleteSignals(instance.id, Map.empty, null).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult =
        Some(
          instance.copy(
            signals = SignalSet(signal)
          )
        )

      actualResult should smartEqual(expectedResult)
    }

    "correctly delete signal" in {
      val updateTime = DateTimeGen.next
      val newUpdateTime = updateTime.plusDays(1)
      val signal = BanSignalGen.withoutSwitchOff.next.copy(timestamp = updateTime)
      val signalToDelete = UnbanSignalGen.withoutSwitchOff.next.copy(timestamp = updateTime)
      val signalsToDeleteMap =
        Map(
          signalToDelete.key -> Tombstone(newUpdateTime, None)
        )
      val instance =
        InstanceGen.next.copy(
          signals = SignalSet(signal, signalToDelete),
          essentialsUpdateTime = updateTime
        )
      testUpsert(instance)

      instanceDao.deleteSignals(instance.id, signalsToDeleteMap, null).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult =
        Some(
          instance.copy(
            signals = SignalSet(signal)
          )
        )
      actualResult should smartEqual(expectedResult)

      val actualSignalsUpdateTime = actualResult.flatMap(_.signals.updateTime)
      val expectedSignalsUpdateTime = Some(newUpdateTime)
      actualSignalsUpdateTime shouldBe expectedSignalsUpdateTime
    }

    "correctly delete multiple signals" in {
      val updateTime = DateTimeGen.next
      val newUpdateTime = updateTime.plusDays(1)
      val signal = BanSignalGen.withoutSwitchOff.next.copy(timestamp = updateTime)
      val signalToDelete1 = UnbanSignalGen.withoutSwitchOff.next.copy(timestamp = updateTime)
      val signalToDelete2 = UnbanSignalGen.withoutSwitchOff.next.copy(timestamp = updateTime)
      val signalsToDeleteMap =
        Map(
          signalToDelete1.key -> Tombstone(newUpdateTime, None),
          signalToDelete2.key -> Tombstone(newUpdateTime, None)
        )
      val instance =
        InstanceGen.next.copy(
          signals = SignalSet(signal, signalToDelete1, signalToDelete2),
          essentialsUpdateTime = updateTime
        )
      testUpsert(instance)

      instanceDao.deleteSignals(instance.id, signalsToDeleteMap, null).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult =
        Some(
          instance.copy(
            signals = SignalSet(signal)
          )
        )
      actualResult should smartEqual(expectedResult)

      val actualSignalsUpdateTime = actualResult.flatMap(_.signals.updateTime)
      val expectedSignalsUpdateTime = Some(newUpdateTime)
      actualSignalsUpdateTime shouldBe expectedSignalsUpdateTime
    }

    "do nothing if signal doesn't exist" in {
      val signal = BanSignalGen.next
      val nonexistentSignal = UnbanSignalGen.next
      val instance = InstanceGen.next.copy(signals = SignalSet(signal))
      testUpsert(instance)

      val signalsToDeleteMap =
        Map(
          nonexistentSignal.key -> Tombstone(DateTimeGen.next, None)
        )
      instanceDao.deleteSignals(instance.id, signalsToDeleteMap, null).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult =
        Some(
          instance.copy(
            signals = SignalSet(signal)
          )
        )

      actualResult should smartEqual(expectedResult)
    }
  }

  "get by instance id" should { // TODO remove

    "return existent instances" in {
      val instance = InstanceGen.next
      testUpsert(instance)

      val actualInstance = instanceDao.getOpt(instance.id, allowExpired = false).await

      actualInstance should be(Some(instance))
    }

    "not return nonexistent instance" in {
      val nonexistentId = InstanceIdGen.next
      val none = instanceDao.getOpt(nonexistentId, allowExpired = false).await

      none should be(None)
    }
  }

  "get by external id" should { // TODO remove

    "return existent instance" in {
      val externalId = ExternalIdGen.next
      val oldInstance = instanceGen(externalId).next
      val newInstance =
        instanceGen(externalId).next.copy(
          id = oldInstance.id,
          essentialsUpdateTime = oldInstance.essentialsUpdateTime.plusDays(1)
        )
      testUpsert(oldInstance)
      testUpsert(newInstance)

      val actualInstance = instanceDao.getOpt(externalId, allowExpired = false).await
      actualInstance should smartEqual(
        Some(
          newInstance.copy(
            signals = oldInstance.signals ++ newInstance.signals,
            metadata = MetadataSet.Empty
          )
        )
      )
    }

    "not return nonexistent instance" in {
      val nonexistentExternalId = ExternalIdGen.next

      val none = instanceDao.getOpt(nonexistentExternalId, allowExpired = false).await
      none should be(None)
    }
  }

  "updateContext" should {

    "correctly update context" in {
      val context = ContextGen.next
      val instance = InstanceGen.next.copy(context = context)
      testUpsert(instance)

      val newContext = ContextGen.suchThat(c => c != context).next
      instanceDao.updateContext(instance.id, newContext).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult = Some(instance.copy(context = newContext))
      actualResult should smartEqual(expectedResult)
    }

    "correctly update with default" in {
      val context = ContextGen.suchThat(c => c != Context.Default).next
      val instance = InstanceGen.next.copy(context = context)
      testUpsert(instance)

      instanceDao.updateContext(instance.id, Context.Default).await
      val actualResult1 = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult1 = Some(instance.copy(context = Context.Default))
      actualResult1 should smartEqual(expectedResult1)

      instanceDao.updateContext(instance.id, context).await
      val actualResult2 = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult2 = Some(instance)
      actualResult2 should smartEqual(expectedResult2)
    }

    "correctly update context with non-ascii tag" in {
      val context = ContextGen.next
      val instance = InstanceGen.next.copy(context = context)
      testUpsert(instance)

      val newContext =
        ContextGen.suchThat(c => c.visibility != context.visibility).next.copy(tag = Some("Архххх \n\b\t ℌ¼カ"))
      instanceDao.updateContext(instance.id, newContext).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult = Some(instance.copy(context = newContext))
      actualResult should smartEqual(expectedResult)
    }
  }

  "addSwitchOffs" should {

    "correctly adds several switch offs" in {
      val firstSignal = BanSignalGen.withoutSwitchOff.next
      val secondSignal = WarnSignalGen.withoutSwitchOff.next
      val thirdSignal = TagSignalGen.withoutSwitchOff.next
      val signals = SignalSet(firstSignal, secondSignal, thirdSignal)
      val instance = InstanceGen.next.copy(signals = signals)
      testUpsert(instance)

      val firstSwitchOff = SignalSwitchOffGen.next
      val secondSwitchOff = SignalSwitchOffGen.next
      val switchOffsMap = Map(firstSignal.key -> firstSwitchOff, secondSignal.key -> secondSwitchOff)
      instanceDao.addSwitchOffs(instance.id, switchOffsMap, null).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await

      val expectedSignals =
        SignalSet(
          firstSignal.withSwitchOff(Some(firstSwitchOff)),
          secondSignal.withSwitchOff(Some(secondSwitchOff)),
          thirdSignal
        )
      val expectedResult =
        Some(
          instance.copy(
            signals = expectedSignals
          )
        )

      actualResult shouldBe expectedResult
    }

    "correctly adds 0 switch offs" in {
      val instance = InstanceGen.next
      testUpsert(instance)
      instanceDao.addSwitchOffs(instance.id, Map.empty, null).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult = Some(instance)

      actualResult shouldBe expectedResult
    }
  }

  "deleteSwitchOffs" should {

    "correctly deletes switch offs" in {
      val updateTime = DateTimeGen.next
      val newUpdateTime = updateTime.plusDays(1)
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
      val signalsToDeleteMap =
        Map(
          firstSignal.key -> Tombstone(newUpdateTime, None),
          thirdSignal.key -> Tombstone(newUpdateTime, None)
        )
      val instance =
        InstanceGen.next.copy(
          signals = signals,
          essentialsUpdateTime = updateTime
        )
      testUpsert(instance)

      instanceDao.deleteSwitchOffs(instance.id, signalsToDeleteMap, null).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedSignals =
        SignalSet(
          firstSignal.withSwitchOff(None),
          secondSignal,
          thirdSignal
        )
      val expectedResult =
        Some(
          instance.copy(
            signals = expectedSignals
          )
        )
      actualResult should smartEqual(expectedResult)

      val actualSignalsUpdateTime = actualResult.flatMap(_.signals.updateTime)
      val expectedSignalsUpdateTime = Some(newUpdateTime)
      actualSignalsUpdateTime shouldBe expectedSignalsUpdateTime
    }

    "correctly deletes 0 switch offs" in {
      val instance = InstanceGen.next
      testUpsert(instance)
      instanceDao.deleteSwitchOffs(instance.id, Map.empty, null).await

      val actualResult = instanceDao.getOpt(instance.id, allowExpired = false).await
      val expectedResult = Some(instance)

      actualResult shouldBe expectedResult
    }
  }

  "upsertMetadata" should {

    "correctly add new metadata" in {
      val instance = InstanceGen.next
      testUpsert(instance)
      val metadataSet = MetadataSetGen.next
      metadataSet.toList.traverse(instanceDao.upsertMetadata(instance.id, _, null)).await
      val actualMeta = instanceDao.getOpt(instance.id, allowExpired = false).await.get.metadata
      actualMeta shouldBe metadataSet
    }

    "do nothing if instance doesn't exist" in {
      val metadata = MetadataGen.next
      val nonexistentInstanceId = InstanceIdGen.next
      instanceDao.upsertMetadata(nonexistentInstanceId, metadata, null).await
    }
  }

  "delete" should {

    "correctly delete instance" in {
      val instance = InstanceGen.next
      val id = instance.id
      testUpsert(instance)
      instanceDao.deleteInstance(id).await
      instanceDao.getOpt(id, allowExpired = false).await shouldBe None
    }

    "correctly delete non-existent instance" in {
      val id = InstanceIdGen.next
      instanceDao.deleteInstance(id).await
      instanceDao.getOpt(id, allowExpired = false).await shouldBe None
    }
  }

  "deleteMetadata" should {

    "correctly delete metadata" in {
      val metadata = OffersStatisticsMetadataGen.next
      val instance = InstanceGen.next.copy(metadata = MetadataSet(metadata))
      testUpsert(instance)
      instanceDao.deleteMetadata(instance.id, metadata.`type`, null).await

      val actualResult = instanceDao.getOpt(instance.externalId, allowExpired = false).await
      val expectedResult = Some(instance.copy(metadata = MetadataSet.Empty))
      actualResult shouldBe expectedResult
    }

    "correctly delete non-existent metadata" in {
      val metadata = OffersStatisticsMetadataGen.next
      val instance = InstanceGen.next.copy(metadata = MetadataSet.Empty)
      testUpsert(instance)
      instanceDao.deleteMetadata(instance.id, metadata.`type`, null).await

      val actualResult = instanceDao.getOpt(instance.externalId, allowExpired = false).await
      val expectedResult = Some(instance)
      actualResult shouldBe expectedResult
    }
  }

//  "getObjectIds" should {
//
//    "return current instance ids" in {
//      val user = UserGen.next
//      val objectId1 = ObjectIdGen.next
//      val objectId2 = ObjectIdGen.suchThat(_ != objectId1).next
//
//      val externalId1 = ExternalId(user, objectId1)
//      val externalId2 = ExternalId(user, objectId2)
//
//      val instances1 = instanceGen(externalId1).next(2).toList
//      val instance2 = instanceGen(externalId2).next
//      val instances = instances1 :+ instance2
//      for (instance <- instances) testUpsert(instance)
//
//      val actualResult = instanceDao.getObjectIds(user).await
//      val expectedResult = Set(objectId1, objectId2)
//
//      actualResult shouldBe expectedResult
//    }
//
//    "return big object_id list" in {
//      val user = UserGen.next
//
//      val instances = instanceGen(Service.GENERAL).next(3500).toList.zipWithIndex.map {
//        case (instance, i) => instance.copy(id = instanceIdGen(ExternalId(user, i.toString)).next)
//      }
//
//      instances.traverse { instance => println(instance.externalId.objectId); instanceDao.upsert(instance) }.await
//
//      for (instance <- instances) testUpsert(instance)
//
//      val actualResult = instanceDao.getObjectIds(user).await
//
//      actualResult shouldBe (0 until 3500).map(_.toString).toSet
//    }
//  }

  protected def testUpsert(instance: Instance): Unit = instanceDao.upsert(instance).await
}
