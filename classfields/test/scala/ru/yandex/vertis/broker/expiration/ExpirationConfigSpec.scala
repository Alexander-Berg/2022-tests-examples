package ru.yandex.vertis.broker.expiration

import ru.yandex.vertis.broker.expiration.ExpirationConfig.ExpirationFromTypes
import vertis.zio.test.ZioSpecBase

import java.time.{Instant, LocalDate}
import java.time.temporal.ChronoUnit._
import java.time.format.DateTimeParseException
import vertis.core.time.DateTimeUtils

/** @author kusaeva
  */
class ExpirationConfigSpec extends ZioSpecBase {

  "ExpirationConfig" when {
    "expire from table name" should {
      val config = ExpirationConfig(10, ExpirationFromTypes.name)
      val instant = Instant.now

      "return expiration correctly" in ioTest {
        checkM("succeed") {
          config
            .getExpiration("2021-01-01", instant)
            .map(_ shouldBe DateTimeUtils.toInstant(LocalDate.of(2021, 1, 11)))
        }
      }
      "fail when partition can't be parsed" in ioTest {
        checkFailed[Any, Throwable, DateTimeParseException](config.getExpiration("2021-01-01_01", instant))
      }

      "expire from create time" should {
        val config = ExpirationConfig(10, ExpirationFromTypes.createTime)
        val instant = Instant.now
        val expected = instant.plus(config.days, DAYS)

        "return expiration correctly" in ioTest {
          checkM("succeed") {
            config
              .getExpiration("2021-01-01", instant)
              .map(_ shouldBe expected)
          }
        }
        "not fail when partition can't be parsed" in ioTest {
          checkM("succeed") {
            config
              .getExpiration("2021-01-01_01", instant)
              .map(_ shouldBe expected)
          }
        }
      }
    }
  }
}
