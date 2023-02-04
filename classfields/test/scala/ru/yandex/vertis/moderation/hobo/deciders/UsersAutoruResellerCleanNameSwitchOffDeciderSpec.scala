package ru.yandex.vertis.moderation.hobo.deciders

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.Inside
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.hobo.proto.Model.AutoruResellerCleanNameResolution.Value
import ru.yandex.vertis.hobo.proto.Model.AutoruResellerCleanNameResolution.Value.{Verdict => ResolutionVerdict}
import ru.yandex.vertis.hobo.proto.Model.{AutoruResellerCleanNameResolution, QueueId, Task => HoboTask}
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.HoboResolutionDecider.Decision
import ru.yandex.vertis.moderation.hobo.deciders.HoboGenerators._
import ru.yandex.vertis.moderation.model.DetailedReason.UserReseller
import ru.yandex.vertis.moderation.model.Domain.UsersAutoru
import ru.yandex.vertis.moderation.model.ModerationRequest.AddSwitchOffs
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  banSignalGen,
  essentialsGen,
  hoboSignalGen,
  instanceGen,
  warnSignalGen
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal.{AutomaticSource, ManualSource, Signal, SignalSet}
import ru.yandex.vertis.moderation.proto.Model.Domain.{UsersAutoru => UsersAutoruDomains}
import ru.yandex.vertis.moderation.proto.Model.Service

import scala.jdk.CollectionConverters.seqAsJavaListConverter

@RunWith(classOf[JUnitRunner])
class UsersAutoruResellerCleanNameSwitchOffDeciderSpec extends SpecBase with Inside {

  import ru.yandex.vertis.moderation.hobo.deciders.UsersAutoruResellerCleanNameSwitchOffDeciderSpec._

  private val decider = new UsersAutoruResellerCleanNameSwitchOffDecider

  private val provenOwnerOk =
    AutoruResellerCleanNameResolutionValueGen.next.setVerdict(ResolutionVerdict.CLEAN_NAME_PROVEN_OWNER_OK)
  private val additionalDocs =
    AutoruResellerCleanNameResolutionValueGen.next.setVerdict(ResolutionVerdict.CLEAN_NAME_ADDITIONAL_DOCS)
  private val returnQuota =
    AutoruResellerCleanNameResolutionValueGen.next.setVerdict(ResolutionVerdict.CLEAN_NAME_RETURN_QUOTA)
  private val warnSignal =
    warnSignalGen(Service.USERS_AUTORU).withUserResellerReason.withoutSwitchOff.withAutomaticSource.next
  private val warnSignalWithSwitchOff = warnSignalGen(Service.USERS_AUTORU).withUserResellerReason.withSwitchOff.next
  private val banSignal =
    banSignalGen(Service.USERS_AUTORU).withUserResellerReason.withoutSwitchOff.withManualSource.next
  private val banSignalWithSwitchOff =
    banSignalGen(Service.USERS_AUTORU).withUserResellerReason.withAutomaticSource.withSwitchOff.next
  private val hoboSignal = hoboSignalGen(Service.USERS_AUTORU).withUserResellerReason.withoutSwitchOff.next
  private val hoboSignalWithSwitchOff = hoboSignalGen(Service.USERS_AUTORU).withUserResellerReason.withSwitchOff.next
  private val banSignalWithDomainCars =
    banSignalGen(Service.USERS_AUTORU).withUserResellerReason.withoutSwitchOff
      .withDomains(Seq("CARS", "MOTORCYCLE", "LCV"))
      .withAutomaticSource
      .next
  private val instance = instanceGen(essentialsGen(Service.USERS_AUTORU)).next
  private val timestamp = DateTime.now

  "UsersAutoruResellerCleanNameSwitchOffDecider" should {
    "does not touch task with no resolution" in {
      val hoboTask = createHoboTask()
      val decision = decider.decide(instance, hoboTask, timestamp, 1)
      decision shouldBe Decision.Empty
    }

    "ignores non CLEAN_NAME_RETURN_QUOTA verdicts" in {
      val hoboTask = createHoboTask(provenOwnerOk, additionalDocs)
      val decision = decider.decide(instance, hoboTask, timestamp, 1)
      decision shouldBe Decision.Empty
    }

    "ignores warn signal with switch off" in {
      val signals = SignalSet(warnSignalWithSwitchOff)
      val hoboTask = createHoboTask(returnQuota)
      val decision = decider.decide(instance.copy(signals = signals), hoboTask, timestamp, 1)
      decision shouldBe Decision.Empty
    }

    "ignores ban signal with switch off" in {
      val signals = SignalSet(banSignalWithSwitchOff)
      val hoboTask = createHoboTask(returnQuota)
      val decision = decider.decide(instance.copy(signals = signals), hoboTask, timestamp, 1)
      decision shouldBe Decision.Empty
    }

    "ignores hobo signal with switch off" in {
      val signals = SignalSet(hoboSignalWithSwitchOff)
      val hoboTask = createHoboTask(returnQuota)
      val decision = decider.decide(instance.copy(signals = signals), hoboTask, timestamp, 1)
      decision shouldBe Decision.Empty
    }

    "sets switch-off for every ban/warn signal with USER_RESELLER reason without switch-off" in {
      val signals =
        SignalSet(
          banSignal,
          banSignalWithSwitchOff,
          hoboSignal,
          hoboSignalWithSwitchOff,
          warnSignal,
          warnSignalWithSwitchOff
        )
      val inst = instance.copy(signals = signals)
      val hoboTask = createHoboTask(returnQuota)
      val decision = decider.decide(inst, hoboTask, timestamp, 1)

      decision.requests should have size 1

      inside(decision.requests.head) { case AddSwitchOffs(externalId, sources, `timestamp`, 1) =>
        externalId shouldBe inst.externalId
        sources should have size 3
        val expectedKeys = Seq(banSignal, hoboSignal, warnSignal).map(_.key)
        val actualKeys = sources.map(_.key)
        actualKeys should contain theSameElementsAs expectedKeys

        sources.find(_.key == banSignal.key).get.source shouldBe a[ManualSource]
        sources.find(_.key == warnSignal.key).get.source shouldBe ModerationSource
      }
      decision.offersRequests shouldBe Seq.empty
    }
    "set switchoff signals with resolution CLEAN_NAME_RETURN_QUOTA by domain" in {
      val banSignalLCV = banSignalWithDomainCars.copy(domain = UsersAutoru(UsersAutoruDomains.LCV))
      val banSignalCars = banSignalWithDomainCars.copy(domain = UsersAutoru(UsersAutoruDomains.CARS))
      val signals =
        SignalSet(
          banSignalLCV,
          banSignalCars
        )
      val inst = instance.copy(signals = signals)
      val hoboTask = createHoboTask(returnQuotaWithDomains(Seq(UsersAutoruDomains.CARS)))
      val decision = decider.decide(inst, hoboTask, timestamp, 1)

      inside(decision.requests.head) { case AddSwitchOffs(externalId, sources, `timestamp`, 1) =>
        externalId shouldBe inst.externalId
        sources should have size 1
        val expectedKeys = Seq(banSignalCars).map(_.key)
        val actualKeys = sources.map(_.key)
        actualKeys should contain theSameElementsAs expectedKeys
      }
      decision.offersRequests shouldBe Seq.empty
      decision.requests should have size 1
    }

    "sets switch off-signals with resolution CLEAN_NAME_RETURN_QUOTA when hobo has no domain" in {
      val signals =
        SignalSet(
          banSignalWithDomainCars,
          banSignal
        )
      val inst = instance.copy(signals = signals)
      val hoboTask = createHoboTask(returnQuotaWithDomains(Seq.empty))
      val decision = decider.decide(inst, hoboTask, timestamp, 1)
      inside(decision.requests.head) { case AddSwitchOffs(externalId, sources, `timestamp`, 1) =>
        externalId shouldBe inst.externalId
        sources should have size 2
        val expectedKeys = Seq(banSignalWithDomainCars, banSignal).map(_.key)
        val actualKeys = sources.map(_.key)
        actualKeys should contain theSameElementsAs expectedKeys

      }
      decision.offersRequests shouldBe Seq.empty
      decision.requests should have size 1
    }

    "doesn't set switch-offs  with resolution CLEAN_NAME_RETURN_QUOTA when hobo has different domains" in {
      val signals =
        SignalSet(
          banSignalWithDomainCars.copy(domain = UsersAutoru(UsersAutoruDomains.LCV), switchOff = None),
          banSignalWithDomainCars.copy(domain = UsersAutoru(UsersAutoruDomains.MOTORCYCLE), switchOff = None)
        )
      val inst = instance.copy(signals = signals)
      val hoboTask = createHoboTask(returnQuotaWithDomains(Seq(UsersAutoruDomains.CARS)))
      val decision = decider.decide(inst, hoboTask, timestamp, 1)
      decision.offersRequests shouldBe Seq.empty
      decision.requests shouldBe Seq.empty
    }

  }
}

object UsersAutoruResellerCleanNameSwitchOffDeciderSpec {
  implicit class RichSignalGen[S <: Signal](val gen: Gen[S]) extends AnyVal {
    def withUserResellerReason: Gen[S] =
      gen.suchThat { signal =>
        signal.getDetailedReasons.exists {
          case _: UserReseller => true
          case _               => false
        }
      }

    def withSwitchOff: Gen[S] = gen.suchThat(_.switchOff.isDefined)

    def withoutSwitchOff: Gen[S] = gen.suchThat(_.switchOff.isEmpty)

    def withAutomaticSource: Gen[S] = gen.suchThat(_.source.isInstanceOf[AutomaticSource])

    def withManualSource: Gen[S] = gen.suchThat(_.source.isInstanceOf[ManualSource])

    def withDomains(domains: Seq[String]): Gen[S] = gen.suchThat(signal => domains.contains(signal.domain.key))
  }

  private def createHoboTask(values: AutoruResellerCleanNameResolution.Value.Builder*): HoboTask = {
    val resolution = AutoruResellerCleanNameResolutionGen.next.clearValues()
    values.foreach(resolution.addValues)

    HoboTaskGen.next
      .setQueue(QueueId.AUTO_RU_RESELLER_CLEAN_NAME)
      .setResolution(resolution)
      .build
  }

  private def returnQuotaWithDomains(usersAutoruDomains: Seq[UsersAutoruDomains]): Value.Builder = {
    AutoruResellerCleanNameResolutionValueGen.next
      .setVerdict(ResolutionVerdict.CLEAN_NAME_RETURN_QUOTA)
      .addAllDomains(usersAutoruDomains.asJava)
  }
}
