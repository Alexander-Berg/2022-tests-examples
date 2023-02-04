package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * Specs on [[Business]]
  *
  * @author alesavin
  */
class UserOfferIdSpec extends AnyWordSpec with Matchers {

  "UserOfferId" should {
    import UserOfferId._

    "support only Uid, YandexUid" in {
      UserOfferId(Uid(1), "2").value should be("uid_1#2")
      UserOfferId(YandexUid("1"), "2").value should be("cookie_1#2")
      intercept[IllegalArgumentException] {
        UserOfferId(Login("1"), "2")
      }
      intercept[IllegalArgumentException] {
        UserOfferId(EmptyUser, "2")
      }
    }

    "be provided from db representation" in {
      unapply("uid_1#2") should be(Some(UserOfferId(Uid(1), "2")))
      unapply("uid_1#2000") should be(Some(UserOfferId(Uid(1), "2000")))
      unapply("uid_100000#2000") should be(Some(UserOfferId(Uid(100000), "2000")))

      unapply("cookie_1#2") should be(Some(UserOfferId(YandexUid("1"), "2")))
      unapply("cookie_1dddd#200") should be(Some(UserOfferId(YandexUid("1dddd"), "200")))

      unapply("uid_d#2") should be(None)
      unapply("uid_#2") should be(None)
      unapply("uid_#2") should be(None)
      unapply("1#2") should be(None)

      unapply("cookie_#2") should be(None)
      unapply("coo#2") should be(None)

      unapply("biz_") should be(None)
      unapply("biz#a") should be(None)
      unapply("_1000") should be(None)
      unapply("_") should be(None)
      unapply("_biz#1000") should be(None)
    }
  }
}
