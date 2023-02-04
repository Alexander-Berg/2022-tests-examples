package ru.auto.cabinet.dao

import java.time.OffsetDateTime

import org.scalatest.flatspec.AsyncFlatSpec
import ru.auto.cabinet.dao.jdbc.JdbcAutoApplyScheduleDao
import ru.auto.cabinet.model.AutoApplySchedule
import ru.auto.cabinet.test.JdbcSpecTemplate

class JdbcAutoApplyScheduleDaoSpec extends AsyncFlatSpec with JdbcSpecTemplate {

  private val dao = new JdbcAutoApplyScheduleDao(office7Database)

  private val schedule1 = AutoApplySchedule(
    "1055684040-3df3",
    deleted = true,
    visible = true,
    epoch = OffsetDateTime.parse("2019-01-17T17:22:06.602+03:00"))

  private val schedule2 = AutoApplySchedule(
    "1064950674-e6e5629a",
    deleted = true,
    visible = true,
    epoch = OffsetDateTime.parse("2019-01-22T10:17:18.412+03:00"))

  private val schedule3 = AutoApplySchedule(
    "1066074070-efbf59c6",
    deleted = true,
    visible = true,
    epoch = OffsetDateTime.parse("2019-01-21T15:49:05.327+03:00"))

  private val schedule4 = AutoApplySchedule(
    "1063167088-7a0ade",
    deleted = false,
    visible = true,
    epoch = OffsetDateTime.parse("2019-01-22T00:46:37.656+03:00")
  )

  "Auto apply schedule dao" should "find all" in {
    for {
      result <- dao.find(updatedSince = None, includeInvisible = false)
    } yield (result should contain).allOf(
      schedule1,
      schedule2,
      schedule3,
      schedule4)
  }

  it should "find all after date" in {
    for {
      result <- dao.find(
        updatedSince = Some(OffsetDateTime.parse("2019-01-20T00:00+03:00")),
        includeInvisible = false)
    } yield (result should contain).allOf(schedule2, schedule3, schedule4)
  }

  it should "find nothing on too late date" in {
    for {
      result <- dao.find(
        updatedSince = Some(OffsetDateTime.parse("2019-01-22T11:00+03:00")),
        includeInvisible = false)
    } yield result shouldBe empty
  }
}
