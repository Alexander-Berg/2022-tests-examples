package ru.yandex.vos2.model

import org.apache.commons.text.RandomStringGenerator
import org.scalacheck.Gen
import ru.yandex.vos2.BasicsModel.{CompositeStatus, TrustLevel}

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

  val TrustLevelGen: Gen[TrustLevel] =
    Gen.oneOf(TrustLevel.values)

  val CompositeStatusGen: Gen[CompositeStatus] =
    Gen.oneOf(CompositeStatus.values)

  val ShortEngStringGen =
    new RandomStringGenerator.Builder()
      .withinRange(Array('a', 'z'))
      .build()

  def genRusString(length: Int): Gen[String] = Gen.pick(length, 'а' to 'я').map(_.mkString)

  val BoolGen = Gen.oneOf(true, false)
}
