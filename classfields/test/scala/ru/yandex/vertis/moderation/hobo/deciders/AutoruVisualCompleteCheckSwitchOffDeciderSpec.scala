package ru.yandex.vertis.moderation.hobo.deciders

import org.junit.runner.RunWith
import org.scalatest.Inside
import org.scalatest.junit.JUnitRunner
import ru.yandex.extdata.core.gens.Producer.generatorAsProducer
import ru.yandex.vertis.hobo.proto.Model.{AutoruVisualResolution, QueueId, Task => ProtoHoboTask}
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.deciders.AutoruVisualCompleteCheckSwitchOffDeciderSpec.createHoboTask
import ru.yandex.vertis.moderation.hobo.deciders.HoboGenerators._
import ru.yandex.vertis.moderation.model.ModerationRequest.AddSwitchOffs
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  instanceGen,
  BanSignalGen,
  HoboSignalGen,
  InheritedSourceMarkerGen,
  SourceGen,
  SourceWithoutMarkerGen
}
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service}
import ru.yandex.vertis.moderation.util.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class AutoruVisualCompleteCheckSwitchOffDeciderSpec extends SpecBase with Inside {

  private val decider = new AutoruVisualCompleteCheckSwitchOffDecider

  "AutoruVisualCompleteCheckSwitchOffDecider" should {

    "decide to switch off ban signal with ts earlier than task start time" in {
      val resolution =
        AutoruVisualResolutionGen.next.setCompleteCheck(AutoruVisualResolution.CompleteCheck.COMPLETE_CHECK_OK)
      val task = createHoboTask(resolution)
      val banSignal =
        BanSignalGen.next.copy(
          switchOff = None,
          timestamp = DateTimeUtil.fromMillis(2),
          source = SourceWithoutMarkerGen.next
        )
      val instance = instanceGen(Service.AUTORU).next.copy(signals = SignalSet(banSignal))
      val ts = DateTimeUtil.now

      val decision = decider.decide(instance, task, ts, 1)

      decision.offersRequests shouldBe Seq.empty
      decision.requests should have size 1

      inside(decision.requests.head) { case AddSwitchOffs(_, switchOffSources, _, _) =>
        switchOffSources should have size 1
        switchOffSources.head.key shouldBe banSignal.key
      }
    }

    "do nothing if ban signal timestamp is after task's start time" in {
      val resolution =
        AutoruVisualResolutionGen.next.setCompleteCheck(AutoruVisualResolution.CompleteCheck.COMPLETE_CHECK_OK)
      val task = createHoboTask(resolution)
      val banSignal =
        BanSignalGen.next.copy(
          switchOff = None,
          timestamp = DateTimeUtil.fromMillis(7),
          source = SourceWithoutMarkerGen.next
        )
      val instance = instanceGen(Service.AUTORU).next.copy(signals = SignalSet(banSignal))
      val ts = DateTimeUtil.now

      val decision = decider.decide(instance, task, ts, 1)

      decision.offersRequests shouldBe Seq.empty
      decision.requests should have size 0
    }

    "do nothing if inherited ban signal timestamp is before task's start time" in {
      val resolution =
        AutoruVisualResolutionGen.next.setCompleteCheck(AutoruVisualResolution.CompleteCheck.COMPLETE_CHECK_OK)
      val task = createHoboTask(resolution)
      val banSignal =
        BanSignalGen.next.copy(
          switchOff = None,
          timestamp = DateTimeUtil.fromMillis(7),
          source = SourceGen.map(_.withMarker(InheritedSourceMarkerGen.next)).next
        )
      val instance = instanceGen(Service.AUTORU).next.copy(signals = SignalSet(banSignal))
      val ts = DateTimeUtil.now

      val decision = decider.decide(instance, task, ts, 1)

      decision.offersRequests shouldBe Seq.empty
      decision.requests shouldBe Seq.empty
    }

    "do nothing if task is NOT from AUTO_RU_BANNED_REVALIDATION_VISUAL queue" in {
      val resolution =
        AutoruVisualResolutionGen.next.setCompleteCheck(AutoruVisualResolution.CompleteCheck.COMPLETE_CHECK_OK)
      val task = createHoboTask(resolution, QueueId.AUTO_RU_WARNED_REVALIDATION_VISUAL)
      val banSignal =
        BanSignalGen.next.copy(
          switchOff = None,
          timestamp = DateTimeUtil.fromMillis(2),
          source = SourceWithoutMarkerGen.next
        )
      val instance = instanceGen(Service.AUTORU).next.copy(signals = SignalSet(banSignal))
      val ts = DateTimeUtil.now

      val decision = decider.decide(instance, task, ts, 1)

      decision.offersRequests shouldBe Seq.empty
      decision.requests shouldBe Seq.empty
    }

    "do nothing if COMPLETE_CHECK_FAILED" in {
      val resolution =
        AutoruVisualResolutionGen.next.setCompleteCheck(AutoruVisualResolution.CompleteCheck.COMPLETE_CHECK_FAILED)
      val task = createHoboTask(resolution)
      val banSignal =
        BanSignalGen.next.copy(
          switchOff = None,
          timestamp = DateTimeUtil.fromMillis(2),
          source = SourceWithoutMarkerGen.next
        )
      val instance = instanceGen(Service.AUTORU).next.copy(signals = SignalSet(banSignal))
      val ts = DateTimeUtil.now

      val decision = decider.decide(instance, task, ts, 1)

      decision.offersRequests shouldBe Seq.empty
      decision.requests shouldBe Seq.empty
    }

    "decide to switch off hobo signal with HoboCheckType != PROVEN_OWNER with ts earlier than task start time" in {
      val resolution =
        AutoruVisualResolutionGen.next.setCompleteCheck(AutoruVisualResolution.CompleteCheck.COMPLETE_CHECK_OK)
      val task = createHoboTask(resolution)
      val hoboSignal =
        HoboSignalGen.next.copy(
          switchOff = None,
          timestamp = DateTimeUtil.fromMillis(2),
          source = SourceWithoutMarkerGen.next,
          `type` = HoboCheckType.COMPLAINTS
        )
      val instance = instanceGen(Service.AUTORU).next.copy(signals = SignalSet(hoboSignal))
      val ts = DateTimeUtil.now

      val decision = decider.decide(instance, task, ts, 1)

      decision.offersRequests shouldBe Seq.empty
      decision.requests should have size 1

      inside(decision.requests.head) { case AddSwitchOffs(_, switchOffSources, _, _) =>
        switchOffSources should have size 1
        switchOffSources.head.key shouldBe hoboSignal.key
      }
    }

    "do nothing in case of hobo signal with HoboCheckType == PROVEN_OWNER with ts earlier than task start time" in {
      val resolution =
        AutoruVisualResolutionGen.next.setCompleteCheck(AutoruVisualResolution.CompleteCheck.COMPLETE_CHECK_OK)
      val task = createHoboTask(resolution)
      val hoboSignal =
        HoboSignalGen.next.copy(
          switchOff = None,
          timestamp = DateTimeUtil.fromMillis(2),
          source = SourceWithoutMarkerGen.next,
          `type` = HoboCheckType.PROVEN_OWNER
        )
      val instance = instanceGen(Service.AUTORU).next.copy(signals = SignalSet(hoboSignal))
      val ts = DateTimeUtil.now

      val decision = decider.decide(instance, task, ts, 1)

      decision.offersRequests shouldBe Seq.empty
      decision.requests shouldBe Seq.empty
    }
  }

}

object AutoruVisualCompleteCheckSwitchOffDeciderSpec {

  private def createHoboTask(resolution: AutoruVisualResolution.Builder,
                             queueId: QueueId = QueueId.AUTO_RU_BANNED_REVALIDATION_VISUAL
                            ): ProtoHoboTask =
    HoboTaskGen.next
      .setQueue(queueId)
      .setResolution(resolution)
      .setStartTime(5)
      .build
}
