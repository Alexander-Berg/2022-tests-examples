package vertis.yt.zio.hooch

import vertis.yt.hooch.testkit.{TestInput, TestOutput}
import vertis.yt.zio.hooch.input.Input
import vertis.yt.zio.hooch.jobs.JobsReducer
import vertis.yt.zio.hooch.output.Output
import vertis.yt.zio.hooch.state.StateStorage
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase

/**
  */
class HoochSpec extends ZioSpecBase {

  "Hooch" should {
    "do nothing if inputs are empty" in ioTest {
      testStateless[Any, Int](
        inputs = Seq(TestInput.empty)
      ).run.flatMap { result =>
        check {
          result shouldBe empty
        }
      }
    }

    "pass inputs to output" in ioTest {
      testStateless[Any, Int](
        inputs = Seq(TestInput.of(1, 2, 3))
      ).run.flatMap { result =>
        check {
          (result.toSeq should contain).theSameElementsInOrderAs(Seq(1, 2, 3))
        }
      }
    }

    "allow reducer to make jobs" in ioTest {
      testStateless[Any, Int](
        inputs = Seq(TestInput.of(1, 2, 3)),
        jobsReducer = JobsReducer.pure[Any, Int](_ => Seq(42))
      ).run.flatMap { result =>
        check {
          result.toSeq should contain theSameElementsAs (Seq(42))
        }
      }
    }

    "allow output filter jobs" in ioTest {
      testStateless[Any, Int](
        inputs = Seq(TestInput.of(1, 2, 3)),
        output = TestOutput.acceptOnly(2)
      ).run.flatMap { result =>
        check {
          result.toSeq should contain theSameElementsAs (Seq(2))
        }
      }
    }
  }

  private def testStateless[R, T](
      inputs: Seq[Input[R, Any, T]],
      jobsReducer: JobsReducer[R, Any, T, T] = JobsReducer.identity[T],
      output: Output[R, Iterable[T], T] = TestOutput.passToState[T],
      stateStorage: StateStorage[Any, Iterable[T]] = StateStorage.transient(Iterable.empty[Int])) =
    new Hooch[R with BaseEnv, Iterable[T], T, T](
      inputs,
      jobsReducer,
      output,
      stateStorage
    )

}
