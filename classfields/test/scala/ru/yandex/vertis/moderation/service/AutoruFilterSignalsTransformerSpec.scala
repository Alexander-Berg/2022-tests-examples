package ru.yandex.vertis.moderation.service

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.DetailedReason
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal.{HoboSignal, Inherited, NoMarker}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.service.impl.transformer.{FilterSignalsTransformer, _}

/**
  * Specs for [[AutoruFilterSignalsTransformer]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class AutoruFilterSignalsTransformerSpec extends SpecBase {

  private val transformer: FilterSignalsTransformer = AutoruFilterSignalsTransformer

  private val DateTimeGen =
    CoreGenerators.dateTimeGen(
      nearTo = AutoruFilterSignalsTransformer.IgnoreSignalsInterval.from.minusDays(20),
      deltaDays = 10
    )

  "AutoruFilterSignalsTransformer" should {
    "skip Ban signal with obsolete NO_ANSWER" in {
      val time = DateTimeGen.next
      val instance = {
        val i = InstanceGen.next
        val ae = AutoruEssentialsGen.next
        i.copy(essentials = ae.copy(actualizeTime = Some(time)))
      }
      val signal = {
        val h = BanSignalGen.next
        h.copy(
          source = h.source.withMarker(NoMarker),
          detailedReason = nextFilteredByActualizeTimeReason(),
          timestamp = time.minus(100L)
        )
      }
      transformer(instance, signal) should be(false)
    }
    "pass Ban signal with non-obsolete NO_ANSWER" in {
      val time = DateTimeGen.next
      val instance = {
        val i = InstanceGen.next
        val ae = AutoruEssentialsGen.next
        i.copy(essentials = ae.copy(actualizeTime = Some(time)))
      }
      val signal = {
        val h = BanSignalGen.next
        h.copy(
          source = h.source.withMarker(NoMarker),
          detailedReason = nextFilteredByActualizeTimeReason(),
          timestamp = time.plus(100L)
        )
      }
      transformer(instance, signal) should be(true)
    }
    "pass Ban signal with obsolete inherited NO_ANSWER" in {
      val time = DateTimeGen.next
      val instance = {
        val i = InstanceGen.next
        val ae = AutoruEssentialsGen.next
        i.copy(essentials = ae.copy(actualizeTime = Some(time)))
      }
      val signal = {
        val h = BanSignalGen.next
        h.copy(
          source = h.source.withMarker(Inherited(Service.USERS_AUTORU)),
          detailedReason = nextFilteredByActualizeTimeReason(),
          timestamp = time.minus(1L)
        )
      }
      transformer(instance, signal) should be(true)
    }
    "skip Hobo signal with obsolete NO_ANSWER" in {
      val time = DateTimeGen.next
      val instance = {
        val i = InstanceGen.next
        val ae = AutoruEssentialsGen.next
        i.copy(essentials = ae.copy(actualizeTime = Some(time)))
      }
      val signal = {
        val h = HoboSignalGen.next
        h.copy(
          source = h.source.withMarker(NoMarker),
          result = HoboSignal.Result.Bad(Set(nextFilteredByActualizeTimeReason()), None),
          timestamp = time.minus(1L)
        )
      }
      transformer(instance, signal) should be(false)
    }
    "skip Hobo signal with non-obsolete NO_ANSWER" in {
      val time = DateTimeGen.next
      val instance = {
        val i = InstanceGen.next
        val ae = AutoruEssentialsGen.next
        i.copy(essentials = ae.copy(actualizeTime = Some(time)))
      }
      val signal = {
        val h = HoboSignalGen.next
        h.copy(
          source = h.source.withMarker(NoMarker),
          result = HoboSignal.Result.Bad(Set(nextFilteredByActualizeTimeReason()), None),
          timestamp = time.plus(1L)
        )
      }
      transformer(instance, signal) should be(true)
    }
    "pass Hobo signal with obsolete NO_ANSWER and other reason" in {
      val time = DateTimeGen.next
      val instance = {
        val i = InstanceGen.next
        val ae = AutoruEssentialsGen.next
        i.copy(essentials = ae.copy(actualizeTime = Some(time)))
      }
      val signal = {
        val h = HoboSignalGen.next
        h.copy(
          source = h.source.withMarker(NoMarker),
          result = HoboSignal.Result.Bad(Set(nextFilteredByActualizeTimeReason(), DetailedReason.WrongAddress), None),
          timestamp = time.minus(1L)
        )
      }
      transformer(instance, signal) should be(true)
    }
  }

  private def nextFilteredByActualizeTimeReason(): DetailedReason =
    Gen.oneOf(AutoruFilterSignalsTransformer.FilteredByActualizeTime.toSeq).next
}
