package ru.yandex.vertis.vsquality.utils.kafka_utils

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Gen.alphaNumChar

import scala.language.implicitConversions

object Arbitraries {

  val alphaNumStr: Arbitrary[String] =
    for {
      n   <- Gen.chooseNum(16, 32)
      str <- Gen.listOfN(n, alphaNumChar).map(_.mkString)
    } yield str

  implicit private def wrap[A](g: Gen[A]): Arbitrary[A] = Arbitrary(g)

  def generateSeq[T](n: Int, filter: T => Boolean = (_: T) => true)(implicit arb: Arbitrary[T]): Seq[T] =
    Iterator
      .continually(arb.arbitrary.suchThat(filter).sample)
      .flatten
      .take(n)
      .toSeq

  def generate[T](filter: T => Boolean = (_: T) => true)(implicit arb: Arbitrary[T]): T = generateSeq[T](1, filter).head

}
