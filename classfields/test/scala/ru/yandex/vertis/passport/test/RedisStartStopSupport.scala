package ru.yandex.vertis.passport.test

import org.scalatest.BeforeAndAfterAll

trait RedisStartStopSupport extends BeforeAndAfterAll {
  this: RedisSupport =>

  override protected def beforeAll(): Unit = {
    cluster.start()
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    cluster.stop()
  }

}
