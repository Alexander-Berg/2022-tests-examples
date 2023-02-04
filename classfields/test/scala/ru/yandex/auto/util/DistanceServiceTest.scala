package ru.yandex.auto.util

import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpec}

class DistanceServiceTest extends WordSpec with Matchers {
  val Msk = GeoPoint(55.7558, 37.6173)
  val SPb = GeoPoint(59.9311, 30.3609)
  val VladV = GeoPoint(43.1332, 131.9113)

  "distance between Msk and SPb by Haversine" in {

    val dist = DistanceCalculator.haversineDistanceGrad(Msk.latitude, Msk.longitude, SPb.latitude, SPb.longitude)

    println(dist)
    (Math.abs(dist - 630) < 10) shouldBe true
  }

  "distance between Msk and SPb. Fast formula" in {
    val (aLat, aLon) = (55.7558, 37.6173)
    val (bLat, bLon) = (59.9311, 30.3609)

    val dist = DistanceCalculator.fastFCCDistanceGrad(aLat, aLon, bLat, bLon)

    println(dist)
    (Math.abs(dist - 630) < 10) shouldBe true
  }

  "distance between Msk and Vladivostok" in {
    println(compare(Msk, VladV))
    println(compare(Msk, SPb))
  }

  private def compare(a: GeoPoint, b: GeoPoint): (Double, Double, Double) = {
    val cheap = DistanceCalculator((a.latitude + b.latitude) / 2)
    (
      DistanceCalculator.haversineDistanceGrad(a.latitude, a.longitude, b.latitude, b.longitude),
      DistanceCalculator.fastFCCDistanceGrad(a.latitude, a.longitude, b.latitude, b.longitude),
      cheap.cheapDistance(a.latitude, a.longitude, b.latitude, b.longitude)
    )
  }

  "speed test" in {
    val latGen = Gen.choose(0d, 90d)
    val lonGen = Gen.choose(0d, 90d)

    val coordGen = for {
      aLat <- latGen
      bLat <- latGen
      aLon <- lonGen
      bLon <- lonGen
    } yield (aLat, bLat, aLon, bLon)

    val ammo = Iterator.continually(coordGen.sample).take(1000000).flatten.toSeq

    println(benchmark(ammo, (d4: D4) => DistanceCalculator.haversineDistanceGrad(d4._1, d4._2, d4._3, d4._4)))
    println(benchmark(ammo, (d4: D4) => DistanceCalculator.fastFCCDistanceGrad(d4._1, d4._2, d4._3, d4._4)))
    println(benchmark(ammo, (d4: D4) => DistanceCalculator.fastFCCDistanceGrad(d4._1, d4._2, d4._3, d4._4)))
  }

  type D4 = (Double, Double, Double, Double)

  def benchmark(ammo: Seq[D4], func: D4 => Double): Long = {
    val start = System.currentTimeMillis()
    for (coord <- ammo) func(coord)
    val end = System.currentTimeMillis()
    end - start
  }
}
