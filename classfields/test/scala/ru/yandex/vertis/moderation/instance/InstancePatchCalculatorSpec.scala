package ru.yandex.vertis.moderation.instance

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.instance.InstancePatch.{AddMetadata, DeleteMetadataByType}
import ru.yandex.vertis.moderation.{RequestContext, SpecBase}
import ru.yandex.vertis.moderation.model.instance.{
  EssentialsPatch,
  ExternalId,
  Instance,
  InstanceIdImpl,
  InstanceSource,
  RealtyEssentials
}
import ru.yandex.vertis.moderation.model.{Domain, ModerationRequest}
import ru.yandex.vertis.moderation.model.context.{Context, ContextSource}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.generators.RichGen
import ru.yandex.vertis.moderation.model.instance.user.{Owner, OwnerJournalRecord}
import ru.yandex.vertis.moderation.model.signal.{
  AutomaticSource,
  BanSignalSource,
  NoMarker,
  SignalFactory,
  SignalInfoSet,
  SignalSet,
  SignalSwitchOff,
  TagSignal,
  Tombstone
}
import ru.yandex.vertis.moderation.model.ModerationRequest.InstanceUpdateRequest
import ru.yandex.vertis.moderation.model.meta.MetadataSet
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Signal.SignalType
import ru.yandex.vertis.moderation.proto.Model.Visibility
import ru.yandex.vertis.moderation.service.InstanceFactory
import ru.yandex.vertis.moderation.util.DateTimeUtil

import scala.concurrent.duration._

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class InstancePatchCalculatorSpec extends SpecBase {

  val calculator: InstancePatchCalculator = new InstancePatchCalculatorImpl(EssentialsPatchCalculatorImpl)

  val daysAgo: DateTime = DateTimeUtil.now().minusDays(2)
  val now: DateTime = DateTimeUtil.now()
  implicit val rc: RequestContext =
    RequestContext(
      requestTime = daysAgo,
      currentTime = now,
      depth = IntGen.next
    )
  val rooms = 1
  val testEssentials: RealtyEssentials = RealtyEssentialsGen.next.copy(rooms = Some(rooms))
  val tag = "abc"
  val testContextSource: ContextSource = ContextSourceGen.next.copy(tag = Some(tag))
  val testInstanceSource: InstanceSource =
    InstanceSourceGen.next.copy(
      signals = Set.empty,
      context = testContextSource,
      essentials = RealtyEssentialsGen.next
    )
  val testInstance: Instance = InstanceFactory.newInstance(testInstanceSource, createTime = daysAgo)
  val externalId: ExternalId = testInstance.externalId

  case class TestCase(description: String,
                      instance: Instance,
                      request: InstanceUpdateRequest,
                      expectedResult: InstancePatch
                     )

  val testCases: Seq[TestCase] =
    Seq(
      {
        val updatedEssentials = testEssentials.copy(rooms = Some(rooms + 1))
        val updatedInstanceSource = testInstanceSource.copy(essentials = updatedEssentials)
        val updatedInstancePayload =
          EssentialsPatch(
            id = testInstance.id,
            essentials = updatedEssentials,
            createTime = testInstance.createTime,
            essentialsUpdateTime = rc.currentTime,
            Seq.empty
          )
        TestCase(
          description = "PushInstance (essentials changed)",
          instance = testInstance,
          request = ModerationRequest.PushInstance(updatedInstanceSource, rc.requestTime, rc.depth),
          expectedResult = InstancePatch(essentials = Some(updatedInstancePayload))
        )
      }, {
        val context = testInstanceSource.context.copy(tag = Some(tag + "!"))
        val updatedInstanceSource = testInstanceSource.copy(context = context)
        TestCase(
          description = "PushInstance (context changed)",
          instance = testInstance,
          request = ModerationRequest.PushInstance(updatedInstanceSource, rc.requestTime, rc.depth),
          expectedResult = InstancePatch(context = Some(Context(context, rc.currentTime)))
        )
      }, {
        val banSignalSource = BanSignalSourceGen.withoutMarker.withoutSwitchOff.next
        val banSignal = SignalFactory.newSignal(banSignalSource, rc.currentTime)
        val updatedInstanceSource = testInstanceSource.copy(signals = Set(banSignalSource))
        TestCase(
          description = "PushInstance (add signals)",
          instance = testInstance,
          request = ModerationRequest.PushInstance(updatedInstanceSource, rc.requestTime, rc.depth),
          expectedResult = InstancePatch(addSignals = Map(banSignal.key -> banSignal))
        )
      }, {
        val indexErrorSignal = IndexErrorSignalGen.withoutMarker.withoutSwitchOff.next
        TestCase(
          description = "PushInstance (delete index error signal)",
          instance = testInstance.copy(signals = SignalSet(indexErrorSignal)),
          request = ModerationRequest.PushInstance(testInstanceSource, rc.requestTime, rc.depth),
          expectedResult =
            InstancePatch(deleteSignalKeys = Map(indexErrorSignal.key -> Tombstone(rc.currentTime, None)))
        )
      }, {
        val signalSource = SignalSourceGen.withoutMarker.withoutSwitchOff.next
        val signalWithoutSwitchOff = SignalFactory.newSignal(signalSource, rc.requestTime)
        val initialInstance = testInstance.copy(signals = SignalSet(signalWithoutSwitchOff))

        val signalKey = signalSource.getKey
        val switchOffSource = SignalSwitchOffSourceGen.next.copy(key = signalKey)
        val signalSourceWithSwitchOff = signalSource.withSwitchOff(Some(switchOffSource))
        val instanceSource = testInstanceSource.copy(signals = Set(signalSourceWithSwitchOff))
        val switchOff = SignalSwitchOff(switchOffSource, rc.currentTime)
        TestCase(
          description = "PushInstance (add switch offs)",
          instance = initialInstance,
          request = ModerationRequest.PushInstance(instanceSource, rc.requestTime, rc.depth),
          expectedResult = InstancePatch(addSwitchOffs = Map(signalKey -> switchOff))
        )
      },
      TestCase(
        description = "PushInstance (no changes)",
        instance = testInstance,
        request = ModerationRequest.PushInstance(testInstanceSource, rc.requestTime, rc.depth),
        expectedResult = InstancePatch.Empty
      ), {
        val newContextSource = ContextSourceGen.suchThat(_ != testContextSource).next
        val newContext = Context(newContextSource, rc.currentTime)
        TestCase(
          description = "ChangeContext (context changed)",
          instance = testInstance,
          request = ModerationRequest.ChangeContext(externalId, newContextSource, rc.requestTime, rc.depth),
          expectedResult = InstancePatch(context = Some(newContext))
        )
      },
      TestCase(
        description = "ChangeContext (no changes)",
        instance = testInstance,
        request = ModerationRequest.ChangeContext(externalId, testInstanceSource.context, rc.requestTime, rc.depth),
        expectedResult = InstancePatch.Empty
      ), {
        val signalSource1 = BanSignalSourceGen.withoutMarker.withoutSwitchOff.next
        val signal1 = SignalFactory.newSignal(signalSource1, daysAgo)
        val signalSource2 = WarnSignalSourceGen.withoutMarker.withoutSwitchOff.next
        val signal2 = SignalFactory.newSignal(signalSource2, rc.currentTime)
        TestCase(
          description = "AppendSignals (two signals, one added)",
          instance = testInstance.copy(signals = SignalSet(signal1)),
          request =
            ModerationRequest.AppendSignals
              .withInitialDepth(externalId, Seq(signalSource1, signalSource2), rc.requestTime),
          expectedResult = InstancePatch(addSignals = Map(signal2.key -> signal2))
        )
      }, {
        val weight = 1.0
        val signalSource1 = WarnSignalSourceGen.withoutMarker.withoutSwitchOff.next
        val signal1 = SignalFactory.newSignal(signalSource1, daysAgo)
        val signalSource2 = signalSource1.copy(weight = weight + 1.0)
        val signal2 = SignalFactory.newSignal(signalSource2, rc.currentTime)
        TestCase(
          description = "AppendSignals (one changed signal)",
          instance = testInstance.copy(signals = SignalSet(signal1)),
          request = ModerationRequest.AppendSignals.withInitialDepth(externalId, Seq(signalSource2), rc.requestTime),
          expectedResult = InstancePatch(addSignals = Map(signal2.key -> signal2))
        )
      }, {
        val signalSource1 = BanSignalSourceGen.withoutSwitchOff.withoutMarker.next
        val signal1 = SignalFactory.newSignal(signalSource1, daysAgo)
        val key = signal1.key
        val switchOffSource = SignalSwitchOffSourceGen.next.copy(key = key)
        val switchOff = SignalSwitchOff(switchOffSource, rc.currentTime)
        val signalSource2 = signalSource1.withSwitchOff(Some(switchOffSource))
        TestCase(
          description = "AppendSignals (switch off added)",
          instance = testInstance.copy(signals = SignalSet(signal1)),
          request = ModerationRequest.AppendSignals.withInitialDepth(externalId, Seq(signalSource2), rc.requestTime),
          expectedResult = InstancePatch(addSwitchOffs = Map(signal1.key -> switchOff))
        )
      }, {
        val comment = "a"
        val signalSourceWithoutSwitchOff = BanSignalSourceGen.withoutMarker.next
        val key = signalSourceWithoutSwitchOff.getKey
        val switchOffSource1 = SignalSwitchOffSourceGen.next.copy(comment = Some(comment), key = key)
        val signalSource1 = signalSourceWithoutSwitchOff.withSwitchOff(Some(switchOffSource1))
        val signal1 = SignalFactory.newSignal(signalSource1, rc.currentTime)
        val switchOffSource2 = switchOffSource1.copy(comment = Some(comment + "!"))
        val switchOff2 = SignalSwitchOff(switchOffSource2, rc.currentTime)
        val signalSource2 = signalSource1.withSwitchOff(Some(switchOffSource2))
        TestCase(
          description = "AppendSignals (switch off changed)",
          instance = testInstance.copy(signals = SignalSet(signal1)),
          request = ModerationRequest.AppendSignals.withInitialDepth(externalId, Seq(signalSource2), rc.requestTime),
          expectedResult = InstancePatch(addSwitchOffs = Map(signal1.key -> switchOff2))
        )
      }, {
        val signalSource = BanSignalSourceGen.withoutSwitchOff.withoutMarker.next
        val signal = SignalFactory.newSignal(signalSource, daysAgo)
        TestCase(
          description = "AppendSignals (one signal, no changes)",
          instance = testInstance.copy(signals = SignalSet(signal)),
          request = ModerationRequest.AppendSignals.withInitialDepth(externalId, Seq(signalSource), rc.requestTime),
          expectedResult = InstancePatch.Empty
        )
      },
      TestCase(
        description = "AppendSignals (zero signals)",
        instance = testInstance,
        request = ModerationRequest.AppendSignals.withInitialDepth(externalId, Seq.empty, rc.requestTime),
        expectedResult = InstancePatch.Empty
      ), {
        val sourceOfSignal = AutomaticSourceGen.next.copy(marker = NoMarker)
        val sourceOfSwitchOff = sourceOfSignal

        val signalSource: BanSignalSource =
          BanSignalSourceGen.withoutSwitchOff.withoutMarker.next.copy(
            source = sourceOfSignal
          )
        val key = signalSource.getKey

        val switchOffSource = SignalSwitchOffSourceGen.next.copy(source = sourceOfSwitchOff, key = key)
        val existentSignalSource =
          signalSource.copy(
            switchOffSource = Some(switchOffSource),
            source = sourceOfSignal
          )
        val existentSignal = SignalFactory.newSignal(existentSignalSource, daysAgo)
        TestCase(
          description = "AppendSignals (delete switch off if signal has automatic source with same application)",
          instance = testInstance.copy(signals = SignalSet(existentSignal)),
          request = ModerationRequest.AppendSignals.withInitialDepth(externalId, Seq(signalSource), rc.requestTime),
          expectedResult = InstancePatch(deleteSwitchOffs = Map(key -> Tombstone(rc.currentTime, None)))
        )
      }, {
        val sourceOfSignal = AutomaticSourceGen.next.copy(marker = NoMarker)
        val sourceOfSwitchOff = ManualSourceGen.next.copy(marker = NoMarker)

        val signalSource: BanSignalSource =
          BanSignalSourceGen.withoutSwitchOff.withoutMarker.next.copy(
            source = sourceOfSignal
          )
        val key = signalSource.getKey

        val switchOffSource = SignalSwitchOffSourceGen.next.copy(source = sourceOfSwitchOff, key = key)
        val existentSignalSource =
          signalSource.copy(
            switchOffSource = Some(switchOffSource),
            source = sourceOfSignal
          )
        val existentSignal = SignalFactory.newSignal(existentSignalSource, daysAgo)
        TestCase(
          description = "AppendSignals (delete switch off if signal has automatic source with different application)",
          instance = testInstance.copy(signals = SignalSet(existentSignal)),
          request = ModerationRequest.AppendSignals.withInitialDepth(externalId, Seq(signalSource), rc.requestTime),
          expectedResult = InstancePatch.Empty
        )
      }, {
        val sourceOfSignal = ManualSourceGen.next.copy(marker = NoMarker)
        val sourceOfSwitchOff = AutomaticSourceGen.next.copy(marker = NoMarker)

        val signalSource: BanSignalSource =
          BanSignalSourceGen.withoutSwitchOff.withoutMarker.next.copy(
            source = sourceOfSignal
          )
        val key = signalSource.getKey

        val switchOffSource = SignalSwitchOffSourceGen.next.copy(source = sourceOfSwitchOff, key = key)
        val existentSignalSource =
          signalSource.copy(
            switchOffSource = Some(switchOffSource),
            source = sourceOfSignal
          )
        val existentSignal = SignalFactory.newSignal(existentSignalSource, daysAgo)
        TestCase(
          description = "AppendSignals (delete switch off if signal has manual source)",
          instance = testInstance.copy(signals = SignalSet(existentSignal)),
          request = ModerationRequest.AppendSignals.withInitialDepth(externalId, Seq(signalSource), rc.requestTime),
          expectedResult = InstancePatch(deleteSwitchOffs = Map(key -> Tombstone(rc.currentTime, None)))
        )
      }, {
        val banSignal = BanSignalGen.withoutMarker.next
        val nonExistentKey = WarnSignalGen.withoutMarker.next.key
        TestCase(
          description = "RemoveSignals (two signals, one existent)",
          instance = testInstance.copy(signals = SignalSet(banSignal)),
          request =
            ModerationRequest
              .RemoveSignals(externalId, Set(banSignal.key, nonExistentKey), None, rc.requestTime, rc.depth),
          expectedResult = InstancePatch(deleteSignalKeys = Map(banSignal.key -> Tombstone(rc.currentTime, None)))
        )
      }, {
        val nonExistentKey = WarnSignalGen.withoutMarker.next.key
        TestCase(
          description = "RemoveSignals (one signal, no existent)",
          instance = testInstance,
          request = ModerationRequest.RemoveSignals(externalId, Set(nonExistentKey), None, rc.requestTime, rc.depth),
          expectedResult = InstancePatch.Empty
        )
      },
      TestCase(
        description = "RemoveSignals (zero signal)",
        instance = testInstance,
        request = ModerationRequest.RemoveSignals(externalId, Set.empty, None, rc.requestTime, rc.depth),
        expectedResult = InstancePatch.Empty
      ), {
        val metadata = ReviewsStatisticsMetadataGen.next
        TestCase(
          description = "UpsertMetadata (no previous meta with this type)",
          instance = testInstance.copy(metadata = MetadataSet.Empty),
          request = ModerationRequest.UpsertMetadata(externalId, metadata, rc.requestTime, rc.depth),
          expectedResult = InstancePatch(metadataAction = Some(metadata).map(x => Left(AddMetadata(x))))
        )
      }, {
        val metadata = ReviewsStatisticsMetadataGen.next
        val delta = FiniteDurationGen.?.next.getOrElse(0.seconds)
        val prevMetadata = metadata.copy(timestamp = metadata.timestamp.minus(delta.toMillis))
        TestCase(
          description = "UpsertMetadata (previous meta is not newer then upserted)",
          instance = testInstance.copy(metadata = testInstance.metadata + prevMetadata),
          request = ModerationRequest.UpsertMetadata(externalId, metadata, rc.requestTime, rc.depth),
          expectedResult = InstancePatch(metadataAction = Some(metadata).map(x => Left(AddMetadata(x))))
        )
      }, {
        val oldMetadata = OffersStatisticsMetadataGen.next
        val newMetadata =
          OffersStatisticsMetadataGen.next
            .copy(timestamp = oldMetadata.timestamp.plusMinutes(1))
        TestCase(
          description = "UpsertMetadata (not upsert metadata if has the newer one)",
          instance = testInstance.copy(metadata = testInstance.metadata + newMetadata),
          request = ModerationRequest.UpsertMetadata(externalId, oldMetadata, rc.requestTime, rc.depth),
          expectedResult = InstancePatch.Empty
        )
      }, {
        val signalWithoutSwitchOff1 = BanSignalGen.withoutSwitchOff.next
        val switchOffSource1 = SignalSwitchOffSourceGen.next.copy(key = signalWithoutSwitchOff1.key)
        val switchOff1 = SignalSwitchOff(switchOffSource1, daysAgo)
        val signal1 = signalWithoutSwitchOff1.copy(switchOff = Some(switchOff1))
        val signalWithoutSwitchOff2 = BanSignalGen.withoutSwitchOff.next
        val switchOffSource2 = SignalSwitchOffSourceGen.next.copy(key = signalWithoutSwitchOff2.key)
        val switchOff2 = SignalSwitchOff(switchOffSource2, rc.currentTime)
        TestCase(
          description = "AddSwitchOffs (two switch offs, one added)",
          instance = testInstance.copy(signals = SignalSet(signal1)),
          request =
            ModerationRequest
              .AddSwitchOffs(externalId, Seq(switchOffSource1, switchOffSource2), rc.requestTime, rc.depth),
          expectedResult = InstancePatch(addSwitchOffs = Map(switchOffSource2.key -> switchOff2))
        )
      }, {
        val comment = "a"
        val signalWithoutSwitchOff = BanSignalGen.withoutSwitchOff.next
        val switchOffSource1 =
          SignalSwitchOffSourceGen.next.copy(
            key = signalWithoutSwitchOff.key,
            comment = Some(comment)
          )
        val switchOff1 = SignalSwitchOff(switchOffSource1, daysAgo)
        val signal = signalWithoutSwitchOff.copy(switchOff = Some(switchOff1))
        val switchOffSource2 = switchOffSource1.copy(comment = Some(comment + "!"))
        val switchOff2 = SignalSwitchOff(switchOffSource2, rc.currentTime)
        TestCase(
          description = "AddSwitchOffs (one updated switch off)",
          instance = testInstance.copy(signals = SignalSet(signal)),
          request = ModerationRequest.AddSwitchOffs(externalId, Seq(switchOffSource2), rc.requestTime, rc.depth),
          expectedResult = InstancePatch(addSwitchOffs = Map(switchOffSource2.key -> switchOff2))
        )
      }, {
        val signalWithoutSwitchOff = BanSignalGen.withoutMarker.withoutSwitchOff.next
        val manualSwitchOff =
          SignalSwitchOffGen.next.copy(
            source = ManualSourceGen.next
          )
        val signal = signalWithoutSwitchOff.withSwitchOff(Some(manualSwitchOff))
        val automaticSwitchOffSource =
          SignalSwitchOffSourceGen.next.copy(
            source = AutomaticSourceGen.next,
            key = signal.key
          )
        TestCase(
          description = "AddSwitchOffs (not update switch off if existent is manual and new is automatic)",
          instance = testInstance.copy(signals = SignalSet(signal)),
          request =
            ModerationRequest.AddSwitchOffs(externalId, Seq(automaticSwitchOffSource), rc.requestTime, rc.depth),
          expectedResult = InstancePatch.Empty
        )
      }, {
        val signalWithoutSwitchOff = BanSignalGen.withoutSwitchOff.next
        val switchOffSource = SignalSwitchOffSourceGen.next.copy(key = signalWithoutSwitchOff.key)
        val switchOff = SignalSwitchOff(switchOffSource, daysAgo)
        val signal = signalWithoutSwitchOff.copy(switchOff = Some(switchOff))
        TestCase(
          description = "AddSwitchOffs (one switch off, no changes)",
          instance = testInstance.copy(signals = SignalSet(signal)),
          request = ModerationRequest.AddSwitchOffs(externalId, Seq(switchOffSource), rc.requestTime, rc.depth),
          expectedResult = InstancePatch.Empty
        )
      },
      TestCase(
        description = "AddSwitchOffs (zero switch offs)",
        instance = testInstance,
        request = ModerationRequest.AddSwitchOffs(externalId, Seq.empty, rc.requestTime, rc.depth),
        expectedResult = InstancePatch.Empty
      ), {
        val switchOff = SignalSwitchOffGen.next
        val signal = BanSignalGen.next.copy(switchOff = Some(switchOff))
        val nonExistentKey = WarnSignalGen.next.key
        TestCase(
          description = "DeleteSwitchOffs (two keys, one existent)",
          instance = testInstance.copy(signals = SignalSet(signal)),
          request =
            ModerationRequest
              .DeleteSwitchOffs(externalId, Set(signal.key, nonExistentKey), None, rc.requestTime, rc.depth),
          expectedResult = InstancePatch(deleteSwitchOffs = Map(signal.key -> Tombstone(rc.currentTime, None)))
        )
      }, {
        val nonExistentKey = WarnSignalGen.next.key
        TestCase(
          description = "DeleteSwitchOffs (one keys, no existent)",
          instance = testInstance,
          request = ModerationRequest.DeleteSwitchOffs(externalId, Set(nonExistentKey), None, rc.requestTime, rc.depth),
          expectedResult = InstancePatch.Empty
        )
      },
      TestCase(
        description = "DeleteSwitchOffs (zero keys)",
        instance = testInstance,
        request = ModerationRequest.DeleteSwitchOffs(externalId, Set.empty, None, rc.requestTime, rc.depth),
        expectedResult = InstancePatch.Empty
      ), {
        val signal = InheritedSignalGen.withoutSwitchOff.next
        val owner = Owner(externalId.user, SignalSet(signal))
        val prev = Owner.empty(externalId.user)
        val ownerRecord = OwnerJournalRecord(rc.requestTime, rc.depth, owner, prev)
        TestCase(
          description = "TouchOwnerSignals (add signal)",
          instance = testInstance,
          request = ModerationRequest.TouchOwnerSignals(externalId.objectId, ownerRecord),
          expectedResult = InstancePatch(addSignals = Map(signal.key -> signal))
        )
      }, {
        val info = "a"
        val signal = InheritedSignalGen.next.withInfo(Some(info))
        val updatedSignal = signal.withInfo(info = Some(info + "!"))
        val owner = Owner(externalId.user, SignalSet(updatedSignal))
        val prev = Owner.empty(externalId.user)
        val ownerRecord = OwnerJournalRecord(rc.requestTime, rc.depth, owner, prev)
        val expectedSignal = updatedSignal.withSwitchOff(None)
        TestCase(
          description = "TouchOwnerSignals (change signal)",
          instance = testInstance.copy(signals = SignalSet(signal)),
          request = ModerationRequest.TouchOwnerSignals(externalId.objectId, ownerRecord),
          expectedResult = InstancePatch(addSignals = Map(expectedSignal.key -> expectedSignal))
        )
      }, {
        val signal = InheritedSignalGen.withoutSwitchOff.next
        val owner = Owner(externalId.user, SignalSet.Empty)
        val prev = Owner.empty(externalId.user)
        val ownerRecord = OwnerJournalRecord(rc.requestTime, rc.depth, owner, prev)
        TestCase(
          description = "TouchOwnerSignals (delete signal)",
          instance = testInstance.copy(signals = SignalSet(signal)),
          request = ModerationRequest.TouchOwnerSignals(externalId.objectId, ownerRecord),
          expectedResult = InstancePatch(deleteSignalKeys = Map(signal.key -> Tombstone(rc.currentTime, None)))
        )
      }, {
        val signal = InheritedSignalGen.withoutSwitchOff.next
        val switchOff = SignalSwitchOffGen.next
        val owner = Owner(externalId.user, SignalSet(signal.withSwitchOff(Some(switchOff))))
        val prev = Owner.empty(externalId.user)
        val ownerRecord = OwnerJournalRecord(rc.requestTime, rc.depth, owner, prev)
        TestCase(
          description = "TouchOwnerSignals (add switch off)",
          instance = testInstance.copy(signals = SignalSet(signal)),
          request = ModerationRequest.TouchOwnerSignals(externalId.objectId, ownerRecord),
          expectedResult = InstancePatch(addSwitchOffs = Map(signal.key -> switchOff))
        )
      }, {
        val signal = InheritedSignalGen.withoutSwitchOff.next
        val switchOff = SignalSwitchOffGen.next
        val owner = Owner(externalId.user, SignalSet(signal))
        val prev = Owner.empty(externalId.user)
        val ownerRecord = OwnerJournalRecord(rc.requestTime, rc.depth, owner, prev)
        TestCase(
          description = "TouchOwnerSignals (delete switch off)",
          instance = testInstance.copy(signals = SignalSet(signal.withSwitchOff(Some(switchOff)))),
          request = ModerationRequest.TouchOwnerSignals(externalId.objectId, ownerRecord),
          expectedResult = InstancePatch(deleteSwitchOffs = Map(signal.key -> Tombstone(rc.currentTime, None)))
        )
      }, {
        val signal = InheritedSignalGen.withoutSwitchOff.next
        val owner = Owner(externalId.user, SignalSet(signal))
        val prev = Owner.empty(externalId.user)
        val ownerRecord = OwnerJournalRecord(rc.requestTime, rc.depth, owner, prev)
        TestCase(
          description = "TouchOwnerSignals (no changes)",
          instance = testInstance.copy(signals = SignalSet(signal)),
          request = ModerationRequest.TouchOwnerSignals(externalId.objectId, ownerRecord),
          expectedResult = InstancePatch.Empty
        )
      }, {
        val metadataSet = MetadataSet(ProvenOwnerMetadataGen.next)
        val currentInstance = testInstance.copy(metadata = metadataSet)
        TestCase(
          description = "DeleteMetadata (available to delete)",
          instance = currentInstance,
          request = ModerationRequest.DeleteMetadata(externalId, "proven_owner", rc.requestTime, rc.depth),
          expectedResult = InstancePatch(metadataAction = Some("proven_owner").map(x => Right(DeleteMetadataByType(x))))
        )
      }, {
        val metadataSet = MetadataSet(ProvenOwnerMetadataGen.next)
        val currentInstance = testInstance.copy(metadata = metadataSet)
        TestCase(
          description = "DeleteMetadata (not available to delete)",
          instance = currentInstance,
          request = ModerationRequest.DeleteMetadata(externalId, "pica", rc.requestTime, rc.depth),
          expectedResult = InstancePatch.Empty
        )
      }
    )

  "calculate" should {
    testCases.foreach { case TestCase(description, instance, request, expectedResult) =>
      description in {
        val actualResult = calculator.calculatePatch(instance, request, now)
        actualResult should smartEqual(expectedResult)
      }
    }
  }
  "calculate" should {
    val tags = Set("WAS_ACTIVE_BEFORE_DEALER_BAN")
    val dealer = DealerUserGen.next
    val externalId = ExternalId(dealer, "1116255833-10a8277f")
    val autoruInstance =
      testInstance.copy(
        essentials = AutoruEssentialsGen.next,
        context = Context.Default,
        id = InstanceIdImpl(externalId).toId
      )
    val signal = InheritedSignalGen.filter(_.signalType == SignalType.BAN).withoutSwitchOff.next
    val owner = Owner(dealer, SignalSet(signal))
    val prev = Owner.empty(dealer)
    val ownerRecord = OwnerJournalRecord(rc.requestTime, rc.depth, owner, prev)
    "add tag signal with previous status" in {
      val request = ModerationRequest.TouchOwnerSignals(autoruInstance.externalId.objectId, ownerRecord)
      val InstancePatch(_, _, addSignals, deleteSignalKeys, _, _, _) =
        calculator.calculatePatch(autoruInstance, request, now)
      val addTagSignals = addSignals.values.filter(_.signalType == SignalType.TAG)
      addTagSignals.size shouldBe 1
      deleteSignalKeys.size shouldBe 0
      addTagSignals.head.asInstanceOf[TagSignal].tags shouldBe tags
    }
    "do nothing if context was not active" in {
      val inactive = autoruInstance.copy(context = Context.apply(Visibility.INACTIVE, None, None))
      val request = ModerationRequest.TouchOwnerSignals(inactive.externalId.objectId, ownerRecord)
      val InstancePatch(_, _, addSignals, deleteSignalKeys, _, _, _) = calculator.calculatePatch(inactive, request, now)
      addSignals.values.count(_.signalType == SignalType.TAG) shouldBe 0
      deleteSignalKeys.size shouldBe 0
    }
    "delete tag signal if switch off was added" in {
      val switchedOffSignal = signal.withSwitchOff(Some(SignalSwitchOffGen.next))
      val signalSet = SignalSet(switchedOffSignal)
      val instanceWithSwitchOff =
        autoruInstance
          .copy(signals =
            signalSet ++ SignalSet(
              TagSignalGen
                .map(_.copy(tags = tags))
                .next
                .withSwitchOff(None)
            )
          )
      val newOwner = Owner(dealer, signalSet)
      val newOwnerRecord = ownerRecord.copy(owner = newOwner, prev = owner)
      val request = ModerationRequest.TouchOwnerSignals(instanceWithSwitchOff.externalId.objectId, newOwnerRecord)
      val InstancePatch(_, _, addSignals, deleteSignalKeys, _, _, _) =
        calculator.calculatePatch(instanceWithSwitchOff, request, now)
      addSignals.values.count(_.signalType == SignalType.TAG) shouldBe 0
      deleteSignalKeys.size shouldBe 1

    }
  }
}
