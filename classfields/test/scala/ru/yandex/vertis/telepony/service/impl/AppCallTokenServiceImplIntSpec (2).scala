package ru.yandex.vertis.telepony.service.impl

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}
import ru.yandex.vertis.telepony.model.{AppCallToken, AppCallTokenInfo, RedirectId, TypedDomain, Username}
import ru.yandex.vertis.telepony.model.TypedDomains.{autoru_def, billing_realty}
import ru.yandex.vertis.telepony.service.impl.AppCallTokenServiceImpl.{IllegalFieldsUpdateException, NotExistingTokenException, NotExistingTokenTargetPairException}

import scala.annotation.nowarn

class AppCallTokenServiceImplIntSpec extends SpecBase with IntegrationSpecTemplate with BeforeAndAfterEach {

  private lazy val service =
    new AppCallTokenServiceImpl(sharedDualDb, appCallTokenHistoryDao)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    sharedDualDb.master.run("clear", appCallTokenHistoryDao.clear()).futureValue
  }

  private def getTokenInfo(token: AppCallToken, redirectId: RedirectId): Option[AppCallTokenInfo] = {
    val action = appCallTokenHistoryDao.get(token, redirectId)
    sharedDualDb.master.run("get", action).futureValue
  }

  @nowarn
  private def assertAppCallTokenInfo(
      token: AppCallToken,
      redirectId: RedirectId,
      sourceUsername: Username,
      targetUsername: Option[Username],
      domain: TypedDomain
    )(result: Option[AppCallTokenInfo]): Unit = {
    val isEqual = result match {
      case Some(AppCallTokenInfo(`token`, `sourceUsername`, `targetUsername`, `domain`, `redirectId`, _)) => true
      case _ => false
    }
    isEqual shouldBe true
  }

  "AppCallTokenService" should {
    "create token" in {
      val token = AppCallToken("token")
      val redirectId = RedirectId("redirect-id")
      val domain = autoru_def
      val sourceUsername = "sourceUsername"
      val targetUsername = Some("targetUsername")

      service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId).futureValue

      val result = getTokenInfo(token, redirectId)
      assertAppCallTokenInfo(token, redirectId, sourceUsername, targetUsername, domain)(result)
    }

    "update targetUsername if was not defined" in {
      val token = AppCallToken("token")
      val redirectId = RedirectId("redirect-id")
      val domain = autoru_def
      val sourceUsername = "sourceUsername"
      val targetUsername = Some("targetUsername")

      service.createOrUpdateToken(token, sourceUsername, None, domain, redirectId).futureValue

      val result = getTokenInfo(token, redirectId)
      assertAppCallTokenInfo(token, redirectId, sourceUsername, None, domain)(result)

      service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId).futureValue

      val result2 = getTokenInfo(token, redirectId)
      assertAppCallTokenInfo(token, redirectId, sourceUsername, targetUsername, domain)(result2)
    }

    "skip update if the same" in {
      val token = AppCallToken("token")
      val redirectId = RedirectId("redirect-id")
      val domain = autoru_def
      val sourceUsername = "sourceUsername"
      val targetUsername = Some("targetUsername")

      service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId).futureValue
      val result = getTokenInfo(token, redirectId)
      assertAppCallTokenInfo(token, redirectId, sourceUsername, targetUsername, domain)(result)

      service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId).futureValue
      val list = sharedDualDb.slave.run("get", appCallTokenHistoryDao.list(token)).futureValue
      list shouldBe result.toList
    }

    "get source username" in {
      val token = AppCallToken("token")
      val redirectId = RedirectId("redirect-id")
      val domain = autoru_def
      val sourceUsername = "sourceUsername"
      val targetUsername = Some("targetUsername")

      service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId).futureValue
      val actualSourceUsername = service.getSourceUsername(token, targetUsername.get).futureValue
      actualSourceUsername shouldBe sourceUsername
    }

    "get domain" in {
      val token = AppCallToken("token")
      val redirectId = RedirectId("redirect-id")
      val domain = autoru_def
      val sourceUsername = "sourceUsername"
      val targetUsername = Some("targetUsername")

      service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId).futureValue

      val actualDomain = service.getDomain(token).futureValue
      actualDomain shouldBe domain
    }

    "get actual redirect" in {
      val token = AppCallToken("token")
      val domain = autoru_def
      val sourceUsername = "sourceUsername"
      val targetUsername = Some("targetUsername")

      val redirectId1 = RedirectId("redirect-id1")
      service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId1).futureValue

      val redirectId2 = RedirectId("redirect-id2")
      service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId2).futureValue

      val redirectId3 = RedirectId("redirect-id3")
      service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId3).futureValue
      val afterExpected = DateTime.now()

      val redirectId4 = RedirectId("redirect-id4")
      service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId4).futureValue

      val redirectId5 = RedirectId("redirect-id5")
      service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId5).futureValue

      val result = service.getActualRedirect(token, afterExpected).futureValue
      result shouldBe redirectId3
    }

    "fail" when {

      "update with another domain" in {
        val token = AppCallToken("token")
        val redirectId = RedirectId("redirect-id")
        val sourceUsername = "sourceUsername"
        val targetUsername = Some("targetUsername")

        service.createOrUpdateToken(token, sourceUsername, targetUsername, autoru_def, redirectId).futureValue
        val result = service.createOrUpdateToken(token, sourceUsername, targetUsername, billing_realty, redirectId)
        result.failed.futureValue shouldBe an[IllegalFieldsUpdateException]
      }

      "update with another source username" in {
        val token = AppCallToken("token")
        val redirectId = RedirectId("redirect-id")
        val domain = autoru_def
        val targetUsername = Some("targetUsername")

        service.createOrUpdateToken(token, "sourceUsername", targetUsername, domain, redirectId).futureValue
        val result = service.createOrUpdateToken(token, "anotherSourceUsername", targetUsername, domain, redirectId)
        result.failed.futureValue shouldBe an[IllegalFieldsUpdateException]
      }

      "update defined target username" in {
        val token = AppCallToken("token")
        val redirectId = RedirectId("redirect-id")
        val domain = autoru_def
        val sourceUsername = "sourceUsername"
        val targetUsername = Some("targetUsername")

        service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId).futureValue
        val result = service.createOrUpdateToken(token, sourceUsername, None, domain, redirectId)
        result.failed.futureValue shouldBe an[IllegalFieldsUpdateException]
      }

      "get not existing token" in {
        val result = service.getSourceUsername(AppCallToken("some_token"), "some_username")
        result.failed.futureValue shouldBe an[NotExistingTokenException]
      }

      "get source username with empty target username" in {
        val token = AppCallToken("token")
        val redirectId = RedirectId("redirect-id")
        val domain = autoru_def
        val sourceUsername = "sourceUsername"

        service.createOrUpdateToken(token, sourceUsername, None, domain, redirectId).futureValue

        val result = service.getSourceUsername(token, "targetUsername")
        result.failed.futureValue shouldBe an[NotExistingTokenTargetPairException]
      }

      "get source username with incorrect target username" in {
        val token = AppCallToken("token")
        val redirectId = RedirectId("redirect-id")
        val domain = autoru_def
        val sourceUsername = "sourceUsername"
        val targetUsername = Some("targetUsername")

        service.createOrUpdateToken(token, sourceUsername, targetUsername, domain, redirectId).futureValue

        val result = service.getSourceUsername(token, "another_targetUsername")
        result.failed.futureValue shouldBe an[NotExistingTokenTargetPairException]
      }

      "get domain if no one token exist" in {
        val result = service.getDomain(AppCallToken("some_token"))
        result.failed.futureValue shouldBe an[NotExistingTokenException]
      }

      "get actual redirect if no one token exist" in {
        val result = service.getActualRedirect(AppCallToken("some_token"), DateTime.now())
        result.failed.futureValue shouldBe an[NotExistingTokenException]
      }
    }
  }

}
