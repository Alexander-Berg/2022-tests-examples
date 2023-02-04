package ru.yandex.vertis.general.darkroom.model.testkit

import ru.yandex.vertis.general.darkroom.model.{ComputationResult, DarkroomResult, FunctionArguments}
import zio.random.Random
import zio.test.magnolia.DeriveGen
import zio.test.{Gen, Sized}
import java.time.Instant
import java.time.temporal.{ChronoField, ChronoUnit}

object DarkroomResultGen {

  private val limitedInstant =
    Gen
      .instant(Instant.now().minus(30, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS))
      .map(_.`with`(ChronoField.NANO_OF_SECOND, 0))
  val queued = limitedInstant.map(DarkroomResult.Queued(false, _))

  val error = for {
    message <- Gen.alphaNumericString
    instant <- limitedInstant
  } yield DarkroomResult.Error(message, instant)

  def ofType[T <: ComputationResult](res: Gen[Random with Sized, T]): Gen[Random with Sized, DarkroomResult[T]] = {
    for {
      time <- limitedInstant
      res <- Gen.oneOf(queued, error, res.map(DarkroomResult.Success(_, time)))
    } yield res
  }
}
