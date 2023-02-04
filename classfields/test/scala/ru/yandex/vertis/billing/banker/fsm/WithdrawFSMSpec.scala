package ru.yandex.vertis.billing.banker.fsm

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.banker.fsm.WithdrawFSM.WithdrawState
import ru.yandex.vertis.billing.banker.model.Account.Info
import ru.yandex.vertis.billing.banker.model.Payload.Limit
import ru.yandex.vertis.billing.banker.model.Spendings

/**
  * Specs for [[WithdrawFSM]]
  */
class WithdrawFSMSpec extends AnyWordSpec with Matchers {

  "WithdrawFSM" should {

    val sp = Spendings()

    "be correct when start from empty state and have no exist trs" in {
      val s = WithdrawState(Info.Empty, sp, None, None)
      WithdrawFSM(s).s shouldBe s
      WithdrawFSM(s)(Back).s shouldBe s
      WithdrawFSM(s)(Back)(Back).s shouldBe s
      WithdrawFSM(s)(Back)(Forward(0L)).s shouldBe s
      WithdrawFSM(s)(Forward(10L)).s match {
        case WithdrawState(Info(0, 0, 0, 10), _, None, Some(10L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(10L))(Forward(5L)).s match {
        case WithdrawState(Info(0, 0, 0, 15), _, None, Some(15L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(10L))(Forward(5L))(Back).s match {
        case WithdrawState(Info(0, 0, 0, 0), _, None, Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "be correct if exist withdraw transaction and empty incoming" in {
      val s = WithdrawState(Info(0, 50, 0, 0), sp, Some(20), None)
      WithdrawFSM(s).s shouldBe s
      WithdrawFSM(s)(Back).s match {
        case WithdrawState(Info(0, 30, 0, 0), _, Some(0L), None) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Back).s match {
        case WithdrawState(Info(0, 30, 0, 0), _, Some(0L), None) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(10L)).s match {
        case WithdrawState(Info(0, 30, 0, 10), _, Some(0L), Some(10L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(100L)).s match {
        case WithdrawState(Info(0, 30, 0, 100), _, Some(0L), Some(100L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(100L))(Back).s match {
        case WithdrawState(Info(0, 30, 0, 0), _, Some(0L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "be correct if exist withdraw transaction and incoming" in {
      val s = WithdrawState(Info(50, 40, 0, 0), sp, Some(20), None)
      WithdrawFSM(s).s shouldBe s
      WithdrawFSM(s)(Back).s match {
        case WithdrawState(Info(50, 20, 0, 0), _, Some(0L), None) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Back).s match {
        case WithdrawState(Info(50, 20, 0, 0), _, Some(0L), None) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(100L)).s match {
        case WithdrawState(Info(50, 50, 0, 70), _, Some(30L), Some(70L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(100L))(Forward(1L)).s match {
        case WithdrawState(Info(50, 50, 0, 71), _, Some(30L), Some(71L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(100L))(Forward(1L))(Back).s match {
        case WithdrawState(Info(50, 20, 0, 0), _, Some(0L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "be correct if exist withdraw, overdraft and incoming" in {
      val s = WithdrawState(Info(50, 40, 0, 10), sp, Some(20), Some(10))
      WithdrawFSM(s).s shouldBe s
      WithdrawFSM(s)(Back).s match {
        case WithdrawState(Info(50, 20, 0, 0), _, Some(0L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Back).s match {
        case WithdrawState(Info(50, 20, 0, 0), _, Some(0L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Forward(15L)).s match {
        case WithdrawState(Info(50, 50, 0, 15), _, Some(30L), Some(15L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(15L)).s match {
        case WithdrawState(Info(50, 35, 0, 0), _, Some(15L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(15L))(Back).s match {
        case WithdrawState(Info(50, 20, 0, 0), _, Some(0L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(15L))(Back)(Forward(45L)).s match {
        case WithdrawState(Info(50, 50, 0, 15), _, Some(30L), Some(15L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(15L))(Forward(45L)).s match {
        case WithdrawState(Info(50, 50, 0, 30), _, Some(30L), Some(30L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "be correct with daily limits" in {
      val s =
        WithdrawState(Info(200, 100, 0, 10), Spendings(Some(30), Some(50)), withdraw = Some(20), overdraft = Some(10))

      WithdrawFSM(s).s shouldBe s
      WithdrawFSM(s)(Back).s match {
        case WithdrawState(Info(200, 80, 0, 0), Spendings(Some(10), Some(30)), Some(0L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Back).s match {
        case WithdrawState(Info(200, 80, 0, 0), Spendings(Some(10), Some(30)), Some(0L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(7L)).s match {
        case WithdrawState(Info(200, 87, 0, 0), Spendings(Some(17), Some(37)), Some(7L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(301L)).s match {
        case WithdrawState(Info(200, 200, 0, 181), Spendings(Some(130), Some(150)), Some(120L), Some(181L)) =>
          info("Done")
        case other => fail(s"Unexpected $other")
      }

      val dl = 35L

      WithdrawFSM(s)(Back)(Forward(27L, Limit(daily = Some(dl)))).s match {
        case WithdrawState(Info(200, 105, 0, 2), Spendings(Some(35), Some(55)), Some(25L), Some(2L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(3L, Limit(daily = Some(dl)))).s match {
        case WithdrawState(Info(200, 83, 0, 0), Spendings(Some(13), Some(33)), Some(3L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(25L, Limit(daily = Some(dl)))).s match {
        case WithdrawState(Info(200, 105, 0, 0), Spendings(Some(35), Some(55)), Some(25L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "be correct with weekly limits" in {
      val s = WithdrawState(Info(200, 100, 0, 10), Spendings(Some(30), Some(50)), Some(20), Some(10))

      val wl = 60L

      WithdrawFSM(s)(Back)(Forward(7L, Limit(weekly = Some(wl)))).s match {
        case WithdrawState(Info(200, 87, 0, 0), Spendings(Some(17), Some(37)), Some(7L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(30L, Limit(weekly = Some(wl)))).s match {
        case WithdrawState(Info(200, 110, 0, 0), Spendings(Some(40), Some(60)), Some(30L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(107L, Limit(weekly = Some(wl)))).s match {
        case WithdrawState(Info(200, 110, 0, 77), Spendings(Some(40), Some(60)), Some(30L), Some(77L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "be correct with mixed limits" in {
      val s = WithdrawState(Info(200, 100, 0, 10), Spendings(Some(25), Some(50)), Some(20), Some(10))

      val dl = 35L
      val wl = 60L

      WithdrawFSM(s)(Back).s match {
        case WithdrawState(Info(200, 80, 0, 0), Spendings(Some(5), Some(30)), Some(0L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }

      WithdrawFSM(s)(Back)(Forward(6L, Limit(daily = Some(dl), weekly = Some(wl)))).s match {
        case WithdrawState(Info(200, 86, 0, 0), Spendings(Some(11), Some(36)), Some(6L), Some(0L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(32L, Limit(daily = Some(dl), weekly = Some(wl)))).s match {
        case WithdrawState(Info(200, 110, 0, 2), Spendings(Some(35), Some(60)), Some(30L), Some(2L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      WithdrawFSM(s)(Back)(Forward(100L, Limit(daily = Some(dl), weekly = Some(wl)))).s match {
        case WithdrawState(Info(200, 110, 0, 70), Spendings(Some(35), Some(60)), Some(30L), Some(70L)) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
  }
}
