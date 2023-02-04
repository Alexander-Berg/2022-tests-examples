package common.zio.testkit

import zio.random._
import zio.test.{Gen, Sized}

import java.time.Instant
import java.time.temporal.ChronoUnit

object CommonGen {

  val anyString: Gen[Random with Sized, String] =
    Gen.anyString.map(_.filter(_ != '\u0000'))

  val anyString1: Gen[Random with Sized, String] =
    Gen.anyString.filter(_.nonEmpty).map(_.filter(_ != '\u0000'))

  val anyInstant: Gen[Random, Instant] =
    Gen
      .instant(
        Instant.parse("1990-01-01T00:00:00Z"),
        Instant.parse("2100-01-01T00:00:00Z")
      )
      .map(_.truncatedTo(ChronoUnit.MILLIS))

  def listOfShuffled[R, A](gens: Gen[R, A]*): Gen[R with Random, List[A]] = {
    collectAllShuffled(gens)
  }

  def collectAllShuffled[R <: Random, A](gens: Seq[Gen[R, A]]): Gen[R, List[A]] = {
    Gen.zipAll(gens).mapM(shuffle(_))
  }
}
