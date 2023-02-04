package ru.yandex.vertis.moderation.model.signal

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.moderation.model.autoru.{ExperimentInfo, RichCategory}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.instance.Essentials
import ru.yandex.vertis.moderation.model.signal.InheritedSignalFilteringPolicySpec._
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain}
import ru.yandex.vertis.moderation.opinion.InheritedSignalFilteringPolicy
import ru.yandex.vertis.moderation.opinion.SignalFilteringPolicy.isUniversal
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Category
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.DetailedReasonUtil.isUserResellerDetailedReason
import ru.yandex.vertis.moderation.{Globals, SpecBase}

/**
  * Specs for [[ru.yandex.vertis.moderation.opinion.InheritedSignalFilteringPolicy]]
  *
  * @author sunlight
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class InheritedSignalFilteringPolicySpec extends SpecBase with PropertyChecks {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1000)

  private val FilteringPolicy = InheritedSignalFilteringPolicy

  "InheritedSignalFilteringPolicy" should {

    "pass any signal with NoMarker and correct domain" in {
      forAll(SignalWithNoMarkerGen) { signal =>
        FilteringPolicy(signal, signal.domain, EssentialsGen.next) should be(true)
      }
    }
    "skip any signal with NoMarker and mismatch on domains, not universal" in {
      forAll(SignalWithNoMarkerGen) { signal =>
        val essentials = EssentialsGen.next
        implicit val service = Essentials.getService(essentials)
        val signal2 = SignalWithNoMarkerGen.suchThat(s => s.domain != signal.domain && s.isNotUniversal).next
        FilteringPolicy(signal2, signal.domain, EssentialsGen.next) should be(false)
      }
    }
  }

  "InheritedSignalFilteringPolicy for Autoru users" should {

    implicit val service: Service = Service.USERS_AUTORU

    "pass any USERS_AUTORU signal with correct domain and essentials" in {
      forAll(SignalWithNoMarkerGen) { signal =>
        val domain = UserAutoruDomains.next
        val signalWithDomain = signal.withDomain(domain)
        val essentials = UserAutoruEssentialsGen.next
        FilteringPolicy(
          signalWithDomain,
          domain,
          essentials
        ) should be(true)
      }
    }
    "pass any USERS_AUTORU signal with universal reasons" in {
      forAll(SignalWithNoMarkerGen.suchThat(_.getDetailedReasons.nonEmpty)) { signal =>
        val domain = UserAutoruDomains.next
        val withUniversal = signal.withDomain(domain).withDetailedReasons(universalReasonsSetGen(service).next)
        val essentials = UserAutoruEssentialsGen.next
        FilteringPolicy(
          withUniversal,
          domain,
          essentials
        ) should be(true)
      }
    }
    "skip any USERS_AUTORU signal with mismatch domain" in {
      forAll(SignalWithNoMarkerGen.suchThat(_.isNotUniversal)) { signal =>
        val domain = UserAutoruDomains.next
        val domain2 = UserAutoruDomains.suchThat(_ != domain).next
        val signalWithDomain = signal.withDomain(domain)
        val essentials = UserAutoruEssentialsGen.next
        FilteringPolicy(
          signalWithDomain,
          domain2,
          essentials
        ) should be(false)
      }
    }

  }

  "InheritedSignalFilteringPolicy for Autoru" should {

    implicit val service: Service = Service.AUTORU

    "pass any inherited USERS_AUTORU signal with correct domain and essentials" in {
      forAll(SignalInheritedUsersAutoru) { signal =>
        val category = AutoruCategoryGen.next
        val signalWithDomain = signal.withDomain(category.toUserDomain)
        val essentials =
          AutoruEssentialsGen.next.copy(
            category = Some(category),
            isPlacedForFree = Some(true),
            experimentInfo = None
          )
        FilteringPolicy(
          signalWithDomain,
          Domain.Autoru.default,
          essentials
        ) should be(true)
      }
    }

    "skip any inherited USERS_AUTORU signal without matched essentials" in {
      forAll(SignalInheritedUsersAutoru.suchThat(_.isNotUniversal)) { signal =>
        val category = AutoruCategoryGen.next
        val signalWithDomain = signal.withDomain(category.toUserDomain)
        val essentials = AutoruEssentialsGen.suchThat(!_.category.contains(category)).next.copy(experimentInfo = None)
        FilteringPolicy(
          signalWithDomain,
          Domain.Autoru.default,
          essentials
        ) should be(false)
      }
    }

    "skip inherited USERS_AUTORU signal with USER_RESELLER and !isPlacedForFree" in {
      forAll(SignalInheritedUsersAutoru.suchThat(_.getDetailedReasons.nonEmpty)) { signal =>
        val withReseller = signal.withDetailedReasons(Set(DetailedReason.UserReseller(None, Seq.empty)))
        val category = AutoruCategoryGen.next
        val essentials =
          AutoruEssentialsGen.next.copy(
            category = Some(category),
            isPlacedForFree = Some(false),
            experimentInfo = None
          )
        FilteringPolicy(
          withReseller,
          category.toUserDomain,
          essentials
        ) should be(false)
      }
    }

    "skip inherited USERS_AUTORU signal with USER_RESELLER if reseller is protected" in {
      forAll(SignalInheritedUsersAutoru.suchThat(_.getDetailedReasons.nonEmpty)) { signal =>
        val withReseller = signal.withDetailedReasons(Set(DetailedReason.UserReseller(None, Seq.empty)))
        val category = AutoruCategoryGen.next
        val essentials =
          AutoruEssentialsGen.next.copy(
            category = Some(category),
            isPlacedForFree = Some(true),
            experimentInfo = Some(ExperimentInfo(Some(true)))
          )
        FilteringPolicy(
          withReseller,
          category.toUserDomain,
          essentials
        ) should be(false)
      }
    }

    "skip inherited USERS_AUTORU signal with non-autoru domain" in {
      forAll(SignalInheritedUsersAutoru.suchThat(_.isNotUniversal)) { signal =>
        val category = AutoruCategoryGen.next
        val signalWithDomain = signal.withDomain(UserAutoruDomainsNotPresentInAutoru.next)
        val essentials = AutoruEssentialsGen.next.copy(category = Some(category), experimentInfo = None)
        FilteringPolicy(
          signalWithDomain,
          Domain.Autoru.default,
          essentials
        ) should be(false)
      }
    }
    "skip any inherited USERS_AUTORU signal if category and domain mismatch" in {
      forAll(SignalInheritedUsersAutoru.suchThat(_.isNotUniversal)) { signal =>
        val category = AutoruCategoryGen.suchThat(_.toUserDomain != signal.domain).next
        val essentials = AutoruEssentialsGen.suchThat(_.category.contains(category)).next.copy(experimentInfo = None)
        FilteringPolicy(
          signal,
          category.toUserDomain,
          essentials
        ) should be(false)
      }
    }

    "pass any inherited USERS_AUTORU with universal reasons" in {
      forAll(SignalInheritedUsersAutoru.suchThat(_.getDetailedReasons.nonEmpty)) { signal =>
        val withUniversal = signal.withDetailedReasons(universalReasonsSetGen(service).next)
        val category = AutoruCategoryGen.next
        val essentials = AutoruEssentialsGen.next.copy(experimentInfo = None)
        FilteringPolicy(
          withUniversal,
          category.toUserDomain,
          essentials
        ) should be(true)
      }
    }
  }

  "InheritedSignalFilteringPolicy for Auto reviews" should {

    implicit val service: Service = Service.AUTO_REVIEWS
    val autoReviewDomain = Domain.AutoReviews(Model.Domain.AutoReviews.DEFAULT_AUTO_REVIEWS)
    val userAutoruReviewDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.REVIEWS)

    "pass with no marker and AUTO_REVIEWS domain" in {
      forAll(SignalWithNoMarkerGen) { signal =>
        val essentials = AutoReviewsEssentialsGen.next
        val withDomain = signal.withDomain(autoReviewDomain)
        FilteringPolicy(withDomain, autoReviewDomain, essentials) shouldBe true
      }
    }
    "pass inherited USERS_AUTORU with UsersAutoru.REVIEWS domain" in {
      forAll(SignalInheritedUsersAutoru.suchThat(!_.getDetailedReasons.exists(isUserResellerDetailedReason))) {
        signal =>
          val essentials = AutoReviewsEssentialsGen.next
          val withDomain = signal.withDomain(userAutoruReviewDomain)
          FilteringPolicy(withDomain, autoReviewDomain, essentials) shouldBe true
      }
    }
    "skip inherited USERS_AUTORU with UsersAutoru.REVIEWS domain if reason = USER_RESELLER" in {
      forAll(
        SignalInheritedUsersAutoru
          .suchThat {
            case _: UnbanSignal => false
            case _: TagSignal   => false
            case _              => true
          }
          .map(_.withDetailedReasons(Set(DetailedReason.UserReseller(None, Seq.empty))))
      ) { signal =>
        val essentials = AutoReviewsEssentialsGen.next
        val withDomain = signal.withDomain(userAutoruReviewDomain)
        FilteringPolicy(withDomain, autoReviewDomain, essentials) shouldBe false
      }
    }
    "skip inherited USERS_AUTORU with other domains" in {
      forAll(SignalInheritedUsersAutoru.suchThat { s =>
        s.isNotUniversal && s.domain != userAutoruReviewDomain
      }) { signal =>
        val essentials = AutoReviewsEssentialsGen.next
        FilteringPolicy(signal, autoReviewDomain, essentials) shouldBe false
      }
    }
    "pass inherited USERS_AUTORU with universal reasons" in {
      forAll(SignalInheritedUsersAutoru.suchThat(_.getDetailedReasons.nonEmpty)) { signal =>
        val withUniversal =
          signal.withDetailedReasons(universalReasonsSetGen(service).next).withDomain(userAutoruReviewDomain)
        val essentials = AutoReviewsEssentialsGen.next
        FilteringPolicy(
          withUniversal,
          autoReviewDomain,
          essentials
        ) should be(true)
      }
    }
  }
}

object InheritedSignalFilteringPolicySpec {

  val SignalWithNoMarkerGen: Gen[Signal] = SignalGen.map(_.withMarker(NoMarker))

  val SignalInheritedUsersAutoru: Gen[Signal] = SignalGen.map(_.withMarker(Inherited(Service.USERS_AUTORU)))

  val AutoruCategoryGen: Gen[Category] = Gen.oneOf(Category.values())

  def universalReasonsSetGen(service: Service): Gen[Set[DetailedReason]] =
    for {
      numReasons <- Gen.choose(1, 3)
      reasons    <- Gen.listOfN(numReasons, Gen.oneOf(Globals.universalReasons(service).toSeq))
    } yield reasons.toSet

  val UserAutoruDomains: Gen[Domain.UsersAutoru] =
    for {
      d <- Gen.oneOf(Model.Domain.UsersAutoru.values())
    } yield Domain.UsersAutoru(d)

  val UserAutoruDomainsNotPresentInAutoru: Gen[Domain.UsersAutoru] =
    for {
      d <-
        Gen.oneOf(
          Model.Domain.UsersAutoru.AUTOPARTS,
          Model.Domain.UsersAutoru.FORUM,
          Model.Domain.UsersAutoru.REVIEWS,
          Model.Domain.UsersAutoru.REVIEWS_COMMENTS,
          Model.Domain.UsersAutoru.MESSAGES
        )
    } yield Domain.UsersAutoru(d)

  implicit class RichSignalForSpec(val signal: Signal) extends AnyVal {

    def withDomain(domain: Domain): Signal =
      signal match {
        case ws: WarnSignal        => ws.copy(domain = domain)
        case bs: BanSignal         => bs.copy(domain = domain)
        case us: UnbanSignal       => us.copy(domain = domain)
        case ts: TagSignal         => ts.copy(domain = domain)
        case hs: HoboSignal        => hs.copy(domain = domain)
        case ies: IndexErrorSignal => ies.copy(domain = domain)
      }

    def withDetailedReasons(detailedReasons: Set[DetailedReason]): Signal =
      signal match {
        case ws: WarnSignal        => ws.copy(detailedReason = detailedReasons.head)
        case bs: BanSignal         => bs.copy(detailedReason = detailedReasons.head)
        case us: UnbanSignal       => us
        case ts: TagSignal         => ts
        case hs: HoboSignal        => hs.copy(result = HoboSignal.Result.Bad(detailedReasons, None))
        case ies: IndexErrorSignal => ies.copy(detailedReasons = detailedReasons)
      }

    def isNotUniversal(implicit service: Service): Boolean = !isUniversal(Service.AUTORU)(signal)
  }

}
