package ru.yandex.vertis.billing.event

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.event.BaggageExtractorSpec.{ExampleFromLog, WithNewPhoneShowId}
import ru.yandex.vertis.billing.event.Generator.EventRecordGen
import ru.yandex.vertis.billing.event.model.CallRevenueBaggageExtractor
import ru.yandex.vertis.billing.model_core.BaggagePayload.PhoneShowIdentifier
import ru.yandex.vertis.billing.model_core.gens.{PhoneShowIdentifierGen, Producer}
import ru.yandex.vertis.billing.model_core.{BaggagePayload, EmptyUser, EventTypes, YandexUid}
import ru.yandex.vertis.billing.settings.RealtyTasksServiceComponents

import scala.util.{Failure, Success}

/**
  * Spec on [[ru.yandex.vertis.billing.event.model.BaggageExtractor]]
  *
  * @author ruslansd
  */
class BaggageExtractorSpec extends AnyWordSpec with Matchers with EventsProviders {

  private val extractor =
    CallRevenueBaggageExtractor(RealtyTasksServiceComponents, EventTypes.CallsRevenue)

  private val user = YandexUid("10")

  val identifierGen: Gen[PhoneShowIdentifier] = for {
    id <- PhoneShowIdentifierGen
      .suchThat(s => s.redirectPhone.isDefined && s.tag.isDefined)
  } yield id

  val callEvent =
    EventRecordGen
      .next(1)
      .toList
      .map(withPhoneShowId(identifierGen.next))
      .map(withCallRevenue(10L))
      .map(withFields(Iterable((Extractor.YandexUidCellName, user.id))))
      .head

  "Baggage extractor" should {

    "extract event with yandex uid if it exists" in {

      extractor.extract(callEvent) match {
        case Success(baggage) =>
          baggage.user should be(user)
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "extract event without yandex uid" in {
      val without = callEvent.withoutValue(Extractor.YandexUidCellName)

      extractor.extract(without) match {
        case Success(baggage) =>
          baggage.user should be(EmptyUser)
        case other =>
          fail(s"Unexpected $other")
      }
    }
    "extract event with phone show id" in {

      extractor.extract(WithNewPhoneShowId) match {
        case Success(baggage) =>
          baggage.payload match {
            case BaggagePayload.CallWithOptRevenue(id, _, _) =>
              val phone =
                WithNewPhoneShowId.values
                  .get(Extractor.PhoneShowRedirectPhone)
                  .flatMap(Extractor.parsePhone)
              id.objectId shouldBe WithNewPhoneShowId.values(Extractor.PhoneShowObjectId)
              id.tag shouldBe WithNewPhoneShowId.values.get(Extractor.PhoneShowTag)
              id.redirectPhone shouldBe phone
            case other =>
              fail(s"Unexpected $other")
          }
        case Failure(e) =>
          fail(s"Unexpected", e)
      }
    }
    "extract example from log" in {

      extractor.extract(ExampleFromLog) match {
        case Success(baggage) =>
          baggage.payload match {
            case BaggagePayload.CallWithOptRevenue(id, _, _) =>
              id.tag shouldBe None
              id.redirectPhone.isDefined shouldBe true
            case other =>
              fail(s"Unexpected $other")
          }
        case Failure(e) =>
          fail(s"Unexpected", e)
      }
    }
  }

}

object BaggageExtractorSpec {

  val WithNewPhoneShowId = EventRecord(
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
      "billing.context.work_policy.days@2.to" -> "2017-06-04",
      "billing.header.owner.id.client_id" -> "2698109",
      "billing.instance.id" -> "7ef1716ce94f378fd969444b9d7a402a",
      "billing.header.owner.version" -> "1",
      "billing.header.order.approximate_amount" -> "1765000",
      "billing.header.owner.id.version" -> "1",
      "billing.header.product.goods@0.placement.cost.per_call.units" -> "200000",
      "billing.context.work_policy.days@1.times@0.to" -> "17:00:00.000",
      "billing.header.order.owner.version" -> "1",
      "billing.header.owner.resource_ref@0.version" -> "1",
      "billing.header.settings.is_enabled" -> "true",
      "billing.call.revenue" -> "200000",
      "billing.header.settings.version" -> "1",
      "billing.context.version" -> "1",
      "billing.header.order.version" -> "1",
      "billing.header.product.goods@0.version" -> "1",
      "billing.context.work_policy.days@0.times@0.to" -> "20:00:00.000",
      "billing.context.work_policy.time_zone" -> "Europe/Moscow",
      "billing.context.work_policy.days@0.times@0.from" -> "10:00:00.000",
      "user_uid" -> "92386907",
      "source" -> "map",
      Extractor.PhoneShowObjectId -> "0e7ca5f2-4e5a-40fe-b2c4-58faf94a0986",
      Extractor.PhoneShowRedirectPhone -> "+78123836308",
      Extractor.PhoneShowTag -> "2gis"
    )
  )

  val ExampleFromLog = EventRecord(
    "test",
    "" :: Nil,
    Map(
      "tskv_format" -> "realty-front-log",
      "timestamp" -> "2017-10-12T10:38:45.516Z",
      "project" -> "realty",
      "format_version" -> "1",
      "component" -> "card_show",
      "user_agent" -> "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 YaBrowser/17.9.1.768 Yowser/2.5 Safari/537.36",
      "view_type" -> "desktop",
      "position" -> "0",
      "offer_id" -> "site:51794",
      "site_id" -> "51794",
      "offer_category" -> "APARTMENT",
      "offer_type" -> "SELL",
      "card_type" -> "newbuilding",
      "query_id" -> "d2ece4db711dab0a161c0038fda1cfa2",
      "phone_owner_id" -> "ag:200667",
      "offer_campaign_id" -> "8922dfb6-25d9-4ce3-a5a8-077f6ff4988c",
      "billing.header.order.memo" -> "-",
      "billing.header.order.total_income" -> "52000000",
      "billing.header.product.goods@0.placement.cost.version" -> "1",
      "billing.header.order.id" -> "1569",
      "billing.header.id" -> "8922dfb6-25d9-4ce3-a5a8-077f6ff4988c",
      "billing.header.order.owner.client_id" -> "1021977",
      "billing.header.order.total_spent" -> "14108300",
      "billing.header.product.version" -> "1",
      "billing.header.name" -> "Пулковский 2",
      "billing.context.work_policy.days@0.from" -> "2017-10-12",
      "billing.header.order.text" -> "Яндекс.Недвижимость, Александр Недвижимость",
      "billing.context.work_policy.days@0.to" -> "2017-10-16",
      "billing.header.owner.resource_ref@0.developer_id" -> "200667",
      "billing.header.order.product_key" -> "default",
      "billing.phone.objectId" -> "8922dfb6-25d9-4ce3-a5a8-077f6ff4988c",
      "billing.header.version" -> "1",
      "billing.active.deadline" -> "2017-10-12T12:24:23.114Z",
      "billing.header.order.commit_amount" -> "37891700",
      "billing.offer.id" -> "7(812)2904890/51794",
      "billing.header.owner.id.client_id" -> "1021977",
      "billing.instance.id" -> "bd6fe0eb668353413cfed1bd44049ada",
      "billing.header.owner.version" -> "1",
      "billing.header.order.approximate_amount" -> "37891700",
      "billing.header.owner.id.version" -> "1",
      "billing.phone.redirectPhone" -> "+78122904890",
      "billing.header.product.goods@0.placement.cost.per_call.units" -> "6000000",
      "billing.header.order.owner.version" -> "1",
      "billing.header.owner.resource_ref@0.version" -> "1",
      "billing.header.settings.is_enabled" -> "true",
      "billing.call.revenue" -> "6000000",
      "billing.header.settings.version" -> "1",
      "billing.context.version" -> "1",
      "billing.header.order.version" -> "1",
      "billing.header.product.goods@0.version" -> "1",
      "billing.context.work_policy.days@0.times@0.to" -> "21:00:00.000",
      "billing.context.work_policy.time_zone" -> "Europe/Moscow",
      "billing.context.work_policy.days@0.times@0.from" -> "09:00:00.000",
      "locale" -> "ru",
      "rid" -> "2",
      "portal_rid" -> "9999",
      "testing_group" -> "4",
      "passport_testing_group" -> "16",
      "traffic_from" -> "direct",
      "user_yandex_uid" -> "13101531500283720",
      "user_ip" -> "77.88.2.232",
      "user_is_in_yandex" -> "true"
    )
  )

}
