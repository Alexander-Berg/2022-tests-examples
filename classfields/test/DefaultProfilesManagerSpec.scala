package ru.yandex.vertis.general.snatcher.logic.test

import java.net.URI

import common.clients.sms.model.PhoneDeliveryParams
import common.clients.sms.testkit.TestSmsSender
import common.zio.clients.s3.testkit.TestS3
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.gateway.clients.router.testkit.RouterClientMock
import ru.yandex.vertis.general.snatcher.logic.ScrapingHubHelper.ScrapingHubConfig
import ru.yandex.vertis.general.snatcher.logic.SmsNotificationHelper.SmsHelperConfig
import ru.yandex.vertis.general.snatcher.logic.{
  LocksProvider,
  ProfilesManager,
  ScrapingHubHelper,
  SmsNotificationHelper
}
import ru.yandex.vertis.general.snatcher.model._
import ru.yandex.vertis.general.snatcher.storage.ProfilesDao
import ru.yandex.vertis.general.snatcher.storage.postgresql.locks.PgLocksDao
import ru.yandex.vertis.general.snatcher.storage.postgresql.profiles.PgProfilesDao
import common.zio.logging.Logging
import zio.clock.Clock
import zio.test.Assertion._
import zio.test._
import zio.{ZIO, ZLayer}

object DefaultProfilesManagerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("DefaultProfilesManager")(
      testM("Get status for profile without seller") {
        for {
          profiles <- ZIO.service[ProfilesDao.Service]
          profileHash = "WithoutSeller"
          profile = SellerProfile(profileHash, "someUrl", ProfileStatus.New, None, None, 1, None)
          _ <- profiles.insertOrIgnore(Seq(profile)).transactIO
          result <- ProfilesManager.getProfileInfo(profileHash, SellerId.UserId(777L))
        } yield assert(result)(equalTo(profile))
      },
      testM("Get status for profile owned by requested seller") {
        for {
          profiles <- ZIO.service[ProfilesDao.Service]
          profileHash = "withSeller"
          profile = SellerProfile(
            profileHash,
            "someUrl",
            ProfileStatus.WaitParsed,
            Some(SellerId.UserId(555L)),
            None,
            1,
            None
          )
          _ <- profiles.insertOrIgnore(Seq(profile)).transactIO
          result <- ProfilesManager.getProfileInfo(profileHash, SellerId.UserId(555L))
        } yield assert(result)(equalTo(profile))
      },
      testM("Fail if profile not found") {
        val profileHash = "notFound"
        for {
          result <- ProfilesManager.getProfileInfo(profileHash, SellerId.UserId(333)).run
        } yield assert(result)(fails(equalTo(ProfileNotFound(profileHash))))
      },
      testM("Fail if profile belongs to another seller") {
        for {
          profiles <- ZIO.service[ProfilesDao.Service]
          profileHash = "anotherSeller"
          profile = SellerProfile(
            profileHash,
            "someUrl",
            ProfileStatus.WaitParsed,
            Some(SellerId.UserId(111L)),
            None,
            1,
            None
          )
          _ <- profiles.insertOrIgnore(Seq(profile)).transactIO
          result <- ProfilesManager.getProfileInfo(profileHash, SellerId.UserId(222L)).run
        } yield assert(result)(fails(equalTo(InvalidOwner(profileHash, SellerId.UserId(222L)))))
      },
      testM("Fail if seller already owns another profile") {
        for {
          profiles <- ZIO.service[ProfilesDao.Service]
          profileHash = "alreadyOwns"
          profile1 = SellerProfile(
            "someStrangeHash",
            "someUrl",
            ProfileStatus.WaitParsed,
            Some(SellerId.UserId(999L)),
            None,
            1,
            None
          )
          profile2 = SellerProfile(profileHash, "someUrl", ProfileStatus.New, None, None, 1, None)
          _ <- profiles.insertOrIgnore(Seq(profile1, profile2)).transactIO
          result <- ProfilesManager.getProfileInfo(profileHash, SellerId.UserId(999L)).run
        } yield assert(result)(fails(equalTo(UserOwnsAnotherProfile(SellerId.UserId(999L)))))
      },
      testM("Allow sending sms second time") {
        for {
          profiles <- ZIO.service[ProfilesDao.Service]
          profileUrl = new URI("https://www.avito.ru/aaa")
          profileHash = "31c287aec4ff50269de4523bdfc4707b"
          profile = SellerProfile(profileHash, profileUrl.toString, ProfileStatus.SmsSent, None, None, 1, None)
          _ <- profiles.insertOrIgnore(Seq(profile)).transactIO
          result <- ProfilesManager.prepareProfileForTransfer(profileUrl, "+79843467856", Some(7L)).run
        } yield assert(result)(succeeds(anything))
      },
      testM("Fail if trying to send sms for already processing profile") {
        for {
          profiles <- ZIO.service[ProfilesDao.Service]
          profileUrl = new URI("https://www.avito.ru/000store")
          profileHash = "18133f29ea2927333ef07e06244a8721"
          profile = SellerProfile(profileHash, profileUrl.toString, ProfileStatus.Processed, None, None, 1, None)
          _ <- profiles.insertOrIgnore(Seq(profile)).transactIO
          result <- ProfilesManager.prepareProfileForTransfer(profileUrl, "+79843467856", Some(7L)).run
        } yield assert(result)(fails(equalTo(AlreadyProcessing(profile.hash, profile.status))))
      },
      testM("Save profile and send sms for new profile") {
        for {
          _ <- RouterClientMock.setRouteResponse(_ => ZIO.succeed("/very-strange-path"))
          profileUrl = new URI("https://www.avito.ru/storemoreokean")
          phone = "+79843467856"
          _ <- ProfilesManager.prepareProfileForTransfer(profileUrl, phone, Some(6L))
          sent <- TestSmsSender.allSent
        } yield assert(sent.values.map(_.params))(contains(PhoneDeliveryParams(phone)))
      }
    ).provideCustomLayerShared {
      val clock = ZLayer.requires[Clock]
      val transactor = TestPostgresql.managedTransactor
      val dao = transactor >+> (PgProfilesDao.live ++ PgLocksDao.live)
      val locks = dao ++ clock >>> LocksProvider.live
      val s3 = TestS3.mocked
      val scrapingHubConfig = ZLayer.succeed(ScrapingHubConfig("mock", "mock"))
      val scrapingHubHelper = clock ++ s3 ++ scrapingHubConfig >>> ScrapingHubHelper.live
      val routerMock = RouterClientMock.layer
      val smsMock = TestSmsSender.layer
      val smsHelperConfig = ZLayer.succeed(SmsHelperConfig("o.test.vertis.yandex.ru"))
      val smsNotificationHelper = routerMock ++ smsMock ++ smsHelperConfig >>> SmsNotificationHelper.live
      val log = Logging.live
      val profiles = log ++ dao ++ locks ++ scrapingHubHelper ++ smsNotificationHelper >>> ProfilesManager.live
      profiles ++ dao ++ routerMock ++ smsMock
    }
}
