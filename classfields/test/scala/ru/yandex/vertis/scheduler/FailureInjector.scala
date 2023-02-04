package ru.yandex.vertis.scheduler

import java.util.concurrent.ThreadLocalRandom

import scala.util.{Failure, Try}

/**
 * Supports failures injecting for testing purposes.
 *
 * @author dimas
 */
trait FailureInjector {
  /**
   * Determines failure happening probability
   */
  def failProbability: Float

  def shouldFail(): Boolean =
    ThreadLocalRandom.current().nextFloat() <= failProbability

  def nextFailure[A](): Try[A] =
    Failure(new Exception("Failure injected exception"))

  def withProbableFailure[A](action: => Try[A]): Try[A] =
    if (shouldFail())
      nextFailure()
    else
      action

  require(failProbability > 0 && failProbability < 1,
    "Fail probability should be in (0, 1)")
}
