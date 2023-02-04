package vertis.zio.util

import vertis.zio.test.ZioSpecBase
import zio.{Ref, ZIO}

/**
  */
class ZioExtraSpec extends ZioSpecBase {

  "ZioExtra.collectWhileSuccess" should {
    "work for empty input" in ioTest {
      for {
        r1 <- ZioExtra.collectWhileSuccess(List.empty[Long])(ZIO.succeed(_))
        r2 <- ZioExtra.collectWhileSuccess(Seq.empty[Int])(ZIO.succeed(_))
      } yield {
        r1 shouldBe empty
        r2 shouldBe empty
      }
    }

    "work when everything is honky-dory" in ioTest {
      val source = Seq(1, 2, 3)
      for {
        r <- ZioExtra.collectWhileSuccess(source)(ZIO.succeed(_))
      } yield {
        (r should contain).theSameElementsInOrderAs(source)
      }
    }

    "return nothing if everything fail" in ioTest {
      val source = Seq(false, true, true)
      for {
        r <- ZioExtra.collectWhileSuccess(source)(fuuu)
      } yield {
        r shouldBe empty
      }
    }

    "return first success only" in ioTest {
      val source = Seq(true, false, true)
      for {
        r <- ZioExtra.collectWhileSuccess(source)(fuuu)
      } yield {
        r should contain theSameElementsAs (Seq(true))
      }
    }

    "not try to run effect after first fail" in ioTest {
      val source = Seq(true, false, true, true, true)
      for {
        ref <- Ref.make(0)
        r <- ZioExtra.collectWhileSuccess(source) { v =>
          ref.update(_ + 1) *> fuuu(v)
        }
        runCount <- ref.get
      } yield {
        r should contain theSameElementsAs (Seq(true))
        runCount shouldBe 2
      }
    }

  }

  private def fuuu(b: Boolean) = if (b) ZIO.succeed(b) else ZIO.fail("some error")

}
