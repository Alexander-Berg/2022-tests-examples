package ru.auto.salesman.test

import cats.data.NonEmptySet
import cats.implicits._
import org.scalatest.enablers.Collecting

import scala.collection.GenTraversable

/** Instances to simplify [[org.scalatest.Inspectors.forEvery]] usage.
  */
trait CollectingInstances {

  implicit def collectingNatureOfNonEmptySet[A]: Collecting[A, NonEmptySet[A]] =
    new Collecting[A, NonEmptySet[A]] {

      def loneElementOf(collection: NonEmptySet[A]): Option[A] =
        if (collection.size == 1) Some(collection.head) else None

      def sizeOf(collection: NonEmptySet[A]): Int =
        collection.size.toInt

      def genTraversableFrom(collection: NonEmptySet[A]): GenTraversable[A] =
        collection.toSortedSet
    }
}
