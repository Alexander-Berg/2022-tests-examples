package vertis.yt.zio.hooch.input

import vertis.yt.hooch.testkit.BaseHoochYtSpec

import java.time.LocalDate
import vertis.yt.zio.hooch.state.JustLastCheck

class YtDayInputIntSpec extends BaseHoochYtSpec {

  "YtDayInput" should {
    "not fail on non-existent table (probably?)" in testEnv { env =>
      val tablePath = basePath.child("nonexistent")
      YtDayInput.statelessNoTx(env.yt, tablePath).changes().flatMap { result =>
        check {
          result shouldBe empty
        }
      }
    }

    "report all dirs (stateless)" in testEnv { env =>
      val tablePath = basePath.child("all")
      val input = YtDayInput.statelessNoTx(env.yt, tablePath)
      val someDays = Seq(LocalDate.now(), LocalDate.now().minusDays(45))
      val oneMoreDay = LocalDate.now().minusDays(30)
      for {
        _ <- env.makeDayTables(tablePath, someDays: _*)
        days1 <- input.changes()
        _ <- check {
          days1.map(_.day) should contain theSameElementsAs someDays
        }
        _ <- env.makeDayTables(tablePath, oneMoreDay)
        days2 <- input.changes()
        _ <- check {
          days2.map(_.day) should contain theSameElementsAs (someDays :+ oneMoreDay)
        }
      } yield ()
    }

    "respect window" in testEnv { env =>
      val tablePath = basePath.child("window")
      val input = YtDayInput.statelessNoTx(env.yt, tablePath, Some(1))
      val someDays = Seq(LocalDate.now(), LocalDate.now().minusDays(45))
      for {
        _ <- env.makeDayTables(tablePath, someDays: _*)
        days <- input.changes()
        _ <- check {
          days.map(_.day) should contain theSameElementsAs (Seq(LocalDate.now()))
        }
      } yield ()
    }

    "report changed dirs only (stateful)" in testEnv { env =>
      val tablePath = basePath.child("stateful")
      val input = YtDayInput.statefulNoTx[JustLastCheck](env.yt, tablePath)
      val someDays = Seq(LocalDate.now(), LocalDate.now().minusDays(45))
      val oneMoreDay = LocalDate.now().minusDays(30)
      for {
        _ <- env.makeDayTables(tablePath, someDays: _*)
        days1 <- input.changes(JustLastCheck(None))
        _ <- check {
          days1.map(_.day) should contain theSameElementsAs someDays
        }
        newState = JustLastCheck(Some(days1.map(_.lastChange).max))
        _ <- env.makeDayTables(tablePath, oneMoreDay)
        days2 <- input.changes(newState)
        _ <- check {
          days2.map(_.day) should contain theSameElementsAs (Seq(oneMoreDay))
        }
      } yield ()
    }
  }

}
