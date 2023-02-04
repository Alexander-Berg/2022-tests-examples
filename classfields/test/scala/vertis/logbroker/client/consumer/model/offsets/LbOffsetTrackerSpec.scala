package vertis.logbroker.client.consumer.model.offsets

import vertis.logbroker.client.consumer.model.offsets.LbOffsetTracker.CookieOffsets
import vertis.zio.test.ZioSpecBase
import zio.{RIO, Ref, ZIO}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class LbOffsetTrackerSpec extends ZioSpecBase {

  "LbOffsetTracker" should {
    "commit an exact fitting cookie" in offsetTest { tracker =>
      val offsets = Map(0 -> 10L, 1 -> 100500L)
      val cookie = 1L
      for {
        _ <- tracker.update(_.read(CookieOffsets(cookie, offsets)))
        cookies <- tracker.modify(_.commit(offsets))
        _ <- check("cookie is staged to commit")(cookies shouldBe Seq(cookie))
      } yield ()
    }

    "commit cookies only then they are fully consumed" in offsetTest { tracker =>
      val offsetsOne = Map(0 -> 10L, 1 -> 100500L)
      val cookieOne = 1L
      val offsetsTwo = Map(0 -> 20L, 1 -> 200500L, 2 -> 50L)
      val cookieTwo = 2L
      val commitOne = Map(0 -> 15L, 1 -> 200500L, 2 -> 50L)
      val commitTwo = Map(0 -> 20L)
      for {
        _ <- tracker.update(_.read(CookieOffsets(cookieOne, offsetsOne)))
        _ <- tracker.update(_.read(CookieOffsets(cookieTwo, offsetsTwo)))
        cookiesOne <- tracker.modify(_.commit(commitOne))
        _ <- check("first cookie is staged to commit")(cookiesOne shouldBe Seq(cookieOne))
        cookiesTwo <- tracker.modify(_.commit(commitTwo))
        _ <- check("second cookie is staged to commit")(cookiesTwo shouldBe Seq(cookieTwo))
      } yield ()
    }

    "commit multiple cookies" in offsetTest { tracker =>
      val one = CookieOffsets(1L, Map(0 -> 10L))
      val two = CookieOffsets(2L, Map(0 -> 20L, 1 -> 30L))
      val three = CookieOffsets(3L, Map(2 -> 50L))
      val four = CookieOffsets(4L, Map(0 -> 21L))
      for {
        _ <- ZIO.foreach(Seq(one, two, three, four))(c => tracker.update(_.read(c)))
        cookies <- tracker.modify(_.commit(Map(0 -> 20L, 1 -> 30L, 2 -> 50L)))
        _ <- check("all cookies are staged to commit")(cookies shouldBe Seq(1L, 2L, 3L))
      } yield ()
    }

    "commit cookies in order" in offsetTest { tracker =>
      val one = CookieOffsets(1L, Map(0 -> 10L))
      val two = CookieOffsets(2L, Map(1 -> 30L))
      for {
        _ <- ZIO.foreach(Seq(one, two))(c => tracker.update(_.read(c)))
        cookies <- tracker.modify(_.commit(Map(0 -> 9L, 1 -> 30L)))
        _ <- check("no cookies to commit")(cookies shouldBe empty)
        cookies <- tracker.modify(_.commit(Map(0 -> 10L)))
        _ <- check("two cookies to commit")(cookies shouldBe Seq(1L, 2L))
      } yield ()
    }
  }

  private def offsetTest(io: Ref[LbOffsetTracker] => RIO[zio.ZEnv, Unit]): Unit =
    ioTest(Ref.make(new LbOffsetTracker()) >>= io)
}
