package vertis.yt.zio.hooch.input

import java.time.LocalDate
import vertis.zio.test.ZioSpecBase
import DayInput._
import vertis.yt.hooch.testkit.TestInput

/**
  */
class DayInputSpec extends ZioSpecBase {

  "RichDayInput.yesterdayOnly" should {
    "return only yesterday day" in ioTest {
      val yesterday = LocalDate.now().minusDays(1)
      TestInput.ofDays(yesterday, LocalDate.now()).yesterdayOnly.changes(()).flatMap { result =>
        check {
          result.map(_.day) should contain theSameElementsAs (Seq(yesterday))
        }
      }
    }

    "return nothing if there is no yesterday" in ioTest {
      TestInput.ofDays(LocalDate.now()).yesterdayOnly.changes(()).flatMap { result =>
        check {
          result shouldBe empty
        }
      }
    }
  }

  "DayInput.plusDay" should {
    "add day to every input" in ioTest {
      val inputs = Seq(LocalDate.now(), LocalDate.now().minusDays(4))
      val expected = Seq(LocalDate.now().plusDays(1), LocalDate.now().minusDays(3))
      TestInput.ofDays(inputs: _*).plusDay.changes(()).flatMap { result =>
        check {
          result.map(_.day) should contain theSameElementsAs expected

        }
      }
    }
  }

}
