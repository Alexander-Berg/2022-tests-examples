package ru.yandex.vertis.passport.service.log

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.dao.impl.mysql.{AutoruAuthLogDao, AutoruBlacklistLogDao, AutoruSmsLogDao, AutoruVerificationBadlogDao}
import ru.yandex.vertis.passport.model.{ApiPayload, AuthMethod, ClientInfo, RequestContext}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import ru.yandex.vertis.passport.util.mysql.DualDatabase

/**
  *
  * @author zvez
  */
class AutoruEventLogSpec extends WordSpec with SpecBase with MySqlSupport {

  val authLogDao = new AutoruAuthLogDao(DualDatabase(dbs.legacyLogs))
  val badAuthDao = new AutoruVerificationBadlogDao(DualDatabase(dbs.legacyUsers))
  val blacklistLogDao = new AutoruBlacklistLogDao(DualDatabase(dbs.legacyLogs))
  val smsLogDao = new AutoruSmsLogDao(DualDatabase(dbs.legacyUsers))
  val log = new AutoruEventLog(authLogDao, badAuthDao, blacklistLogDao, smsLogDao)

  implicit override val requestContext: RequestContext = wrap(
    ApiPayload("123", clientInfo = ClientInfo(ip = Some("127.0.0.1")))
  )

  "AutoruUserEventLog" should {
    "transform ipv4 address to long" in {
      import AutoruEventLog.ipAsNumber
      ipAsNumber(None) shouldBe 0L
      ipAsNumber(Some("wong")) shouldBe 0L
      ipAsNumber(Some("213.180.218.197")) shouldBe 3585399493L
      ipAsNumber(Some("84.201.164.77")) shouldBe 1422500941L
      ipAsNumber(Some("2a02:6b8:0:40c:99c9:656c:6a2e:a068")) shouldBe 0
    }

    "save UserLoggedIn event into auth_log" in {
      val event = ModelGenerators.userLoggedIn.next
      log.logEvent(event).futureValue

      val saved = authLogDao.getLast(event.userId, 1).futureValue.head
      saved.userId shouldBe event.userId
      saved.ipProxy shouldBe requestContext.clientInfo.ip
      event.authMethod match {
        case Some(AuthMethod.ImpersonatedBy(userId)) =>
          saved.transformFromUserId.value shouldBe userId
        case _ =>
          saved.transformFromUserId shouldNot be('defined)
      }

    }

    "save UserBadLogin event into verification_badlog" in {
      val event = ModelGenerators.userBadLogin.next
      log.logEvent(event).futureValue

      val lastEvents = badAuthDao.getLast(10).futureValue
      lastEvents.count(_.userValue == event.login) shouldBe 1
    }
  }

}
