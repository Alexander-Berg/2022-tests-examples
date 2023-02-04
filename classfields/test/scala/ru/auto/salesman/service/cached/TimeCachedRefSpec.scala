package ru.auto.salesman.service.cached

import org.joda.time.DateTime
import ru.auto.salesman.Task
import ru.auto.salesman.test.{BaseSpec, TestException}

import scala.concurrent.duration.DurationInt

class TimeCachedRefSpec extends BaseSpec {

  private val testValue = 1
  private val nextValue = 2
  private val cacheDuration = 30.seconds
  private val updateFunction = mockFunction[Int, Task[Int]]
  private val currentTime = DateTime.now()

  private val outdatedTime =
    currentTime.plus(cacheDuration.plus(10.second).toMillis)

  "cache value and returns it if not outdated" in {
    val res = (for {
      cached <- TimeCachedRef.make(testValue, cacheDuration)
      v <- cached.computeIfOutdated(_ => Task.succeed(nextValue))
    } yield v).success.value

    res shouldBe testValue
  }

  "cache value and compute new value if outdated" in {
    updateFunction.expects(testValue).once().returningZ(nextValue)

    val res = (for {
      cached <- TimeCachedRef
        .make(testValue, cacheDuration)
        .provideConstantClock(currentTime)
      v <- cached
        .computeIfOutdated(updateFunction(_))
        .provideConstantClock(outdatedTime)
    } yield v).success.value

    res shouldBe nextValue
  }

  "cache computed value after outdated" in {
    updateFunction.expects(testValue).once().returningZ(nextValue)

    val res = (for {
      cached <- TimeCachedRef
        .make(testValue, cacheDuration)
        .provideConstantClock(currentTime)
      _ <- cached
        .computeIfOutdated(updateFunction(_))
        .provideConstantClock(outdatedTime)
      v <- cached
        .computeIfOutdated(updateFunction(_))
        .provideConstantClock(
          outdatedTime.plus(cacheDuration.minus(1.second).toMillis)
        )
    } yield v).success.value

    res shouldBe nextValue
  }

  "fails if compute new value fails" in {
    updateFunction.expects(testValue).once().throwingZ(new TestException())

    val res = (for {
      cached <- TimeCachedRef
        .make(testValue, cacheDuration)
        .provideConstantClock(currentTime)
      v <- cached
        .computeIfOutdated(updateFunction(_))
        .provideConstantClock(outdatedTime)
    } yield v).failure.exception

    res shouldBe an[TestException]
  }

  "return value directly by get" in {
    val res = (for {
      cached <- TimeCachedRef
        .make(testValue, cacheDuration)
        .provideConstantClock(currentTime)
      v <- cached.get.provideConstantClock(outdatedTime)
    } yield v).success.value

    res shouldBe testValue
  }
}
