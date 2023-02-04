package ru.yandex.vertis.parsing.realty.parsers.smartagent

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.{JsArray, JsString, Json}
import ru.vertis.holocron.realty.SellerType
import ru.yandex.realty.proto.GeoPoint
import ru.yandex.realty.proto.offer.{BuildingType, CommercialType, FlatType, PricingPeriod}
import ru.yandex.vertis.parsing.parsers.ParsedValue
import ru.yandex.vertis.parsing.realty.ParsingRealtyModel.{OfferCategory, OfferType}
import ru.yandex.vertis.parsing.realty.parsers.smartagent.cian.SmartAgentCianRealtyParser
import ru.yandex.vertis.parsing.realty.parsers.{LayoutType, TotalFloorsType}
import ru.yandex.vertis.parsing.util.DateUtils

/*
Тестирует общие методы обоих парсеров (циана и авито)
 */
@RunWith(classOf[JUnitRunner])
class SmartAgentRealtyParserTest extends FunSuite {
  test("photos") {
    assert(
      SmartAgentCianRealtyParser.parsePhotos(
        Json.obj(
          "images" -> JsArray(
            Seq(
              JsString(
                "https://cdn-p.cian.site/images/23/422/331/nezhiloe-pomeshcenie-zhukovskiy-ulica-gagarina-1332243277-1.jpg"
              ),
              JsString(
                "https://cdn-p.cian.site/images/23/422/331/nezhiloe-pomeshcenie-zhukovskiy-ulica-gagarina-1332243279-1.jpg"
              )
            )
          )
        )
      ) == ParsedValue.Expected(
        Seq(
          "https://cdn-p.cian.site/images/23/422/331/nezhiloe-pomeshcenie-zhukovskiy-ulica-gagarina-1332243277-1.jpg",
          "https://cdn-p.cian.site/images/23/422/331/nezhiloe-pomeshcenie-zhukovskiy-ulica-gagarina-1332243279-1.jpg"
        )
      )
    )
  }

  test("parse display publishing date") {
    assert(
      SmartAgentCianRealtyParser.parseDisplayedPublishDate(
        Json.obj(
          "listing_date" -> "2022-06-28"
        )
      ) == ParsedValue.Expected(DateUtils.jodaParse("2022-06-28"))
    )
  }

  test("description") {
    assert(
      SmartAgentCianRealtyParser.parseDescription(
        Json.obj(
          "listing_description" -> "Сдается квартира"
        )
      ) == ParsedValue.Expected("Сдается квартира")
    )
  }

  test("phones") {
    assert(
      SmartAgentCianRealtyParser.parsePhones(
        Json.obj(
          "listing_phone" -> "79852893066"
        )
      ) == ParsedValue.Expected(Seq(ParsedValue.Expected("79852893066")))
    )
  }

  test("phones unexpected") {
    assert(
      SmartAgentCianRealtyParser.parsePhones(
        Json.obj(
          "listing_phone" -> "phone"
        )
      ) == ParsedValue.Unexpected("phone")
    )
  }

  test("user id") {
    assert(
      SmartAgentCianRealtyParser.parseUserId(
        Json.obj(
          "listing_userid" -> 13309188
        )
      ) == ParsedValue.Expected(13309188)
    )
  }

  test("user name") {
    assert(
      SmartAgentCianRealtyParser.parseUserName(
        Json.obj(
          "listing_username" -> "Паша Волков"
        )
      ) == ParsedValue.Expected("Паша Волков")
    )
  }

  test("agent fee") {
    assert(
      SmartAgentCianRealtyParser.parseAgentFee(
        Json.obj(
          "realty_agent_fee" -> 30
        )
      ) == ParsedValue.Expected(30L)
    )
  }

  test("total floors") {
    assert(
      SmartAgentCianRealtyParser.parseTotalFloors(
        Json.obj(
          "realty_building_totalfloors" -> 10
        )
      ) == ParsedValue.Expected(TotalFloorsType.Number(10))
    )
  }

  test("building type") {
    assert(
      SmartAgentCianRealtyParser.parseBuildingType(
        Json.obj(
          "realty_building_type" -> "Кирпичный"
        )
      ) == ParsedValue.Expected(BuildingType.BUILDING_TYPE_BRICK)
    )
    assert(
      SmartAgentCianRealtyParser.parseBuildingType(
        Json.obj(
          "realty_building_type" -> "Монолитный"
        )
      ) == ParsedValue.Expected(BuildingType.BUILDING_TYPE_MONOLIT)
    )
    assert(
      SmartAgentCianRealtyParser.parseBuildingType(
        Json.obj(
          "realty_building_type" -> "Блочный"
        )
      ) == ParsedValue.Expected(BuildingType.BUILDING_TYPE_BLOCK)
    )
    assert(
      SmartAgentCianRealtyParser.parseBuildingType(
        Json.obj(
          "realty_building_type" -> "Деревянный"
        )
      ) == ParsedValue.Expected(BuildingType.BUILDING_TYPE_WOOD)
    )
    assert(
      SmartAgentCianRealtyParser.parseBuildingType(
        Json.obj(
          "realty_building_type" -> "Панельный"
        )
      ) == ParsedValue.Expected(BuildingType.BUILDING_TYPE_PANEL)
    )
  }

  test("deposit") {
    assert(
      SmartAgentCianRealtyParser.parseDeposit(
        Json.obj(
          "realty_deposit" -> "39000"
        )
      ) == ParsedValue.Expected(39000L)
    )
  }

  test("floor") {
    assert(
      SmartAgentCianRealtyParser.parseFloor(
        Json.obj(
          "realty_floor" -> 1
        )
      ) == ParsedValue.Expected(1)
    )
  }

  test("address") {
    assert(
      SmartAgentCianRealtyParser.parseAddress(
        Json.obj(
          "realty_location" -> "Санкт-Петербург, Гагаринское, Витебский проспект, 61к5"
        )
      ) == ParsedValue.Expected("Санкт-Петербург, Гагаринское, Витебский проспект, 61к5")
    )
  }

  test("geo point") {
    assert(
      SmartAgentCianRealtyParser.parseGeoPoint(
        Json.obj(
          "realty_location_lat" -> "60.0631",
          "realty_location_lon" -> "37.1410"
        )
      ) == ParsedValue.Expected(GeoPoint.newBuilder().setLatitude(60.0631f).setLongitude(37.1410f).build())
    )
  }

  test("price") {
    assert(
      SmartAgentCianRealtyParser.parsePrice(
        Json.obj(
          "realty_price" -> "39000"
        )
      ) == ParsedValue.Expected(39000L)
    )
  }

  test("flat type") {
    assert(
      SmartAgentCianRealtyParser.parseFlatType(
        Json.obj(
          "realty_property_new" -> "Вторичка"
        )
      ) == ParsedValue.Expected(FlatType.FLAT_TYPE_SECONDARY)
    )
    assert(
      SmartAgentCianRealtyParser.parseFlatType(
        Json.obj(
          "realty_property_new" -> "Новостройки"
        )
      ) == ParsedValue.Expected(FlatType.FLAT_TYPE_NEW_FLAT)
    )
  }

  test("commercial type") {
    assert(
      SmartAgentCianRealtyParser.parseCommercialType(
        Json.obj(
          "realty_property_type" -> "Складские помещения"
        )
      ) == ParsedValue.Expected(CommercialType.COMMERCIAL_TYPE_WAREHOUSE)
    )
    assert(
      SmartAgentCianRealtyParser.parseCommercialType(
        Json.obj(
          "realty_property_type" -> "Торговые помещения"
        )
      ) == ParsedValue.Expected(CommercialType.COMMERCIAL_TYPE_RETAIL)
    )
    assert(
      SmartAgentCianRealtyParser.parseCommercialType(
        Json.obj(
          "realty_property_type" -> "Офисные помещения"
        )
      ) == ParsedValue.Expected(CommercialType.COMMERCIAL_TYPE_OFFICE)
    )
  }

  test("offer category") {
    assert(
      SmartAgentCianRealtyParser.parseOfferCategory(
        Json.obj(
          "realty_property_type" -> "Складские помещения"
        )
      ) == ParsedValue.Expected(OfferCategory.COMMERCIAL)
    )
    assert(
      SmartAgentCianRealtyParser.parseOfferCategory(
        Json.obj(
          "realty_property_type" -> "Коммерческая недвижимость"
        )
      ) == ParsedValue.Expected(OfferCategory.COMMERCIAL)
    )
    assert(
      SmartAgentCianRealtyParser.parseOfferCategory(
        Json.obj(
          "realty_property_type" -> "Офисные помещения"
        )
      ) == ParsedValue.Expected(OfferCategory.COMMERCIAL)
    )
    assert(
      SmartAgentCianRealtyParser.parseOfferCategory(
        Json.obj(
          "realty_property_type" -> "Торговые помещения"
        )
      ) == ParsedValue.Expected(OfferCategory.COMMERCIAL)
    )
    assert(
      SmartAgentCianRealtyParser.parseOfferCategory(
        Json.obj(
          "realty_property_type" -> "Таунхаусы"
        )
      ) == ParsedValue.Expected(OfferCategory.HOUSE)
    )
    assert(
      SmartAgentCianRealtyParser.parseOfferCategory(
        Json.obj(
          "realty_property_type" -> "Комнаты"
        )
      ) == ParsedValue.Expected(OfferCategory.ROOMS)
    )
    assert(
      SmartAgentCianRealtyParser.parseOfferCategory(
        Json.obj(
          "realty_property_type" -> "Квартиры"
        )
      ) == ParsedValue.Expected(OfferCategory.APARTMENT)
    )
    assert(
      SmartAgentCianRealtyParser.parseOfferCategory(
        Json.obj(
          "realty_property_type" -> "Дома, Дачи, Коттеджи"
        )
      ) == ParsedValue.Expected(OfferCategory.HOUSE)
    )
    assert(
      SmartAgentCianRealtyParser.parseOfferCategory(
        Json.obj(
          "realty_property_type" -> "Квартира студия"
        )
      ) == ParsedValue.Expected(OfferCategory.APARTMENT)
    )
  }

  test("rooms") {
    assert(
      SmartAgentCianRealtyParser.parseRooms(
        Json.obj(
          "realty_rooms" -> "30"
        )
      ) == ParsedValue.Expected(LayoutType.Rooms(30))
    )
    assert(
      SmartAgentCianRealtyParser.parseRooms(
        Json.obj(
          "realty_property_type" -> "Квартира студия",
          "realty_rooms" -> "30"
        )
      ) == ParsedValue.Expected(LayoutType.Studio)
    )
  }

  test("kitchen space") {
    assert(
      SmartAgentCianRealtyParser.parseKitchenArea(
        Json.obj(
          "realty_space_kitchen" -> "30.0"
        )
      ) == ParsedValue.Expected(30.0f)
    )
  }

  test("living space") {
    assert(
      SmartAgentCianRealtyParser.parseLivingArea(
        Json.obj(
          "realty_space_living" -> "30.0"
        )
      ) == ParsedValue.Expected(30.0f)
    )
  }

  test("total area") {
    assert(
      SmartAgentCianRealtyParser.parseTotalArea(
        Json.obj(
          "realty_space_total" -> "30.0"
        )
      ) == ParsedValue.Expected(30.0f)
    )
  }

  test("pricing period") {
    assert(
      SmartAgentCianRealtyParser.parsePricingPeriod(
        Json.obj(
          "realty_subtype" -> "Посуточно"
        )
      ) == ParsedValue.Expected(PricingPeriod.PRICING_PERIOD_PER_DAY)
    )
    assert(
      SmartAgentCianRealtyParser.parsePricingPeriod(
        Json.obj(
          "realty_subtype" -> "На длительный срок"
        )
      ) == ParsedValue.Expected(PricingPeriod.PRICING_PERIOD_PER_MONTH)
    )
  }

  test("offer type") {
    assert(
      SmartAgentCianRealtyParser.parseOfferType(
        Json.obj(
          "realty_type" -> "Продам"
        )
      ) == ParsedValue.Expected(OfferType.SELL)
    )
    assert(
      SmartAgentCianRealtyParser.parseOfferType(
        Json.obj(
          "realty_type" -> "Сдам"
        )
      ) == ParsedValue.Expected(OfferType.OWNER_RENT)
    )
    assert(
      SmartAgentCianRealtyParser.parseOfferType(
        Json.obj(
          "realty_type" -> "Куплю"
        )
      ) == ParsedValue.Expected(OfferType.BUY)
    )
    assert(
      SmartAgentCianRealtyParser.parseOfferType(
        Json.obj(
          "realty_type" -> "Сниму"
        )
      ) == ParsedValue.Expected(OfferType.CLIENT_RENT)
    )
  }

  test("seller name") {
    assert(
      SmartAgentCianRealtyParser.parseSellerName(
        Json.obj(
          "seller_name" -> "Риэлтрейд"
        )
      ) == ParsedValue.Expected("Риэлтрейд")
    )
  }

  test("seller type") {
    assert(
      SmartAgentCianRealtyParser.parseSellerType(
        Json.obj(
          "seller_type" -> "owner"
        )
      ) == ParsedValue.Expected(SellerType.SELLER_TYPE_OWNER)
    )
    assert(
      SmartAgentCianRealtyParser.parseSellerType(
        Json.obj(
          "seller_type" -> "agency"
        )
      ) == ParsedValue.Expected(SellerType.SELLER_TYPE_AGENCY)
    )
    assert(
      SmartAgentCianRealtyParser.parseSellerType(
        Json.obj(
          "seller_type" -> "Частное лицо"
        )
      ) == ParsedValue.Expected(SellerType.SELLER_TYPE_OWNER)
    )
    assert(
      SmartAgentCianRealtyParser.parseSellerType(
        Json.obj(
          "seller_type" -> "Агентство"
        )
      ) == ParsedValue.Expected(SellerType.SELLER_TYPE_AGENCY)
    )
  }

  test("seller url") {
    assert(
      SmartAgentCianRealtyParser.parseSellerUrl(
        Json.obj(
          "seller_url" -> "https://www.cian.ru/agents/14700/"
        )
      ) == ParsedValue.Expected("https://www.cian.ru/agents/14700/")
    )
  }

  test("price = 0") {
    assert(
      SmartAgentCianRealtyParser.parsePrice(
        Json.obj(
          "realty_price" -> "0"
        )
      ) == ParsedValue.NoValue
    )
  }

  test("price < 0") {
    assert(
      SmartAgentCianRealtyParser.parsePrice(
        Json.obj(
          "realty_price" -> "-5"
        )
      ) == ParsedValue.Unexpected("-5")
    )
  }
}
