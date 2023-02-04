package vertis.yt.zio.hooch.output

import vertis.yt.hooch.testkit.BaseHoochYtSpec

import java.time.LocalDate
import vertis.yt.zio.hooch.jobs.DayPartition

class PointYtOutputIntSpec extends BaseHoochYtSpec {

  "StatelessYtDayOutput" should {
    "process day, but only once" in testEnv { env =>
      val tablePath = basePath.child("process")
      val output = PointYtOutput.simpleNoTx[DayPartition](env.yt, tablePath) { () =>
        env.makeTable(tablePath)
      }

      val job = DayPartition.actual(LocalDate.now())
      for {
        res1 <- output.shouldProcess(job)
        _ <- check {
          res1 shouldBe true
        }
        _ <- output.consume((), Seq(job))
        exists <- env.yt.exists(tablePath)
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
