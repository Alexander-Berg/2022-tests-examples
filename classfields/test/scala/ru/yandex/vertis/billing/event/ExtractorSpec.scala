package ru.yandex.vertis.billing.event

import org.joda.time.{DateTimeZone, LocalDate, LocalTime}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.events.Dsl
import ru.yandex.vertis.billing.microcore_model.Properties
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.protobuf.kv.Converter

import scala.jdk.CollectionConverters._

/**
  * Specs on [[Extractor]]
  *
  * @author alesavin
  */
class ExtractorSpec extends AnyWordSpec with Matchers {

  /*
    cat /var/log/node-init-cluster/realty2/realty-front.log | grep "redirect" | tail -1 | tr "\t" "\n" | awk '{split($0,a,"="); print "\""a[1]"\" -> \""a[2]"\","; }'
   */

  "Extractor" should {

    "extract CampaignHeader" in {
      val record = EventRecord(
        "test",
        "" :: Nil,
        Map(
          "billing.header.order.memo" -> "-",
          "billing.header.order.total_income" -> "35000000",
          "billing.header.product.goods@0.placement.cost.version" -> "1",
          "billing.header.order.id" -> "1587",
          "billing.header.id" -> "0070f36e-2aa7-4986-af48-8f335b6b869b",
          "billing.header.order.owner.client_id" -> "7999760",
          "billing.header.order.owner.agency_id" -> "1160097",
          "billing.header.order.total_spent" -> "25000000",
          "billing.header.product.version" -> "1",
          "billing.header.name" -> "Лукино-Варино",
          "billing.header.order.text" -> "Яндекс.Недвижимость, ГК СУ 22",
          "billing.header.owner.resource_ref@0.developer_id" -> "205129",
          "billing.header.order.product_key" -> "default",
          "billing.header.version" -> "1",
          "billing.active.deadline" -> "2016-05-19T16:49:51.454Z",
          "billing.header.order.commit_amount" -> "10000000",
          "billing.offer.id" -> "7(921)7895782/7(812)3836301",
          "billing.header.owner.id.client_id" -> "7999760",
          "billing.instance.id" -> "da94f48b4f488791393bb8440f8b96e",
          "billing.header.owner.version" -> "1",
          "billing.header.owner.id.agency_id" -> "1160097",
          "billing.header.settings.call_settings.phone.phone" -> "7895782",
          "billing.header.settings.call_settings.objectId" -> "0070f36e-2aa7-4986-af48-8f335b6b869b",
          "billing.header.order.approximate_amount" -> "10000000",
          "billing.header.owner.id.version" -> "1",
          "billing.header.product.goods@0.placement.cost.per_call.units" -> "1155000",
          "billing.header.settings.call_settings.metrika_phone.country" -> "7",
          "billing.header.order.owner.version" -> "1",
          "billing.header.settings.call_settings.phone.code" -> "921",
          "billing.header.settings.call_settings.phone.country" -> "7",
          "billing.header.owner.resource_ref@0.version" -> "1",
          "billing.header.settings.is_enabled" -> "true",
          "billing.call.revenue" -> "0",
          "billing.header.settings.version" -> "1",
          "billing.header.order.version" -> "1",
          "billing.header.product.goods@0.version" -> "1",
          "billing.header.settings.call_settings.metrika_phone.phone" -> "3836301",
          "billing.header.settings.call_settings.metrika_phone.code" -> "812"
        ),
        None
      )

      val record2 = EventRecord(
        "test",
        "" :: Nil,
        Map(
          "view_type" -> "desktop",
          "offer_id" -> "site:83571",
          "site_id" -> "83571",
          "offer_category" -> "APARTMENT",
          "offer_type" -> "SELL",
          "offer_url" -> "https://realty.csfront01gt.yandex.ru/newbuilding/83571",
          "offer_traffic_from" -> "direct",
          "card_type" -> "newbuilding",
          "query_id" -> "45038f53438b9bf79ffbf367a0257c62",
          "phone_owner_id" -> "ag:205129",
          "offer_campaign_id" -> "0070f36e-2aa7-4986-af48-8f335b6b869b",
          "billing.header.order.memo" -> "-",
          "billing.header.order.total_income" -> "35000000",
          "billing.header.product.goods@0.placement.cost.version" -> "1",
          "billing.header.order.id" -> "1587",
          "billing.header.id" -> "0070f36e-2aa7-4986-af48-8f335b6b869b",
          "billing.header.order.owner.client_id" -> "7999760",
          "billing.header.order.owner.agency_id" -> "1160097",
          "billing.header.order.total_spent" -> "25330000",
          "billing.header.product.version" -> "1",
          "billing.header.name" -> "Лукино-Варино",
          "billing.header.settings.call_settings.redirectId" -> "-tjrf7XKlOE",
          "billing.header.order.text" -> "Яндекс.Недвижимость, ГК СУ 22",
          "billing.header.owner.resource_ref@0.developer_id" -> "205129",
          "billing.header.order.product_key" -> "default",
          "billing.header.version" -> "1",
          "billing.active.deadline" -> "2016-05-19T19:30:53.188Z",
          "billing.header.order.commit_amount" -> "9670000",
          "billing.offer.id" -> "7(921)7895782/7(812)3836301",
          "billing.header.settings.call_settings.redirectPhone.phone" -> "3836301",
          "billing.header.owner.id.client_id" -> "7999760",
          "billing.instance.id" -> "b4b67f7cb9fc8d3a9c839ac70f644f16",
          "billing.header.owner.version" -> "1",
          "billing.header.owner.id.agency_id" -> "1160097",
          "billing.header.settings.call_settings.phone.phone" -> "7895782",
          "billing.header.order.approximate_amount" -> "9670000",
          "billing.header.settings.call_settings.source_type" -> "METRIKA",
          //          "billing.header.settings.call_settings.source_type" -> "TELEPONY",
          "billing.header.owner.id.version" -> "1",
          "billing.header.product.goods@0.placement.cost.per_call.units" -> "1155000",
          "billing.header.settings.call_settings.metrika_phone.country" -> "7",
          "billing.header.order.owner.version" -> "1",
          "billing.header.settings.call_settings.phone.code" -> "921",
          "billing.header.settings.call_settings.phone.country" -> "7",
          "billing.header.owner.resource_ref@0.version" -> "1",
          "billing.header.settings.is_enabled" -> "true",
          "billing.call.revenue" -> "0",
          "billing.header.settings.version" -> "1",
          "billing.header.order.version" -> "1",
          "billing.header.settings.call_settings.redirectPhone.country" -> "7",
          "billing.header.product.goods@0.version" -> "1",
          "billing.header.settings.call_settings.redirectPhone.code" -> "812",
          "billing.header.settings.call_settings.metrika_phone.phone" -> "3836301",
          "billing.header.settings.call_settings.metrika_phone.code" -> "812",
          "phone_number" -> "+78123836301",
          "locale" -> "ru",
          "rid" -> "2",
          "portal_rid" -> "2",
          "testing_group" -> "1",
          "user_yandex_uid" -> "5401270671393777907",
          "user_ip" -> "2a02:6b8:0:5::32",
          "user_is_in_yandex" -> "true"
        ),
        None
      )

      val record4 = EventRecord(
        "test",
        "" :: Nil,
        Map(
          "tskv_format" -> "realty-front-log",
          "timestamp" -> "2017-05-31T10:00:13.991Z",
          "project" -> "realty-api",
          "format_version" -> "1",
          "component" -> "phone_show",
          "offer_campaign_id" -> "0e7ca5f2-4e5a-40fe-b2c4-58faf94a0986",
          "locale" -> "ru",
          "billing.header.order.memo" -> "-",
          "billing.header.order.total_income" -> "192877000",
          "billing.header.product.goods@0.placement.cost.version" -> "1",
          "billing.header.order.id" -> "1001",
          "billing.header.id" -> "0e7ca5f2-4e5a-40fe-b2c4-58faf94a0986",
          "billing.header.order.owner.client_id" -> "2698109",
          "billing.context.work_policy.days@1.to" -> "2017-06-03",
          "billing.header.order.total_spent" -> "191112000",
          "billing.header.product.version" -> "1",
          "billing.header.name" -> "ЭВРИКА",
          "billing.context.work_policy.days@0.from" -> "2017-05-31",
          "billing.header.settings.call_settings.redirectId" -> "HQzjDTpSxpU",
          "billing.context.work_policy.days@1.from" -> "2017-06-03",
          "billing.header.order.text" -> "Яндекс.Недвижимость, АДВЕКС",
          "billing.context.work_policy.days@0.to" -> "2017-06-02",
          "billing.header.owner.resource_ref@0.developer_id" -> "150902",
          "billing.header.order.product_key" -> "default",
          "billing.header.version" -> "1",
          "billing.context.work_policy.days@1.times@0.from" -> "11:00:00.000",
          "billing.active.deadline" -> "2017-05-31T11:29:17.395Z",
          "billing.header.order.commit_amount" -> "1765000",
          "billing.context.work_policy.days@2.from" -> "2017-06-04",
          "billing.offer.id" -> "7(812)3220055/7(812)3845667",
          "billing.header.settings.call_settings.redirectPhone.phone" -> "3845667",
          "billing.context.work_policy.days@2.to" -> "2017-06-04",
          "billing.header.owner.id.client_id" -> "2698109",
          "billing.instance.id" -> "7ef1716ce94f378fd969444b9d7a402a",
          "billing.header.owner.version" -> "1",
          "billing.header.settings.call_settings.phone.phone" -> "3220055",
          "billing.header.order.approximate_amount" -> "1765000",
          "billing.header.settings.call_settings.source_type" -> "TELEPONY",
          "billing.header.owner.id.version" -> "1",
          "billing.header.product.goods@0.placement.cost.per_call.units" -> "200000",
          "billing.header.settings.call_settings.metrika_phone.country" -> "7",
          "billing.context.work_policy.days@1.times@0.to" -> "17:00:00.000",
          "billing.header.order.owner.version" -> "1",
          "billing.header.settings.call_settings.phone.code" -> "812",
          "billing.header.settings.call_settings.phone.country" -> "7",
          "billing.header.owner.resource_ref@0.version" -> "1",
          "billing.header.settings.is_enabled" -> "true",
          "billing.call.revenue" -> "200000",
          "billing.header.settings.version" -> "1",
          "billing.context.version" -> "1",
          "billing.header.order.version" -> "1",
          "billing.header.settings.call_settings.redirectPhone.country" -> "7",
          "billing.header.product.goods@0.version" -> "1",
          "billing.context.work_policy.days@0.times@0.to" -> "20:00:00.000",
          "billing.context.work_policy.time_zone" -> "Europe/Moscow",
          "billing.context.work_policy.days@0.times@0.from" -> "10:00:00.000",
          "billing.header.settings.call_settings.redirectPhone.code" -> "812",
          "billing.header.settings.call_settings.metrika_phone.phone" -> "3845667",
          "billing.header.settings.call_settings.metrika_phone.code" -> "812",
          "user_uid" -> "92386907",
          "source" -> "map"
        )
      )
      val record5 = EventRecord(
        "test",
        "" :: Nil,
        Map(
          "tskv_format" -> "realty-front-log",
          "timestamp" -> "2017-05-31T11:40:56.953+0300",
          "project" -> "realty-api",
          "billing.header.owner.id.client_id" -> "7769680",
          "billing.header.settings.call_settings.phone.country" -> "7",
          "billing.header.order.commit_amount" -> "22370000",
          "billing.header.order.total_income" -> "130000000",
          "billing.header.owner.version" -> "1",
          "billing.header.owner.resource_ref@0.version" -> "1",
          "billing.header.owner.id.version" -> "1",
          "billing.context.work_policy.days@0.from" -> "2017-05-31",
          "billing.header.owner.resource_ref@0.developer_id" -> "188725",
          "billing.instance.id" -> "a286a5ffc99ef38e744149128b74e03f",
          "billing.header.name" -> "ВЛюблино",
          "billing.header.settings.call_settings.redirectPhone.country" -> "7",
          "billing.header.order.version" -> "1",
          "billing.header.order.approximate_amount" -> "22370000",
          "billing.context.version" -> "1",
          "billing.header.order.product_key" -> "default",
          "billing.active.deadline" -> "2017-05-31T10:13:16.685Z",
          "billing.header.settings.call_settings.metrika_phone.code" -> "495",
          "billing.header.settings.call_settings.source_type" -> "TELEPONY",
          "billing.offer.id" -> "7(495)9819607/7(495)7877985",
          "billing.header.settings.version" -> "1",
          "billing.header.settings.call_settings.phone.code" -> "495",
          "billing.header.settings.is_enabled" -> "true",
          "billing.header.version" -> "1",
          "billing.header.order.owner.client_id" -> "7769680",
          "billing.header.settings.call_settings.redirectPhone.code" -> "495",
          "billing.context.work_policy.days@0.times@0.from" -> "09:00:00.000",
          "billing.header.settings.call_settings.redirectPhone.phone" -> "7877985",
          "billing.header.settings.call_settings.redirectId" -> "qHdSI2f7K_4",
          "billing.header.id" -> "847f71e7-2e37-4bc0-a7d5-c384849ad255",
          "billing.header.settings.call_settings.phone.phone" -> "9819607",
          "billing.context.work_policy.days@0.to" -> "2017-06-04",
          "billing.header.order.total_spent" -> "107630000",
          "billing.header.product.goods@0.placement.cost.per_call.units" -> "6220000",
          "billing.header.order.owner.version" -> "1",
          "billing.header.order.id" -> "1524",
          "billing.header.settings.call_settings.metrika_phone.country" -> "7",
          "billing.header.order.memo" -> "-",
          "billing.header.product.goods@0.placement.cost.version" -> "1",
          "billing.call.revenue" -> "6220000",
          "billing.context.work_policy.days@0.times@0.to" -> "21:00:00.000",
          "billing.context.work_policy.time_zone" -> "Europe/Moscow",
          "billing.header.order.text" -> "Яндекс.Недвижимость, Мортон-Инвест",
          "billing.header.product.version" -> "1",
          "billing.header.settings.call_settings.metrika_phone.phone" -> "7877985",
          "billing.header.product.goods@0.version" -> "1",
          "offer_id" -> "null",
          "offer_partner_id" -> "null",
          "component" -> "phone_show",
          "time" -> "null",
          "offer_campaign_id" -> "847f71e7-2e37-4bc0-a7d5-c384849ad255",
          "locale" -> "ru"
        )
      )

      val ch = Extractor.getCampaignHeader(record).get
      info(ch.toString)
      info(ch.settings.toString)

      val ch2 = Extractor.getCampaignHeader(record2).get
      info(ch2.toString)
      info(ch2.settings.toString)

      val ch4 = Extractor.getCampaignHeader(record4).get
      val eventContext = Extractor.getEventContext(record4)
      info(ch4.toString)
      info(eventContext.toString)
      val ch5 = Extractor.getCampaignHeader(record5).get
      println(s"SETTINGS ${ch5.settings.callSettings}")
      val eventContext2 = Extractor.getEventContext(record5)
      info(ch5.toString)
      info(eventContext2.toString)
    }

    "extract work time policy" in {
      val timeIntervals =
        Iterable(
          Dsl.localTimeInterval(new LocalTime("09:00"), new LocalTime("13:00")),
          Dsl.localTimeInterval(new LocalTime("14:00"), new LocalTime("19:00"))
        )

      val dateInterval =
        Iterable(
          Dsl.localDateInterval(
            new LocalDate("2016-09-10"),
            new LocalDate("2016-09-13"),
            timeIntervals.asJavaCollection
          ),
          Dsl
            .localDateInterval(new LocalDate("2016-09-15"), new LocalDate("2016-09-17"), timeIntervals.asJavaCollection)
        )

      val workPolicy = Dsl.timetable(dateInterval.asJavaCollection, DateTimeZone.forID("+03:00"))
      val eventContext = Dsl.eventContext(workPolicy)
      val kv = Converter
        .toKeyValue(eventContext, Some(Properties.BILLING_EVENT_CONTEXT))
        .get
      val event = EventRecord("test", Nil, kv)
      Extractor.getEventContext(event).workPolicy match {
        case Some(parsed) =>
          parsed shouldBe Conversions.timetableFromMessage(workPolicy).get
        case other =>
          fail(s"Unexpected $other")
      }
    }

  }
}
