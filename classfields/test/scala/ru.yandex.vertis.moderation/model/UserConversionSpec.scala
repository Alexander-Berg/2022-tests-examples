package ru.yandex.vertis.moderation.model

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.User

/**
  * Specs for [[User]] conversions
  *
  * @author sunlight
  */
@RunWith(classOf[JUnitRunner])
class UserConversionSpec extends SpecBase {

  "conversion" should {

    "successfully convert yandex id" in {
      val user = CoreGenerators.UserYandexGen.next
      user should be(User(user.key))
    }

    "successfully convert partner id" in {
      val user = CoreGenerators.UserPartnerGen.next
      user should be(User(user.key))
    }

    "successfully convert dealer id" in {
      val user = CoreGenerators.DealerUserGen.next
      user should be(User(user.key))
    }

    "successfully convert auto.ru id" in {
      val user = CoreGenerators.AutoruUserGen.next
      user should be(User(user.key))
    }

    "successfully convert deprecated 'external user' to 'partner user'" in {
      val partnerId = CoreGenerators.UserIdGen.next
      val uselessId = CoreGenerators.UserIdGen.next
      val externalUserId = s"external_${partnerId}_$uselessId"

      val actualUser = User(externalUserId)
      val expectedUser = User.Partner(partnerId)

      actualUser should be(expectedUser)
    }
  }

  "User.fromAutoruExternalFormat" should {
    "successfully convert user from external autoru format" in {
      val number = Gen.choose(0, 10000).next
      val userAutoru = s"user:$number"
      User.fromAutoruExternalFormat(userAutoru) shouldBe User.Autoru(number.toString)
    }

    "successfully convert dealer from external autoru format" in {
      val number = Gen.choose(0, 10000).next
      val dealerAutoru = s"dealer:$number"
      User.fromAutoruExternalFormat(dealerAutoru) shouldBe User.Dealer(number.toString)
    }

    "fail on unknown autoru prefix" in {
      intercept[IllegalArgumentException] {
        User.fromAutoruExternalFormat(CoreGenerators.StringGen.next)
      }
    }

    "fail on non digit user id" in {
      intercept[IllegalArgumentException] {
        val idGen =
          for {
            prefix <- Gen.oneOf("user", "dealer")
            id     <- CoreGenerators.StringGen
          } yield s"$prefix:$id"
        User.fromAutoruExternalFormat(idGen.next)
      }
    }

  }
}
