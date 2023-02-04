package ru.yandex.realty.unifier

import play.api.libs.json._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.util.crypto.Crypto

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 12.05.17
  */
@RunWith(classOf[JUnitRunner])
class SalesDepartmentsUnifierTest extends FlatSpec with Matchers {
  val crypto = new Crypto("^tz+nmyi3(crf$8k")
  val u = new SalesDepartmentsUnifier(crypto)

  "SalesDepartmentsUnifier" should "correct unify site in search" in {
    val json =
      Json.parse("""
        |{
        |"appMiniSnippetImages": [
        |"//avatars.mdst.yandex.net/get-realty/3022/newbuilding.189895.21069139/app_snippet_mini",
        |"//avatars.mdst.yandex.net/get-realty/3274/newbuilding.189895.21120511/app_snippet_mini",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120509/app_snippet_mini",
        |"//avatars.mdst.yandex.net/get-realty/3019/newbuilding.189895.21120507/app_snippet_mini",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120505/app_snippet_mini",
        |"//avatars.mdst.yandex.net/get-realty/2899/newbuilding.189895.21120503/app_snippet_mini",
        |"//avatars.mdst.yandex.net/get-realty/2957/newbuilding.189895.21120493/app_snippet_mini"
        |],
        |"filteredOffers": 4,
        |"name": "Родной Город. Каховская",
        |"developerName": "РГ-Девелопмент",
        |"appMiddleSnippetImages": [
        |"//avatars.mdst.yandex.net/get-realty/3022/newbuilding.189895.21069139/app_snippet_middle",
        |"//avatars.mdst.yandex.net/get-realty/3274/newbuilding.189895.21120511/app_snippet_middle",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120509/app_snippet_middle",
        |"//avatars.mdst.yandex.net/get-realty/3019/newbuilding.189895.21120507/app_snippet_middle",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120505/app_snippet_middle",
        |"//avatars.mdst.yandex.net/get-realty/2899/newbuilding.189895.21120503/app_snippet_middle",
        |"//avatars.mdst.yandex.net/get-realty/2957/newbuilding.189895.21120493/app_snippet_middle"
        |],
        |"locativeFullName": "в ЖК «Родной Город. Каховская»",
        |"location": {
        |"point": {
        |"latitude": 55.65456771850586,
        |"longitude": 37.602264404296875,
        |"precision": "EXACT"
        |},
        |"subjectFederationId": 1,
        |"metroStations": [
        |{
        |"metroGeoId": 20443,
        |"name": "Каховская",
        |"rgbColor": "29b1a6",
        |"timeToMetro": {
        |"value": 4,
        |"type": "ON_FOOT"
        |}
        |},
        |{
        |"metroGeoId": 20446,
        |"name": "Севастопольская",
        |"rgbColor": "a2a5b4",
        |"timeToMetro": {
        |"value": 7,
        |"type": "ON_FOOT"
        |}
        |},
        |{
        |"metroGeoId": 20445,
        |"name": "Нахимовский проспект",
        |"rgbColor": "a2a5b4",
        |"timeToMetro": {
        |"value": 15,
        |"type": "ON_FOOT"
        |}
        |},
        |{
        |"metroGeoId": 20425,
        |"name": "Варшавская",
        |"rgbColor": "29b1a6",
        |"timeToMetro": {
        |"value": 15,
        |"type": "ON_FOOT"
        |}
        |},
        |{
        |"metroGeoId": 20447,
        |"name": "Чертановская",
        |"rgbColor": "a2a5b4",
        |"timeToMetro": {
        |"value": 11,
        |"type": "ON_TRANSPORT"
        |}
        |}
        |],
        |"rgid": 193303,
        |"geoId": 213,
        |"address": "Внутренний проезд, вл. 8",
        |"settlementGeoId": 213,
        |"settlementRgid": 165705
        |},
        |"specialProposals": [
        |{
        |"specialProposalType": "discount",
        |"description": "Скидка 3%"
        |},
        |{
        |"specialProposalType": "mortgage",
        |"description": "Ипотека 11.4%",
        |"minRate": "11.4"
        |},
        |{
        |"specialProposalType": "installment",
        |"description": "Рассрочка на 1 год",
        |"interestFree": false,
        |"durationMonths": 12
        |}
        |],
        |"exactMatch": true,
        |"state": "UNFINISHED",
        |"zhkType": "ZHK",
        |"fullName": "ЖК «Родной Город. Каховская»",
        |"grouping": {
        |"totalApartments": 4,
        |"area": {
        |"from": 34.0,
        |"to": 104.0
        |}
        |},
        |"price": {
        |"minPricePerMeter": 204598,
        |"rooms": {
        |"1": {
        |"hasOffers": false,
        |"soldout": false,
        |"to": 9342300,
        |"priceRatioToMarket": 0,
        |"status": "ON_SALE",
        |"from": 7028230,
        |"currency": "RUR",
        |"areas": {
        |"from": "34.2",
        |"to": "45.1"
        |}
        |},
        |"2": {
        |"hasOffers": false,
        |"soldout": false,
        |"to": 16770600,
        |"priceRatioToMarket": 0,
        |"status": "ON_SALE",
        |"from": 11869200,
        |"currency": "RUR",
        |"areas": {
        |"from": "54",
        |"to": "70.3"
        |}
        |},
        |"OPEN_PLAN": {
        |"soldout": false,
        |"currency": "RUR",
        |"hasOffers": false,
        |"priceRatioToMarket": 0
        |},
        |"STUDIO": {
        |"soldout": false,
        |"currency": "RUR",
        |"hasOffers": false,
        |"priceRatioToMarket": 0
        |},
        |"3": {
        |"hasOffers": false,
        |"soldout": false,
        |"to": 21558020,
        |"priceRatioToMarket": 0,
        |"status": "ON_SALE",
        |"from": 17104470,
        |"currency": "RUR",
        |"areas": {
        |"from": "83.6",
        |"to": "92.3"
        |}
        |},
        |"PLUS_4": {
        |"hasOffers": false,
        |"soldout": false,
        |"to": 26596200,
        |"priceRatioToMarket": 0,
        |"status": "ON_SALE",
        |"from": 21506100,
        |"currency": "RUR",
        |"areas": {
        |"from": "104.5",
        |"to": "114"
        |}
        |}
        |},
        |"to": 26596200,
        |"priceRatioToMarket": 0,
        |"maxPricePerMeter": 310566,
        |"from": 7028230,
        |"currency": "RUR",
        |"totalOffers": 0
        |},
        |"buildingClass": "BUSINESS",
        |"sourceNames": [
        |"",
        |"",
        |"",
        |"",
        |"",
        |"",
        |""
        |],
        |"locativeName": "в «Родной Город. Каховская»",
        |"appLargeImages": [
        |"//avatars.mdst.yandex.net/get-realty/3022/newbuilding.189895.21069139/app_large",
        |"//avatars.mdst.yandex.net/get-realty/3274/newbuilding.189895.21120511/app_large",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120509/app_large",
        |"//avatars.mdst.yandex.net/get-realty/3019/newbuilding.189895.21120507/app_large",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120505/app_large",
        |"//avatars.mdst.yandex.net/get-realty/2899/newbuilding.189895.21120503/app_large",
        |"//avatars.mdst.yandex.net/get-realty/2957/newbuilding.189895.21120493/app_large"
        |],
        |"appLargeSnippetImages": [
        |"//avatars.mdst.yandex.net/get-realty/3022/newbuilding.189895.21069139/app_snippet_large",
        |"//avatars.mdst.yandex.net/get-realty/3274/newbuilding.189895.21120511/app_snippet_large",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120509/app_snippet_large",
        |"//avatars.mdst.yandex.net/get-realty/3019/newbuilding.189895.21120507/app_snippet_large",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120505/app_snippet_large",
        |"//avatars.mdst.yandex.net/get-realty/2899/newbuilding.189895.21120503/app_snippet_large",
        |"//avatars.mdst.yandex.net/get-realty/2957/newbuilding.189895.21120493/app_snippet_large"
        |],
        |"id": 189895,
        |"updateDate": "2017-04-26T07:51:24.142Z",
        |"appSmallSnippetImages": [
        |"//avatars.mdst.yandex.net/get-realty/3022/newbuilding.189895.21069139/app_snippet_small",
        |"//avatars.mdst.yandex.net/get-realty/3274/newbuilding.189895.21120511/app_snippet_small",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120509/app_snippet_small",
        |"//avatars.mdst.yandex.net/get-realty/3019/newbuilding.189895.21120507/app_snippet_small",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120505/app_snippet_small",
        |"//avatars.mdst.yandex.net/get-realty/2899/newbuilding.189895.21120503/app_snippet_small",
        |"//avatars.mdst.yandex.net/get-realty/2957/newbuilding.189895.21120493/app_snippet_small"
        |],
        |"filteredArea": {
        |"from": 34.0,
        |"to": 104.0
        |},
        |"appMiddleImages": [
        |"//avatars.mdst.yandex.net/get-realty/3022/newbuilding.189895.21069139/app_middle",
        |"//avatars.mdst.yandex.net/get-realty/3274/newbuilding.189895.21120511/app_middle",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120509/app_middle",
        |"//avatars.mdst.yandex.net/get-realty/3019/newbuilding.189895.21120507/app_middle",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120505/app_middle",
        |"//avatars.mdst.yandex.net/get-realty/2899/newbuilding.189895.21120503/app_middle",
        |"//avatars.mdst.yandex.net/get-realty/2957/newbuilding.189895.21120493/app_middle"
        |],
        |"finishedApartments": false,
        |"salesDepartment": {
        |"isRedirectPhones": false,
        |"name": "Dizzy test",
        |"dump": {
        |"billing.header.order.memo": "-",
        |"billing.header.order.total_income": "1000000000",
        |"billing.header.product.goods@0.placement.cost.version": "1",
        |"billing.header.order.id": "2000",
        |"billing.header.id": "e5815e72-a5a7-450b-9b50-9c7b2efe1465",
        |"billing.header.order.owner.client_id": "38846439",
        |"billing.context.work_policy.days@1.to": "2017-05-14",
        |"billing.header.order.total_spent": "0",
        |"billing.header.product.version": "1",
        |"billing.header.name": "Родной Город. Каховская",
        |"billing.context.work_policy.days@0.from": "2017-05-12",
        |"billing.header.settings.call_settings.redirectId": "EGndK1KhyMs",
        |"billing.context.work_policy.days@1.from": "2017-05-13",
        |"billing.header.order.text": "Яндекс.Недвижимость, Dizzy realty",
        |"billing.context.work_policy.days@0.to": "2017-05-12",
        |"billing.header.owner.resource_ref@0.developer_id": "312637",
        |"billing.header.order.product_key": "default",
        |"billing.context.work_policy.days@2.times@0.to": "21:00:00.000",
        |"billing.header.version": "1",
        |"billing.active.deadline": "2017-05-12T12:18:37.990Z",
        |"billing.header.order.commit_amount": "1000000000",
        |"billing.context.work_policy.days@2.from": "2017-05-15",
        |"billing.offer.id": "7(495)3333333/7(495)3635406",
        |"billing.header.settings.call_settings.redirectPhone.phone": "3635406",
        |"billing.context.work_policy.days@2.to": "2017-05-16",
        |"billing.header.owner.id.client_id": "38846439",
        |"billing.instance.id": "c93f4c09a8d48ca0fcb9bfd917f07d7d",
        |"billing.header.owner.version": "1",
        |"billing.header.settings.call_settings.phone.phone": "3333333",
        |"billing.header.order.approximate_amount": "1000000000",
        |"billing.header.settings.call_settings.source_type": "TELEPONY",
        |"billing.header.owner.id.version": "1",
        |"billing.header.product.goods@0.placement.cost.per_call.units": "17435000",
        |"billing.header.settings.call_settings.metrika_phone.country": "7",
        |"billing.header.order.owner.version": "1",
        |"billing.header.settings.call_settings.phone.code": "495",
        |"billing.header.settings.call_settings.phone.country": "7",
        |"billing.header.owner.resource_ref@0.version": "1",
        |"billing.header.settings.is_enabled": "true",
        |"billing.call.revenue": "6175000",
        |"billing.context.work_policy.days@2.times@0.from": "09:00:00.000",
        |"billing.header.settings.version": "1",
        |"billing.context.version": "1",
        |"billing.header.order.version": "1",
        |"billing.header.settings.call_settings.redirectPhone.country": "7",
        |"billing.header.product.goods@0.version": "1",
        |"billing.context.work_policy.days@0.times@0.to": "21:00:00.000",
        |"billing.context.work_policy.time_zone": "Europe/Moscow",
        |"billing.context.work_policy.days@0.times@0.from": "09:00:00.000",
        |"billing.header.settings.call_settings.redirectPhone.code": "495",
        |"billing.header.settings.call_settings.metrika_phone.phone": "3635406",
        |"billing.header.settings.call_settings.metrika_phone.code": "495"
        |},
        |"weekTimetable": [
        |{
        |"dayFrom": 1,
        |"dayTo": 5,
        |"timePattern": [
        |{
        |"open": "09:00",
        |"close": "21:00"
        |}
        |]
        |}
        |],
        |"phones": [
        |"+74953635406"
        |],
        |"id": 312637
        |},
        |"developers": [
        |{
        |"name": "РГ-Девелопмент",
        |"url": "http://rg-dev.ru/",
        |"logo": "//avatars.mdst.yandex.net/get-realty/3274/company.236596.21084070/builder_logo_info",
        |"id": 279844,
        |"objects": {
        |"finished": 0,
        |"unfinished": 4
        |}
        |}
        |],
        |"fullImages": [
        |"//avatars.mdst.yandex.net/get-realty/3022/newbuilding.189895.21069139/large",
        |"//avatars.mdst.yandex.net/get-realty/3274/newbuilding.189895.21120511/large",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120509/large",
        |"//avatars.mdst.yandex.net/get-realty/3019/newbuilding.189895.21120507/large",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120505/large",
        |"//avatars.mdst.yandex.net/get-realty/2899/newbuilding.189895.21120503/large",
        |"//avatars.mdst.yandex.net/get-realty/2957/newbuilding.189895.21120493/large"
        |],
        |"siteSpecialProposals": [
        |{
        |"specialProposalType": "discount",
        |"description": "Скидка 3%"
        |},
        |{
        |"specialProposalType": "mortgage",
        |"description": "Ипотека 10.0%",
        |"minRate": "10.0"
        |},
        |{
        |"specialProposalType": "installment",
        |"description": "Рассрочка на 1 год",
        |"interestFree": false,
        |"durationMonths": 12
        |}
        |],
        |"promoPhoneNumbers": [
        |"84952213306"
        |],
        |"minicardImages": [
        |"//avatars.mdst.yandex.net/get-realty/3022/newbuilding.189895.21069139/minicard",
        |"//avatars.mdst.yandex.net/get-realty/3274/newbuilding.189895.21120511/minicard",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120509/minicard",
        |"//avatars.mdst.yandex.net/get-realty/3019/newbuilding.189895.21120507/minicard",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120505/minicard",
        |"//avatars.mdst.yandex.net/get-realty/2899/newbuilding.189895.21120503/minicard",
        |"//avatars.mdst.yandex.net/get-realty/2957/newbuilding.189895.21120493/minicard"
        |],
        |"deliveryDates": [
        |{
        |"houses": 3,
        |"polygons": [],
        |"finished": false,
        |"year": 2018,
        |"quarter": 2,
        |"phaseName": "1 очередь"
        |}
        |],
        |"images": [
        |"//avatars.mdst.yandex.net/get-realty/3022/newbuilding.189895.21069139/main",
        |"//avatars.mdst.yandex.net/get-realty/3274/newbuilding.189895.21120511/main",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120509/main",
        |"//avatars.mdst.yandex.net/get-realty/3019/newbuilding.189895.21120507/main",
        |"//avatars.mdst.yandex.net/get-realty/2941/newbuilding.189895.21120505/main",
        |"//avatars.mdst.yandex.net/get-realty/2899/newbuilding.189895.21120503/main",
        |"//avatars.mdst.yandex.net/get-realty/2957/newbuilding.189895.21120493/main"
        |]
        |}
      """.stripMargin)

    val unified = u.unify(json, UnificationContext(isMobile = true, antirobotDegradation = false))
    println(Json.prettyPrint(unified))
    (unified \\ "statParams").nonEmpty should be(true)
    (unified \\ "dump").nonEmpty should be(true)
    (unified \\ "dump").head should be(JsNull)
  }

  it should "correct unify site in card" in {
    val json = Json.parse(
      """
         |{
         |"construction": [{"year": 2016, "quarter": 3, "photos": [{"mini": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137973/minicard", "appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137973/app_snippet_middle", "full": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137973/large", "appLarge": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137973/app_large", "appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137973/app_snippet_mini", "appMiddle": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137973/app_middle", "appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137973/app_snippet_small", "appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137973/app_snippet_large", "cosmic": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137973/cosmic"}, {"mini": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137971/minicard", "appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137971/app_snippet_middle", "full": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137971/large", "appLarge": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137971/app_large", "appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137971/app_snippet_mini", "appMiddle": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137971/app_middle", "appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137971/app_snippet_small", "appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137971/app_snippet_large", "cosmic": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137971/cosmic"}, {"mini": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137965/minicard", "appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137965/app_snippet_middle", "full": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137965/large", "appLarge": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137965/app_large", "appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137965/app_snippet_mini", "appMiddle": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137965/app_middle", "appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137965/app_snippet_small", "appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137965/app_snippet_large", "cosmic": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137965/cosmic"}]}, {"year": 2016, "quarter": 4, "photos": [{"mini": "//avatars.mdst.yandex.net/get-realty/3013/newbuilding.21504.21138001/minicard", "appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/3013/newbuilding.21504.21138001/app_snippet_middle", "full": "//avatars.mdst.yandex.net/get-realty/3013/newbuilding.21504.21138001/large", "appLarge": "//avatars.mdst.yandex.net/get-realty/3013/newbuilding.21504.21138001/app_large", "appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/3013/newbuilding.21504.21138001/app_snippet_mini", "appMiddle": "//avatars.mdst.yandex.net/get-realty/3013/newbuilding.21504.21138001/app_middle", "appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/3013/newbuilding.21504.21138001/app_snippet_small", "appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/3013/newbuilding.21504.21138001/app_snippet_large", "cosmic": "//avatars.mdst.yandex.net/get-realty/3013/newbuilding.21504.21138001/cosmic"}, {"mini": "//avatars.mdst.yandex.net/get-realty/2935/newbuilding.21504.21137997/minicard", "appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2935/newbuilding.21504.21137997/app_snippet_middle", "full": "//avatars.mdst.yandex.net/get-realty/2935/newbuilding.21504.21137997/large", "appLarge": "//avatars.mdst.yandex.net/get-realty/2935/newbuilding.21504.21137997/app_large", "appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2935/newbuilding.21504.21137997/app_snippet_mini", "appMiddle": "//avatars.mdst.yandex.net/get-realty/2935/newbuilding.21504.21137997/app_middle", "appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2935/newbuilding.21504.21137997/app_snippet_small", "appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2935/newbuilding.21504.21137997/app_snippet_large", "cosmic": "//avatars.mdst.yandex.net/get-realty/2935/newbuilding.21504.21137997/cosmic"}, {"mini": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137991/minicard", "appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137991/app_snippet_middle", "full": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137991/large", "appLarge": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137991/app_large", "appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137991/app_snippet_mini", "appMiddle": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137991/app_middle", "appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137991/app_snippet_small", "appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137991/app_snippet_large", "cosmic": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21137991/cosmic"}, {"mini": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137975/minicard", "appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137975/app_snippet_middle", "full": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137975/large", "appLarge": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137975/app_large", "appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137975/app_snippet_mini", "appMiddle": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137975/app_middle", "appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137975/app_snippet_small", "appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137975/app_snippet_large", "cosmic": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137975/cosmic"}, {"mini": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137977/minicard", "appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137977/app_snippet_middle", "full": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137977/large", "appLarge": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137977/app_large", "appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137977/app_snippet_mini", "appMiddle": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137977/app_middle", "appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137977/app_snippet_small", "appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137977/app_snippet_large", "cosmic": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137977/cosmic"}]}, {"year": 2017, "quarter": 1, "photos": [{"mini": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21138015/minicard", "appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21138015/app_snippet_middle", "full": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21138015/large", "appLarge": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21138015/app_large", "appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21138015/app_snippet_mini", "appMiddle": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21138015/app_middle", "appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21138015/app_snippet_small", "appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21138015/app_snippet_large", "cosmic": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21138015/cosmic"}, {"mini": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21138009/minicard", "appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21138009/app_snippet_middle", "full": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21138009/large", "appLarge": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21138009/app_large", "appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21138009/app_snippet_mini", "appMiddle": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21138009/app_middle", "appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21138009/app_snippet_small", "appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21138009/app_snippet_large", "cosmic": "//avatars.mdst.yandex.net/get-realty/3274/newbuilding.21504.21138009/cosmic"}]}],
         |"name": "Водный",
         |"locativeFullName": "в квартале «Водный»",
         |"permalink": "1677416017",
         |"location": {
         |"point": {
         |"latitude": 55.840728759765625,
         |"longitude": 37.49440383911133,
         |"precision": "EXACT"
         |},
         |"subjectFederationId": 1,
         |"metroStations": [
         |{
         |"metroGeoId": 20370,
         |"name": "Водный Стадион",
         |"rgbColor": "0a6f20",
         |"timeToMetro": {
         |"value": 8,
         |"type": "ON_FOOT"
         |}
         |},
         |{
         |"metroGeoId": 20369,
         |"name": "Речной Вокзал",
         |"rgbColor": "0a6f20",
         |"timeToMetro": {
         |"value": 13,
         |"type": "ON_TRANSPORT"
         |}
         |},
         |{
         |"metroGeoId": 20371,
         |"name": "Войковская",
         |"rgbColor": "0a6f20",
         |"timeToMetro": {
         |"value": 13,
         |"type": "ON_TRANSPORT"
         |}
         |},
         |{
         |"metroGeoId": 152923,
         |"name": "Коптево",
         |"rgbColor": "",
         |"timeToMetro": {
         |"value": 16,
         |"type": "ON_TRANSPORT"
         |}
         |},
         |{
         |"metroGeoId": 152922,
         |"name": "Балтийская",
         |"rgbColor": "",
         |"timeToMetro": {
         |"value": 22,
         |"type": "ON_TRANSPORT"
         |}
         |}
         |],
         |"rgid": 193295,
         |"geoId": 213,
         |"address": "Москва, Кронштадтский бул., 6, к. 1-6",
         |"settlementGeoId": 213,
         |"settlementRgid": 165705
         |},
         |"specialProposals": {
         |"mortgagesSummary": [
         |{
         |"maxRate": "14.0",
         |"maxAmount": 60000000,
         |"bankName": "Сбербанк",
         |"minDuration": 12,
         |"programs": [],
         |"minRate": "12.9",
         |"minInitialPayment": 15.0,
         |"minAmount": 300000,
         |"maxDuration": 360
         |},
         |{
         |"maxRate": "14.7",
         |"maxAmount": 200000000,
         |"bankName": "ВТБ 24",
         |"minDuration": 12,
         |"programs": [],
         |"minRate": "11.4",
         |"minInitialPayment": 15.0,
         |"minAmount": 500000,
         |"maxDuration": 600
         |},
         |{
         |"maxRate": "14.25",
         |"maxAmount": 30000000,
         |"bankName": "Банк Возрождение",
         |"minDuration": 12,
         |"programs": [],
         |"minRate": "11.4",
         |"minInitialPayment": 5.0,
         |"minAmount": 300000,
         |"maxDuration": 360
         |}
         |],
         |"allMortgages": [
         |{
         |"maxRate": "14.7",
         |"maxAmount": 200000000,
         |"bankName": "ВТБ 24",
         |"minDuration": 12,
         |"programs": [
         |{
         |"maxRate": 14.0,
         |"name": "Ипотека 11.4%",
         |"maxAmount": 8000000,
         |"minDuration": 60,
         |"isMilitary": false,
         |"minRate": 11.399999618530273,
         |"minInitialPayment": 20.0,
         |"maxDuration": 360
         |},
         |{
         |"maxRate": 14.699999809265137,
         |"name": "Ипотека 13.35%",
         |"maxAmount": 200000000,
         |"minDuration": 12,
         |"isPrivileged": false,
         |"isMilitary": false,
         |"minRate": 13.350000381469727,
         |"minInitialPayment": 15.0,
         |"minAmount": 500000,
         |"maxDuration": 600
         |}
         |],
         |"minRate": "11.4",
         |"minInitialPayment": 15.0,
         |"minAmount": 500000,
         |"maxDuration": 600
         |},
         |{
         |"maxRate": "11.95",
         |"maxAmount": 30000000,
         |"bankName": "Банк Открытие",
         |"minDuration": 60,
         |"programs": [
         |{
         |"maxRate": 11.949999809265137,
         |"name": "Ипотека 11.95%",
         |"maxAmount": 30000000,
         |"minDuration": 60,
         |"isPrivileged": true,
         |"isMilitary": false,
         |"minRate": 11.949999809265137,
         |"minInitialPayment": 20.0,
         |"minAmount": 500000,
         |"maxDuration": 360
         |}
         |],
         |"minRate": "11.95",
         |"minInitialPayment": 20.0,
         |"minAmount": 500000,
         |"maxDuration": 360
         |},
         |{
         |"maxRate": "14.85",
         |"maxAmount": 200000000,
         |"bankName": "ВТБ Банк Москвы",
         |"minDuration": 12,
         |"programs": [
         |{
         |"maxRate": 14.850000381469727,
         |"name": "Ипотека 13.85%",
         |"maxAmount": 200000000,
         |"minDuration": 12,
         |"isPrivileged": false,
         |"isMilitary": false,
         |"minRate": 13.850000381469727,
         |"minInitialPayment": 40.0,
         |"minAmount": 500000,
         |"maxDuration": 600
         |},
         |{
         |"maxRate": 14.350000381469727,
         |"name": "Ипотека 13.35%",
         |"maxAmount": 200000000,
         |"minDuration": 12,
         |"isPrivileged": false,
         |"isMilitary": false,
         |"minRate": 13.350000381469727,
         |"minInitialPayment": 15.0,
         |"minAmount": 500000,
         |"maxDuration": 600
         |},
         |{
         |"name": "Ипотека 11.4%",
         |"maxAmount": 8000000,
         |"minDuration": 36,
         |"isPrivileged": true,
         |"isMilitary": false,
         |"minRate": 11.399999618530273,
         |"minInitialPayment": 20.0,
         |"maxDuration": 360
         |}
         |],
         |"minRate": "11.4",
         |"minInitialPayment": 15.0,
         |"minAmount": 500000,
         |"maxDuration": 600
         |},
         |{
         |"maxRate": "14.25",
         |"maxAmount": 30000000,
         |"bankName": "Банк Возрождение",
         |"minDuration": 12,
         |"programs": [
         |{
         |"maxRate": 13.5,
         |"name": "Ипотека 13%",
         |"maxAmount": 30000000,
         |"minDuration": 12,
         |"isPrivileged": false,
         |"isMilitary": false,
         |"minRate": 13.0,
         |"minInitialPayment": 5.0,
         |"maxDuration": 360
         |},
         |{
         |"maxRate": 14.25,
         |"name": "Ипотека 11.4%",
         |"maxAmount": 8000000,
         |"minDuration": 12,
         |"isMilitary": false,
         |"minRate": 11.399999618530273,
         |"minInitialPayment": 20.0,
         |"minAmount": 300000,
         |"maxDuration": 360
         |}
         |],
         |"minRate": "11.4",
         |"minInitialPayment": 5.0,
         |"minAmount": 300000,
         |"maxDuration": 360
         |},
         |{
         |"maxRate": "14.0",
         |"maxAmount": 60000000,
         |"bankName": "Сбербанк",
         |"minDuration": 12,
         |"programs": [
         |{
         |"maxRate": 14.0,
         |"name": "Ипотека 12.9%",
         |"maxAmount": 60000000,
         |"minDuration": 12,
         |"isPrivileged": false,
         |"isMilitary": false,
         |"minRate": 12.899999618530273,
         |"minInitialPayment": 15.0,
         |"minAmount": 300000,
         |"maxDuration": 360
         |}
         |],
         |"minRate": "12.9",
         |"minInitialPayment": 15.0,
         |"minAmount": 300000,
         |"maxDuration": 360
         |},
         |{
         |"maxRate": "15.5",
         |"maxAmount": 20000000,
         |"bankName": "Транскапиталбанк",
         |"minDuration": 12,
         |"programs": [
         |{
         |"maxRate": 15.5,
         |"name": "Ипотека 14.5%",
         |"maxAmount": 20000000,
         |"minDuration": 12,
         |"isPrivileged": false,
         |"isMilitary": false,
         |"minRate": 14.5,
         |"minInitialPayment": 20.0,
         |"minAmount": 300000,
         |"maxDuration": 300
         |}
         |],
         |"minRate": "14.5",
         |"minInitialPayment": 20.0,
         |"minAmount": 300000,
         |"maxDuration": 300
         |},
         |{
         |"bankName": "ДельтаКредит",
         |"programs": [
         |{
         |"name": "Ипотека 13.5%",
         |"isPrivileged": false,
         |"isMilitary": false,
         |"minRate": 13.5,
         |"minInitialPayment": 20.0,
         |"maxDuration": 300
         |}
         |],
         |"minRate": "13.5",
         |"minInitialPayment": 20.0,
         |"maxDuration": 300
         |}
         |],
         |"specialProposalsSummary": [
         |{
         |"specialProposalType": "discount",
         |"shortDescription": "Скидка 9%",
         |"fullDescription": "На квартиры в построенных корпусах предоставляется скидка до 9%.",
         |"mainProposal": true
         |},
         |{
         |"specialProposalType": "discount",
         |"shortDescription": "Скидка 3%",
         |"fullDescription": "Скидка 3% - при 100% оплате квартир и апартаментов",
         |"mainProposal": false
         |},
         |{
         |"specialProposalType": "sale",
         |"shortDescription": "Скидка 15% на паркинг",
         |"fullDescription": "Машиноместа доступны со скидкой до 15%.",
         |"mainProposal": false
         |},
         |{
         |"fullDescription": "Рассрочка до 1 года в корпусе \"Гамбург\".",
         |"shortDescription": "Рассрочка на 1 год",
         |"durationMonths": 12,
         |"mainProposal": false,
         |"specialProposalType": "installment"
         |}
         |],
         |"allSpecialProposals": [
         |{
         |"specialProposalType": "discount",
         |"shortDescription": "Скидка 9%",
         |"fullDescription": "На квартиры в построенных корпусах предоставляется скидка до 9%."
         |},
         |{
         |"specialProposalType": "discount",
         |"shortDescription": "Скидка 3%",
         |"fullDescription": "Скидка 3% - при 100% оплате квартир и апартаментов"
         |},
         |{
         |"specialProposalType": "sale",
         |"shortDescription": "Скидка 15% на паркинг",
         |"fullDescription": "Машиноместа доступны со скидкой до 15%."
         |},
         |{
         |"specialProposalType": "sale",
         |"shortDescription": "Скидка 15% на паркинг",
         |"fullDescription": "Скидка до 15% на машиноместа",
         |"endDate": "2018-04-30T00:00:00.000+03:00"
         |},
         |{
         |"specialProposalType": "sale",
         |"shortDescription": "Скидка 35%",
         |"fullDescription": "Участники MR Club могут получить скидки до 35% на ассортимент партнеров компании. Карта MR Club доступна покупателям в офисах продаж проектов компании."
         |},
         |{
         |"specialProposalType": "installment",
         |"shortDescription": "Рассрочка на 1 год",
         |"fullDescription": "Рассрочка до 1 года в корпусе \"Гамбург\".",
         |"durationMonths": 12
         |},
         |{
         |"fullDescription": "Беспроцентная рассрочка на 3 месяца, первый взнос 50%.",
         |"shortDescription": "Беспроцентная рассрочка на 3 мес.",
         |"durationMonths": 3,
         |"freeInstallment": true,
         |"specialProposalType": "installment",
         |"initialPaymentPercents": 50.0
         |}
         |]
         |},
         |"dit": {
         |"permission": true
         |},
         |"description": "Современный жилой квартал «Водный» расположен на севере Москвы в двух шагах от станции метро «Водный стадион». Панорамные виды на достопримечательности Москвы из окон, эксклюзивные архитектурные решения, развитая инфраструктура в пешеходной доступности, обширная паркинговая зона, эффективные планировки - все это делает «Водный» одним из самых привлекательных объектов Москвы.",
         |"buildingFeatures": {
         |"totalFloors": 29,
         |"state": "UNFINISHED",
         |"zhkType": "KVARTAL",
         |"parkings": [
         |{
         |"type": "UNDERGROUND",
         |"parkingSpaces": 1408,
         |"available": true
         |}
         |],
         |"totalApartments": 1776,
         |"privateTerritory": true,
         |"interiorFinish": {
         |"type": "TURNKEY",
         |"text": "Итальянская компания Co-Progetti эксклюзивно для MR Group предлагает реализацию комплексного интерьерного решения и отделки в апартаментах и квартирах МФК «Водный».",
         |"images": [
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137893/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137893/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137893/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137893/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137893/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137893/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137893/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137893/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137893/cosmic"
         |},
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/3220/newbuilding.21504.21137903/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/3220/newbuilding.21504.21137903/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/3220/newbuilding.21504.21137903/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/3220/newbuilding.21504.21137903/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/3220/newbuilding.21504.21137903/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/3220/newbuilding.21504.21137903/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/3220/newbuilding.21504.21137903/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/3220/newbuilding.21504.21137903/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/3220/newbuilding.21504.21137903/cosmic"
         |},
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137905/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137905/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137905/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137905/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137905/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137905/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137905/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137905/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137905/cosmic"
         |},
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137907/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137907/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137907/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137907/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137907/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137907/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137907/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137907/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137907/cosmic"
         |},
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21137909/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21137909/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21137909/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21137909/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21137909/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21137909/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21137909/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21137909/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/2957/newbuilding.21504.21137909/cosmic"
         |},
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137911/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137911/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137911/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137911/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137911/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137911/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137911/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137911/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/2941/newbuilding.21504.21137911/cosmic"
         |},
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137913/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137913/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137913/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137913/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137913/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137913/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137913/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137913/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137913/cosmic"
         |},
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/3019/newbuilding.21504.21137915/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/3019/newbuilding.21504.21137915/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/3019/newbuilding.21504.21137915/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/3019/newbuilding.21504.21137915/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/3019/newbuilding.21504.21137915/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/3019/newbuilding.21504.21137915/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/3019/newbuilding.21504.21137915/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/3019/newbuilding.21504.21137915/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/3019/newbuilding.21504.21137915/cosmic"
         |},
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137919/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137919/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137919/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137919/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137919/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137919/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137919/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137919/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/3022/newbuilding.21504.21137919/cosmic"
         |},
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137925/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137925/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137925/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137925/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137925/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137925/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137925/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137925/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137925/cosmic"
         |},
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137931/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137931/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137931/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137931/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137931/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137931/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137931/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137931/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/2991/newbuilding.21504.21137931/cosmic"
         |},
         |{
         |"mini": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137855/minicard",
         |"appMiddleSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137855/app_snippet_middle",
         |"full": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137855/large",
         |"appLarge": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137855/app_large",
         |"appMiniSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137855/app_snippet_mini",
         |"appMiddle": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137855/app_middle",
         |"appSmallSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137855/app_snippet_small",
         |"appLargeSnippet": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137855/app_snippet_large",
         |"cosmic": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21137855/cosmic"
         |}
         |]
         |},
         |"isApartment": true,
         |"walls": {
         |"type": "MONOLIT",
         |"text": "Долговечный материал, позволяет приступить к отделочным работам почти сразу же после завершения строительства."
         |},
         |"finishedApartments": true,
         |"class": "BUSINESS",
         |"parking": {
         |"type": "UNKNOWN",
         |"available": true
         |}
         |},
         |"fullName": "квартал «Водный»",
         |"facts": {
         |"transportAccessibility": {
         |"publicTransportStops": 0,
         |"metroStations": 0,
         |"railwayStations": 0,
         |"congestionHighways": 0
         |},
         |"environmentalConditions": {
         |"garbageDisposals": 0,
         |"parks": 0,
         |"waterPonds": 0,
         |"factories": 0,
         |"squares": 0
         |},
         |"housingInfrastructure": {
         |"schools": 0,
         |"storeProducts": 0,
         |"clinics": 0,
         |"shoppingСenters": 0,
         |"restaurants": 0,
         |"kindergardens": 0
         |},
         |"hazardousFacilities": {
         |"industrialZones": 0,
         |"prisons": 0,
         |"cemeteries": 0
         |}
         |},
         |"price": {
         |"minPricePerMeter": 146999,
         |"rooms": {
         |"1": {
         |"hasOffers": true,
         |"soldout": false,
         |"to": 9043200,
         |"priceRatioToMarket": 0,
         |"status": "ON_SALE",
         |"from": 4884000,
         |"currency": "RUR",
         |"areas": {
         |"from": "26.3",
         |"to": "49.3"
         |}
         |},
         |"2": {
         |"hasOffers": true,
         |"soldout": false,
         |"to": 13998600,
         |"priceRatioToMarket": 0,
         |"status": "ON_SALE",
         |"from": 6913500,
         |"currency": "RUR",
         |"areas": {
         |"from": "41.9",
         |"to": "72.1"
         |}
         |},
         |"OPEN_PLAN": {
         |"soldout": false,
         |"currency": "RUR",
         |"hasOffers": false,
         |"priceRatioToMarket": 0
         |},
         |"STUDIO": {
         |"hasOffers": true,
         |"soldout": false,
         |"to": 5454000,
         |"priceRatioToMarket": 0,
         |"status": "ON_SALE",
         |"from": 4884000,
         |"currency": "RUR",
         |"areas": {
         |"from": "26.3",
         |"to": "27"
         |}
         |},
         |"3": {
         |"hasOffers": true,
         |"soldout": false,
         |"to": 16473000,
         |"priceRatioToMarket": 0,
         |"status": "ON_SALE",
         |"from": 16473000,
         |"currency": "RUR",
         |"areas": {
         |"from": "96.9",
         |"to": "96.9"
         |}
         |},
         |"PLUS_4": {
         |"soldout": false,
         |"currency": "RUR",
         |"hasOffers": false,
         |"priceRatioToMarket": 0
         |}
         |},
         |"to": 16473000,
         |"priceRatioToMarket": 0,
         |"maxPricePerMeter": 206000,
         |"averagePricePerMeter": 182389,
         |"from": 4884000,
         |"currency": "RUR",
         |"totalOffers": 390
         |},
         |"developer": {
         |"name": "MR Group",
         |"url": "http://mr-group.ru/",
         |"logo": "//avatars.mdst.yandex.net/get-realty/2957/company.268784.21088028/builder_logo_info",
         |"id": 268784,
         |"objects": {
         |"finished": 8,
         |"unfinished": 10
         |}
         |},
         |"locativeName": "в «Водный»",
         |"ditInfo": [
         |{
         |"name": "Многофункциональный комплекс 1-я очередь 1-й пусковой комплекс",
         |"organizationOGRN": "1077763151553",
         |"documentNumber": "RU77124000-007860",
         |"organizationINN": "7714720783",
         |"organizationName": "ЗАО Закрытое акционерное общество \"Бизнес центр \"Кронштадтский\"",
         |"id": "1C2510872CF17CE6C32573BD00466EA7",
         |"address": "Головинское шоссе, вл. 5",
         |"planDateEnd": "2013-12-31+04:00",
         |"deleted": "false"
         |},
         |{
         |"name": "Многофункциональный комплекс 1-я очередь 2-й пусковой комплекс",
         |"organizationOGRN": "1077763151553",
         |"documentNumber": "RU77124000-007859",
         |"organizationINN": "7714720783",
         |"organizationName": "ЗАО Закрытое акционерное общество \"Бизнес центр \"Кронштадтский\"",
         |"id": "3EA2DBA88A344AD3831CAB59ADC37D11",
         |"address": "Головинское шоссе, вл. 5",
         |"planDateEnd": "2015-12-31+03:00",
         |"deleted": "false"
         |},
         |{
         |"name": "Многофункциональный комплекс, 2-я очередь",
         |"organizationOGRN": "1127746023734",
         |"documentNumber": "RU77124000-007963",
         |"organizationINN": "7714862178",
         |"organizationName": "ООО Общество с ограниченной ответственностью \"Жилищная корпорация\"",
         |"id": "8C2E59AC36874E72A6D19BAB6730B696",
         |"address": "Головинское шоссе вл. 5",
         |"planDateEnd": "2016-12-31+03:00",
         |"deleted": "false"
         |},
         |{
         |"name": "Многоэтажный жилой дом",
         |"organizationOGRN": "1127746023734",
         |"documentNumber": "77-124000-013007-2016",
         |"organizationINN": "7714862178",
         |"organizationName": "ООО Общество с ограниченной ответственностью \"Жилищная корпорация\"",
         |"id": "B5F467643A0E470C946B005CA5C252C2",
         |"address": "Москва, САО, район Головинский, Головинское шоссе, вл. 5, корп. 6",
         |"planDateEnd": "2017-12-31+03:00",
         |"deleted": "false"
         |}
         |],
         |"transactionTerms": {
         |"mortgage": true,
         |"permission": true,
         |"fz214": true,
         |"documentsUrl": "http://vodny2.ru/pages/about/pd/",
         |"agreementType": "DDU",
         |"installment": true
         |},
         |"salesClosed": false,
         |"documents": {
         |"buildingPermitDocs": [
         |{
         |"url": "http://s3.mds.yandex.net/get/verba/C8866C124172B8A6A1BFEE29C26FC6C2.pdf",
         |"name": "Разрешение на строительство от 20.07.2016 (корпус 6).pdf"
         |}
         |],
         |"projectDeclarationDocs": [
         |{
         |"url": "http://s3.mds.yandex.net:80/get/verba/93A11961D320D26C154D2B60E48B612C.docx",
         |"name": "Изменения в проектную декларацию от 16.09.2016 (корпус 6).docx"
         |},
         |{
         |"url": "http://s3.mds.yandex.net:80/get/verba/6C4F72C99D8F5B9FDCE2B0CC403BB43C.docx",
         |"name": "Проектная декларация от 21.07.2016 (корпус 6).docx"
         |}
         |],
         |"operationActDocs": [
         |{
         |"url": "http://s3.mds.yandex.net:80/get/verba/2683A4826A62EE3DA7756EE4B154D8CA.pdf",
         |"name": "Разрешение на ввод в эксплуатацию от 31.08.2015 (2 очередь).pdf"
         |}
         |]
         |},
         |"id": 21504,
         |"mainSpecialProposal": {
         |"specialProposalType": "discount",
         |"description": "Скидка 9%"
         |},
         |"flatStatus": "ON_SALE",
         |"salesDepartments": [
         |{
         |"isRedirectPhones": false,
         |"name": "Dizzy test",
         |"dump": {
         |"billing.header.order.memo": "-",
         |"billing.header.order.total_income": "500000000",
         |"billing.header.product.goods@0.placement.cost.version": "1",
         |"billing.header.order.id": "1743",
         |"billing.header.id": "e354ff54-49b1-4be0-9fa8-5d55c14fda05",
         |"billing.header.order.owner.client_id": "13596652",
         |"billing.context.work_policy.days@1.to": "2017-05-14",
         |"billing.header.order.total_spent": "96660000",
         |"billing.header.product.version": "1",
         |"billing.header.name": "Водный",
         |"billing.context.work_policy.days@0.from": "2017-05-12",
         |"billing.header.settings.call_settings.redirectId": "qaaWhx86x1g",
         |"billing.context.work_policy.days@1.from": "2017-05-13",
         |"billing.header.order.text": "Яндекс.Недвижимость, Dizzy 1",
         |"billing.context.work_policy.days@0.to": "2017-05-12",
         |"billing.header.owner.resource_ref@0.developer_id": "312637",
         |"billing.header.order.product_key": "default",
         |"billing.context.work_policy.days@2.times@0.to": "21:00:00.000",
         |"billing.header.version": "1",
         |"billing.active.deadline": "2017-05-12T10:58:23.177Z",
         |"billing.header.order.commit_amount": "403340000",
         |"billing.context.work_policy.days@2.from": "2017-05-15",
         |"billing.offer.id": "7(812)9287472/7(812)3836302",
         |"billing.header.settings.call_settings.redirectPhone.phone": "3836302",
         |"billing.context.work_policy.days@2.to": "2017-05-16",
         |"billing.header.owner.id.client_id": "13596652",
         |"billing.instance.id": "6ca8850532755301eed2b365dc8a6b30",
         |"billing.header.owner.version": "1",
         |"billing.header.settings.call_settings.phone.phone": "9287472",
         |"billing.header.order.approximate_amount": "403340000",
         |"billing.header.settings.call_settings.source_type": "TELEPONY",
         |"billing.header.owner.id.version": "1",
         |"billing.header.product.goods@0.placement.cost.per_call.units": "2635000",
         |"billing.header.settings.call_settings.metrika_phone.country": "7",
         |"billing.header.order.owner.version": "1",
         |"billing.header.settings.call_settings.phone.code": "812",
         |"billing.header.settings.call_settings.phone.country": "7",
         |"billing.header.owner.resource_ref@0.version": "1",
         |"billing.header.settings.is_enabled": "true",
         |"billing.call.revenue": "1930000",
         |"billing.context.work_policy.days@2.times@0.from": "09:00:00.000",
         |"billing.header.settings.version": "1",
         |"billing.context.version": "1",
         |"billing.header.order.version": "1",
         |"billing.header.settings.call_settings.redirectPhone.country": "7",
         |"billing.header.product.goods@0.version": "1",
         |"billing.context.work_policy.days@0.times@0.to": "21:00:00.000",
         |"billing.context.work_policy.time_zone": "Europe/Moscow",
         |"billing.context.work_policy.days@0.times@0.from": "09:00:00.000",
         |"billing.header.settings.call_settings.redirectPhone.code": "812",
         |"billing.header.settings.call_settings.metrika_phone.phone": "3836302",
         |"billing.header.settings.call_settings.metrika_phone.code": "812"
         |},
         |"weekTimetable": [
         |{
         |"dayFrom": 1,
         |"dayTo": 5,
         |"timePattern": [
         |{
         |"open": "09:00",
         |"close": "21:00"
         |}
         |]
         |}
         |],
         |"phones": [
         |"+78123836302"
         |],
         |"id": 312637
         |},
         |{
         |"isRedirectPhones": false,
         |"name": "MR Group",
         |"dump": {
         |"billing.header.order.memo": "-",
         |"billing.header.order.total_income": "100000000",
         |"billing.header.product.goods@0.placement.cost.version": "1",
         |"billing.header.order.id": "1736",
         |"billing.header.id": "b5610a28-300f-467a-a970-13d2005a4a20",
         |"billing.header.order.owner.client_id": "13427405",
         |"billing.header.order.total_spent": "8320000",
         |"billing.header.product.version": "1",
         |"billing.header.name": "Водный",
         |"billing.context.work_policy.days@0.from": "2017-05-12",
         |"billing.header.settings.call_settings.redirectId": "yg3Bz6p6bTs",
         |"billing.header.order.text": "Яндекс.Недвижимость, MR Group",
         |"billing.context.work_policy.days@0.to": "2017-05-16",
         |"billing.header.owner.resource_ref@0.developer_id": "192398",
         |"billing.header.order.product_key": "default",
         |"billing.header.version": "1",
         |"billing.active.deadline": "2017-05-12T10:57:39.640Z",
         |"billing.header.order.commit_amount": "91680000",
         |"billing.offer.id": "7(812)0000000/7(812)3836299",
         |"billing.header.settings.call_settings.redirectPhone.phone": "3836299",
         |"billing.header.owner.id.client_id": "13427405",
         |"billing.instance.id": "10159b8c414edb631b754176b53d9ec2",
         |"billing.header.owner.version": "1",
         |"billing.header.settings.call_settings.phone.phone": "0000000",
         |"billing.header.order.approximate_amount": "91680000",
         |"billing.header.settings.call_settings.source_type": "TELEPONY",
         |"billing.header.owner.id.version": "1",
         |"billing.header.product.goods@0.placement.cost.per_call.units": "1930000",
         |"billing.header.settings.call_settings.metrika_phone.country": "7",
         |"billing.header.order.owner.version": "1",
         |"billing.header.settings.call_settings.phone.code": "812",
         |"billing.header.settings.call_settings.phone.country": "7",
         |"billing.header.owner.resource_ref@0.version": "1",
         |"billing.header.settings.is_enabled": "true",
         |"billing.call.revenue": "450000",
         |"billing.header.settings.version": "1",
         |"billing.context.version": "1",
         |"billing.header.order.version": "1",
         |"billing.header.settings.call_settings.redirectPhone.country": "7",
         |"billing.header.product.goods@0.version": "1",
         |"billing.context.work_policy.days@0.times@0.to": "21:00:00.000",
         |"billing.context.work_policy.time_zone": "Europe/Moscow",
         |"billing.context.work_policy.days@0.times@0.from": "09:00:00.000",
         |"billing.header.settings.call_settings.redirectPhone.code": "812",
         |"billing.header.settings.call_settings.metrika_phone.phone": "3836299",
         |"billing.header.settings.call_settings.metrika_phone.code": "812"
         |},
         |"weekTimetable": [
         |{
         |"dayFrom": 1,
         |"dayTo": 7,
         |"timePattern": [
         |{
         |"open": "09:00",
         |"close": "21:00"
         |}
         |]
         |}
         |],
         |"phones": [
         |"+78123836299"
         |],
         |"logo": "//avatars.mdst.yandex.net/get-realty/2991/company.192398.21070939/builder_logo_info",
         |"id": 192398
         |}
         |],
         |"developers": [
         |{
         |"name": "MR Group",
         |"url": "http://mr-group.ru/",
         |"logo": "//avatars.mdst.yandex.net/get-realty/2957/company.268784.21088028/builder_logo_info",
         |"id": 268784,
         |"objects": {
         |"finished": 8,
         |"unfinished": 10
         |}
         |}
         |],
         |"siteSpecialProposals": {
         |"mortgagesSummary": [
         |{
         |"bankName": "Сбербанк",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "10.9",
         |"minInitialPayment": 15.0
         |},
         |{
         |"bankName": "ВТБ 24",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "10.4",
         |"minInitialPayment": 15.0
         |},
         |{
         |"bankName": "Транскапиталбанк",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "9.75",
         |"minInitialPayment": 30.0
         |}
         |],
         |"allMortgages": [
         |{
         |"bankName": "Сбербанк",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "10.9",
         |"minInitialPayment": 15.0
         |},
         |{
         |"bankName": "ВТБ 24",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "10.4",
         |"minInitialPayment": 15.0
         |},
         |{
         |"bankName": "Транскапиталбанк",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "9.75",
         |"minInitialPayment": 30.0
         |},
         |{
         |"bankName": "Россельхозбанк",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "10.25",
         |"minInitialPayment": 20.0
         |},
         |{
         |"bankName": "ВТБ Банк Москвы",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "10.75",
         |"minInitialPayment": 15.0
         |},
         |{
         |"bankName": "Абсолют Банк",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "10.9",
         |"minInitialPayment": 20.0
         |},
         |{
         |"maxAmount": 30000000,
         |"bankName": "Банк Возрождение",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "10.9",
         |"minInitialPayment": 0.0
         |},
         |{
         |"bankName": "Российский капитал",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "11.0",
         |"minInitialPayment": 20.0
         |},
         |{
         |"bankName": "ДельтаКредит",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "11.5",
         |"minInitialPayment": 15.0
         |},
         |{
         |"bankName": "Уралсиб",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "12.25",
         |"minInitialPayment": 10.0
         |},
         |{
         |"bankName": "Банк Зенит",
         |"hasPriviledgedMortgage": true,
         |"hasMilitaryMortgage": false,
         |"programs": [],
         |"minRate": "13.5",
         |"minInitialPayment": 20.0
         |}
         |],
         |"specialProposalsSummary": [
         |{
         |"specialProposalType": "discount",
         |"shortDescription": "Скидка 9%",
         |"fullDescription": "На квартиры в построенных корпусах предоставляется скидка до 9%.",
         |"mainProposal": true
         |},
         |{
         |"specialProposalType": "discount",
         |"shortDescription": "Скидка 3%",
         |"fullDescription": "Скидка 3% - при 100% оплате квартир и апартаментов",
         |"mainProposal": false
         |},
         |{
         |"specialProposalType": "sale",
         |"shortDescription": "Скидка 15% на паркинг",
         |"fullDescription": "Машиноместа доступны со скидкой до 15%.",
         |"mainProposal": false
         |},
         |{
         |"fullDescription": "Рассрочка до 1 года в корпусе \"Гамбург\".",
         |"shortDescription": "Рассрочка на 1 год",
         |"durationMonths": 12,
         |"mainProposal": false,
         |"specialProposalType": "installment"
         |}
         |],
         |"allSpecialProposals": [
         |{
         |"specialProposalType": "discount",
         |"shortDescription": "Скидка 9%",
         |"fullDescription": "На квартиры в построенных корпусах предоставляется скидка до 9%."
         |},
         |{
         |"specialProposalType": "discount",
         |"shortDescription": "Скидка 3%",
         |"fullDescription": "Скидка 3% - при 100% оплате квартир и апартаментов"
         |},
         |{
         |"specialProposalType": "sale",
         |"shortDescription": "Скидка 15% на паркинг",
         |"fullDescription": "Машиноместа доступны со скидкой до 15%."
         |},
         |{
         |"specialProposalType": "sale",
         |"shortDescription": "Скидка 15% на паркинг",
         |"fullDescription": "Скидка до 15% на машиноместа",
         |"endDate": "2018-04-30T00:00:00.000+03:00"
         |},
         |{
         |"specialProposalType": "sale",
         |"shortDescription": "Скидка 35%",
         |"fullDescription": "Участники MR Club могут получить скидки до 35% на ассортимент партнеров компании. Карта MR Club доступна покупателям в офисах продаж проектов компании."
         |},
         |{
         |"specialProposalType": "installment",
         |"shortDescription": "Рассрочка на 1 год",
         |"fullDescription": "Рассрочка до 1 года в корпусе \"Гамбург\".",
         |"durationMonths": 12
         |},
         |{
         |"fullDescription": "Беспроцентная рассрочка на 3 месяца, первый взнос 50%.",
         |"shortDescription": "Беспроцентная рассрочка на 3 мес.",
         |"durationMonths": 3,
         |"freeInstallment": true,
         |"specialProposalType": "installment",
         |"initialPaymentPercents": 50.0
         |}
         |]
         |},
         |"promoPhoneNumbers": [
         |"+74953577509"
         |],
         |"mainSiteSpecialProposal": {
         |"specialProposalType": "discount",
         |"description": "Скидка 9%"
         |},
         |"deliveryDates": [
         |{
         |"houses": 5,
         |"polygons": [
         |{
         |"latitudes": [
         |55.84130859375,
         |55.84163284301758,
         |55.84154510498047,
         |55.841209411621094,
         |55.84103775024414,
         |55.841087341308594,
         |55.8409309387207,
         |55.84096908569336
         |],
         |"longitudes": [
         |37.49397659301758,
         |37.493709564208984,
         |37.493324279785156,
         |37.493316650390625,
         |37.49348068237305,
         |37.493682861328125,
         |37.493804931640625,
         |37.49397277832031
         |]
         |},
         |{
         |"latitudes": [
         |55.841190338134766,
         |55.840858459472656,
         |55.840518951416016,
         |55.840614318847656,
         |55.840938568115234,
         |55.84128189086914
         |],
         |"longitudes": [
         |37.494110107421875,
         |37.494110107421875,
         |37.494380950927734,
         |37.49475860595703,
         |37.49475860595703,
         |37.49448776245117
         |]
         |},
         |{
         |"latitudes": [
         |55.841400146484375,
         |55.841064453125,
         |55.84072494506836,
         |55.84081268310547,
         |55.84113693237305,
         |55.84148406982422
         |],
         |"longitudes": [
         |37.494911193847656,
         |37.494903564453125,
         |37.49518966674805,
         |37.49556350708008,
         |37.49557113647461,
         |37.49528503417969
         |]
         |},
         |{
         |"latitudes": [
         |55.8411979675293,
         |55.840858459472656,
         |55.84052276611328,
         |55.84062576293945,
         |55.84095001220703,
         |55.841285705566406
         |],
         |"longitudes": [
         |37.494110107421875,
         |37.494102478027344,
         |37.4943733215332,
         |37.4947624206543,
         |37.494754791259766,
         |37.49447250366211
         |]
         |},
         |{
         |"latitudes": [
         |55.84098815917969,
         |55.84065246582031,
         |55.8403205871582,
         |55.84041976928711,
         |55.84074020385742,
         |55.84108352661133
         |],
         |"longitudes": [
         |37.4932975769043,
         |37.493282318115234,
         |37.49357223510742,
         |37.493953704833984,
         |37.493953704833984,
         |37.49366760253906
         |]
         |}
         |],
         |"finished": true,
         |"year": 2015,
         |"quarter": 4,
         |"phaseName": "1 очередь"
         |},
         |{
         |"houses": 1,
         |"polygons": [
         |{
         |"latitudes": [
         |55.84000015258789,
         |55.8394889831543,
         |55.83949661254883,
         |55.83974838256836,
         |55.83998107910156,
         |55.84001922607422
         |],
         |"longitudes": [
         |37.49283981323242,
         |37.49281692504883,
         |37.4935188293457,
         |37.493526458740234,
         |37.49352264404297,
         |37.49348449707031
         |]
         |}
         |],
         |"finished": false,
         |"year": 2018,
         |"quarter": 2,
         |"phaseName": "2 очередь"
         |}
         |],
         |"images": {
         |"main": "//avatars.mdst.yandex.net/get-realty/2899/newbuilding.21504.21109098/main",
         |"list": [
         |]
         |},
         |"updateTime": 1491650650
         |}
      """.stripMargin
    )

    val unified = u.unify(json, UnificationContext(isMobile = true, antirobotDegradation = false))
    println(Json.prettyPrint(unified))
    (unified \\ "statParams").nonEmpty should be(true)
    (unified \\ "dump").nonEmpty should be(true)
    (unified \\ "dump").head should be(JsNull)
  }

  it should "correct unify offer" in {
    val json = Json.parse(
      """
         |{
         |"offerId": "2332956270710372841",
         |"trust": "NORMAL",
         |"url": "//realty.test.vertis.yandex.ru/offer/2332956270710372841",
         |"signedUrl": "//market-click2.yandex.ru/redir/yi4N2a_AefwDa6gcuw_IdsyqQofJ-qA-W3DgdOgted-xsx8FedYeT6M59S9n5sAiD_LqISd3ues6PrezV78zHQyfNeFkq8v8_6AgkO6zI4rZ0qEu3Opd5Dg9pqd-KYRbgB_23z-XKdAk6OS-nqIUdG_F-d4Ttlc4muEJk-z4tEd1SpS72XasO8hdi6o258fn2Of2dXxF0IoppaUzNayY5FSqqgTMgGrWPZPlCIEwXe9kNE7CEnYkdg?data=UlNrNmk5WktYejY4cHFySjRXSWhXRy1XckxKMGpfV0RvXzZ1ZUNvckRsSG9xYUIzZjRERDdmRm0wcG1EU1JaOTAwdDMxY0RSOHNtakU4SFlsRkpMakszNFAxckwyemM3X0N6YWttVFRCT1FybF9UeWVwdGdhUG5ydjZSTFNidkE&b64e=2&sign=ecd4a0320fc16f6d189fecee115957cd&keyno=0",
         |"internalUrl": "//market-click2.yandex.ru/redir/yi4N2a_AefwDa6gcuw_IdsyqQofJ-qA-W3DgdOgted-xsx8FedYeT6M59S9n5sAiD_LqISd3ues6PrezV78zHQyfNeFkq8v8_6AgkO6zI4rZ0qEu3Opd5Dg9pqd-KYRbgB_23z-XKdAk6OS-nqIUdG_F-d4Ttlc4muEJk-z4tEd1SpS72XasO8hdi6o258fn2Of2dXxF0IoppaUzNayY5FSqqgTMgGrWPZPlCIEwXe9kNE7CEnYkdg?data=UlNrNmk5WktYejR0eWJFYk1LdmtxcVI3RTViRVprTmZqVVQyTGo0Sk8wNDBTbDEyUGpPZFNkZGFQVnptTDVsM3NwaGhmRjNZaEZLeDAxWHJHbjNQUGk4YTE3dUhsWGd5aFZ2OXAtTTZnUldsQUxjZ2lpRG1aZ1VCVVlyMDcwLXo&b64e=2&sign=66dbfcbac3217df142c88face7cb2a6f&keyno=0",
         |"unsignedInternalUrl": "//realty.test.vertis.yandex.ru/offer/2332956270710372841",
         |"httpsInternalUrl": "//market-click2.yandex.ru/redir/yi4N2a_AefwDa6gcuw_IdsyqQofJ-qA-W3DgdOgted-xsx8FedYeT6M59S9n5sAiD_LqISd3ues6PrezV78zHQyfNeFkq8v8_6AgkO6zI4rZ0qEu3Opd5Dg9pqd-KYRbgB_23z-XKdAk6OS-nqIUdG_F-d4Ttlc4muEJk-z4tEd1SpS72XasO8hdi6o258fn2Of2dXxF0IoppaUzNayY5FSqqgTMgGrWPZPlCIEwXe9kNE7CEnYkdg?data=UlNrNmk5WktYejY4cHFySjRXSWhXRy1XckxKMGpfV0RvXzZ1ZUNvckRsSG9xYUIzZjRERDdmRm0wcG1EU1JaOTAwdDMxY0RSOHNtakU4SFlsRkpMakszNFAxckwyemM3X0N6YWttVFRCT1FybF9UeWVwdGdhUG5ydjZSTFNidkE&b64e=2&sign=ecd4a0320fc16f6d189fecee115957cd&keyno=0",
         |"partnerId": "1060357228",
         |"partnerName": "Первое Агентство Недвижимости",
         |"offerType": "SELL",
         |"offerCategory": "APARTMENT",
         |"clusterId": "2068973540131917806",
         |"clusterHeader": true,
         |"clusterSize": 4,
         |"author": {
         |"id": "157195",
         |"category": "AGENCY",
         |"organization": "Первое Агентство Недвижимости",
         |"phones": [
         |"+78123132666"
         |],
         |"extPhones": [
         |{
         |"number": "3132666",
         |"code": "812",
         |"region": "7",
         |"raw": "+78123132666"
         |}
         |],
         |"redirectPhonesFailed": false,
         |"redirectPhones": false,
         |"name": "Первое Агентство Недвижимости"
         |},
         |"creationDate": "2016-11-22T10:35:16Z",
         |"expireDate": "2017-05-14T05:36:04Z",
         |"roomsTotal": 1,
         |"floorsTotal": 11,
         |"floorsOffered": [
         |3
         |],
         |"flatType": "NEW_FLAT",
         |"ceilingHeight": 3.08,
         |"area": {
         |"value": 38.66,
         |"unit": "SQUARE_METER"
         |},
         |"livingSpace": {
         |"value": 16.2,
         |"unit": "SQUARE_METER"
         |},
         |"kitchenSpace": {
         |"value": 10.2,
         |"unit": "SQUARE_METER"
         |},
         |"price": {
         |"currency": "RUR",
         |"value": 5159942,
         |"period": "WHOLE_LIFE",
         |"unit": "WHOLE_OFFER",
         |"trend": "UNCHANGED"
         |},
         |"notForAgents": false,
         |"totalImages": 9,
         |"minicardImages": [
         |"//avatars.mds.yandex.net/get-realty/63416/w1619/minicard",
         |"//avatars.mds.yandex.net/get-realty/51167/w1911/minicard",
         |"//avatars.mds.yandex.net/get-realty/43143/w1570/minicard",
         |"//avatars.mds.yandex.net/get-realty/44639/w1989/minicard",
         |"//avatars.mds.yandex.net/get-realty/59561/w1845/minicard",
         |"//avatars.mds.yandex.net/get-realty/59561/w1828/minicard",
         |"//avatars.mds.yandex.net/get-realty/63221/w1700/minicard",
         |"//avatars.mds.yandex.net/get-realty/54213/w1729/minicard",
         |"//avatars.mds.yandex.net/get-realty/35066/w1248/minicard"
         |],
         |"mainImages": [
         |"//avatars.mds.yandex.net/get-realty/63416/w1619/main",
         |"//avatars.mds.yandex.net/get-realty/51167/w1911/main",
         |"//avatars.mds.yandex.net/get-realty/43143/w1570/main",
         |"//avatars.mds.yandex.net/get-realty/44639/w1989/main",
         |"//avatars.mds.yandex.net/get-realty/59561/w1845/main",
         |"//avatars.mds.yandex.net/get-realty/59561/w1828/main",
         |"//avatars.mds.yandex.net/get-realty/63221/w1700/main",
         |"//avatars.mds.yandex.net/get-realty/54213/w1729/main",
         |"//avatars.mds.yandex.net/get-realty/35066/w1248/main"
         |],
         |"fullImages": [
         |"//avatars.mds.yandex.net/get-realty/63416/w1619/large",
         |"//avatars.mds.yandex.net/get-realty/51167/w1911/large",
         |"//avatars.mds.yandex.net/get-realty/43143/w1570/large",
         |"//avatars.mds.yandex.net/get-realty/44639/w1989/large",
         |"//avatars.mds.yandex.net/get-realty/59561/w1845/large",
         |"//avatars.mds.yandex.net/get-realty/59561/w1828/large",
         |"//avatars.mds.yandex.net/get-realty/63221/w1700/large",
         |"//avatars.mds.yandex.net/get-realty/54213/w1729/large",
         |"//avatars.mds.yandex.net/get-realty/35066/w1248/large"
         |],
         |"alikeImages": [
         |"//avatars.mds.yandex.net/get-realty/63416/w1619/alike",
         |"//avatars.mds.yandex.net/get-realty/51167/w1911/alike",
         |"//avatars.mds.yandex.net/get-realty/43143/w1570/alike",
         |"//avatars.mds.yandex.net/get-realty/44639/w1989/alike",
         |"//avatars.mds.yandex.net/get-realty/59561/w1845/alike",
         |"//avatars.mds.yandex.net/get-realty/59561/w1828/alike",
         |"//avatars.mds.yandex.net/get-realty/63221/w1700/alike",
         |"//avatars.mds.yandex.net/get-realty/54213/w1729/alike",
         |"//avatars.mds.yandex.net/get-realty/35066/w1248/alike"
         |],
         |"cosmicImages": [
         |"//avatars.mds.yandex.net/get-realty/63416/w1619/cosmic",
         |"//avatars.mds.yandex.net/get-realty/51167/w1911/cosmic",
         |"//avatars.mds.yandex.net/get-realty/43143/w1570/cosmic",
         |"//avatars.mds.yandex.net/get-realty/44639/w1989/cosmic",
         |"//avatars.mds.yandex.net/get-realty/59561/w1845/cosmic",
         |"//avatars.mds.yandex.net/get-realty/59561/w1828/cosmic",
         |"//avatars.mds.yandex.net/get-realty/63221/w1700/cosmic",
         |"//avatars.mds.yandex.net/get-realty/54213/w1729/cosmic",
         |"//avatars.mds.yandex.net/get-realty/35066/w1248/cosmic"
         |],
         |"appMiddleImages": [
         |"//avatars.mds.yandex.net/get-realty/63416/w1619/app_middle",
         |"//avatars.mds.yandex.net/get-realty/51167/w1911/app_middle",
         |"//avatars.mds.yandex.net/get-realty/43143/w1570/app_middle",
         |"//avatars.mds.yandex.net/get-realty/44639/w1989/app_middle",
         |"//avatars.mds.yandex.net/get-realty/59561/w1845/app_middle",
         |"//avatars.mds.yandex.net/get-realty/59561/w1828/app_middle",
         |"//avatars.mds.yandex.net/get-realty/63221/w1700/app_middle",
         |"//avatars.mds.yandex.net/get-realty/54213/w1729/app_middle",
         |"//avatars.mds.yandex.net/get-realty/35066/w1248/app_middle"
         |],
         |"appLargeImages": [
         |"//avatars.mds.yandex.net/get-realty/63416/w1619/app_large",
         |"//avatars.mds.yandex.net/get-realty/51167/w1911/app_large",
         |"//avatars.mds.yandex.net/get-realty/43143/w1570/app_large",
         |"//avatars.mds.yandex.net/get-realty/44639/w1989/app_large",
         |"//avatars.mds.yandex.net/get-realty/59561/w1845/app_large",
         |"//avatars.mds.yandex.net/get-realty/59561/w1828/app_large",
         |"//avatars.mds.yandex.net/get-realty/63221/w1700/app_large",
         |"//avatars.mds.yandex.net/get-realty/54213/w1729/app_large",
         |"//avatars.mds.yandex.net/get-realty/35066/w1248/app_large"
         |],
         |"appMiniSnippetImages": [
         |"//avatars.mds.yandex.net/get-realty/63416/w1619/app_snippet_mini",
         |"//avatars.mds.yandex.net/get-realty/51167/w1911/app_snippet_mini",
         |"//avatars.mds.yandex.net/get-realty/43143/w1570/app_snippet_mini",
         |"//avatars.mds.yandex.net/get-realty/44639/w1989/app_snippet_mini",
         |"//avatars.mds.yandex.net/get-realty/59561/w1845/app_snippet_mini",
         |"//avatars.mds.yandex.net/get-realty/59561/w1828/app_snippet_mini",
         |"//avatars.mds.yandex.net/get-realty/63221/w1700/app_snippet_mini",
         |"//avatars.mds.yandex.net/get-realty/54213/w1729/app_snippet_mini",
         |"//avatars.mds.yandex.net/get-realty/35066/w1248/app_snippet_mini"
         |],
         |"appSmallSnippetImages": [
         |"//avatars.mds.yandex.net/get-realty/63416/w1619/app_snippet_small",
         |"//avatars.mds.yandex.net/get-realty/51167/w1911/app_snippet_small",
         |"//avatars.mds.yandex.net/get-realty/43143/w1570/app_snippet_small",
         |"//avatars.mds.yandex.net/get-realty/44639/w1989/app_snippet_small",
         |"//avatars.mds.yandex.net/get-realty/59561/w1845/app_snippet_small",
         |"//avatars.mds.yandex.net/get-realty/59561/w1828/app_snippet_small",
         |"//avatars.mds.yandex.net/get-realty/63221/w1700/app_snippet_small",
         |"//avatars.mds.yandex.net/get-realty/54213/w1729/app_snippet_small",
         |"//avatars.mds.yandex.net/get-realty/35066/w1248/app_snippet_small"
         |],
         |"appMiddleSnippetImages": [
         |"//avatars.mds.yandex.net/get-realty/63416/w1619/app_snippet_middle",
         |"//avatars.mds.yandex.net/get-realty/51167/w1911/app_snippet_middle",
         |"//avatars.mds.yandex.net/get-realty/43143/w1570/app_snippet_middle",
         |"//avatars.mds.yandex.net/get-realty/44639/w1989/app_snippet_middle",
         |"//avatars.mds.yandex.net/get-realty/59561/w1845/app_snippet_middle",
         |"//avatars.mds.yandex.net/get-realty/59561/w1828/app_snippet_middle",
         |"//avatars.mds.yandex.net/get-realty/63221/w1700/app_snippet_middle",
         |"//avatars.mds.yandex.net/get-realty/54213/w1729/app_snippet_middle",
         |"//avatars.mds.yandex.net/get-realty/35066/w1248/app_snippet_middle"
         |],
         |"appLargeSnippetImages": [
         |"//avatars.mds.yandex.net/get-realty/63416/w1619/app_snippet_large",
         |"//avatars.mds.yandex.net/get-realty/51167/w1911/app_snippet_large",
         |"//avatars.mds.yandex.net/get-realty/43143/w1570/app_snippet_large",
         |"//avatars.mds.yandex.net/get-realty/44639/w1989/app_snippet_large",
         |"//avatars.mds.yandex.net/get-realty/59561/w1845/app_snippet_large",
         |"//avatars.mds.yandex.net/get-realty/59561/w1828/app_snippet_large",
         |"//avatars.mds.yandex.net/get-realty/63221/w1700/app_snippet_large",
         |"//avatars.mds.yandex.net/get-realty/54213/w1729/app_snippet_large",
         |"//avatars.mds.yandex.net/get-realty/35066/w1248/app_snippet_large"
         |],
         |"apartment": {},
         |"location": {
         |"rgid": 417899,
         |"geoId": 2,
         |"subjectFederationId": 10174,
         |"settlementRgid": 417899,
         |"settlementGeoId": 2,
         |"address": "Санкт-Петербург, Пионерская улица, 50",
         |"point": {
         |"latitude": 59.96354,
         |"longitude": 30.27317,
         |"precision": "EXACT"
         |},
         |"metro": {
         |"metroGeoId": 20333,
         |"name": "Чкаловская",
         |"metroTransport": "ON_FOOT",
         |"timeToMetro": 17,
         |"latitude": 59.960987,
         |"longitude": 30.292103,
         |"minTimeToMetro": 17
         |},
         |"streetAddress": "Пионерская улица, 50",
         |"metroList": [
         |{
         |"metroGeoId": 20333,
         |"name": "Чкаловская",
         |"metroTransport": "ON_FOOT",
         |"timeToMetro": 17,
         |"latitude": 59.960987,
         |"longitude": 30.292103,
         |"minTimeToMetro": 17
         |},
         |{
         |"metroGeoId": 20332,
         |"name": "Спортивная",
         |"metroTransport": "ON_TRANSPORT",
         |"timeToMetro": 13,
         |"latitude": 59.950222,
         |"longitude": 30.288324,
         |"minTimeToMetro": 13
         |},
         |{
         |"metroGeoId": 20334,
         |"name": "Крестовский остров",
         |"metroTransport": "ON_FOOT",
         |"timeToMetro": 22,
         |"latitude": 59.971798,
         |"longitude": 30.259378,
         |"minTimeToMetro": 22
         |},
         |{
         |"metroGeoId": 20336,
         |"name": "Петроградская",
         |"metroTransport": "ON_TRANSPORT",
         |"timeToMetro": 15,
         |"latitude": 59.9665,
         |"longitude": 30.31144,
         |"minTimeToMetro": 15
         |}
         |]
         |},
         |"house": {
         |"bathroomUnit": "MATCHED",
         |"housePart": false
         |},
         |"building": {
         |"builtYear": 2018,
         |"builtQuarter": 2,
         |"buildingState": "UNFINISHED",
         |"buildingType": "MONOLIT_BRICK",
         |"siteId": 48503,
         |"siteName": "Премьер Палас",
         |"siteDisplayName": "ЖК «Премьер Палас»"
         |},
         |"description": "Жилой комплекс «Премьер Палас» возводится в исторической части Петроградской стороны. Благодаря своему расположению большинство квартир имеют хорошие видовые характеристики, из окон видны Финский залив, Нева, панорама Петербурга, в том числе, исторические и парковые территории.\n\nПридомовую территорию застройщик обещает благоустроить и закрыть для посторонних. По проекту, здесь должны появиться тротуары, асфальтированные проезды и открытые парковки, детские и игровые площадки, а также зоны отдыха. Кроме того, внутренний двор планируют комплексно озеленить. Предусмотрена высадка декоративных деревьев, кустарников и устройство газонов. Свои автомобили будущие жильцы смогут оставить в подземном отапливаемом паркинге, который планируют оснастить системами безопасности.\n\nВ процессе реализации проекта в комплексе появятся коммерческие помещения, предназначенные под фитнес-центр, spa-салон и сауну. Все корпуса, согласно проекту, будут оборудованы малошумными лифтами повышенной комфортности; кроме того, в секциях предусмотрены отдельные помещения для круглосуточной службы консьержей. \n\nНеподалёку от будущего комплекса работают детские садики, школы, поликлиника, высшие учебные заведения и другие необходимые социальные объекты. Торговая инфраструктура представлена, в большинстве своём, небольшими магазинами и супермаркетами.",
         |"internal": true,
         |"active": true,
         |"dealType": "UNKNOWN",
         |"raised": false,
         |"premium": false,
         |"placement": false,
         |"promoted": false,
         |"dealStatus": "SALE",
         |"salesDepartments": [
         |{
         |"id": 183435,
         |"name": "ПАН. Первое Агентство Недвижимости",
         |"phones": [
         |"+78123842143"
         |],
         |"weekTimetable": [
         |{
         |"timePattern": [
         |{
         |"open": "09:00",
         |"close": "21:00"
         |}
         |],
         |"dayFrom": 1,
         |"dayTo": 7
         |}
         |],
         |"logo": "//avatars.mdst.yandex.net/get-realty/3423/company.183435.21087340/builder_logo_info",
         |"dump": {
         |"billing.header.owner.id.client_id": "7686350",
         |"billing.header.settings.call_settings.phone.country": "7",
         |"billing.header.order.commit_amount": "1835000",
         |"billing.header.order.total_income": "36420000",
         |"billing.header.owner.version": "1",
         |"billing.header.owner.resource_ref@0.version": "1",
         |"billing.header.owner.id.version": "1",
         |"billing.context.work_policy.days@0.from": "2017-05-12",
         |"billing.header.owner.resource_ref@0.developer_id": "183435",
         |"billing.instance.id": "56e2143d5e6294c755d2ea947a21d32f",
         |"billing.header.name": "Премьер Палас",
         |"billing.header.settings.call_settings.redirectPhone.country": "7",
         |"billing.header.order.version": "1",
         |"billing.header.order.approximate_amount": "1835000",
         |"billing.context.version": "1",
         |"billing.header.order.product_key": "default",
         |"billing.active.deadline": "2017-05-12T10:47:36.749Z",
         |"billing.header.settings.call_settings.metrika_phone.code": "812",
         |"billing.header.settings.call_settings.source_type": "TELEPONY",
         |"billing.offer.id": "7(812)4261534/7(812)3842143",
         |"billing.header.settings.version": "1",
         |"billing.header.settings.call_settings.phone.code": "812",
         |"billing.header.settings.is_enabled": "true",
         |"billing.header.version": "1",
         |"billing.header.order.owner.client_id": "7686350",
         |"billing.header.settings.call_settings.redirectPhone.code": "812",
         |"billing.context.work_policy.days@0.times@0.from": "09:00:00.000",
         |"billing.header.settings.call_settings.redirectPhone.phone": "3842143",
         |"billing.header.settings.call_settings.redirectId": "74J_1U93vOc",
         |"billing.header.id": "c5682412-de43-4863-ae0c-0b6085e12638",
         |"billing.header.settings.call_settings.phone.phone": "4261534",
         |"billing.context.work_policy.days@0.to": "2017-05-16",
         |"billing.header.order.total_spent": "34585000",
         |"billing.header.product.goods@0.placement.cost.per_call.units": "600000",
         |"billing.header.order.owner.version": "1",
         |"billing.header.order.id": "1280",
         |"billing.header.settings.call_settings.metrika_phone.country": "7",
         |"billing.header.order.memo": "-",
         |"billing.header.product.goods@0.placement.cost.version": "1",
         |"billing.call.revenue": "350000",
         |"billing.context.work_policy.days@0.times@0.to": "21:00:00.000",
         |"billing.context.work_policy.time_zone": "Europe/Moscow",
         |"billing.header.order.text": "Яндекс.Недвижимость, ПАН. Первое Агентство Недвижимости",
         |"billing.header.product.version": "1",
         |"billing.header.settings.call_settings.metrika_phone.phone": "3842143",
         |"billing.header.product.goods@0.version": "1"
         |},
         |"redirectPhones": false
         |}
         |],
         |"commissioningDateIndexValue": 20182,
         |"verified": false,
         |"partnerInternalId": "484081",
         |"history": {
         |"prices": [
         |{
         |"date": "2016-12-21T16:59:06Z",
         |"value": 4555189
         |},
         |{
         |"date": "2016-12-28T20:11:09Z",
         |"value": 4599168
         |},
         |{
         |"date": "2017-01-17T06:54:26Z",
         |"value": 4674209
         |},
         |{
         |"date": "2017-01-27T06:26:36Z",
         |"value": 4723411
         |},
         |{
         |"date": "2017-02-01T19:36:52Z",
         |"value": 4755531
         |},
         |{
         |"date": "2017-03-01T19:06:20Z",
         |"value": 4803098
         |},
         |{
         |"date": "2017-03-22T19:21:37Z",
         |"value": 4855869
         |},
         |{
         |"date": "2017-04-04T20:02:48Z",
         |"value": 4904428
         |},
         |{
         |"date": "2017-04-15T18:38:27Z",
         |"value": 5056082
         |},
         |{
         |"date": "2017-05-02T06:32:06Z",
         |"value": 5159942
         |}
         |]
         |},
         |"newBuilding": true,
         |"primarySaleV2": true,
         |"suspicious": false,
         |"openPlan": false,
         |"canSearchOffer": true,
         |"verifiedByYandex": false,
         |"transactionConditionsMap": {
         |"MORTGAGE": false
         |}
         |}
      """.stripMargin
    )

    val unified = u.unify(json, UnificationContext(isMobile = true, antirobotDegradation = false))
    println(Json.prettyPrint(unified))
    (unified \\ "statParams").nonEmpty should be(true)
    (unified \\ "dump").nonEmpty should be(true)
    (unified \\ "dump").head should be(JsNull)
  }
}
