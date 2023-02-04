package ru.yandex.vertis.moderation.hobo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.hobo.Generators.HoboDeciderSourceGen
import ru.yandex.vertis.moderation.hobo.decider.HoboDecider
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal.{AutomaticSource, SignalInfoSet, SignalSet}
import ru.yandex.vertis.moderation.model.{Domain, EssentialsVersion, Opinion, Opinions}
import ru.yandex.vertis.moderation.model.instance.Diff
import ru.yandex.vertis.moderation.model.signal.SignalInfo.Version
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Domain.AgencyCardRealty.DEFAULT_AGENCY_CARD_REALTY
import ru.yandex.vertis.moderation.proto.Model.Domain.Autoru.DEFAULT_AUTORU
import ru.yandex.vertis.moderation.proto.Model.Domain.Realty.DEFAULT_REALTY
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service, Visibility}
import ru.yandex.vertis.moderation.util.{DateTimeUtil, HoboUtil}

import scala.util.{Failure, Success, Try}

/**
  * Specs for [[PremoderationCheckHoboTrigger]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class PremoderationCheckHoboTriggerSpec extends HoboTriggerSpecBase {

  import HoboCheckType._

  private val trigger = new PremoderationCheckHoboTrigger(HoboUtil.instance2SnapshotPayload)

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 5000)

  "PremoderationCheckHoboTrigger" should {

    val SourceReturnNoneParams: Seq[HoboDecider.Source] =
      Seq(
        newSource(
          RealtyEssentialsGen.next,
          SignalSet.Empty,
          Visibility.VISIBLE,
          Opinions.unknown(Service.REALTY),
          newMeaningfullDiff()
        ),
        newSource(
          RealtyEssentialsGen.next,
          SignalSet.Empty,
          Visibility.INVALID,
          Opinions.unknown(Service.REALTY),
          newMeaningfullDiff()
        ),
        newSource(
          AutoruEssentialsGen.next,
          SignalSet.Empty,
          Visibility.DELETED,
          Opinions.unknown(Service.AUTORU),
          newMeaningfullDiff()
        ),
        newSource(
          RealtyEssentialsGen.next,
          SignalSet.Empty,
          Visibility.PREMODERATION_CHECK,
          Opinions(Domain.Realty(DEFAULT_REALTY) -> Opinion.Ok(Set.empty)),
          newMeaningfullDiff()
        ),
        newSource(
          AutoruEssentialsGen.next,
          SignalSet(WarnSignalGen.withoutSwitchOff.next),
          Visibility.PREMODERATION_CHECK,
          Opinions(Domain.Autoru(DEFAULT_AUTORU) -> Opinion.Failed(Set.empty, Set.empty)),
          newMeaningfullDiff()
        ),
        newSource(
          RealtyEssentialsGen.next,
          SignalSet(BanSignalGen.withoutSwitchOff.next),
          Visibility.PREMODERATION_CHECK,
          Opinions(Domain.Realty(DEFAULT_REALTY) -> Opinion.Failed(Set.empty, Set.empty)),
          newMeaningfullDiff()
        ),
        newSource(
          RealtyEssentialsGen.next,
          SignalSet(
            HoboSignalGen
              .map(_.copy(`type` = PREMODERATION_VISUAL))
              .withoutSwitchOff
              .withoutMarker
              .next
          ),
          Visibility.PREMODERATION_CHECK,
          Opinions.unknown(Service.REALTY),
          newMeaningfullDiff()
        ),
        newSource(
          RealtyEssentialsGen.next,
          SignalSet(BanSignalGen.withoutSwitchOff.next),
          Visibility.PREMODERATION_CHECK,
          Opinions(Domain.Realty(DEFAULT_REALTY) -> Opinion.Ok(Set.empty)),
          newMeaningfullDiff()
        )
      )

    SourceReturnNoneParams.zipWithIndex.foreach { case (source, i) =>
      s"return None if has no premoderation condition for case $i" in {
        trigger.toCreate(source) shouldBe None
      }
    }

    "return source for premoderation check" in {
      val source =
        newSource(
          RealtyEssentialsGen.next,
          SignalSet.Empty,
          Visibility.PREMODERATION_CHECK,
          Opinions.unknown(Service.REALTY),
          newMeaningfullDiff()
        )
      trigger.toCreate(source) match {
        case Some(hs) =>
          hs.domain should be(Domain.Realty(DEFAULT_REALTY))
          hs.source should be(AutomaticSource(Application.MODERATION, source.tag))
          hs.`type` should be(PREMODERATION_VISUAL)
          hs.auxInfo shouldBe SignalInfoSet.Empty
          hs.snapshotPayload shouldBe empty
        case other => fail(s"Unexpected $other")
      }
    }
    "return source for PREMODERATION_CHECK for Service.AGENCY_CARD_REALTY" in {
      val essentialsVersion = EssentialsVersion(Some("card_hash"), Some(DateTimeUtil.Zero))
      val source =
        newSource(
          AgencyCardRealtyEssentialsGen.next.copy(essentialsVersion = Some(essentialsVersion)),
          SignalSet.Empty,
          Visibility.PREMODERATION_CHECK,
          Opinions.unknown(Service.AGENCY_CARD_REALTY),
          newMeaningfullDiff()
        )
      trigger.toCreate(source) match {
        case Some(hs) =>
          hs.domain should be(Domain.AgencyCardRealty(DEFAULT_AGENCY_CARD_REALTY))
          hs.source should be(AutomaticSource(Application.MODERATION, source.tag))
          hs.`type` should be(PREMODERATION_VISUAL)
          hs.snapshotPayload should not be empty
          hs.snapshotPayload.get should include(""""essentialsVersion":{"hash":"card_hash","timestamp":0}""")
        case other => fail(s"Unexpected $other")
      }
    }
    "be correct on toCreate for multi generated values" in {
      forAll(HoboDeciderSourceGen) { source =>
        Try(trigger.toCreate(source)) match {
          case Success(Some(hs)) =>
            hs.domain should (be(Domain.Realty(DEFAULT_REALTY)).or(be(Domain.Autoru(DEFAULT_AUTORU))))
            hs.source should be(AutomaticSource(Application.MODERATION, source.tag))
            hs.`type` should be(PREMODERATION_VISUAL)
            source.context.visibility should be(Visibility.PREMODERATION_CHECK)
            hs.auxInfo shouldBe SignalInfoSet.Empty
            hs.snapshotPayload shouldBe empty
          case Success(_)                      => ()
          case Failure(_: NotImplementedError) => ()
          case other                           => fail(s"Unexpected $other")
        }
      }
    }
    "be correct on toCancel for multi generated values" in {
      forAll(HoboDeciderSourceGen) { source =>
        Try(trigger.toCancel(source)) match {
          case Success(Some(ts)) =>
            source.service should (be(Service.REALTY).or(be(Service.AUTORU)))
            ts.key should endWith("hobo_PREMODERATION_VISUAL")
          case Success(None)                   => ()
          case Failure(_: NotImplementedError) => ()
          case other                           => fail(s"Unexpected $other")
        }
      }
    }
  }

  private def newMeaningfullDiff(): Diff =
    Diff.Realty(
      Set(
        Model.Diff.Realty.Value.SIGNALS,
        Model.Diff.Realty.Value.CONTEXT
      )
    )
}
