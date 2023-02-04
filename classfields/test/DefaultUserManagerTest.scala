package ru.yandex.vertis.general.users.logic.test

import common.clients.blackbox.testkit.BlackboxClientTest
import common.clients.personality.testkit.PersonalityClientTest
import common.tvm.model.UserTicket.TicketBody
import common.zio.grpc.client.GrpcClient
import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.common.fail_policy.FailPolicy
import general.globe.api.{GeoServiceGrpc, ReverseGeocodeResponse}
import general.globe.model.{Address, GeoPosition, Toponym}
import ru.yandex.vertis.general.common.clients.clean_web.testkit.CleanWebClientTest
import ru.yandex.vertis.general.common.dictionaries.testkit.TestBansDictionaryService
import ru.yandex.vertis.general.globe.testkit.TestGeoService
import ru.yandex.vertis.general.globe.testkit.TestGeoService.TestGeoService
import ru.yandex.vertis.general.users.logic.testkit.TestMiminoUserEnricher
import ru.yandex.vertis.general.users.logic.{CleanWebHelper, UserManager, UserStore, UserValidator}
import ru.yandex.vertis.general.users.logic.UserValidator.MaxAddressesPerUser
import ru.yandex.vertis.general.users.model.testkit.UserGen
import ru.yandex.vertis.general.users.model.{
  SellingAddress,
  UpdateUserError,
  User,
  UserAddresses,
  UserNotFound,
  YmlPhoneErrorCause
}
import ru.yandex.vertis.general.users.storage.ydb.{YdbQueueDao, YdbUserDao}
import common.zio.logging.Logging
import zio.random.Random
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestClock
import zio.{Task, ZLayer}

object DefaultUserManagerTest extends DefaultRunnableSpec {

  // ./ya tools tvmknife unittest user --default 1
  private val ticket: TicketBody =
    TicketBody(
      "3:user:CA0Q__________9_Gg4KAggBEAEg0oXYzAQoAQ:FUF1bTMjaFuxEyM1-TsldpgNPJ8mXrLR0fgdo-PQ_faY_gvS1wqbzRek0mLUWmhoShgwrUinCIHCfEIPhyTZcBHFhZ4FCm1vkSHPfARZKnf0Ok6aIw2FFWPvmoulU25nwDgL6UWKz-30IgAyTRReotwlOd8jZO_LKWCZT86c8nox"
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultUserManager")(
      testM("Prefer data from storage") {
        checkNM(1)(UserGen.anyUserWithName, UserGen.anyUserId) { (user, userId) =>
          for {
            _ <- UserManager.updateUser(userId, ticket, user)
            result <- UserManager.getUser(userId, ticket)
          } yield assert(result)(
            hasField("publicName", (_: User).publicName, equalTo(user.name)) &&
              hasField("addresses", (_: User).addresses, equalTo(user.addresses)) &&
              hasField("email", (_: User).email, isSome(equalTo("test@yandex.ru"))) &&
              hasField("phones", (_: User).phones, equalTo(Seq("+70000000001", "+70000000002", "+70000000003"))) &&
              hasField("avatar", (_: User).avatar, isSome(equalTo("avatar")))
          )
        }
      },
      testM("Use blackbox as fallback") {
        val userId = User.UserId(556L)
        for {
          result <- UserManager.getUser(userId, ticket)
        } yield assert(result)(
          hasField("publicName", (_: User).publicName, isSome(equalTo("test-user"))) &&
            hasField(
              "addresses",
              (_: User).addresses,
              equalTo(Seq.empty[SellingAddress])
            ) &&
            hasField("email", (_: User).email, isSome(equalTo("test@yandex.ru"))) &&
            hasField("phones", (_: User).phones, equalTo(Seq("+70000000001", "+70000000002", "+70000000003"))) &&
            hasField("avatar", (_: User).avatar, isSome(equalTo("avatar")))
        )
      },
      testM("Получение пользователей батчем") {
        val userId = User.UserId(556L)
        val userId2 = User.UserId(557L)
        for {
          res <- UserManager.getLimitedUserList(List(userId, userId2), FailPolicy.FAIL_FAST)
        } yield assert(res)(hasSize(equalTo(2)))
      },
      testM("Получение не существующих пользователей") {
        val userId = User.UserId(-5L)
        for {
          res <- UserManager.getUser(userId, ticket).run
        } yield assert(res)(fails(equalTo(UserNotFound(userId.id.toString()))))
      },
      testM("Получение не существующих пользователей (limited)") {
        val userId = User.UserId(-5L)
        for {
          res <- UserManager.getLimitedUser(userId).run
        } yield assert(res)(fails(equalTo(UserNotFound(userId.id.toString()))))
      },
      testM("Получение не существующих пользователей (batched, fail-fast)") {
        val userId = User.UserId(-5L)
        for {
          res <- UserManager.getLimitedUserList(List(userId), FailPolicy.FAIL_FAST).run
        } yield assert(res)(fails(equalTo(UserNotFound(userId.id.toString()))))
      },
      testM("Получение не существующих пользователей (batched, fail-never)") {
        val userId = User.UserId(-5L)
        for {
          res <- UserManager.getLimitedUserList(List(userId), FailPolicy.FAIL_NEVER)
        } yield assert(res)(isNonEmpty) &&
          assert(res.head._2.isDeleted)(isTrue)
      },
      testM("Crete user on first get") {
        val userId = User.UserId(556L)
        for {
          first <- UserManager.getUser(userId, ticket)
          created <- zio.clock.instant
          second <- UserManager.getUser(userId, ticket)
        } yield assert(first.created)(isNone) &&
          assert(second.created)(isSome(equalTo(created)))
      },
      testM("Get addresses from datasync") {
        val userId = User.UserId(556L)
        for {
          UserAddresses(home, work) <- UserManager.getUserAddresses(userId, ticket)
        } yield assert(home)(isSome) &&
          assert(work)(isSome) &&
          assert(home)(not(equalTo(work)))
      },
      testM("Validate addresses count") {
        val userId = User.UserId(556L)
        val addresses = Seq.fill(MaxAddressesPerUser + 1)(
          SellingAddress(SellingAddress.GeoPoint(0, 0), None, None, None, None)
        )
        for {
          failed <- UserManager.updateUser(userId, ticket, User.UserInput(None, addresses, None, None, None, None))
        } yield assert(failed)(
          contains(UpdateUserError.ExceededAddressesLimit(MaxAddressesPerUser + 1, MaxAddressesPerUser))
        )
      },
      testM("Validate phone numbers") {
        val userId = User.UserId(556L)
        val okPhone = "+74956665544"
        val notOkPhone = "+44666554433"
        for {
          success <- UserManager.updateUser(userId, ticket, User.UserInput(None, Nil, Some(okPhone), None, None, None))
          updated <- UserManager.getUser(userId, ticket)
          failed <- UserManager.updateUser(
            userId,
            ticket,
            User.UserInput(None, Nil, Some(notOkPhone), None, None, None)
          )
          notUpdated <- UserManager.getUser(userId, ticket)
        } yield assert(success)(isEmpty) &&
          assert(updated.ymlPhone.phone)(isSome(equalTo(okPhone))) &&
          assert(failed)(contains(UpdateUserError.InvalidYmlPhone(YmlPhoneErrorCause.InvalidRegion))) &&
          assert(notUpdated.ymlPhone.phone)(not(isSome(equalTo(notOkPhone))))
      }
    ).provideCustomLayer {
      val ydb = TestYdb.ydb
      val txRunner = ydb >>> Ydb.txRunner
      val userDao = YdbUserDao.live
      val clock = TestClock.any

      def createToponymFromCoordinates(latitude: Double, longitude: Double): Toponym =
        Toponym(
          Toponym.Toponym.Address(
            Address(
              regionId = 225,
              name = s"Это адрес ${(latitude * 1000000).round}",
              position = Some(GeoPosition(latitude = latitude, longitude = longitude))
            )
          )
        )

      val testGlobe = TestGeoService.layer >>> ZLayer.fromFunctionM[TestGeoService with GrpcClient[
        GeoServiceGrpc.GeoService
      ], Nothing, GrpcClient.Service[GeoServiceGrpc.GeoService]](geoService =>
        geoService.get
          .setReverseGeocodeResponse(request =>
            Task(
              ReverseGeocodeResponse(
                toponym =
                  Some(createToponymFromCoordinates(request.position.get.latitude, request.position.get.longitude))
              )
            )
          )
          .as(geoService.get[GrpcClient.Service[GeoServiceGrpc.GeoService]])
      )

      val userStore = (userDao ++ YdbQueueDao.live ++ clock) >>> UserStore.live

      val cleanWebHelper = CleanWebClientTest.live >>> CleanWebHelper.live

      val userValidator = cleanWebHelper >>> UserValidator.live

      val miminoUserEnricher = TestMiminoUserEnricher.layer

      val dictionary = TestBansDictionaryService.layer

      val deps =
        (ydb ++ clock) >+> (userDao ++ userStore ++ txRunner) ++ Logging.live ++ BlackboxClientTest.Test ++
          PersonalityClientTest.Test ++ testGlobe ++ userValidator ++ miminoUserEnricher ++ dictionary
      (deps >>> UserManager.live) ++ txRunner ++ Random.live
    }
  }
}
