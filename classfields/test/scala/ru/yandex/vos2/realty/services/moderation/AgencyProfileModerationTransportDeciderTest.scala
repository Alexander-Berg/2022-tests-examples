package ru.yandex.vos2.realty.services.moderation

import org.junit.runner.RunWith
import org.scalatest.{Inside, Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.UserModel.{AgencyProfile, User}

/**
  * @author pnaydenov
  */
@RunWith(classOf[JUnitRunner])
class AgencyProfileModerationTransportDeciderTest extends WordSpec with Matchers with Inside {
  "AgencyProfileModerationTransportDecider" should {
    "be send if no hash" in {
      val decider = AgencyProfileModerationTransportDecider.Default
      val user = User
        .newBuilder()
        .setUserRef("uid_123")
        .setCurrentAgencyProfile(AgencyProfile.newBuilder().setName("Foo").setOgrn("12345"))
        .build
      val verdict = decider.apply(user).get
      inside(verdict) {
        case ShouldBeSent(instanceSource, hash) =>
      }
    }

    "be send if no old hash" in {
      val decider = AgencyProfileModerationTransportDecider.Default
      val user = User
        .newBuilder()
        .setUserRef("uid_123")
        .setCurrentAgencyProfile(AgencyProfile.newBuilder().setName("Foo").setOgrn("12345"))
        .setHashAgencyProfileModeration("00000")
        .build
      val verdict = decider.apply(user).get
      inside(verdict) {
        case ShouldBeSent(instanceSource, hash) =>
      }
    }

    "not be send twice" in {
      val decider = AgencyProfileModerationTransportDecider.Default
      val user = User
        .newBuilder()
        .setUserRef("uid_123")
        .setCurrentAgencyProfile(AgencyProfile.newBuilder().setName("Foo").setOgrn("12345"))
        .build
      val ShouldBeSent(instanceSource, hash) = decider.apply(user).get
      val user2 = user.toBuilder.setHashAgencyProfileModeration(hash).build()
      val verdict = decider.apply(user2).get
      inside(verdict) {
        case NotChanged =>
      }
    }
  }
}
