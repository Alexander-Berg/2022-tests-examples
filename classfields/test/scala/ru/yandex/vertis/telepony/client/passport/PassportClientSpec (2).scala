package ru.yandex.vertis.telepony.client.passport

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.model.RefinedSource
import ru.yandex.vertis.telepony.service.PassportClient

/**
  * @author neron
  */
trait PassportClientSpec extends SpecBase with SimpleLogging {

  def passportClient: PassportClient

  "passport" should {
    "return user id by phone" in {
      val userId = passportClient.getUserId(RefinedSource("74955107044")).futureValue
      log.info(s"UserId: $userId")
    }
  }

}
