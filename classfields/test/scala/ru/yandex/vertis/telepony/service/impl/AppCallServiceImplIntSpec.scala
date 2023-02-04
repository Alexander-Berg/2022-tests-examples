package ru.yandex.vertis.telepony.service.impl

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.generator.AppCallGenerator.AppCallGen
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.ActualRedirect
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

class AppCallServiceImplIntSpec extends SpecBase with IntegrationSpecTemplate with BeforeAndAfterEach {

  override protected def afterEach(): Unit = {
    super.afterEach()
    appCallDao.clear().futureValue
    redirectDaoV2.clear().databaseValue.futureValue
    operatorNumberDaoV2.clear().databaseValue.futureValue
  }

  private def createRedirect(): ActualRedirect = {
    val redirect = ActualRedirectGen.next
    operatorNumberDaoV2.create(redirect.source).databaseValue.futureValue
    redirectDaoV2.create(redirect).databaseValue.futureValue
    redirect
  }

  "AppCallServiceImpl" should {
    "check existing" in {
      val redirect = createRedirect()

      val appCall = AppCallGen.next.copy(redirect = redirect.asHistoryRedirect)
      appCallService.exists(appCall.id).futureValue shouldEqual false

      appCallService.createOrUpdate(appCall).futureValue
      appCallService.exists(appCall.id).futureValue shouldEqual true
    }
  }

}
