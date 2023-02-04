package ru.yandex.vertis.moderation.hobo.deciders

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.Inside
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.hobo.proto.Model._
import ru.yandex.vertis.hobo.proto.Common.AutoruProvenOwnerResolution
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.deciders.HoboGenerators._
import ru.yandex.vertis.moderation.model.ModerationRequest.{AppendSignals, RemoveSignals}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  instanceGen,
  DateTimeGen,
  DomainAutoruGen,
  HoboSignalGen,
  HoboSignalTaskGen,
  InstanceGen,
  ManualSourceGen,
  NoSourceMarkerGen
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal.{AutomaticSource, HoboSignal, HoboSignalSourceInternal, SignalSet}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service}

@RunWith(classOf[JUnitRunner])
class UpdateHoboSignalsDeciderSpec extends SpecBase with Inside {
  private val decider = new UpdateHoboSignalsDecider(Service.AUTORU)

  private val TrueFalse = () => HoboTaskGen.next.setResolution(TrueFalseResolutionGen.next)
  private val CallCenter = () => HoboTaskGen.next.setResolution(CallCenterResolutionGen.next)
  private val RealtyVisual = () => HoboTaskGen.next.setResolution(RealtyVisualResolutionGen.next)
  private val ResellerCall = () => HoboTaskGen.next.setResolution(ResellersCallResolutionGen.next)
  private val StoCall = () => HoboTaskGen.next.setResolution(StoCallResolutionGen.next)
  private val AutoruVisual = () => HoboTaskGen.next.setResolution(AutoruVisualResolutionGen.next)
  private val AutoruCall = () => HoboTaskGen.next.setResolution(AutoruCallResolutionGen.next)
  private val StoComplaints = () => HoboTaskGen.next.setResolution(StoComplaintsResolutionGen.next)
  private val SuspiciousCall = () => HoboTaskGen.next.setResolution(SuspiciousCallResolutionGen.next)
  private val PaidCall = () => HoboTaskGen.next.setResolution(PaidCallResolutionGen.next)
  private val AutoruComplaintsReseller =
    () => HoboTaskGen.next.setResolution(AutoruComplaintsResellerResolutionGen.next)
  private val RedirectCheck = () => HoboTaskGen.next.setResolution(RedirectCheckResolutionGen.next)
  private val AutoruReviews = () => HoboTaskGen.next.setResolution(AutoruReviewsResolutionGen.next)
  private val AutoruPurchasing = () => HoboTaskGen.next.setResolution(AutoruPurchasingResolutionGen.next)
  private val TeleponyMarking = () => HoboTaskGen.next.setResolution(TeleponyMarkingResolutionGen.next)
  private val RealtyComplaintsReseller =
    () => HoboTaskGen.next.setResolution(RealtyComplaintsResellerResolutionGen.next)
  private val CallCenterPhoto = () => HoboTaskGen.next.setResolution(CallCenterPhotoResolutionGen.next)
  private val SpamCall = () => HoboTaskGen.next.setResolution(SpamCallResolutionGen.next)
  private val AutoruTamagotchi = () => HoboTaskGen.next.setResolution(AutoruTamagotchiResolutionGen.next)
  private val AutoruLoyaltyDealers = () => HoboTaskGen.next.setResolution(AutoruLoyaltyDealersResolutionGen.next)
  private val VinReport = () => HoboTaskGen.next.setResolution(VinReportResolutionGen.next)
  private val Callgate = () => HoboTaskGen.next.setResolution(CallgateResolutionGen.next)
  private val AutoruPremoderationDealer =
    () => HoboTaskGen.next.setResolution(AutoruPremoderationDealerResolutionGen.next)
  private val RealtyAgencyCardsCheck = () => HoboTaskGen.next.setResolution(RealtyAgencyCardsCheckResolutionGen.next)
  private val AutoruAnalyticUsersCheck =
    () => HoboTaskGen.next.setResolution(AutoruAnalyticUsersCheckResolutionGen.next)
  private val AutoruProvenOwner = () => HoboTaskGen.next.setResolution(AutoruProvenOwnerResolutionGen.next)
  private val AutoruResellersRevalidation =
    () => HoboTaskGen.next.setResolution(AutoruResellersRevalidationResolutionGen.next)
  private val RealtySuspiciousUsers = () => HoboTaskGen.next.setResolution(RealtySuspiciousUsersResolutionGen.next)
  private val AutoruCallMarking = () => HoboTaskGen.next.setResolution(AutoruCallMarkingResolutionGen.next)
  private val RealtyChatSuspiciousCheck =
    () => HoboTaskGen.next.setResolution(RealtyChatSuspiciousCheckResolutionGen.next)
  private val AutoruResellerCleanName = () => HoboTaskGen.next.setResolution(AutoruResellerCleanNameResolutionGen.next)

  private val AllTaskGenerators: Map[Class[_], () => Task.Builder] =
    Map(
      classOf[TrueFalseResolution] -> TrueFalse,
      classOf[CallCenterResolution] -> CallCenter,
      classOf[RealtyVisualResolution] -> RealtyVisual,
      classOf[ResellersCallResolution] -> ResellerCall,
      classOf[StoCallResolution] -> StoCall,
      classOf[AutoruVisualResolution] -> AutoruVisual,
      classOf[AutoruCallResolution] -> AutoruCall,
      classOf[StoComplaintsResolution] -> StoComplaints,
      classOf[SuspiciousCallResolution] -> SuspiciousCall,
      classOf[PaidCallResolution] -> PaidCall,
      classOf[AutoruComplaintsResellerResolution] -> AutoruComplaintsReseller,
      classOf[RedirectCheckResolution] -> RedirectCheck,
      classOf[AutoruReviewsResolution] -> AutoruReviews,
      classOf[AutoruPurchasingResolution] -> AutoruPurchasing,
      classOf[TeleponyMarkingResolution] -> TeleponyMarking,
      classOf[RealtyComplaintsResellerResolution] -> RealtyComplaintsReseller,
      classOf[CallCenterPhotoResolution] -> CallCenterPhoto,
      classOf[SpamCallResolution] -> SpamCall,
      classOf[AutoruTamagotchiResolution] -> AutoruTamagotchi,
      classOf[AutoruLoyaltyDealersResolution] -> AutoruLoyaltyDealers,
      classOf[VinReportResolution] -> VinReport,
      classOf[CallgateResolution] -> Callgate,
      classOf[AutoruPremoderationDealerResolution] -> AutoruPremoderationDealer,
      classOf[RealtyAgencyCardsCheckResolution] -> RealtyAgencyCardsCheck,
      classOf[AutoruAnalyticUsersCheckResolution] -> AutoruAnalyticUsersCheck,
      classOf[AutoruProvenOwnerResolution] -> AutoruProvenOwner,
      classOf[AutoruResellersRevalidationResolution] -> AutoruResellersRevalidation,
      classOf[RealtySuspiciousUsersResolution] -> RealtySuspiciousUsers,
      classOf[AutoruCallMarkingResolution] -> AutoruCallMarking,
      classOf[RealtyChatSuspiciousCheckResolution] -> RealtyChatSuspiciousCheck,
      classOf[AutoruResellerCleanNameResolution] -> AutoruResellerCleanName
    )

  "UpdateSignalsDecider.decide" should {

    AllTaskGenerators.foreach { case (clazz, taskGenerator) =>
      s"survives ${clazz.getSimpleName}" in {
        val instance = InstanceGen.next
        val hoboTask = taskGenerator.apply.build
        noException should be thrownBy decider.decide(instance, hoboTask, DateTime.now, 1)
      }
    }

    "happy path" in {
      // GIVEN
      val task = HoboSignalTaskGen.next
      val signal1: HoboSignal =
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
      val instance = instanceGen(Service.AUTORU).next.copy(signals = SignalSet(Seq(signal1, signal2)))
      val hoboTask =
        HoboTaskGen.next
          .setKey(task.key)
          .setQueue(QueueId.valueOf(task.queue))
          .build
      val timestamp = DateTime.now

      // WHEN
      val decision = decider.decide(instance, hoboTask, timestamp, 1)

      // THEN
      decision.offersRequests shouldBe Seq.empty

      decision.requests should have size 2

      inside(decision.requests(0)) { case AppendSignals(externalId, signalSources, `timestamp`, 1, _) =>
        externalId shouldBe instance.externalId
        signalSources.head
          .asInstanceOf[HoboSignalSourceInternal]
          .`type` shouldBe HoboCheckType.WARNED_REVALIDATION_VISUAL
      }

      inside(decision.requests(1)) { case RemoveSignals(externalId, signalKeys, source, `timestamp`, 1) =>
        externalId shouldBe instance.externalId
        signalKeys shouldBe Set(signal2.key)
        source shouldBe Some(AutomaticSource(Application.HOBO))
      }
    }
  }
}
