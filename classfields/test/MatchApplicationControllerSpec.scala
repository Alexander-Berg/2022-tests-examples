package auto.dealers.match_maker.logic

import java.util.UUID

import ru.auto.api.search.SearchModel.{CatalogFilter, SearchRequestParameters}
import auto.dealers.match_maker.logic.clients.TeleponyClient
import auto.dealers.match_maker.logic.clients.TeleponyClient.TeleponyClient
import auto.dealers.match_maker.logic.dao.MatchApplicationDao
import ru.auto.match_maker.model.api.ApiModel._
import auto.dealers.match_maker.util.ApiExceptions.{
  ApiException,
  MatchApplicationExpiredException,
  MatchApplicationNotBelongToDealer,
  MatchApplicationNotFoundException
}
import auto.dealers.match_maker.util.TimestampUtils._
import ru.yandex.vertis.mockito.MockitoSupport._
import common.zio.app.Application.Application
import common.zio.app.{AppInfo, Application, Environments}
import common.zio.logging.Logging
import zio.test.Assertion._
import zio.test.{mock => _, _}
import zio.{Task, ULayer, ZIO}

import scala.concurrent.duration._

object MatchApplicationControllerSpec extends DefaultRunnableSpec {
  import MatchApplicationControllerSpecOps._

  def spec =
    suite("MatchApplicationController")(
      testM("Should create default match application") {
        for {
          defaultApp <- getDefaultMatchApplicationBuilder
          id <- MatchApplicationController.createMatchApplication(defaultApp.build(), 3.days, 10)
        } yield assertCompletes
      }.provideLayer(Logging.live ++ TestMatchApplicationDao.make.toLayer ++ TestBlacklistedPhones.empty),
      testM("Should not create app for blacklisted user") {
        for {
          defaultApp <- getDefaultMatchApplicationBuilder
          id <- MatchApplicationController.createMatchApplication(defaultApp.build(), 3.days, 10)
          result <- MatchApplicationDao.get(id).either
        } yield assert(result)(isLeft(isSubtype[MatchApplicationNotFoundException](anything)))
      }.provideLayer(
        Logging.live ++ TestMatchApplicationDao.make.toLayer ++ TestBlacklistedPhones.make("+79999999999")
      ),
      testM(
        "Should generate same ids for equal applications created in one minute so dao not throws duplicate exception"
      ) {
        for {
          firstApp <- getDefaultMatchApplicationBuilder
          _ <- MatchApplicationController.createMatchApplication(firstApp.build(), 3.days, 10)

          secondApp <- getDefaultMatchApplicationBuilder
          result <- MatchApplicationController.createMatchApplication(secondApp.build(), 3.days, 10).either
        } yield assert(result)(isRight(isNonEmptyString))
      }.provideLayer(Logging.live ++ TestMatchApplicationDao.make.toLayer ++ TestBlacklistedPhones.empty),
      testM("Should generate default redirect number for app in non-prod env") {
        for {
          redirect <- MatchApplicationController.getUserPhone(getAppWithPhone("+77777777777", 1L), 1L, 7.days)
        } yield assert(redirect.getRedirectPhone)(equalTo("+79999999999"))
      }.provideLayer(getRedirectPhoneMockedEnv()),
      testM("Should generate some specific number for app in prod env") {
        for {
          redirect <- MatchApplicationController.getUserPhone(getAppWithPhone("+77777777777", 1L), 1L, 7.days)
        } yield assert(redirect.getRedirectPhone)(equalTo("+79977997799"))
      }.provideLayer(getRedirectPhoneMockedEnv("+79977997799", Environments.Stable)),
      testM("Should fail redirect creating when user phone fails validation") {
        for {
          redirect <- MatchApplicationController.getUserPhone(getAppWithPhone("wrong", 1L), 1L, 7.days).either
        } yield assert(redirect)(isLeft(isSubtype[IllegalArgumentException](anything)))
      }.provideLayer(getRedirectPhoneMockedEnv(env = Environments.Stable)),
      testM("Should fail redirect creating when M.A. not belong to dealer") {
        for {
          redirect <- MatchApplicationController.getUserPhone(getAppWithPhone("wrong", 2L), 1L, 7.days).either
        } yield assert(redirect)(isLeft(isSubtype[MatchApplicationNotBelongToDealer](anything)))
      }.provideLayer(getRedirectPhoneMockedEnv(env = Environments.Stable)),
      testM("Should fail when application expired") {
        for {
          application <- ZIO.effectTotal(
            MatchApplication
              .newBuilder()
              .setExpireDate(now().proto)
              .addTarget(Target.newBuilder().setClientId("1"))
              .build()
          )
          redirect <-
            MatchApplicationController
              .getUserPhone(application, 1L, 7.days)
              .either
        } yield assert(redirect)(isLeft(isSubtype[MatchApplicationExpiredException](anything)))
      }.provideLayer(getRedirectPhoneMockedEnv())
    )
}

object MatchApplicationControllerSpecOps {

  def getDefaultMatchApplicationBuilder: ZIO[Any, Nothing, MatchApplication.Builder] =
    for {
      id <- ZIO.effectTotal(UUID.randomUUID().toString)
      userInfo <- ZIO.effectTotal(
        UserInfo
          .newBuilder()
          .setName("someName")
          .setUserId(72)
          .setPhone("+79999999999")
      )
      userProposal <- ZIO.effectTotal(
        UserProposal
          .newBuilder()
          .setSearchParams(
            SearchRequestParameters
              .newBuilder()
              .addCatalogFilter(CatalogFilter.newBuilder().setMark("BMW"))
              .addRid(213)
          )
      )
      app <- ZIO.effectTotal(
        MatchApplication
          .newBuilder()
          .setId(id)
          .setUserInfo(userInfo)
          .setUserProposal(userProposal)
      )
    } yield app

  def getRedirectPhoneMockedEnv(
      redirectPhone: String = "",
      env: Environments.Value = Environments.Development): ULayer[Application with TeleponyClient] = {

    val teleponyClient = ZIO.effectTotal {
      val telepony = mock[TeleponyClient.Service]
      when(telepony.getOrCreateRedirect(?, ?))
        .thenReturn(Task(RedirectPhoneInfo.newBuilder().setRedirectPhone(redirectPhone).build()))
      telepony
    }

    val application = ZIO.effectTotal {
      new Application.Service {
        override def info: ZIO[Any, Nothing, AppInfo] = ZIO.succeed(AppInfo(environment = env))
      }
    }

    application.toLayer ++ teleponyClient.toLayer
  }

  def getAppWithPhone(phone: String, dealerId: Long): MatchApplication =
    MatchApplication
      .newBuilder()
      .setUserInfo(UserInfo.newBuilder().setPhone(phone))
      .setExpireDate(now().add(7.day).proto)
      .addTarget(Target.newBuilder().setClientId(dealerId.toString))
      .build
}
