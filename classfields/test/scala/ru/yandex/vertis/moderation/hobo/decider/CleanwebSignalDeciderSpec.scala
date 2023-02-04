package ru.yandex.vertis.moderation.hobo.decider

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.decider.CleanwebSignalDeciderSpec.{createSource, createUnchagedSignalSource}
import ru.yandex.vertis.moderation.model.DetailedReason
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  essentialsGen,
  hoboSignalGen,
  DetailedReasonGen,
  InstanceGen,
  OpinionsGen,
  SignalSetGen
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.generators.RichGen
import ru.yandex.vertis.moderation.model.instance.Diff
import ru.yandex.vertis.moderation.model.signal.HoboSignal.Task
import ru.yandex.vertis.moderation.model.signal.{AutomaticSource, HoboSignal, SignalSet}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service}
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.proto.Model
@RunWith(classOf[JUnitRunner])
class CleanwebSignalDeciderSpec extends SpecBase {
  "CleanwebSignalDecider" should {
    "create AUTORU requests" in {
      val decider = new CleanwebSignalDecider

      val s = createSource(Service.AUTORU, Diff.Autoru(Set(Model.Diff.Autoru.Value.SIGNALS)))

      val res = decider.decide(s).futureValue

      res.nonEmpty shouldBe true
    }

    "create REALTY requests" in {
      val decider = new CleanwebSignalDecider

      val s = createSource(Service.REALTY, Diff.Realty(Set(Model.Diff.Realty.Value.SIGNALS)))

      val res = decider.decide(s).futureValue

      res.nonEmpty shouldBe true
    }

    "ignore unchanged signals" in {
      val decider = new CleanwebSignalDecider

      val s = createUnchagedSignalSource(Service.AUTORU, Diff.Autoru(Set(Model.Diff.Autoru.Value.SIGNALS)))

      val res = decider.decide(s).futureValue

      res.isEmpty shouldBe true

    }
  }
}

object CleanwebSignalDeciderSpec {
  def createSource(service: Service, diff: Diff): HoboDecider.Source = {
    val signal =
      hoboSignalGen(service).next.copy(
        source =
          AutomaticSource(
            Application.HOBO
          ),
        `type` = HoboCheckType.CLEAN_WEB,
        result =
          HoboSignal.Result.Warn(
            Set(DetailedReason.Stopword),
            Some("text_auto_policy, clean_web_moderation_end")
          ),
        switchOff = None,
        task = Some(Task("queue", "key"))
      )

    val oldSignal = signal.copy(result = HoboSignal.Result.Undefined)
    val essentials = essentialsGen(service).next
    val instance =
      InstanceGen.next.copy(
        signals = SignalSet(List(signal)),
        essentials = essentials
      )

    HoboDecider.Source(
      instance,
      Some(instance.copy(signals = SignalSet(List(oldSignal)))),
      diff,
      OpinionsGen.next,
      DateTimeUtil.now(),
      0
    )
  }

  def createUnchagedSignalSource(service: Service, diff: Diff): HoboDecider.Source = {
    val signal =
      hoboSignalGen(service).next.copy(
        source =
          AutomaticSource(
            Application.HOBO
          ),
        `type` = HoboCheckType.CLEAN_WEB,
        result =
          HoboSignal.Result.Warn(
            Set(DetailedReason.Stopword),
            Some("text_auto_policy, clean_web_moderation_end")
          ),
        switchOff = None,
        task = Some(Task("queue", "key"))
      )

    val instance =
      InstanceGen.next.copy(
        signals = SignalSet(List(signal))
      )

    HoboDecider.Source(
      instance,
      None,
      diff,
      OpinionsGen.next,
      DateTimeUtil.now(),
      0
    )
  }
}
