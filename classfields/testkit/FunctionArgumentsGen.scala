package ru.yandex.vertis.general.darkroom.model.testkit

import ru.yandex.vertis.general.darkroom.model.{DarkroomResult, FunctionArguments}
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.test.magnolia.DeriveGen

object FunctionArgumentsGen {
  val mainColor = DeriveGen[FunctionArguments.GeneralMainColorArguments]

  val preview = DeriveGen[FunctionArguments.PreviewArguments]

  val ratio = DeriveGen[FunctionArguments.RatioArguments]

  val rotate = DeriveGen[FunctionArguments.RotationArguments]
}
