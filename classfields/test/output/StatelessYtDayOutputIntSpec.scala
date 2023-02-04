package vertis.yt.zio.hooch.output

import vertis.yt.hooch.testkit.BaseHoochYtSpec

import java.time.LocalDate
import vertis.yt.model.YPaths.RichYPath
import vertis.yt.zio.hooch.jobs.DayPartition

class StatelessYtDayOutputIntSpec extends BaseHoochYtSpec {

  "StatelessYtDayOutput" should {
    "process day, but only once" in testEnv { env =>
      val tablePath = basePath.child("process")
      val output = StatelessYtDayOutput.simpleNoTx(env.yt, tablePath) { dp =>
        env.makeDayTables(tablePath, dp.day)
      }

      val job = DayPartition.actual(LocalDate.now())
      for {
        res1 <- output.shouldProcess(job)
        _ <- check {
          res1 shouldBe true
        }
        _ <- output.consume((), Seq(job))
        exists <- env.yt.exists(tablePath.dayChild(job.day))
        _ <- check {
          exists shouldBe true
        }
        res2 <- output.shouldProcess(job)
        _ <- check {
          res2 shouldBe false
        }
      } yield ()
    }
  }

}
