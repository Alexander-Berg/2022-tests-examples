package ru.yandex.vertis.story.api.util

import org.scalacheck.Gen
import org.scalacheck.Gen.Choose
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

trait BasicGenerators {

  implicit val chooseDuration: Choose[FiniteDuration] =
    Choose.xmap[Long, FiniteDuration](_.millis, _.toMillis)

  def readableString(min: Int, max: Int): Gen[String] =
    for {
      length <- Gen.choose(min, max)
      chars <- Gen.listOfN(length, Gen.alphaNumChar)
    } yield chars.mkString

  val readableString: Gen[String] = readableString(5, 10)

  val bool: Gen[Boolean] = Gen.oneOf(true, false)

  val choose: Gen[Int] = Gen.choose(5, 100000)

  def list[I](min: Int, max: Int, g: Gen[I]): Gen[List[I]] =
    for {
      n <- Gen.choose(min, max)
      result <- Gen.listOfN(n, g)
    } yield result

  def set[I](min: Int, max: Int, g: Gen[I]): Gen[Set[I]] =
    list(min, max, g).map(_.toSet)
}

object BasicGenerators extends BasicGenerators
