package ru.yandex.vertis.scoring.dao.scores.impl

import eu.timepit.refined.auto._
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.scoring.dao.YdbSpecBase
import ru.yandex.vertis.scoring.dao.model.ScoresUpdate
import ru.yandex.vertis.scoring.model.PassportUid
import vertis.scoring.model.Badge.Level

import java.time.Instant

class YdbScoresDaoSpec extends YdbSpecBase {
  lazy val dao = new YdbScoresDao(ydbWrapper)

  val puid: PassportUid = 777L
  val timestamp1 = Instant.ofEpochMilli(1)
  val timestamp2 = Instant.ofEpochMilli(2)

  val scoresUpdate1 = ScoresUpdate(puid, Level.THIRTY, timestamp1)
  val scoresUpdate2 = ScoresUpdate(puid, Level.NONE, timestamp2)

  "YdbScoresDao" should {
    "put score and extract the same value" in {
      dao.getScore(puid).await shouldBe None
      dao.putScore(scoresUpdate1).await
      dao.getScore(puid).await shouldBe Some(scoresUpdate1)
    }
    "overwrite old score with new" in {
      dao.putScore(scoresUpdate1).await
      dao.getScore(puid).await shouldBe Some(scoresUpdate1)
      dao.putScore(scoresUpdate2).await
      dao.getScore(puid).await shouldBe Some(scoresUpdate2)
    }
  }
}
