package common.palma.test

import common.palma.testkit.MockPalma
import zio.{Runtime, ZLayer}

object MockPalmaSpec extends BasePalmaSpec {

  // т.к. пальма – это внешний statefull сервис, тут мы притворяемся что стейт сохраняется между запросами.
  // альтернативный вариант – написать тесты, которые очищают свои данные за собой.
  // но это сложно сделать в интеграционном тесте.
  private val mock = Runtime.default.unsafeRun(MockPalma.make)

  val layer = ZLayer.succeed(mock)
}
