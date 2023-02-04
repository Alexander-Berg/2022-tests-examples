package ru.yandex.extdata.core.validator

import org.scalatest.{Matchers, WordSpecLike}

/**
  * @author evans
  */
class VolumeSizeValidatorSpec extends WordSpecLike with Matchers {

  "VolumeSizeValidator" should {
    "block in case deviation problem" in {
      val Blocked(180, _) = VolumeSizeValidator.validate(
        actual = 99,
        expected = 200,
        deviationLowerThreshold = 10,
        deviationUpperThreshold = 1000,
        deviationFactor = 0.5,
        correctionCoefficient = 0.8
      )
    }
    "block in case big deviation diff" in {
      val Blocked(161, _) = VolumeSizeValidator.validate(
        actual = 1,
        expected = 200,
        deviationLowerThreshold = 10,
        deviationUpperThreshold = 1000,
        deviationFactor = 0.5,
        correctionCoefficient = 0.8
      )
    }

    "pass in simple case" in {
      val Passed(197, _) = VolumeSizeValidator.validate(
        actual = 181,
        expected = 200,
        deviationLowerThreshold = 10,
        deviationUpperThreshold = 1000,
        deviationFactor = 0.5,
        correctionCoefficient = 0.8
      )
    }

    "pass when small volumes" in {
      val Passed(10, _) = VolumeSizeValidator.validate(
        actual = 10,
        expected = 80,
        deviationLowerThreshold = 100,
        deviationUpperThreshold = 1000,
        deviationFactor = 0.5,
        correctionCoefficient = 0.8
      )
    }

    "block for too big volumes" in {
      val Blocked(999, _) = VolumeSizeValidator.validate(
        actual = 1001,
        expected = 999,
        deviationLowerThreshold = 100,
        deviationUpperThreshold = 1000,
        deviationFactor = 0.5,
        correctionCoefficient = 0.8
      )
    }
  }
}
