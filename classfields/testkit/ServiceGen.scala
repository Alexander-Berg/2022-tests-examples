package ru.yandex.vertis.general.gost.model.testkit

import ru.yandex.vertis.general.gost.model.Service
import zio.random.Random
import zio.test.{Gen, Sized}

object ServiceGen {

  val anyService: Gen[Any, Service] = Gen.unit.map(_ => Service())

  val anyServices: Gen[Random with Sized, List[Service]] = Gen.listOf(anyService)
}
