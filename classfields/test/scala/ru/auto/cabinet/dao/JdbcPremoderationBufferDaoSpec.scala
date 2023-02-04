package ru.auto.cabinet.dao

import org.scalatest.flatspec.AsyncFlatSpec
import ru.auto.cabinet.dao.jdbc.JdbcPremoderationBufferDao
import ru.auto.cabinet.dao.jdbc.JdbcPremoderationBufferDao.ModerationStatus
import ru.auto.cabinet.mocks.SalonInfoMock
import ru.auto.cabinet.test.JdbcSpecTemplate

import java.time.Instant
import java.time.temporal.ChronoUnit

class JdbcPremoderationBufferDaoSpec
    extends AsyncFlatSpec
    with JdbcSpecTemplate {

  private val dao =
    new JdbcPremoderationBufferDao(office7Database, office7Database)

  val ts = Instant.now().truncatedTo(ChronoUnit.MILLIS)

  "JdbcPremoderationBufferDao insert" should "save new data" in {
    for {
      _ <- dao.upsert(
        JdbcPremoderationBufferDao
          .PremoderationDataUpsertRequest(1, SalonInfoMock.salonInfo, ts, 100L))
      result <- dao
        .getActiveClientData(1)
        .map(_.head)
        .map(_.copy(created = ts))

    } yield assert(
      result == JdbcPremoderationBufferDao.PremoderationDataResponse(
        1,
        1,
        SalonInfoMock.salonInfo,
        ModerationStatus.New,
        Set.empty,
        ts,
        ts,
        100L))
  }

  "JdbcPremoderationBufferDao insert" should "save updated previous data" in {
    for {
      _ <- dao.upsert(
        JdbcPremoderationBufferDao
          .PremoderationDataUpsertRequest(1, SalonInfoMock.salonInfo, ts, 100L))
      _ <- dao.upsert(
        JdbcPremoderationBufferDao.PremoderationDataUpsertRequest(
          1,
          SalonInfoMock.salonInfo.copy(title = "new_title"),
          ts,
          100L))
      result <- dao.getActiveClientData(1)
    } yield assert(
      result.map(_.copy(created = ts)) == List(
        JdbcPremoderationBufferDao.PremoderationDataResponse(
          1,
          1,
          SalonInfoMock.salonInfo.copy(title = "new_title"),
          ModerationStatus.New,
          Set.empty,
          ts,
          ts,
          100L)))
  }

  "JdbcPremoderationBufferDao getActiveClientData" should "receive no data" in {
    for {
      _ <- dao.upsert(
        JdbcPremoderationBufferDao
          .PremoderationDataUpsertRequest(1, SalonInfoMock.salonInfo, ts, 100L))
      _ <- dao.changeStatuses(Map((1L -> (ModerationStatus.Outdated, None))))
      result <- dao.getActiveClientData(1)
    } yield assert(result == List.empty)
  }
}
