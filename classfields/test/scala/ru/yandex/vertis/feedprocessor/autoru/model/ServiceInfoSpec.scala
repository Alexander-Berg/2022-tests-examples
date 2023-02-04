package ru.yandex.vertis.feedprocessor.autoru.model

import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._

/**
  * @author pnaydenov
  */
class ServiceInfoSpec extends WordSpecBase {
  "ServiceInfo" should {
    "correctly serialize to json" in {
      val si = serviceInfoGen().next
      ServiceInfo.fromJson(si.toJson()) shouldEqual si
    }
  }
}
