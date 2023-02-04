package ru.auto.cabinet.test.gens

import org.scalacheck.Gen

object CollectionGens {

  def setOf[A](g: Gen[A]): Gen[Set[A]] = Gen.listOf(g).map(_.toSet)

  def mapOf[K, V](kg: Gen[K], vg: Gen[V]): Gen[Map[K, V]] =
    Gen.mapOf(Gen.zip(kg, vg))
}
