package ru.yandex.vertis.telepony.dao.jdbc

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.generator.AppBackCallGenerator.AppBackCallGen
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.ActualRedirect
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

class JdbcAppBackCallDaoIntSpec extends SpecBase with IntegrationSpecTemplate with BeforeAndAfterEach {

  override protected def afterEach(): Unit = {
    super.afterEach()
    appBackCallDao.clear().futureValue
    redirectDaoV2.clear().databaseValue.futureValue
    operatorNumberDaoV2.clear().databaseValue.futureValue
  }

  private def createRedirect(): ActualRedirect = {
    val redirect = ActualRedirectGen.next
    operatorNumberDaoV2.create(redirect.source).databaseValue.futureValue
    redirectDaoV2.create(redirect).databaseValue.futureValue
    redirect
  }

  "AppBackCallDao" should {
    "check existing" in {
      val redirect = createRedirect()

      val appBackCall = AppBackCallGen.next.copy(redirect = redirect.asHistoryRedirect)
      appBackCallService.exists(appBackCall.id).futureValue shouldEqual false

      appBackCallService.createOrUpdate(appBackCall).futureValue
      appBackCallService.exists(appBackCall.id).futureValue shouldEqual true
    }
    "upsert" in {
      val redirect = createRedirect()

      val appBackCall = AppBackCallGen.next.copy(redirect = redirect.asHistoryRedirect)

      appBackCallService.createOrUpdate(appBackCall).futureValue
      appBackCallService.createOrUpdate(appBackCall).futureValue
    }
  }

}
