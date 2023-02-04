package ru.yandex.vertis.moderation.hobo

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.feature.EmptyFeatureRegistry
import ru.yandex.vertis.moderation.hobo.Generators.HoboDeciderSourceGen
import ru.yandex.vertis.moderation.hobo.decider.HoboDecider
import ru.yandex.vertis.moderation.model.signal.AutomaticSource
import ru.yandex.vertis.moderation.model.{Domain, SignalKey}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Domain.Autoru.DEFAULT_AUTORU
import ru.yandex.vertis.moderation.proto.Model.Domain.Realty.DEFAULT_REALTY
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service, Visibility}

import scala.util.{Failure, Success, Try}

/**
  * Specs for [[HoboTriggerSet]]
  *
  * @author alesavin
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class HoboTriggerSetSpec extends SpecBase with PropertyChecks {

  import HoboCheckType._

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 10000)

  implicit private val featureRegistry: FeatureRegistry = EmptyFeatureRegistry

  private val Triggers = HoboTriggerSet.forService(Service.REALTY, isEnvironmentStable = true)

  "DefaultHoboTriggerSet" should {
    "be correct on toCreate for multi generated values" in {
      forAll(HoboDeciderSourceGen) { source =>
        Try(Triggers.toCreate(source)) match {
          case Success(set) if set.size == 1 =>
            val hs = set.head
            hs.domain should (be(Domain.Realty(DEFAULT_REALTY)).or(be(Domain.Autoru(DEFAULT_AUTORU))))
            hs.source should be(AutomaticSource(Application.MODERATION))
            hs.`type` should (be(PREMODERATION_VISUAL).or(
              be(COMPLAINTS).or(be(COMPLAINTS_VISUAL).or(be(BANNED_REVALIDATION_VISUAL)))
            ))
            source.service should (be(Service.REALTY).or(be(Service.AUTORU)))
            (hs.`type`, source.context.visibility) match {
              case (PREMODERATION_VISUAL, Visibility.PREMODERATION_CHECK) => ()
              case (COMPLAINTS, Visibility.VISIBLE)                       => ()
              case (COMPLAINTS_VISUAL, Visibility.VISIBLE)                => ()
              case (BANNED_REVALIDATION_VISUAL, Visibility.INVALID)       => ()
              case other                                                  => fail(s"Unexpected $other")
            }
          case Success(set) =>
            set should be(empty)
          case Failure(_: NotImplementedError) => ()
          case other                           => fail(s"Unexpected $other")
        }
      }
    }
    "be correct on toCancel for multi generated values" in {

      def checkKey(source: HoboDecider.Source)(key: SignalKey): Unit =
        key match {
          case k if k.endsWith("hobo_PREMODERATION_VISUAL") =>
            source.effectiveSignals.getUncompleteHoboWithType(PREMODERATION_VISUAL) should not be empty
          case k if k.endsWith("hobo_COMPLAINTS") =>
            source.effectiveSignals.getUncompleteHoboWithType(COMPLAINTS) should not be empty
          case k if k.endsWith("hobo_COMPLAINTS_VISUAL") =>
            source.effectiveSignals.getUncompleteHoboWithType(COMPLAINTS_VISUAL) should not be empty
          case k if k.endsWith("hobo_BANNED_REVALIDATION_VISUAL") =>
            source.effectiveSignals.getUncompleteHoboWithType(BANNED_REVALIDATION_VISUAL) should not be empty
          case other => fail(s"Unexpected $other")
        }

      forAll(HoboDeciderSourceGen) { source =>
        Try(Triggers.toCancel(source)) match {
          case Success(set) if set.size == 1 && source.context.visibility == Visibility.VISIBLE =>
            source.service should (be(Service.REALTY).or(be(Service.AUTORU)))
            val ts = set.head
            ts.key match {
              case k if k.endsWith("hobo_COMPLAINTS") =>
                source.effectiveSignals.getGoodHoboWithType(COMPLAINTS) should not be empty
              case k if k.endsWith("hobo_COMPLAINTS_VISUAL") =>
                source.effectiveSignals.getGoodHoboWithType(COMPLAINTS_VISUAL) should not be empty
              case other => fail(s"Unexpected $other")
            }
          case Success(set) if set.size == 1 =>
            source.service should (be(Service.REALTY).or(be(Service.AUTORU)))
            checkKey(source)(set.head.key)
          case Success(set) if set.size == 2 =>
            source.service should (be(Service.REALTY).or(be(Service.AUTORU)))
            val keys = set.map(_.key)
            keys.size should be(2)
            keys.foreach(checkKey(source))
          // there was a chance to catch 3 tumbstones if your are really lucky
          case Success(set) if set.size == 3 =>
            source.service should (be(Service.REALTY).or(be(Service.AUTORU)))
            val keys = set.map(_.key)
            keys.size should be(3)
            keys.foreach(checkKey(source))
            keys.find(_.contains("hobo_COMPLAINTS")) should not be empty
          case Success(set) =>
            set should be(empty)
          case Failure(_: NotImplementedError) => ()
          case other                           => fail(s"Unexpected $other")
        }
      }
    }
  }
}
