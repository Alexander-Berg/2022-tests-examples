package ru.yandex.vertis.general.snatcher.storage.test

import cats.data.NonEmptyList
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.snatcher.model.{ProfileStatus, SellerProfile}
import ru.yandex.vertis.general.snatcher.storage.ProfilesDao
import ru.yandex.vertis.general.snatcher.storage.postgresql.profiles.PgProfilesDao
import zio.ZIO
import zio.test._
import zio.test.Assertion._

import scala.concurrent.duration._

object PgProfilesDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PgProfilesDao")(
      testM("Insert profile and get by hash") {
        val hash = "someStrangeHash"
        val profile = SellerProfile(hash, "http://neveroyatniy.url", ProfileStatus.New, None, Some(7L), 1, Some(5))
        for {
          dao <- ZIO.service[ProfilesDao.Service]
          inserted <- dao.insertOrIgnore(Seq(profile)).transactIO
          result <- dao.getProfileByHash(hash).transactIO
        } yield assert(inserted)(equalTo(1)) && assert(result)(isSome(equalTo(profile)))
      },
      testM("Get none for unknown hash") {
        for {
          dao <- ZIO.service[ProfilesDao.Service]
          result <- dao.getProfileByHash("Unknown hash").transactIO
        } yield assert(result)(isNone)
      },
      testM("Do not insert on conflict") {
        val hash = "repeatedHash"
        val profile = SellerProfile(hash, "", ProfileStatus.New, None, None, 1, None)
        for {
          dao <- ZIO.service[ProfilesDao.Service]
          firstResult <- dao.insertOrIgnore(Seq(profile)).transactIO
          secondResult <- dao.insertOrIgnore(Seq(profile)).transactIO
        } yield assert(firstResult)(equalTo(1)) && assert(secondResult)(equalTo(0))
      },
      testM("Update existing profile") {
        val hash = "updateProfile"
        val profile = SellerProfile(hash, "", ProfileStatus.New, None, None, 1, None)
        for {
          dao <- ZIO.service[ProfilesDao.Service]
          _ <- dao.insertOrIgnore(Seq(profile)).transactIO
          updated = profile.copy(
            status = ProfileStatus.WaitParsed,
            sellerId = Some(SellerId.UserId(655L)),
            activeOffersAmount = Some(4)
          )
          _ <- dao.updateProfile(updated).transactIO
          result <- dao.getProfileByHash(hash).transactIO
        } yield assert(result)(isSome(equalTo(updated)))
      },
      testM("Get only PROCESSED profiles") {
        val hash1 = "processed"
        val profile1 = SellerProfile(hash1, "", ProfileStatus.Processed, None, None, 1, None)
        val hash2 = "unprocessed"
        val profile2 = SellerProfile(hash2, "", ProfileStatus.WaitParsed, None, None, 1, None)
        for {
          dao <- ZIO.service[ProfilesDao.Service]
          _ <- dao.insertOrIgnore(Seq(profile1, profile2)).transactIO
          result <- dao.getOldInStatus(ProfileStatus.Processed, 0.minutes).transactIO
        } yield assertTrue(result.size == 1 && result.contains(profile1))
      },
      testM("Do not get recently updated") {
        val hash = "recentlyUpdated"
        val profile = SellerProfile(hash, "", ProfileStatus.SentToFeeds, None, None, 1, None)
        for {
          dao <- ZIO.service[ProfilesDao.Service]
          _ <- dao.insertOrIgnore(Seq(profile)).transactIO
          result <- dao.getOldInStatus(ProfileStatus.SentToFeeds, 10.minutes).transactIO
        } yield assert(result)(not(contains(profile)))
      },
      testM("Get profile by companyId") {
        val hash = "byCompanyId"
        val companyId = 10L
        val profile = SellerProfile(hash, "", ProfileStatus.SmsSent, None, Some(companyId), 1, None)
        for {
          dao <- ZIO.service[ProfilesDao.Service]
          _ <- dao.insertOrIgnore(Seq(profile)).transactIO
          result <- dao.getProfileByCompanyId(companyId).transactIO
        } yield assert(result)(isSome(equalTo(profile)))
      }
    ).provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> PgProfilesDao.live
    )
}
