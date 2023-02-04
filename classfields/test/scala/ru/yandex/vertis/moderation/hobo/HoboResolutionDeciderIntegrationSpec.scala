package ru.yandex.vertis.moderation.hobo

import com.google.protobuf
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.InMemoryFeatureRegistry
import ru.yandex.vertis.hobo.proto.Common.AutoruProvenOwnerResolution
import ru.yandex.vertis.hobo.proto.Model.{Resolution => ProtoHoboResolution, Task => ProtoHoboTask, _}
import ru.yandex.vertis.moderation.feature.ModerationFeatureTypes
import ru.yandex.vertis.moderation.hobo.HoboResolutionDecider.Decision
import ru.yandex.vertis.moderation.hobo.ProtobufImplicits._
import ru.yandex.vertis.moderation.model.DetailedReason.UserReseller
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{ExternalId, Instance}
import ru.yandex.vertis.moderation.model.meta.{DealerResolutionType, Metadata, MetadataSet}
import ru.yandex.vertis.moderation.model.signal.{
  AutomaticSource,
  HoboSignal,
  IndexErrorSignal,
  NoMarker,
  SignalInfo,
  SignalInfoSet,
  SignalSet,
  SignalSwitchOff
}
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, ModerationRequest}
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Metadata.ProvenOwnerMetadata
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service}
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.{Globals, SpecBase}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class HoboResolutionDeciderIntegrationSpec extends SpecBase {

  private def makeDecider(service: Service): HoboResolutionDecider = {
    val featureRegistry = new InMemoryFeatureRegistry(ModerationFeatureTypes)
    val decidersChain = HoboResolutionDecider.buildHoboResolutionDecidersChain(service)
    new CompositeHoboResolutionDecider(decidersChain)
  }

  private val autoruDecider = makeDecider(Service.AUTORU)
  private val usersAutoruDecider = makeDecider(Service.USERS_AUTORU)
  private val realtyDecider = makeDecider(Service.REALTY)
  private val dealersAutoruDecider = makeDecider(Service.DEALERS_AUTORU)

  private case class TestCase(description: String,
                              instance: Instance,
                              hoboTask: ProtoHoboTask,
                              check: Decision => Boolean,
                              decider: HoboResolutionDecider = autoruDecider
                             )

  private val periodId = "period1"
  private val now = DateTimeUtil.now()
  private val autoruInstance = instanceGen(essentialsGen(Service.AUTORU)).next
  private val userAutoruInstance = instanceGen(essentialsGen(Service.USERS_AUTORU)).next

  private val resellerTask1 = HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_HYPOTHESIS_RESELLERS.toString)
  private val resellerTask2 = HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_HYPOTHESIS_RESELLERS.toString)
  private val resellerSwitchOff1: SignalSwitchOff =
    SignalSwitchOff(
      source = AutomaticSource(Application.PUNISHER),
      timestamp = now.minusDays(100),
      None,
      Some(365.days)
    )
  private val resellerSignal1: HoboSignal =
    HoboSignalGen.next.copy(
      domain = Domain.UsersAutoru(Model.Domain.UsersAutoru.LCV),
      source = AutomaticSource(Application.NIRVANA, tag = Some("first")),
      timestamp = now.minusDays(200),
      `type` = HoboCheckType.RESELLERS_HYPOTHESIS,
      task = Some(resellerTask1),
      result = HoboSignal.Result.Bad(Set(UserReseller(None, Seq.empty)), Some("damned reseller")),
      switchOff = Some(resellerSwitchOff1),
      allowResultAfter = None,
      finishTime = None
    )
  private val resellerSignal2: HoboSignal =
    resellerSignal1.copy(
      source = AutomaticSource(Application.NIRVANA, tag = Some("second")),
      task = Some(resellerTask2),
      result = HoboSignal.Result.Bad(Set(UserReseller(None, Seq.empty)), Some("damned reseller again")),
      timestamp = now,
      switchOff = None
    )
  private val resellerInstance =
    instanceGen(Service.USERS_AUTORU).next.copy(signals = SignalSet(resellerSignal1) ++ SignalSet(resellerSignal2))
  private val autoruComplaintsReseller =
    AutoruComplaintsResellerResolution
      .newBuilder()
      .setVersion(1)
      .addValues(AutoruComplaintsResellerResolution.Value.RESELLER)
      .addAllResellerDomains(Iterable(Model.Domain.UsersAutoru.CARS, Model.Domain.UsersAutoru.LCV).asJava)
  private val resellerResolution =
    ProtoHoboResolution
      .newBuilder()
      .setVersion(1)
      .setAutoruComplaintsReseller(autoruComplaintsReseller)
  private val resellerHoboTask1 =
    ProtoHoboTask
      .newBuilder()
      .setVersion(1)
      .setQueue(QueueId.valueOf(resellerTask2.queue))
      .setKey(resellerTask2.key)
      .setResolution(resellerResolution)
      .setFinishTime(now.getMillis)
      .build()
  private val resellerHoboTask2 =
    ProtoHoboTask
      .newBuilder()
      .setVersion(1)
      .setQueue(QueueId.valueOf(resellerTask2.queue))
      .setKey(resellerTask2.key)
      .setResolution(resellerResolution)
      .setFinishTime(now.minusDays(101).getMillis)
      .build()

  private val provenOwnerOfferId1 = StringGen.next
  private val provenOwnerVin1 = StringGen.next
  private val provenOwnerVerdict1 = AutoruProvenOwnerResolution.Value.Verdict.PROVEN_OWNER_BAD_PHOTOS
  private val provenOwnerValue1 =
    AutoruProvenOwnerResolution.Value
      .newBuilder()
      .setVerdict(provenOwnerVerdict1)
      .setOfferId(provenOwnerOfferId1)
      .setVin(provenOwnerVin1)
      .build()
  private val provenOwnerOfferId2 = StringGen.next
  private val provenOwnerVin2 = StringGen.next
  private val provenOwnerVerdict2 = AutoruProvenOwnerResolution.Value.Verdict.PROVEN_OWNER_OK
  private val provenOwnerValue2 =
    AutoruProvenOwnerResolution.Value
      .newBuilder()
      .setVerdict(provenOwnerVerdict2)
      .setOfferId(provenOwnerOfferId2)
      .setVin(provenOwnerVin2)
      .build()
  private val provenOwner1 =
    AutoruProvenOwnerResolution
      .newBuilder()
      .addAllValues(Seq(provenOwnerValue1, provenOwnerValue2).asJava)
  private val provenOwner2 =
    AutoruProvenOwnerResolution
      .newBuilder()
      .addAllValues(Seq(provenOwnerValue2).asJava)
  private val realtyBannedRevalidation1 =
    RealtyVisualResolution
      .newBuilder()
      .setVersion(1)
      .addAllValues(Set(RealtyVisualResolution.Value.UNBAN).asJava)
  private val realtyBannedRevalidation2 =
    RealtyVisualResolution
      .newBuilder()
      .setVersion(1)
      .addAllValues(Set(RealtyVisualResolution.Value.BAD_PHOTO).asJava)
  private val provenOwnerResolution1 =
    ProtoHoboResolution
      .newBuilder()
      .setVersion(1)
      .setAutoruProvenOwner(provenOwner1)
  private val provenOwnerResolution2 =
    ProtoHoboResolution
      .newBuilder()
      .setVersion(1)
      .setAutoruProvenOwner(provenOwner2)
  private val realtyBannedRevalidationResolution1 =
    ProtoHoboResolution
      .newBuilder()
      .setVersion(1)
      .setRealtyVisual(realtyBannedRevalidation1)
  private val realtyBannedRevalidationResolution2 =
    ProtoHoboResolution
      .newBuilder()
      .setVersion(1)
      .setRealtyVisual(realtyBannedRevalidation2)
  private val provenOwnerHoboTask1 =
    ProtoHoboTask
      .newBuilder()
      .setVersion(1)
      .setQueue(QueueId.AUTO_RU_PROVEN_OWNER)
      .setKey(StringGen.next)
      .setResolution(provenOwnerResolution1)
      .setFinishTime(now.getMillis)
      .build()
  private val provenOwnerHoboTask2 =
    ProtoHoboTask
      .newBuilder()
      .setVersion(1)
      .setQueue(QueueId.AUTO_RU_PROVEN_OWNER)
      .setKey(StringGen.next)
      .setResolution(provenOwnerResolution2)
      .setFinishTime(now.getMillis)
      .build()
  private val realtyBannedRevalidationTask1 =
    ProtoHoboTask
      .newBuilder()
      .setVersion(1)
      .setQueue(QueueId.REALTY_BANNED_REVALIDATION_VISUAL)
      .setKey(StringGen.next)
      .setResolution(realtyBannedRevalidationResolution1)
      .setFinishTime(now.getMillis)
      .build()
  private val realtyBannedRevalidationTask2 =
    ProtoHoboTask
      .newBuilder()
      .setVersion(1)
      .setQueue(QueueId.REALTY_BANNED_REVALIDATION_VISUAL)
      .setKey(StringGen.next)
      .setResolution(realtyBannedRevalidationResolution2)
      .setFinishTime(now.getMillis)
      .build()

  private val testCases: Seq[TestCase] =
    Seq(
      {
        val task: HoboSignal.Task =
          HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_WARNED_REVALIDATION_VISUAL.toString)
        TestCase(
          description = "AutoruVisualResolution",
          instance = {
            val signal: HoboSignal =
              HoboSignalGen.next.copy(
                domain = DomainAutoruGen.next,
                source = ManualSourceGen.next.copy(marker = NoSourceMarkerGen.next),
                timestamp = DateTimeGen.next,
                `type` = HoboCheckType.WARNED_REVALIDATION_VISUAL,
                task = Some(task),
                result = HoboSignal.Result.Undefined,
                switchOff = None,
                allowResultAfter = None,
                finishTime = None
              )
            val signal2: HoboSignal =
              HoboSignalGen.next.copy(
                domain = DomainAutoruGen.next,
                source = ManualSourceGen.next.copy(marker = NoSourceMarkerGen.next),
                timestamp = DateTimeGen.next,
                `type` = HoboCheckType.CALL_CENTER,
                task = Some(task),
                result = HoboSignal.Result.Undefined,
                switchOff = None,
                allowResultAfter = None,
                finishTime = None
              )
            instanceGen(Service.AUTORU).next.copy(signals = SignalSet(Seq(signal, signal2)))
          },
          hoboTask = {
            val autoruVisual =
              AutoruVisualResolution
                .newBuilder()
                .setVersion(1)
                .addAllValues(Seq(AutoruVisualResolution.Value.DO_NOT_EXIST).asJava)
                .setLicensePlateOnPhotos(AutoruVisualResolution.LicensePlateOnPhotos.CHECK_OK)
                .build
            val resolution =
              ProtoHoboResolution.newBuilder
                .setVersion(1)
                .setAutoruVisual(autoruVisual)
                .build
            ProtoHoboTask
              .newBuilder()
              .setVersion(1)
              .setQueue(QueueId.valueOf(task.queue))
              .setKey(task.key)
              .setResolution(resolution)
              .build
          },
          check = { case Decision(r, _) =>
            r.exists(_.isInstanceOf[ModerationRequest.UpsertMetadata]) &&
              r.exists(_.isInstanceOf[ModerationRequest.AppendSignals]) &&
              r.exists(_.isInstanceOf[ModerationRequest.RemoveSignals])
          }
        )
      }, {
        val task: HoboSignal.Task = HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_LOYALTY_DEALERS.toString)
        val forPeriod = DealerMetadataLoyaltyInfoGen.next.copy(periodId = periodId.toOption)
        TestCase(
          description = "AutoruLoyaltyDealersResolution UNBAN",
          instance = {
            val signal: HoboSignal =
              HoboSignalGen.next.copy(
                domain = DomainDealersAutoruGen.next,
                source = ManualSourceGen.next.copy(marker = NoSourceMarkerGen.next),
                timestamp = DateTimeGen.next,
                `type` = HoboCheckType.LOYALTY_DEALERS,
                task = Some(task),
                result = HoboSignal.Result.Undefined,
                switchOff = None,
                allowResultAfter = None,
                finishTime = None,
                auxInfo = SignalInfoSet(SignalInfo.HoboTaskInfo(autoruLoyaltyDealerPeriodId = periodId))
              )
            val metadata = DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption)
            instanceGen(Service.DEALERS_AUTORU).next.copy(signals = SignalSet(signal), metadata = MetadataSet(metadata))
          },
          hoboTask = {
            val autoruLoyaltyDealers =
              AutoruLoyaltyDealersResolution.newBuilder().setValue(AutoruLoyaltyDealersResolution.Value.UNBAN)
            val resolution =
              ProtoHoboResolution.newBuilder
                .setVersion(1)
                .setAutoruLoyaltyDealers(autoruLoyaltyDealers)
                .build
            ProtoHoboTask
              .newBuilder()
              .setVersion(1)
              .setQueue(QueueId.valueOf(task.queue))
              .setKey(task.key)
              .setResolution(resolution)
              .addAllLabels(Seq(periodId).asJava)
              .build
          },
          check = { case Decision(r, _) =>
            r.exists(_.isInstanceOf[ModerationRequest.AppendSignals]) &&
              r.collect {
                case ModerationRequest.UpsertMetadata(_, meta, _, _)
                     if meta
                       .asInstanceOf[Metadata.Dealer]
                       .forPeriodApproved
                       .exists(_.resolution == forPeriod.resolution) =>
                  ()
              }.nonEmpty
          },
          decider = dealersAutoruDecider
        )
      }, {
        val task: HoboSignal.Task = HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_LOYALTY_DEALERS.toString)
        TestCase(
          description = "AutoruLoyaltyDealersResolution BAN",
          instance = {
            val signal: HoboSignal =
              HoboSignalGen.next.copy(
                domain = DomainDealersAutoruGen.next,
                source = ManualSourceGen.next.copy(marker = NoSourceMarkerGen.next),
                timestamp = DateTimeGen.next,
                `type` = HoboCheckType.LOYALTY_DEALERS,
                task = Some(task),
                result = HoboSignal.Result.Undefined,
                switchOff = None,
                allowResultAfter = None,
                finishTime = None,
                auxInfo = SignalInfoSet(SignalInfo.HoboTaskInfo(autoruLoyaltyDealerPeriodId = periodId))
              )
            val forPeriod = DealerMetadataLoyaltyInfoGen.next.copy(periodId = Some(periodId))
            val metadata = DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption)
            instanceGen(Service.DEALERS_AUTORU).next.copy(signals = SignalSet(signal), metadata = MetadataSet(metadata))
          },
          hoboTask = {
            val autoruLoyaltyDealers =
              AutoruLoyaltyDealersResolution.newBuilder().setValue(AutoruLoyaltyDealersResolution.Value.BAN)
            val resolution =
              ProtoHoboResolution.newBuilder
                .setVersion(1)
                .setAutoruLoyaltyDealers(autoruLoyaltyDealers)
                .build
            ProtoHoboTask
              .newBuilder()
              .setVersion(1)
              .setQueue(QueueId.valueOf(task.queue))
              .setKey(task.key)
              .setResolution(resolution)
              .addAllLabels(Seq(periodId).asJava)
              .build
          },
          check = { case Decision(r, _) =>
            r.exists(_.isInstanceOf[ModerationRequest.AppendSignals]) &&
              r.collect {
                case ModerationRequest.UpsertMetadata(_, meta, _, _)
                     if meta
                       .asInstanceOf[Metadata.Dealer]
                       .forPeriodApproved
                       .exists(_.resolution.`type` == DealerResolutionType.NotLoyal) =>
                  ()
              }.nonEmpty
          },
          decider = dealersAutoruDecider
        )
      }, {
        val task: HoboSignal.Task =
          HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_WARNED_REVALIDATION_VISUAL.toString)
        TestCase(
          description = "CallCenterResolution",
          instance = {
            val signal: HoboSignal =
              HoboSignalGen.next.copy(
                domain = DomainAutoruGen.next,
                source = ManualSourceGen.next.copy(marker = NoSourceMarkerGen.next),
                timestamp = DateTimeGen.next,
                `type` = HoboCheckType.CALL_CENTER,
                task = Some(task),
                result = HoboSignal.Result.Undefined,
                switchOff = None,
                allowResultAfter = None,
                finishTime = None
              )
            val signal2: HoboSignal =
              HoboSignalGen.next.copy(
                domain = DomainAutoruGen.next,
                source = ManualSourceGen.next.copy(marker = NoSourceMarkerGen.next),
                timestamp = DateTimeGen.next,
                `type` = HoboCheckType.AUTOMATIC_QUALITY_CHECK_CALL,
                task = Some(task),
                result = HoboSignal.Result.Undefined,
                switchOff = None,
                allowResultAfter = None,
                finishTime = None
              )
            instanceGen(Service.AUTORU).next.copy(signals = SignalSet(Seq(signal, signal2)))
          },
          hoboTask = {
            val callCenter =
              CallCenterResolution
                .newBuilder()
                .setVersion(1)
                .setValue(CallCenterResolution.Value.RELEVANT)
                .build()
            val resolution =
              ProtoHoboResolution.newBuilder
                .setVersion(1)
                .setCallCenter(callCenter)
                .build
            ProtoHoboTask
              .newBuilder()
              .setVersion(1)
              .setQueue(QueueId.valueOf(task.queue))
              .setKey(task.key)
              .setResolution(resolution)
              .build
          },
          check = { case Decision(r, _) =>
            r.exists(_.isInstanceOf[ModerationRequest.AppendSignals]) &&
              r.exists(_.isInstanceOf[ModerationRequest.RemoveSignals])
          }
        )
      }, {
        val task: HoboSignal.Task = HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_PREMODERATION_DEALER.toString)
        val ts = DateTimeGen.next
        TestCase(
          description = "AutoruPremoderationDealerResolution",
          instance = {
            val signal1: IndexErrorSignal =
              IndexErrorSignalGen.next.copy(
                domain = DomainDealersAutoruGen.next,
                source = SourceWithoutMarkerGen.next,
                timestamp = ts.minus(10L),
                info = None,
                detailedReasons = Set(DetailedReason.NotVerified),
                switchOff = None
              )
            val signal2: HoboSignal =
              HoboSignalGen.next.copy(
                domain = DomainDealersAutoruGen.next,
                source = AutomaticSourceGen.next,
                timestamp = ts,
                `type` = HoboCheckType.PREMODERATION_DEALER,
                task = Some(task),
                result = HoboSignal.Result.Good(None),
                switchOff = None,
                allowResultAfter = None,
                finishTime = None
              )
            instanceGen(Service.DEALERS_AUTORU).next.copy(signals = SignalSet(Seq(signal2, signal1)))
          },
          hoboTask = {
            val autoruPremoderationDealer =
              AutoruPremoderationDealerResolution
                .newBuilder()
                .setValue(AutoruPremoderationDealerResolution.Value.VERIFY)
                .build()
            val resolution =
              ProtoHoboResolution.newBuilder
                .setVersion(1)
                .setAutoruPremoderationDealer(autoruPremoderationDealer)
                .build()
            ProtoHoboTask
              .newBuilder()
              .setVersion(1)
              .setQueue(QueueId.valueOf(task.queue))
              .setKey(task.key)
              .setResolution(resolution)
              .build
          },
          check = { case Decision(r, _) =>
            r.exists(_.isInstanceOf[ModerationRequest.AddSwitchOffs])
          },
          decider = dealersAutoruDecider
        )
      },
      TestCase(
        description = "decide to delete switchOff for AutoruComplaintsResellerResolution",
        instance = resellerInstance,
        hoboTask = resellerHoboTask1,
        check = { case Decision(r, _) =>
          r.exists(_.isInstanceOf[ModerationRequest.DeleteSwitchOffs])
        },
        decider = usersAutoruDecider
      ),
      TestCase(
        description =
          "decide to NOT delete switchOff for AutoruComplaintsResellerResolution cause hobo task finish time is older than switchOff timestamp",
        instance = instanceGen(Service.AUTORU).next,
        hoboTask = resellerHoboTask2,
        check = { case Decision(r, _) =>
          !r.exists(_.isInstanceOf[ModerationRequest.DeleteSwitchOffs])
        }
      ),
      TestCase(
        description = "decide to create offers moderation requests",
        instance = userAutoruInstance,
        hoboTask = provenOwnerHoboTask1,
        check = { case Decision(r, offersRequests) =>
          val idToVinVerdict =
            offersRequests.collect {
              case ModerationRequest.UpsertMetadata(id, Metadata.ProvenOwner(vin, verdict, _, _, _, _, _), _, _) =>
                (id, vin, verdict)
            }.toSet
          val expected =
            Set(
              (
                new ExternalId(userAutoruInstance.externalId.user, provenOwnerOfferId1),
                Some(provenOwnerVin1),
                ProvenOwnerMetadata.Verdict.PROVEN_OWNER_BAD_PHOTOS
              ),
              (
                new ExternalId(userAutoruInstance.externalId.user, provenOwnerOfferId2),
                Some(provenOwnerVin2),
                ProvenOwnerMetadata.Verdict.PROVEN_OWNER_OK
              )
            )
          idToVinVerdict == expected &&
          !r.exists(_.isInstanceOf[ModerationRequest.UpsertMetadata])
        },
        decider = usersAutoruDecider
      ),
      TestCase(
        description = "decide to create UpsertMetadata request for additional photo proven owner check",
        instance = autoruInstance.copy(metadata = MetadataSet()),
        hoboTask = provenOwnerHoboTask2,
        check = { case Decision(r, offersRequests) =>
          r.exists(_.isInstanceOf[ModerationRequest.UpsertMetadata]) &&
            !offersRequests.exists(_.isInstanceOf[ModerationRequest.UpsertMetadata])
        },
        decider = autoruDecider
      ), {
        val source = AutomaticSourceGen.next.copy(marker = NoMarker)
        val hoboSignal =
          hoboSignalGen(Service.REALTY).next.copy(
            `type` = HoboCheckType.BANNED_REVALIDATION_VISUAL,
            source = source,
            switchOff = None
          )
        val rulesHoboSignal =
          hoboSignalGen(Service.REALTY).next.copy(
            `type` = HoboCheckType.MODERATION_RULES,
            source = source,
            switchOff = None
          )
        val banSignal =
          banSignalGen(Service.REALTY).next.copy(
            source = source,
            switchOff = None
          )
        val warnSignal =
          warnSignalGen(Service.REALTY).next.copy(
            source = source,
            switchOff = None
          )
        val instance =
          instanceGen(Service.REALTY).next.copy(
            signals = SignalSet(Iterable(hoboSignal, rulesHoboSignal, banSignal, warnSignal))
          )

        TestCase(
          description = "decide to create AddSwitchOffs for all not inherited signals",
          instance = instance,
          hoboTask = realtyBannedRevalidationTask1,
          check = { case Decision(requests, _) =>
            requests
              .exists {
                case r: ModerationRequest.AddSwitchOffs => r.signalSwitchOffSources.length == 4
                case _                                  => false
              }
          },
          decider = realtyDecider
        )
      }, {
        val source = AutomaticSourceGen.next.copy(marker = InheritedSourceMarkerGen.next)
        val banSignal =
          banSignalGen(Service.REALTY).next.copy(
            source = source,
            switchOff = None
          )
        val instance = instanceGen(Service.REALTY).next.copy(signals = SignalSet(Iterable(banSignal)))

        TestCase(
          description = "decide not to create AddSwitchOffs for inherited ban signals",
          instance = instance,
          hoboTask = realtyBannedRevalidationTask1,
          check = { case Decision(r, _) =>
            !r.exists(_.isInstanceOf[ModerationRequest.AddSwitchOffs])
          },
          decider = realtyDecider
        )
      }, {
        val source = AutomaticSourceGen.next.copy(marker = NoMarker)
        val banSignal =
          banSignalGen(Service.REALTY).next.copy(
            source = source,
            switchOff = None
          )
        val instance = instanceGen(Service.REALTY).next.copy(signals = SignalSet(Iterable(banSignal)))

        TestCase(
          description = "decide not to create AddSwitchOffs for task without unban resolution",
          instance = instance,
          hoboTask = realtyBannedRevalidationTask2,
          check = { case Decision(r, _) =>
            !r.exists(_.isInstanceOf[ModerationRequest.AddSwitchOffs])
          },
          decider = realtyDecider
        )
      }
    )

  "HoboResolutionDeciderImpl.decide" should {
    testCases.foreach { case TestCase(description, instance, hoboTask, check, decider) =>
      description in {
        check(decider.decide(instance, hoboTask, DateTimeGen.next, IntGen.next)) shouldBe true
      }
    }
  }
}

object HoboResolutionDeciderIntegrationSpec {

  implicit def stringToStringValue(s: String): protobuf.StringValue =
    protobuf.StringValue.newBuilder().setValue(s).build()

}
