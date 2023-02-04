package ru.yandex.vertis.telepony.service

import java.net.URL

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator.ShortStr
import ru.yandex.vertis.telepony.generator.Producer._

class RecordServiceSpec extends SpecBase {

  "Record service" should {
    "replace host in s3 url" in {
      val key = ShortStr.next
      val s3Url = new URL(s"https://telepony.s3.mdst.yandex.net/$key")
      val proxyHost = "telepony.test.vertis.yandex.net/extfile"
      val proxyUrl = s"https://telepony.test.vertis.yandex.net/extfile/$key"
      RecordService.buildProxyUrl(s3Url, proxyHost) shouldBe proxyUrl
    }
  }
}
