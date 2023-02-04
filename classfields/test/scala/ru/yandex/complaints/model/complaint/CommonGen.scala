package ru.yandex.complaints.model.complaint

import org.scalacheck.Gen

/**
  * Created by s-reznick on 01.08.16.
  */
object CommonGen {
  val DefaultMaxLex = 10

  def limitedStr(minLen: Int = 1, maxLen: Int = DefaultMaxLex): Gen[String] =
    Gen.choose(minLen, maxLen)
      .flatMap(n => Gen.listOfN(n, Gen.alphaChar).map(_.mkString))
}