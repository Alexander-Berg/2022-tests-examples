package ru.yandex.vertis.moderation.hobo.deciders

import org.junit.runner.RunWith
import org.scalatest.Inside
import org.scalatest.junit.JUnitRunner
import ru.yandex.extdata.core.gens.Producer.generatorAsProducer
import ru.yandex.vertis.hobo.proto.Model.{AutoruVisualResolution, QueueId, Task => ProtoHoboTask}
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.deciders.AutoruVisualCompleteCheckMetadataDeciderSpec.createHoboTask
import ru.yandex.vertis.moderation.hobo.deciders.HoboGenerators._
import ru.yandex.vertis.moderation.model.ModerationRequest.UpsertMetadata
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.instanceGen
import ru.yandex.vertis.moderation.model.meta.Metadata
import ru.yandex.vertis.moderation.proto.Model.Metadata.CompleteCheckMetadata
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class AutoruVisualCompleteCheckMetadataDeciderSpec extends SpecBase with Inside {

  private val decider = new AutoruVisualCompleteCheckMetadataDecider

  "AutoruVisualCompleteCheckMetadataDecider" should {

    "decide to upsert metadata with COMPLETE_CHECK_OK" in {
      val resolution =
        AutoruVisualResolutionGen.next.setCompleteCheck(AutoruVisualResolution.CompleteCheck.COMPLETE_CHECK_OK)
      val task = createHoboTask(resolution)
      val instance = instanceGen(Service.AUTORU).next
      val ts = DateTimeUtil.now

      val decision = decider.decide(instance, task, ts, 1)

      decision.offersRequests shouldBe Seq.empty
      decision.requests should have size 1

      inside(decision.requests.head) { case UpsertMetadata(_, Metadata.CompleteCheck(value, _, _), _, _) =>
        value shouldBe CompleteCheckMetadata.Value.COMPLETE_CHECK_OK
      }
    }

    "decide to upsert metadata with COMPLETE_CHECK_FAILED" in {
      val resolution =
        AutoruVisualResolutionGen.next.setCompleteCheck(AutoruVisualResolution.CompleteCheck.COMPLETE_CHECK_FAILED)
      val task = createHoboTask(resolution)
      val instance = instanceGen(Service.AUTORU).next
      val ts = DateTimeUtil.now

      val decision = decider.decide(instance, task, ts, 1)

      decision.offersRequests shouldBe Seq.empty
      decision.requests should have size 1

      inside(decision.requests.head) { case UpsertMetadata(_, Metadata.CompleteCheck(value, _, _), _, _) =>
        value shouldBe CompleteCheckMetadata.Value.COMPLETE_CHECK_FAILED
      }
    }

    "decide not to upsert any meta in case of another queue" in {
      val resolution =
        AutoruVisualResolutionGen.next.setCompleteCheck(AutoruVisualResolution.CompleteCheck.COMPLETE_CHECK_FAILED)
      val task = createHoboTask(resolution, QueueId.AUTO_RU_WARNED_REVALIDATION_VISUAL)
      val instance = instanceGen(Service.AUTORU).next
      val ts = DateTimeUtil.now

      val decision = decider.decide(instance, task, ts, 1)

      decision.offersRequests shouldBe Seq.empty
      decision.requests shouldBe Seq.empty
    }

    "decide not to upsert any meta in case of another resolution" in {
      val resolution = AutoruCallResolutionGen.next
      val task =
        HoboTaskGen.next
          .setResolution(resolution)
          .build
      val instance = instanceGen(Service.AUTORU).next
      val ts = DateTimeUtil.now

      val decision = decider.decide(instance, task, ts, 1)

      decision.offersRequests shouldBe Seq.empty
      decision.requests shouldBe Seq.empty
    }
  }
}

object AutoruVisualCompleteCheckMetadataDeciderSpec {

  private def createHoboTask(resolution: AutoruVisualResolution.Builder,
                             queueId: QueueId = QueueId.AUTO_RU_BANNED_REVALIDATION_VISUAL
                            ): ProtoHoboTask =
    HoboTaskGen.next
      .setQueue(queueId)
      .setResolution(resolution)
      .build
}
