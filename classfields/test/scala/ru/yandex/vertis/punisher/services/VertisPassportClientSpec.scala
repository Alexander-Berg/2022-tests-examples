package ru.yandex.vertis.punisher.services

import java.time.ZonedDateTime

import com.google.protobuf.Timestamp
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.passport.model.api.ApiModel._
import ru.yandex.passport.model.common.CommonModel.{DomainBan, UserModerationStatus}
import ru.yandex.vertis.moderation.proto.Model.Domain.UsersAutoru
import ru.yandex.vertis.moderation.proto.Model.Reason._
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.services.VertisPassportClient._

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class VertisPassportClientSpec extends BaseSpec {

  private val someTime: ZonedDateTime = ZonedDateTime.parse("2017-08-01T00:00:00+03:00[Europe/Moscow]")

  private val user: UserResult = UserResult.newBuilder.build

  private val client: UserResult = {
    val autoruUserProfile = AutoruUserProfile.newBuilder.setClientId("123")
    val profile = UserProfile.newBuilder.setAutoru(autoruUserProfile)
    val user = User.newBuilder.setProfile(profile)
    UserResult.newBuilder.setUser(user).build
  }

  private val userWithConfirmedEmails: UserResult = {
    val emails =
      Iterable(
        UserEmail.newBuilder.setEmail("test1@ya.ru").setConfirmed(true).build,
        UserEmail.newBuilder.setEmail("test2@ya.ru").setConfirmed(true).build,
        UserEmail.newBuilder.setEmail("test3@ya.ru").setConfirmed(false).build
      )
    val userBuilder = User.newBuilder.addAllEmails(emails.asJava)
    UserResult.newBuilder.setUser(userBuilder).build
  }

  private val notBanned: UserModerationStatus = UserModerationStatus.newBuilder.build

  private val bannedNotHacked: UserModerationStatus = {
    val time =
      Timestamp.newBuilder
        .setSeconds(someTime.toEpochSecond)
    val ban = DomainBan.newBuilder.addReasons(COMMERCIAL.toString).build
    UserModerationStatus.newBuilder
      .setBansUpdated(time)
      .putBans(UsersAutoru.CARS.toString, ban)
      .build
  }

  private val hacked: UserModerationStatus = {
    val ban = DomainBan.newBuilder.addReasons(USER_HACKED.toString).build
    UserModerationStatus.newBuilder.putBans(UsersAutoru.CARS.toString, ban).build
  }

  private val reseller: UserModerationStatus = {
    val domainBan = DomainBan.newBuilder.addReasons(USER_RESELLER.toString).build
    val time =
      Timestamp.newBuilder
        .setSeconds(someTime.toEpochSecond)
        .build
    val bans = Map(UsersAutoru.CARS.toString -> domainBan, UsersAutoru.ATV.toString -> domainBan)
    val resellerFlagUpdatedByCategory = Map(UsersAutoru.CARS.toString -> time, UsersAutoru.ATV.toString -> time)
    UserModerationStatus.newBuilder
      .setReseller(true)
      .putAllBans(bans.asJava)
      .putAllResellerFlagUpdatedByDomain(resellerFlagUpdatedByCategory.asJava)
      .build
  }

  "RichUserResult" should {
    "isAutoruClient" in {
      user.isAutoruClient shouldBe false
      client.isAutoruClient shouldBe true
    }

    "confirmedEmails" in {
      userWithConfirmedEmails.confirmedEmails.toSet shouldBe Set("test1@ya.ru", "test2@ya.ru")
    }
  }

  "RichUserModerationStatus" should {
    "isBanned" in {
      bannedNotHacked.isBanned(UsersAutoru.CARS.toString) shouldBe true
      notBanned.isBanned(UsersAutoru.CARS.toString) shouldBe false
    }

    "isHacked" in {
      hacked.isHacked(UsersAutoru.CARS.toString) shouldBe true
      bannedNotHacked.isHacked(UsersAutoru.CARS.toString) shouldBe false
      notBanned.isHacked(UsersAutoru.CARS.toString) shouldBe false
    }

    "banReasons" in {
      notBanned.banReasons(UsersAutoru.CARS.toString).isEmpty shouldBe true
      bannedNotHacked.banReasons(UsersAutoru.CARS.toString).nonEmpty shouldBe true
    }

    "resellerCategories" in {
      reseller.resellerCategories shouldBe Set(UsersAutoru.CARS, UsersAutoru.ATV)
    }

    "resellerUpdatedByCategories" in {
      reseller.resellerUpdatedByCategories.keySet shouldBe Set(UsersAutoru.CARS, UsersAutoru.ATV)
    }

    "bansUpdated" in {
      bannedNotHacked.bansUpdated shouldBe Some(someTime)
      reseller.bansUpdated shouldBe None
    }
  }
}
