package ru.yandex.vertis.scoring.dao.analyst.impl

import java.time.Instant
import cats.data.NonEmptyList
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.scoring.dao.YtSpecBase
import ru.yandex.vertis.scoring.dao.config.YqlConfig
import ru.yandex.vertis.scoring.dao.model.{SummaryUpdate, YtTable}
import ru.yandex.vertis.scoring.model.{PassportUid, PhoneId, PhoneIds}
import ru.yandex.vertis.scoring.dao.analyst.YqlQueryExecutor
import eu.timepit.refined.auto._

class YtAnalystDiffDaoSpec extends YtSpecBase {

  private val puid: PassportUid = 123L

  private val phoneId: PhoneId = "adsff"

  private val testData =
    SummaryUpdate.AnalystData(
      uid = puid,
      diskActivity = true,
      edaActivity = true,
      edaUser = true,
      edaUserBlocked = false,
      kinopoiskActivity = true,
      kinopoiskUser = true,
      lavkaActivity = true,
      lavkaUser = true,
      lavkaUserBlocked = false,
      musicActivity = true,
      personalPhoneIds = Some(PhoneIds(NonEmptyList.one(phoneId))),
      reviewsActivity = true,
      reviewsUser = true,
      taxiActivity = true,
      taxiUser = true,
      taxiUserBlocked = false,
      updateTime = Instant.now
    )

  //TODO VSMODERATION-5342 disable ignore
  "YtAnalystDiffDao" ignore {
    "getDiff" in {
      val config =
        YqlConfig(
          url =
            s"jdbc:yql://${yqlContainer.getContainerIpAddress}:${yqlContainer.getMappedPort(apiPort)}/hahn?syntaxVersion=1",
          user = "me",
          token = "test"
        )

      val result =
        for {
          client <- YqlQueryExecutorImpl.initialize(config)
          dao = new YtAnalystDiffDao[F](client)
          _      <- insertTestDiffData(client)
          result <- dao.getDiff(YtTable.fromFolderAndName("/folder", "table"), 0, 1000)
        } yield result

      result.await shouldBe List(testData)
    }
  }

  private def insertTestDiffData(client: YqlQueryExecutor[F]): F[Unit] = {
    import testData._
    val values =
      s"""
         | ($uid, $personalPhoneIds, ${updateTime.getEpochSecond}, $diskActivity, $edaActivity,
         | $edaUser, $edaUserBlocked, $kinopoiskActivity, $kinopoiskUser, $lavkaActivity,
         | $lavkaUser, $lavkaUserBlocked, $musicActivity, $reviewsActivity, $reviewsUser,
         | $taxiActivity, $taxiUser, $taxiUserBlocked)
         |""".stripMargin
    client.execute(s"INSERT INTO `diff_123` (${YtAnalystDiffDao.AllFieldsStr}) VALUES $values")
  }
}
