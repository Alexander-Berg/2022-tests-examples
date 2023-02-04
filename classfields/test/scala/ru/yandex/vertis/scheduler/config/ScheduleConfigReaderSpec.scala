package ru.yandex.vertis.scheduler.config

import com.typesafe.config.ConfigFactory
import org.joda.time.{LocalTime, Period}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import pureconfig.loadConfigOrThrow
import ru.yandex.vertis.scheduler.model.Schedule
import ScheduleConfigReader.scheduleConfigReader


/**
  * @author Natalia Ratskevich (reimai@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class ScheduleConfigReaderSpec extends WordSpec with Matchers {

  private val config = ConfigFactory.load("unit-test.application.conf")

  "schedule config" should {

    "parse fixed rate config" in {
      val fixedRate = loadConfigOrThrow[ScheduleConfig](config.getConfig("tasks.cleaner"))
      fixedRate.schedule shouldBe Schedule.EveryMinutes(31)
    }

    "parse fixed time config" in {
      val fixedTime = loadConfigOrThrow[ScheduleConfig](config.getConfig("tasks.materialization.delegate"))
      fixedTime.schedule shouldBe Schedule.AtFixedTime(new LocalTime(1, 30))
    }

    "parse manual config" in {
      val manual = loadConfigOrThrow[ScheduleConfig](config.getConfig("tasks.migration"))
      manual.schedule shouldBe Schedule.Manually
    }

    "parse delegating config" in {
      val retried = loadConfigOrThrow[ScheduleConfig](config.getConfig("tasks.materialization"))

      retried.schedule shouldBe new Schedule.WithRetries(
        Schedule.AtFixedTime(new LocalTime(1, 30)),
        Period.hours(3))
    }
  }
}
