package ru.yandex.auto.searcher.search.grouphandler

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner

import scala.util.Random

@RunWith(classOf[JUnitRunner])
class ProportionsSpec extends WordSpecLike {

  implicit private val rnd: Random = new Random()

  "should work with both short list" in {
    val as = Iterator(1, 1)
    val bs = Iterator(2, 2, 2)

    val props = Array(2, 5)
    val window = props.sum
    val list = new ProportionalIterator(Array(as, bs), props).take(window * 3).toList

    assert(list.toSet == Set(2, 1))
    assert(list.count(_ == 1) == 2)
    assert(list.count(_ == 2) == 3)
  }

  "should work with short list" in {
    val as = Iterator(1, 1)
    val bs = Iterator.continually(2)

    val props = Array(2, 5)
    val window = props.sum
    val list = new ProportionalIterator(Array(as, bs), props).take(window * 3).toList

    assert(list.toSet == Set(2, 1))
    assert(list.count(_ == 1) == 2)
    assert(list.count(_ == 2) == 19)
  }

  "should make a uniform distribution" in {
    val as = Iterator.tabulate(1000)("A" + _)
    val bs = Iterator.tabulate(1000)("B" + _)

    val props = Array(2, 5)
    val window = props.sum
    val iterator = new ProportionalIterator(Array(as, bs), props)

    val histogram = Array.fill[(Int, Int)](window)((0, 0))

    Iterator
      .continually(iterator.take(window).toList)
      .take(150)
      .foreach(sample => {
        println(sample)
        sample.zipWithIndex.foreach {
          case (value, index) =>
            val (ones, twos) = histogram(index)
            if (value.startsWith("A"))
              histogram(index) = (ones + 1, twos)
            else
              histogram(index) = (ones, twos + 1)
        }
      })

    val onesProportion = histogram.map { case (ones, twos) => ones.toFloat / (ones + twos) }
    val mean = onesProportion.sum / onesProportion.length
    val sqMean = onesProportion.map(v => (v - mean) * (v - mean)).sum / (onesProportion.length - 1)

    println(onesProportion.toList)
    println(sqMean)
    assert(sqMean < 0.01)

  }

}
