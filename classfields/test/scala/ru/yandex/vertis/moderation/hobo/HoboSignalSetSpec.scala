package ru.yandex.vertis.moderation.hobo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal.{HoboSignal, SignalSet}
import ru.yandex.vertis.moderation.proto.Model.HoboCheckType

/**
  * Specs for [[HoboSignalSet]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class HoboSignalSetSpec extends SpecBase {

  import HoboCheckType._

  "HoboSignalSet" should {
    "be correct on hasHoboWithType" in {
      SignalSet.Empty.hasHoboWithType(PREMODERATION_VISUAL) should be(false)
      SignalSet(WarnSignalGen.next).hasHoboWithType(PREMODERATION_VISUAL) should be(false)
      SignalSet(BanSignalGen.next).hasHoboWithType(PREMODERATION_VISUAL) should be(false)
      SignalSet(HoboSignalGen.withoutMarker.suchThat(_.`type` != PREMODERATION_VISUAL).next)
        .hasHoboWithType(PREMODERATION_VISUAL) should be(false)
      SignalSet(HoboSignalGen.withoutMarker.suchThat(_.`type` == PREMODERATION_VISUAL).next)
        .hasHoboWithType(PREMODERATION_VISUAL) should be(true)
      SignalSet(HoboSignalGen.withoutMarker.suchThat(_.`type` == PREMODERATION_VISUAL).next, BanSignalGen.next)
        .hasHoboWithType(PREMODERATION_VISUAL) should be(true)
      SignalSet(
        HoboSignalGen.withoutMarker.suchThat(_.`type` == PREMODERATION_VISUAL).next,
        HoboSignalGen.suchThat(_.`type` != PREMODERATION_VISUAL).next
      ).hasHoboWithType(PREMODERATION_VISUAL) should be(true)

      SignalSet(HoboSignalGen.withoutMarker.suchThat(_.`type` == PREMODERATION_VISUAL).next)
        .hasHoboWithType(COMPLAINTS_VISUAL) should be(false)
      SignalSet(HoboSignalGen.withoutMarker.suchThat(_.`type` == COMPLAINTS_VISUAL).next)
        .hasHoboWithType(COMPLAINTS_VISUAL) should be(true)
    }
    "be correct with getUncompleteHoboWithType" in {

      def isUncomplete(signal: HoboSignal) = signal.task.isDefined && signal.result == HoboSignal.Result.Undefined

      SignalSet.Empty.getUncompleteHoboWithType(PREMODERATION_VISUAL) should be(None)
      SignalSet.Empty.getUncompleteHoboWithType(COMPLAINTS) should be(None)
      SignalSet.Empty.getUncompleteHoboWithType(COMPLAINTS_VISUAL) should be(None)
      SignalSet(WarnSignalGen.next).getUncompleteHoboWithType(COMPLAINTS_VISUAL) should be(None)
      SignalSet(BanSignalGen.next).getUncompleteHoboWithType(PREMODERATION_VISUAL) should be(None)

      SignalSet(HoboSignalGen.withoutMarker.suchThat(h => h.`type` == PREMODERATION_VISUAL && isUncomplete(h)).next)
        .getUncompleteHoboWithType(PREMODERATION_VISUAL) should not be None
      SignalSet(HoboSignalGen.withoutMarker.suchThat(h => h.`type` == COMPLAINTS && isUncomplete(h)).next)
        .getUncompleteHoboWithType(COMPLAINTS) should not be None
      SignalSet(HoboSignalGen.withoutMarker.suchThat(h => h.`type` == COMPLAINTS && isUncomplete(h)).next)
        .getUncompleteHoboWithType(PREMODERATION_VISUAL) should be(None)
    }
  }
}
