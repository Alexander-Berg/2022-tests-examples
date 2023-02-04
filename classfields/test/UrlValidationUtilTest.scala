package ru.yandex.vertis.general.feed.logic.test

import ru.yandex.vertis.general.feed.logic.UrlValidationUtil
import zio.test.Assertion._
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object UrlValidationUtilTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("UrlValidationUtilTest")(
      testM("http - валидная схема") {
        for {
          result <- UrlValidationUtil.validate("http://ya.ru")
        } yield assert(result)(isUnit)
      },
      testM("https - валидная схема") {
        for {
          result <- UrlValidationUtil.validate("https://ya.ru")
        } yield assert(result)(isUnit)
      },
      testM("file - невалидная схема") {
        for {
          result <- UrlValidationUtil.validate("file://ya.ru").flip
        } yield assert(result)(isSubtype[IllegalArgumentException](anything))
      }
    )
}
