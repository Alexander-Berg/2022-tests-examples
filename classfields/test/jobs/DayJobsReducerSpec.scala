package vertis.yt.zio.hooch.jobs

import java.time.LocalDate

import vertis.zio.test.ZioSpecBase

/**
  */
class DayJobsReducerSpec extends ZioSpecBase {

  "DayJobsReducer.allInputsAreReady" should {
    "not produce output if it is not in all inputs" in ioTest {
      val left = Nil
      val right = Seq(DayPartition.actual(LocalDate.now()))
      DayJobsReducer.allInputsAreReady.toJobs(IndexedSeq(left, right)).flatMap { result =>
        check {
          result shouldBe empty
        }
      }
    }

    "produce output if all inputs are ready" in ioTest {
      val left = Seq(DayPartition.actual(LocalDate.now()))
      val right = Seq(DayPartition.actual(LocalDate.now()))
      DayJobsReducer.allInputsAreReady.toJobs(IndexedSeq(left, right)).flatMap { result =>
        check {
          result should contain theSameElementsAs right
          result should not contain theSameElementsAs(left)
        }
      }
    }
  }

}
