package ru.yandex.vertis.scheduler

import org.scalacheck.Gen

/**
  * @author dimas
  */
package object producer {

  /**
    * Produces generated test data in useful portions.
    * It provides handy interface for interact with [[Gen]].
    *
    * @param gen [[Gen]] instance for produce values from
    *
    */
  implicit class Producer[T](gen: Gen[T]) {

    /**
      * Provides iterator over next n elements from generator.
      *
      * @param n number of elements in result iterator
      * @return iterator over n elements from wrapped generator
      */
    def nextIterator(n: Int): Iterator[T] = values.take(n)

    /**
      * Extracts next single element from wrapped generator.
      *
      * @return single value produced by wrapped generator
      */
    def next: T = next(1).head

    /**
      * Extracts next n elements from from wrapped generator.
      *
      * @param n number of elements for extract
      * @return iterable with length of n containing elements from wrapped generator
      */
    def next(n: Int): Iterable[T] = values.take(n).toIterable

    /**
      * Infinite iterator over generator values.
      *
      * @return infinite iterator over wrapped generator values
      */
    def values: Iterator[T] = Iterator.continually(gen.sample).flatten

  }

}
