package ru.yandex.vertis.billing.event

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * Spec on CampaignHeader [[Extractor]]
  *
  * @author ruslansd
  */
class CampaignHeaderExtractorSpec extends AnyWordSpec with Matchers with EventsProviders {

  "CampaignHeader" should {

    val m = Map(
      "billing.active.deadline" -> "2016-04-26T19:38:12.437Z",
      "billing.header.id" -> "dfc71245-6ce2-4ed6-b0e3-6ed044372d9f",
      "billing.header.name" -> "gchg",
      "billing.header.order.approximate_amount" -> "1000000",
      "billing.header.order.commit_amount" -> "1000000",
      "billing.header.order.id" -> "1014",
      "billing.header.order.memo" -> "-",
      "billing.header.order.owner.client_id" -> "1189606",
      "billing.header.order.owner.version" -> "1",
      "billing.header.order.product_key" -> "default",
      "billing.header.order.text" -> "Яндекс.Комм.Недвижимость, ГАРУС ВЛАДИМИР",
      "billing.header.order.total_income" -> "1000000",
      "billing.header.order.total_spent" -> "0",
      "billing.header.order.version" -> "1",
      "billing.header.owner.id.client_id" -> "1189606",
      "billing.header.owner.id.version" -> "1",
      "billing.header.owner.resource_ref@0.capa_partner_id" -> "1016376888",
      "billing.header.owner.resource_ref@0.version" -> "1",
      "billing.header.owner.resource_ref@1.capa_partner_id" -> "1062661679",
      "billing.header.owner.resource_ref@1.version" -> "1",
      "billing.header.owner.resource_ref@2.capa_partner_id" -> "1069051264",
      "billing.header.owner.resource_ref@2.version" -> "1",
      "billing.header.owner.resource_ref@3.capa_partner_id" -> "1069051265",
      "billing.header.owner.resource_ref@3.version" -> "1",
      "billing.header.owner.resource_ref@4.capa_partner_id" -> "1069052037",
      "billing.header.owner.resource_ref@4.version" -> "1",
      "billing.header.owner.resource_ref@5.capa_partner_id" -> "1069052039",
      "billing.header.owner.resource_ref@5.version" -> "1",
      "billing.header.owner.resource_ref@6.capa_partner_id" -> "123123",
      "billing.header.owner.resource_ref@6.version" -> "1",
      "billing.header.owner.version" -> "1",
      "billing.header.product.goods@0.placement.cost.version" -> "1",
      "billing.header.product.goods@0.placement.cost.per_indexing.units" -> "100",
      "billing.header.product.goods@0.version" -> "1",
      "billing.header.product.version" -> "1",
      "billing.header.settings.is_enabled" -> "true",
      "billing.header.settings.version" -> "1",
      "billing.header.version" -> "1",
      "billing.instance.id" -> "1ea292bd5b3b40c569de25201c01a98b",
      "format_version" -> "1",
      "offer_campaign_id" -> "dfc71245-6ce2-4ed6-b0e3-6ed044372d9f",
      "offer_id" -> "244178477940044768",
      "billing.offer.id" -> "244178477940044768",
      "offer_partner_id" -> "1069051265",
      "offer_url" -> "//realty.yandex.ru/offer/244178477940044768",
      "rid" -> "213",
      "timestamp" -> "2016-04-25T21:00:00.000Z",
      "tskv_format" -> "testing-realty-indexing-log"
    )

    "extract from specified map" in {
      val record = EventRecord(
        "test",
        "2016-04-25" :: "dfc71245-6ce2-4ed6-b0e3-6ed044372d9f" ::
          "21:00:00.000" :: "empty" ::
          "////realty.yandex.ru/offer/244178477940044768" :: Nil,
        m,
        None
      )
      val c = Extractor.getCampaignHeader(record).get
      info(c.toString)
    }
  }
}
