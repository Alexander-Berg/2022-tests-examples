package ru.yandex.vos2.model

import com.google.protobuf.{Duration, Timestamp}
import org.scalacheck.Gen
import ru.yandex.vertis.TimeRange
import ru.yandex.vos2.BasicsModel.CompositeStatus

import scala.concurrent.duration._

/**
  * @author roose
  */
object CommonGen {

  def limitedStr(minLen: Int = 1, maxLen: Int = 10): Gen[String] =
    Gen.choose(minLen, maxLen).flatMap(n => Gen.listOfN(n, Gen.alphaChar).map(_.mkString))

  def now: Long = System.currentTimeMillis

  val CreateDateGen = Gen.choose(now - 60.days.toMillis, now)

  def updateDateGen(long: Long): Gen[Long] = Gen.frequency(
    (6, Gen.choose(long, now)),
    (2, Gen.choose(long min 3.days.toMillis, now)),
    (2, Gen.choose(long min 1.hour.toMillis, now))
  )

  val CompositeStatusGen: Gen[CompositeStatus] =
    Gen.oneOf(CompositeStatus.values)

  val protobufTimestampGen: Gen[Timestamp] = for {
    seconds <- Gen.chooseNum(0, Long.MaxValue)
    nanos <- Gen.chooseNum(0, Int.MaxValue)
  } yield Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos).build

  val protobufDurationGen: Gen[Duration] = for {
    seconds <- Gen.chooseNum(0, Int.MaxValue)
    nanos <- Gen.chooseNum(0, Int.MaxValue)
  } yield Duration.newBuilder().setSeconds(seconds).setNanos(nanos).build

  val booleanGen: Gen[Boolean] = Gen.oneOf(true, false)

  val timeRangeGen: Gen[TimeRange] = for {
    from <- protobufTimestampGen
    stepSec <- Gen.chooseNum(1, 2592000)
    toSec = from.getSeconds + stepSec
    to = Timestamp.newBuilder().setSeconds(toSec).setNanos(from.getNanos)
  } yield TimeRange.newBuilder().setFrom(from).setTo(to).build
}
