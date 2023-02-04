package ru.yandex.realty.callcenter.dao

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.callcenter.api.ConciergeManager.{DB_PAYLOAD_CALL_AGREE, DB_STATUS_INITIAL, DB_STATUS_PROCESSED}
import ru.yandex.realty.tracing.Traced

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}

@RunWith(classOf[JUnitRunner])
class ConciergeRequestDaoSpec extends AsyncSpecBase with ConciergeRequestDaoBase with BeforeAndAfterAll {

  "ConciergeRequestDaoSpec" should {
    "insert" in {
      val instant = initInstant()
      val record = initRecord("1", instant)
      conciergeRequestDao.insert(record)(Traced.empty).futureValue

      val values =
        conciergeRequestDao.selectLastCallAgree(instant.minus(1, ChronoUnit.MINUTES))(Traced.empty).futureValue

      values.size shouldBe 1
      values.head shouldBe record
    }

    "insert another type" in {
      val instant = initInstant(3)
      val recordWithEmptyPayload = initRecord("2", instant, payload = "")
      conciergeRequestDao.insert(recordWithEmptyPayload)(Traced.empty).futureValue

      val values =
        conciergeRequestDao.selectLastCallAgree(instant.minus(1, ChronoUnit.MINUTES))(Traced.empty).futureValue

      values.size shouldBe 0
    }

    "updateSentFlags" in {
      val instant = initInstant(5)
      val record = initRecord("3", instant)
      conciergeRequestDao.insert(record)(Traced.empty).futureValue

      val values = conciergeRequestDao.selectLastCallAgree(instant.minus(1, ChronoUnit.HOURS))(Traced.empty).futureValue
      values.size shouldBe 1
      values.head shouldBe record

      val updateOfUnknownRecord = conciergeRequestDao.updateSentFlags(Set("4321"))(Traced.empty).futureValue
      updateOfUnknownRecord shouldBe false

      val update = conciergeRequestDao.updateSentFlags(Set(record.id))(Traced.empty).futureValue
      update shouldBe true

      val afterUpdate =
        conciergeRequestDao.selectLastCallAgree(instant.minus(1, ChronoUnit.HOURS))(Traced.empty).futureValue

      afterUpdate.size shouldBe 0
    }

    "selectLastCallAgree" in {
      val instant = initInstant(9)

      val record = initRecord("4", instant)
      conciergeRequestDao.insert(record)(Traced.empty).futureValue

      val recordAlreadySent = initRecord("5", instant, status = DB_STATUS_PROCESSED)
      conciergeRequestDao.insert(recordAlreadySent)(Traced.empty).futureValue

      val oldInstant = initInstant(7)
      val oldRecord = initRecord("6", oldInstant)
      conciergeRequestDao.insert(oldRecord)(Traced.empty).futureValue

      val values = conciergeRequestDao.selectLastCallAgree(instant.minus(1, ChronoUnit.HOURS))(Traced.empty).futureValue

      values.size shouldBe 1
      values.head shouldBe record
    }
  }

  private def initInstant(day: Int = 1): Instant = {
    Instant
      .now()
      .atZone(ZoneId.of("Europe/Moscow"))
      .withYear(2022)
      .withMonth(7)
      .withDayOfMonth(day)
      .withHour(0)
      .withMinute(0)
      .withSecond(0)
      .withNano(0)
      .toInstant
  }

  private def initRecord(
    id: String,
    createDate: Instant,
    payload: String = DB_PAYLOAD_CALL_AGREE,
    status: String = DB_STATUS_INITIAL
  ): ConciergeRequestRecord = {
    ConciergeRequestRecord(
      id = id,
      createDate = createDate,
      payloadType = payload,
      uuid = Some("2"),
      userName = Some("Name"),
      refererUrl = Some("url"),
      rgid = 1,
      phone = "+79999999999",
      comment = Some("comment"),
      status = status,
      callIntervalFrom = Some(createDate),
      callIntervalTo = Some(createDate.plus(12, ChronoUnit.HOURS))
    )
  }

  override def beforeAll(): Unit = {
    database.run(script"sql/schema.sql").futureValue
  }

  override def afterAll(): Unit = {
    database.run(script"sql/deleteTables.sql").futureValue
  }
}
