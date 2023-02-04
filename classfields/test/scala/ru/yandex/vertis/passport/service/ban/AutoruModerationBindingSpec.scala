package ru.yandex.vertis.passport.service.ban

import org.joda.time.DateTime
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.moderation.proto.Autoru.UserAutoruEssentials
import ru.yandex.vertis.moderation.proto.Model._
import ru.yandex.vertis.passport.model._

class AutoruModerationBindingSpec extends WordSpec with Matchers {

  "toModerationInstance" should {

    val user = FullUser(
      id = "123",
      profile = AutoruUserProfile(
        alias = Some("alias"),
        clientId = Some("X"),
        allowOffersShow = Some(true)
      ),
      registrationDate = DateTime.parse("2020-10-10"),
      updated = DateTime.parse("2021-10-10"),
      active = true,
      pwdHash = Some("xyz"),
      passwordDate = None,
      hashingStrategy = PasswordHashingStrategies.Passport,
      emails = Seq(
        UserEmail("some@email.ru", confirmed = true),
        UserEmail("some_other@email.ru", confirmed = true)
      ),
      phones = Seq(
        UserPhone("71234567890"),
        UserPhone("71234567891")
      ),
      socialProfiles = Seq(
        UserSocialProfile(
          provider = SocialProviders.Gosuslugi,
          socialUser = SocialUser("456", trusted = true)
        )
      )
    )

    val expected = InstanceSource
      .newBuilder()
      .setVersion(1)
      .setExternalId(
        ExternalId
          .newBuilder()
          .setVersion(1)
          .setUser(User.newBuilder().setVersion(1).setAutoruUser("123").build())
          .setObjectId("auto_ru_123")
          .build()
      )
      .setEssentials(
        Essentials
          .newBuilder()
          .setVersion(1)
          .setUserAutoru(
            UserAutoruEssentials
              .newBuilder()
              .setVersion(1)
              .setNickname("alias")
              .setClientId("X")
              .setEmail("some@email.ru")
              .setCreateTime(1602277200000L)
              .addPhones("71234567890")
              .addPhones("71234567891")
              .setGosuslugiTrusted(true)
              .setGosuslugiId("456")
              .setAllowUserOffersShow(true)
              .build()
          )
          .build()
      )
      .setContext(
        ContextSource
          .newBuilder()
          .setVersion(1)
          .setVisibility(Visibility.VISIBLE)
          .build()
      )
      .build()

    "convert successfully" in {
      val moderationStatus = EnrichedUserModerationStatus()
      val result = AutoruModerationBinding.toModerationInstance(user, moderationStatus)
      result shouldBe expected
    }

    "convert successfully for reseller" in {
      val moderationStatus = EnrichedUserModerationStatus(reseller = true)
      val result = AutoruModerationBinding.toModerationInstance(user, moderationStatus)
      result shouldBe expected.toBuilder
        .setContext(
          expected.getContext.toBuilder
            .setVisibility(Visibility.PAYMENT_REQUIRED)
            .build()
        )
        .build()
    }

    "convert successfully for banned" in {
      val moderationStatus = EnrichedUserModerationStatus(enrichedBans =
        Some(
          Map(
            "all" -> EnrichedDomainBan(Set(DomainBanReason("A"), DomainBanReason("B")), None)
          )
        )
      )
      val result = AutoruModerationBinding.toModerationInstance(user, moderationStatus)
      result shouldBe expected.toBuilder
        .setContext(
          expected.getContext.toBuilder
            .setVisibility(Visibility.BLOCKED)
            .setTag("all")
            .build()
        )
        .build()
    }

  }

}
