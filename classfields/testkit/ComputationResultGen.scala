package ru.yandex.vertis.general.darkroom.model.testkit

import ru.yandex.vertis.general.darkroom.model.ComputationResult
import zio.test.Gen
import zio.test.magnolia.DeriveGen

object ComputationResultGen {
  val mainColor = DeriveGen[ComputationResult.GeneralMainColor]
  val ratio = DeriveGen[ComputationResult.Ratio]

  val preview = for {
    bytes <- Gen.anyASCIIString
    w <- Gen.anyInt.map(math.abs)
    h <- Gen.anyInt.map(math.abs)
  } yield ComputationResult.Preview(bytes.getBytes, w, h, 1)
  val rotation = DeriveGen[ComputationResult.Rotation]
}
