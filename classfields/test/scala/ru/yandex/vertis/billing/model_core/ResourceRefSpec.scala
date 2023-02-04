package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * Specs on [[ResourceRef]]
  *
  * @author alesavin
  */
class ResourceRefSpec extends AnyWordSpec with Matchers {

  "ResourceRef" should {
    "be parsed from string" in {
      import ResourceRef._
      read("capa_partner_1008241476") should be(Some(PartnerRef("1008241476")))
      read("capa_partner_1") should be(Some(PartnerRef("1")))
      read("offline_biz") should be(Some(OfflineBizRef))
      read("developer_5555") should be(Some(DeveloperRef("5555")))
      read("user_autoru:1") should be(Some(UserResourceRef(AutoRuUid("1"))))
      read("user_puid:2") should be(Some(UserResourceRef(Uid(2L))))
    }
  }
}
