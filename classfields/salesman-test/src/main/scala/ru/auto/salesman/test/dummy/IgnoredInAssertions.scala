package ru.auto.salesman.test.dummy

import ru.auto.salesman.model.FeatureInstance
import ru.auto.salesman.test.model.gens.PromocoderModelGenerators.featureInstanceGen
import ru.yandex.vertis.generators.NetGenerators.asProducer
import zio.{UIO, ZIO}

/** Содержит константы для dummy-методов, не влияющих на ассёршны в тестах, но
  * реализованных так, чтобы тесты, которые дёргают метод, не падали.
  * Если такой dummy-метод начинает влиять на ассёршны, нужно переписывать его,
  * корректно моделируя поведение объекта, запуская нужные сайд-эффекты, а не
  * просто возвращая успешное значение.
  */
object IgnoredInAssertions {

  val unit: UIO[Unit] = ZIO.unit
  val int: UIO[Int] = ZIO.succeed(0)

  val featureInstance: UIO[FeatureInstance] =
    ZIO.succeed(featureInstanceGen.next)
}
