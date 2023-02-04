package ru.yandex.vertis.moderation.hobo.decider

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.{Globals, SpecBase}
import ru.yandex.vertis.moderation.dao.SearchInstanceDao
import ru.yandex.vertis.moderation.feature.EmptyFeatureRegistry
import ru.yandex.vertis.moderation.hobo.decider.HoboDecider.Verdict
import ru.yandex.vertis.moderation.hobo.decider.UserResellerHoboDecider.{MainRegionsCheckType, OtherRegionsCheckType}
import ru.yandex.vertis.moderation.model.DetailedReason
import ru.yandex.vertis.moderation.model.ModerationRequest.InitialDepth
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Diff, Essentials, ExternalId, Instance, InstanceIdImpl}
import ru.yandex.vertis.moderation.model.signal.{BanSignal, SignalFactory, SignalSet}
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service}
import ru.yandex.vertis.moderation.searcher.core.saas.search.AutoruSearchQuery
import ru.yandex.vertis.moderation.searcher.core.util.Range
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.util.DetailedReasonUtil.isUserResellerDetailedReason

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author mpoplavkov
  */
@RunWith(classOf[JUnitRunner])
class UserResellerHoboDeciderSpec extends SpecBase {

  private val searchInstanceDao = mock[SearchInstanceDao]
  implicit private val featureRegistry: EmptyFeatureRegistry.type = EmptyFeatureRegistry
  private val decider: HoboDecider = new UserResellerHoboDecider(searchInstanceDao)

  private val service = Service.USERS_AUTORU
  private val essentials = Essentials.empty(service)
  private val user = UserGen.next
  private val instanceId = InstanceIdImpl(ExternalId.fromUser(user)).toId
  private val hoboSignalMain =
    SignalFactory.newSignal(
      UserResellerHoboDecider.buildHoboSignalSource(service, MainRegionsCheckType),
      DateTimeUtil.now()
    )
  private val hoboSignalKeyMain = hoboSignalMain.key
  private val hoboSignalOther =
    SignalFactory.newSignal(
      UserResellerHoboDecider.buildHoboSignalSource(service, OtherRegionsCheckType),
      DateTimeUtil.now()
    )
  private val hoboSignalKeyOther = hoboSignalOther.key
  private val instance =
    InstanceGen
      .suchThat(i => !(i.signals.toMap.contains(hoboSignalKeyMain) || i.signals.toMap.contains(hoboSignalKeyOther)))
      .suchThat(!_.signals.exists(_.getDetailedReasons.exists(isUserResellerDetailedReason)))
      .next
      .copy(id = instanceId, essentials = essentials)
  private val userResellerBanSignal: BanSignal =
    BanSignalGen.next.copy(detailedReason = DetailedReason.UserReseller(None, Seq.empty), info = None, switchOff = None)
  private val mainSearchQuery =
    AutoruSearchQuery(
      userId = Seq(user.key),
      geobaseId = UserResellerHoboDecider.DefaultMainGeobaseIds,
      priceInfoRub = Some(Range(Some(UserResellerHoboDecider.MinPriceMainRegions), None))
    )
  private val othersSearchQuery =
    AutoruSearchQuery(
      userId = Seq(user.key),
      geobaseId = UserResellerHoboDecider.DefaultOtherGeobaseIds,
      priceInfoRub = Some(Range(Some(UserResellerHoboDecider.MinPriceOtherRegions), None))
    )
  private val defaultBadSignals = instance.signals ++ SignalSet(userResellerBanSignal)
  private val defaultBadInstance = instance.copy(signals = defaultBadSignals)
  private val defaultSource = sourceFromInstance(defaultBadInstance)

  "UserResellerHoboDecider" should {
    "decide to create a hobo task in AUTO_RU_ACCOUNTS_RESELLERS_CALL if instance is suitable for it" in {
      doReturn(Future.successful(1L)).when(searchInstanceDao).count(mainSearchQuery)
      doReturn(Future.successful(0L)).when(searchInstanceDao).count(othersSearchQuery)

      val verdict = decider.decide(defaultSource).futureValue.get
      isNeedCreateWithCheckType(verdict, MainRegionsCheckType) shouldBe true
    }

    "decide not to create a hobo task if it was already created" in {
      val withHoboSignals = defaultBadSignals ++ SignalSet(hoboSignalMain)
      val badInstance = instance.copy(signals = withHoboSignals)
      val source = sourceFromInstance(badInstance)

      doReturn(Future.successful(1L)).when(searchInstanceDao).count(mainSearchQuery)
      doReturn(Future.successful(1L)).when(searchInstanceDao).count(othersSearchQuery)

      decider.decide(source).futureValue shouldBe 'empty
    }

    "decide not to create a hobo task if user has no offers with appropriate geobase" in {
      doReturn(Future.successful(0L)).when(searchInstanceDao).count(mainSearchQuery)
      doReturn(Future.successful(0L)).when(searchInstanceDao).count(othersSearchQuery)

      decider.decide(defaultSource).futureValue shouldBe 'empty
    }

    "decide not to create a hobo task if user has no ban reseller signals" in {
      val source = sourceFromInstance(instance)

      doReturn(Future.successful(1L)).when(searchInstanceDao).count(mainSearchQuery)
      doReturn(Future.successful(1L)).when(searchInstanceDao).count(othersSearchQuery)

      decider.decide(source).futureValue shouldBe 'empty
    }

    "decide not to create a hobo task if user has infection ban reseller signal" in {
      val infectionInfo = Gen.oneOf(UserResellerHoboDecider.InfectionInfos.toSeq).next
      val infectionSignal = userResellerBanSignal.copy(info = Some(infectionInfo))
      val notBadSignals = instance.signals ++ SignalSet(infectionSignal)
      val badInstance = instance.copy(signals = notBadSignals)
      val source = sourceFromInstance(badInstance)

      doReturn(Future.successful(1L)).when(searchInstanceDao).count(mainSearchQuery)
      doReturn(Future.successful(1L)).when(searchInstanceDao).count(othersSearchQuery)

      decider.decide(source).futureValue shouldBe 'empty
    }

    "decide to create a hobo task in AUTO_RU_REGION_ACCOUNTS_RESELLERS_CALL" in {
      doReturn(Future.successful(0L)).when(searchInstanceDao).count(mainSearchQuery)
      doReturn(Future.successful(1L)).when(searchInstanceDao).count(othersSearchQuery)

      val verdict = decider.decide(defaultSource).futureValue.get
      isNeedCreateWithCheckType(verdict, OtherRegionsCheckType) shouldBe true
    }

    "decide to create a hobo task in AUTO_RU_ACCOUNTS_RESELLERS_CALL if instance is suitable for both queues" in {
      doReturn(Future.successful(1L)).when(searchInstanceDao).count(mainSearchQuery)
      doReturn(Future.successful(1L)).when(searchInstanceDao).count(othersSearchQuery)

      val verdict = decider.decide(defaultSource).futureValue.get
      isNeedCreateWithCheckType(verdict, MainRegionsCheckType) shouldBe true
    }
  }

  private def sourceFromInstance(instance: Instance): HoboDecider.Source =
    HoboDecider.Source(
      instance = instance,
      prev = None,
      diff = Diff.empty(service),
      calculator = Globals.opinionCalculator(service),
      timestamp = DateTime.now,
      depth = InitialDepth,
      tag = None
    )

  private def isNeedCreateWithCheckType(verdict: Verdict, checkType: HoboCheckType): Boolean =
    verdict match {
      case HoboDecider.NeedCreate(request) => request.hoboSignalSource.`type` == checkType
      case _                               => false
    }
}
