package common.time

import zio.test.environment.TestEnvironment
import zio.test._
import zio.test.Assertion._

import java.time.Instant

object IntervalSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("IntervalSpec")(
    test("[from; to) contains from") {
      val from = Instant.ofEpochSecond(15)
      val to = Instant.ofEpochSecond(500)
      assert(Interval(from, Some(to)).contains(from))(isTrue)
    },
    test("[from; +inf) contains from") {
      val from = Instant.ofEpochSecond(15)
      assert(Interval(from, to = None).contains(from))(isTrue)
    },
    test("[from; to) doesn't contain to") {
      val from = Instant.ofEpochSecond(15)
      val to = Instant.ofEpochSecond(500)
      assert(Interval(from, Some(to)).contains(to))(isFalse)
    },
    test("[from; to) contains value inside an Interval") {
      val from = Instant.ofEpochSecond(15)
      val value = Instant.ofEpochSecond(500)
      val to = Instant.ofEpochSecond(1000)
      assert(Interval(from, Some(to)).contains(value))(isTrue)
    },
    test("[from; to) doesn't contain value < from") {
      val value = Instant.ofEpochSecond(2)
      val from = Instant.ofEpochSecond(15)
      val to = Instant.ofEpochSecond(1000)
      assert(Interval(from, Some(to)).contains(value))(isFalse)
    },
    test("[from; to) doesn't contain value > to") {
      val from = Instant.ofEpochSecond(1000)
      val to = Instant.ofEpochSecond(15000)
      val value = Instant.ofEpochSecond(20000)
      assert(Interval(from, Some(to)).contains(value))(isFalse)
    },
    test("[from; +inf) contains value > from") {
      val from = Instant.ofEpochSecond(15000)
      val value = Instant.ofEpochSecond(30000)
      assert(Interval(from, to = None).contains(value))(isTrue)
    },
    test("[from; +inf) doesn't contain value < from") {
      val value = Instant.ofEpochSecond(2)
      val from = Instant.ofEpochSecond(15000)
      assert(Interval(from, to = None).contains(value))(isFalse)
    },
    test("interval creation throws on from == to") {
      val from = Instant.ofEpochSecond(5)
      assert(Interval(from, to = Some(from)))(throwsA[IllegalArgumentException])
    },
    test("interval creation throws on from > to") {
      val from = Instant.ofEpochSecond(15)
      val to = Instant.ofEpochSecond(5)
      assert(Interval(from, Some(to)))(throwsA[IllegalArgumentException])
    },
    test("intersection doesn't exist for non-intersecting finite intervals") {
      val result = Interval.intersectionExists(
        List(
          Interval(Instant.ofEpochSecond(10), Some(Instant.ofEpochSecond(50))),
          Interval(Instant.ofEpochSecond(1000), Some(Instant.ofEpochSecond(5000))),
          Interval(Instant.ofEpochSecond(100), Some(Instant.ofEpochSecond(500)))
        )
      )
      assert(result)(isFalse)
    },
    test("intersection doesn't exist for non-intersecting intervals, one of which is infinite") {
      val result = Interval.intersectionExists(
        List(
          Interval(Instant.ofEpochSecond(10), Some(Instant.ofEpochSecond(50))),
          Interval(Instant.ofEpochSecond(1000), to = None),
          Interval(Instant.ofEpochSecond(100), Some(Instant.ofEpochSecond(500)))
        )
      )
      assert(result)(isFalse)
    },
    test("intersection exists for intersecting finite intervals") {
      val result = Interval.intersectionExists(
        List(
          Interval(Instant.ofEpochSecond(10), Some(Instant.ofEpochSecond(300))),
          Interval(Instant.ofEpochSecond(1000), Some(Instant.ofEpochSecond(5000))),
          Interval(Instant.ofEpochSecond(100), Some(Instant.ofEpochSecond(500)))
        )
      )
      assert(result)(isTrue)
    },
    test("intersection exists for intersecting infinite intevals") {
      val result = Interval.intersectionExists(
        List(
          Interval(Instant.ofEpochSecond(10), Some(Instant.ofEpochSecond(50))),
          Interval(Instant.ofEpochSecond(1000), to = None),
          Interval(Instant.ofEpochSecond(100), to = None)
        )
      )
      assert(result)(isTrue)
    },
    test("intersection exists for intersecting finite and infinite intervals") {
      val result = Interval.intersectionExists(
        List(
          Interval(Instant.ofEpochSecond(10), Some(Instant.ofEpochSecond(50))),
          Interval(Instant.ofEpochSecond(1000), to = None),
          Interval(Instant.ofEpochSecond(100), to = Some(Instant.ofEpochSecond(2000)))
        )
      )
      assert(result)(isTrue)
    },
    test("intersection doesn't exist for neighbor intervals") {
      val result = Interval.intersectionExists(
        List(
          Interval(Instant.ofEpochSecond(10), Some(Instant.ofEpochSecond(50))),
          Interval(Instant.ofEpochSecond(50), Some(Instant.ofEpochSecond(100)))
        )
      )
      assert(result)(isFalse)
    },
    test("intersection doesn't exist if there is just one interval") {
      val result = Interval.intersectionExists(List(Interval(Instant.ofEpochSecond(10), None)))
      assert(result)(isFalse)
    },
    test("intersection doesn't exist if there are no intervals") {
      assert(Interval.intersectionExists(Nil))(isFalse)
    }
  )
}
