package ru.yandex.vertis.scoring.dao.summary.impl

import java.time.Instant
import java.time.temporal.ChronoUnit

import cats.data.NonEmptyList
import eu.timepit.refined.auto._
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.scoring.dao.YdbSpecBase
import ru.yandex.vertis.scoring.dao.model.SummaryUpdate
import ru.yandex.vertis.scoring.model.{PassportUid, PhoneIds, Summary}

class YdbSummaryDaoSpec extends YdbSpecBase {
  lazy val dao = new YdbSummaryDao(ydbWrapper)

  val now = Instant.now()
  val `3daysLater` = now.plus(3, ChronoUnit.DAYS)
  val `3daysBefore` = now.plus(3, ChronoUnit.DAYS)

  val analystData =
    SummaryUpdate.AnalystData(
      1234567L,
      diskActivity = false,
      edaActivity = true,
      edaUser = false,
      edaUserBlocked = true,
      kinopoiskActivity = false,
      kinopoiskUser = true,
      lavkaActivity = false,
      lavkaUser = true,
      lavkaUserBlocked = false,
      musicActivity = true,
      personalPhoneIds = Some(PhoneIds(NonEmptyList.of("hash0", "hash1"))),
      reviewsActivity = true,
      reviewsUser = false,
      taxiActivity = true,
      taxiUser = false,
      taxiUserBlocked = true,
      updateTime = now
    )

  val passportData =
    SummaryUpdate.PassportData(
      uid = 1234567L,
      karmaValue = 57,
      karmaAllowUntil = Some(`3daysLater`),
      registrationDate = `3daysBefore`,
      isAnyPhoneConfirmed = true,
      isAnyPhoneBound = false,
      updateTime = now
    )

  private def checkEqualPassportData(summary: Summary, passportData: SummaryUpdate.PassportData) = {
    summary.uid shouldBe passportData.uid
    summary.maybePassportData should not be empty
    val summaryPassportData = summary.maybePassportData.get

    summaryPassportData.uid shouldBe passportData.uid
    summaryPassportData.karmaInfo.value shouldBe passportData.karmaValue
    summaryPassportData.karmaInfo.allowUntil shouldBe passportData.karmaAllowUntil
    summaryPassportData.attributes.registrationDate shouldBe passportData.registrationDate
    summaryPassportData.isAnyPhoneConfirmed shouldBe passportData.isAnyPhoneConfirmed
    summaryPassportData.isAnyPhoneBound shouldBe passportData.isAnyPhoneBound
    summaryPassportData.updateTime shouldBe passportData.updateTime
  }

  private def checkEqualAnalystData(summary: Summary, analystData: SummaryUpdate.AnalystData) = {
    summary.uid shouldBe analystData.uid
    summary.maybeAnalystData should not be empty
    val summaryAnalystData = summary.maybeAnalystData.get

    summaryAnalystData.uid shouldBe analystData.uid
    summaryAnalystData.diskActivity shouldBe analystData.diskActivity
    summaryAnalystData.edaActivity shouldBe analystData.edaActivity
    summaryAnalystData.edaUser shouldBe analystData.edaUser
    summaryAnalystData.edaUserBlocked shouldBe analystData.edaUserBlocked
    summaryAnalystData.kinopoiskActivity shouldBe analystData.kinopoiskActivity
    summaryAnalystData.kinopoiskUser shouldBe analystData.kinopoiskUser
    summaryAnalystData.lavkaActivity shouldBe analystData.lavkaActivity
    summaryAnalystData.lavkaUser shouldBe analystData.lavkaUser
    summaryAnalystData.lavkaUserBlocked shouldBe analystData.lavkaUserBlocked
    summaryAnalystData.musicActivity shouldBe analystData.musicActivity
    summaryAnalystData.personalPhoneIds shouldBe analystData.personalPhoneIds
    summaryAnalystData.reviewsActivity shouldBe analystData.reviewsActivity
    summaryAnalystData.reviewsUser shouldBe analystData.reviewsUser
    summaryAnalystData.taxiActivity shouldBe analystData.taxiActivity
    summaryAnalystData.taxiUser shouldBe analystData.taxiUser
    summaryAnalystData.taxiUserBlocked shouldBe analystData.taxiUserBlocked
    summaryAnalystData.updateTime shouldBe analystData.updateTime
  }

  private def getExistingSummary(puid: PassportUid) = {
    val maybeSummary = dao.getSummary(puid).await
    maybeSummary should not be empty
    maybeSummary.get
  }

  "YdbSummaryDao.BulkUpdate" should {
    "bulk upsert analyst data update" in {
      dao.getSummary(analystData.uid).await shouldBe None
      dao.putBulkSummaryUpdates(NonEmptyList.one(analystData)).await

      val summary = getExistingSummary(analystData.uid)

      checkEqualAnalystData(summary, analystData)
    }

    "bulk upsert passport data update" in {
      dao.getSummary(passportData.uid).await shouldBe None
      dao.putBulkSummaryUpdates(NonEmptyList.one(passportData)).await

      val summary: Summary = getExistingSummary(passportData.uid)

      checkEqualPassportData(summary, passportData)
    }

    "bulk upsert nullify optional fields" in {
      dao.putBulkSummaryUpdates(NonEmptyList.one(passportData)).await
      val allowUntilBefore = getExistingSummary(passportData.uid).maybePassportData.get.karmaInfo.allowUntil
      allowUntilBefore should not be empty

      dao.putBulkSummaryUpdates(NonEmptyList.one(passportData.copy(karmaAllowUntil = None))).await
      val allowUntilAfter = getExistingSummary(passportData.uid).maybePassportData.get.karmaInfo.allowUntil
      allowUntilAfter shouldBe empty
    }

    "bulk upsert few updates for one user" in {
      dao.getSummary(passportData.uid).await shouldBe None
      dao.putBulkSummaryUpdates(NonEmptyList.of(analystData, passportData)).await

      val summary = getExistingSummary(analystData.uid)

      checkEqualAnalystData(summary, analystData)
      checkEqualPassportData(summary, passportData)
    }

    "bulk upsert few updates for few user" in {
      val analystData1 = analystData.copy(uid = 777777L)
      val passportData1 = passportData.copy(uid = 777777L)

      dao.getSummary(passportData.uid).await shouldBe None
      dao
        .putBulkSummaryUpdates(
          NonEmptyList.of(analystData, passportData, analystData1, passportData1)
        )
        .await

      val summary = getExistingSummary(analystData.uid)
      val summary1 = getExistingSummary(analystData1.uid)

      checkEqualAnalystData(summary, analystData)
      checkEqualPassportData(summary, passportData)
      checkEqualAnalystData(summary1, analystData1)
      checkEqualPassportData(summary1, passportData1)
    }
  }

  "YdbSummaryDao.Update" should {
    "create row from update if summary didn't exist" in {
      dao.getSummary(passportData.uid).await shouldBe None
      dao.putSummaryUpdate(passportData).await

      val summary = getExistingSummary(passportData.uid)

      checkEqualPassportData(summary, passportData)
    }

    "overwrite update with new data" in {
      dao.putSummaryUpdate(passportData).await

      val summary = getExistingSummary(passportData.uid)

      checkEqualPassportData(summary, passportData)

      val newPassportData = passportData.copy(karmaValue = 99)

      dao.putSummaryUpdate(newPassportData).await

      val summary1 = getExistingSummary(passportData.uid)

      checkEqualPassportData(summary1, newPassportData)

    }

    "upsert analyst data update" in {
      dao.getSummary(analystData.uid).await shouldBe None
      dao.putSummaryUpdate(analystData).await

      val summary = getExistingSummary(analystData.uid)

      checkEqualAnalystData(summary, analystData)
    }

    "upsert passport data update" in {
      dao.getSummary(passportData.uid).await shouldBe None
      dao.putSummaryUpdate(passportData).await

      val summary = getExistingSummary(passportData.uid)

      checkEqualPassportData(summary, passportData)
    }
  }
}
