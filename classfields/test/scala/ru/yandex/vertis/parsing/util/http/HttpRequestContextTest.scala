package ru.yandex.vertis.parsing.util.http

import org.scalatest.FunSuite

/**
  * TODO
  *
  * @author aborunov
  */
class HttpRequestContextTest extends FunSuite {
  test("fromCache") {
    val ctx = new HttpRequestContext
    assert(!ctx.fromCache)
    ctx.setFromCache(true)
    assert(ctx.fromCache)
    intercept[RuntimeException] {
      ctx.setFromCache(true)
    }
  }
}
