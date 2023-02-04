package ru.yandex.vertis.passport.service.ban

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.service.ban.BanService.UserBan
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

/**
  *
  * @author zvez
  */
trait BanServiceSpec extends WordSpec with SpecBase {

  val service: BanService

  private val domain1 = "users"
  private val domain2 = "all"

  "BanService" should {
    "return empty bans list for not-banned user" in {
      val userId = ModelGenerators.userId.next
      service.getUserBans(userId).futureValue shouldBe empty
    }

    "ban user" in {
      val user = ModelGenerators.legacyUser.next
      service.banUser(user, domain1, "test").futureValue

      service.getUserBans(user.id).futureValue should contain theSameElementsAs Seq(UserBan(domain1))
    }

    "ban phone" in {
      val phone = ModelGenerators.phoneNumber.next
      service.banPhone(phone, "test").futureValue

      service.checkPhoneBanned(phone).futureValue shouldBe true
    }

    "ban user in multiple domain" in {
      val user = ModelGenerators.legacyUser.next
      service.banUser(user, domain1, "test").futureValue
      service.banUser(user, domain2, "test").futureValue

      service.getUserBans(user.id).futureValue should contain theSameElementsAs Seq(UserBan(domain1), UserBan(domain2))
    }

    "remove ban" in {
      val user = ModelGenerators.legacyUser.next
      service.banUser(user, domain1, "test").futureValue
      service.banUser(user, domain2, "test").futureValue

      service.removeUserBan(user.id, domain1).futureValue
      service.getUserBans(user.id).futureValue should contain theSameElementsAs Seq(UserBan(domain2))
    }

    "remove phone ban" in {
      val phone = ModelGenerators.phoneNumber.next
      service.banPhone(phone, "test").futureValue

      service.removePhoneBan(phone).futureValue
      service.checkPhoneBanned(phone).futureValue shouldBe false
    }
  }

}
