package auto.base.lenta.core.src.test.scala.ru.yandex.vertis.lenta.model

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.lenta.utils.ProtobufUtils
import ru.yandex.vertis.lenta.utils.yocto.WeightModifier

import java.time.Instant
import java.util.concurrent.TimeUnit

class WeightModifierTest extends AnyFunSuite {

  val oneDayMillis = TimeUnit.DAYS.toMillis(1)
  val now = System.currentTimeMillis()

  test("WeightModifier getFromDate Plus3") {
    val checkDate = Instant.ofEpochMilli(now - oneDayMillis)
    val timestamp = ProtobufUtils.toProtobufTimestamp(checkDate)

    val modifier = WeightModifier.getFromDate(timestamp)

    assert(modifier == WeightModifier.Plus3)
  }

  test("WeightModifier getFromDate Plus3, 0 days past") {
    val checkDate = Instant.ofEpochMilli(now)
    val timestamp = ProtobufUtils.toProtobufTimestamp(checkDate)

    val modifier = WeightModifier.getFromDate(timestamp)

    assert(modifier == WeightModifier.Plus3)
  }

  test("WeightModifier getFromDate Plus2") {
    val daysPassed = 3
    val checkDate = Instant.ofEpochMilli(now - oneDayMillis * daysPassed)
    val timestamp = ProtobufUtils.toProtobufTimestamp(checkDate)

    val modifier = WeightModifier.getFromDate(timestamp)

    assert(modifier == WeightModifier.Plus2)
  }

  test("WeightModifier getFromDate Plus1") {
    val daysPassed = 60
    val checkDate = Instant.ofEpochMilli(now - oneDayMillis * daysPassed)
    val timestamp = ProtobufUtils.toProtobufTimestamp(checkDate)

    val modifier = WeightModifier.getFromDate(timestamp)

    assert(modifier == WeightModifier.Plus1)
  }

  test("WeightModifier getFromDate Minus1") {
    val daysPassed = 61
    val checkDate = Instant.ofEpochMilli(now - oneDayMillis * daysPassed)
    val timestamp = ProtobufUtils.toProtobufTimestamp(checkDate)

    val modifier = WeightModifier.getFromDate(timestamp)

    assert(modifier == WeightModifier.Minus1)
  }

  test("WeightModifier getFromDate Minus2") {
    val daysPassed = 190
    val checkDate = Instant.ofEpochMilli(now - oneDayMillis * daysPassed)
    val timestamp = ProtobufUtils.toProtobufTimestamp(checkDate)

    val modifier = WeightModifier.getFromDate(timestamp)

    assert(modifier == WeightModifier.Minus2)
  }

  test("WeightModifier getFromDate Minus3") {
    val daysPassed = 1000
    val checkDate = Instant.ofEpochMilli(now - oneDayMillis * daysPassed)
    val timestamp = ProtobufUtils.toProtobufTimestamp(checkDate)

    val modifier = WeightModifier.getFromDate(timestamp)

    assert(modifier == WeightModifier.Minus3)
  }

  test("WeightModifier getFromDate Minus3, timestamp in the future") {
    val daysPassed = -1
    val checkDate = Instant.ofEpochMilli(now - oneDayMillis * daysPassed)
    val timestamp = ProtobufUtils.toProtobufTimestamp(checkDate)

    val modifier = WeightModifier.getFromDate(timestamp)

    assert(modifier == WeightModifier.Minus3)
  }
}
