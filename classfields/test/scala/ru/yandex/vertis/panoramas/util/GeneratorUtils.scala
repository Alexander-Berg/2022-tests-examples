package ru.yandex.vertis.panoramas.util

import org.scalacheck.Gen

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 08.02.17
  */
trait GeneratorUtils {

  implicit class RichGen[T](gen: Gen[T]) {

    def next: T = values.next()

    def list: List[T] = Gen.listOf(gen).next

    def listOf(count: Int): List[T] = Gen.listOfN(count, gen).next

    def values: Iterator[T] = Iterator.continually(gen.sample).flatten
  }

}

object GeneratorUtils extends GeneratorUtils
