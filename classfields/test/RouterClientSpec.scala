package ru.yandex.vertis.general.gateway.clients.router.test

import zio.test._

object RouterClientSpec extends DefaultRunnableSpec {

  def spec =
    suite("RouterClinet")(
      test("simple") {
        assertCompletes
      }
    )
}
