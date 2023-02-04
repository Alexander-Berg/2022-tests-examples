package ru.yandex.vertis.moderation.hobo

import org.joda.time.DateTime
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.decider.HoboDecider
import ru.yandex.vertis.moderation.model.ModerationRequest.InitialDepth
import ru.yandex.vertis.moderation.model.{DetailedReason, Opinions}
import ru.yandex.vertis.moderation.model.context.Context
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  HoboSignalGen,
  HoboSignalTaskGen,
  InstanceGen,
  SourceGen
}
import ru.yandex.vertis.moderation.model.instance.{Diff, Essentials}
import ru.yandex.vertis.moderation.model.signal.{HoboSignal, NoMarker, SignalSet}
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Visibility}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.util.DateTimeUtil

/**
  * @author semkagtn
  */
trait HoboTriggerSpecBase extends SpecBase with PropertyChecks {

  protected def newSource(essentials: Essentials,
                          signals: SignalSet,
                          visibility: Visibility,
                          opinions: Opinions,
                          diff: Diff
                         ): HoboDecider.Source = {
    val instance =
      InstanceGen.next.copy(essentials = essentials, signals = signals, context = Context(visibility, None, None))
    HoboDecider.Source(instance, None, diff, opinions, DateTimeUtil.now(), InitialDepth)
  }

  protected def badHoboSignal(checkType: HoboCheckType,
                              detailedReason: DetailedReason,
                              ts: DateTime = DateTimeUtil.now()
                             ): HoboSignal =
    uncompleteHoboSignal(checkType, ts).copy(
      result = HoboSignal.Result.Bad(Set(detailedReason), None)
    )

  protected def goodHoboSignal(checkType: HoboCheckType, ts: DateTime = DateTimeUtil.now()): HoboSignal =
    uncompleteHoboSignal(checkType, ts).copy(
      result = HoboSignal.Result.Good(None)
    )

  protected def uncompleteHoboSignal(checkType: HoboCheckType, ts: DateTime = DateTimeUtil.now()): HoboSignal =
    HoboSignalGen.next.copy(
      source = SourceGen.next.withMarker(NoMarker),
      `type` = checkType,
      task = Some(HoboSignalTaskGen.next),
      switchOff = None,
      result = HoboSignal.Result.Undefined,
      ttl = None,
      timestamp = ts
    )
}
