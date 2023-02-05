package ru.yandex.market.data.order

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.clean.data.model.dto.PromotionDto
import ru.yandex.market.clean.data.model.dto.stationSubscription.OrderSummaryDto
import ru.yandex.market.testcase.JsonSerializationTestCase
import java.math.BigDecimal

@RunWith(Enclosed::class)
class OrderShopOptionsDtoTest {

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance = OrderShopOptionsDto.testInstance().copy(
            removedByRegroupingItems = setOf("1", "2", "3"),
            summary =
            OrderSummaryDto.testBuilder()
                .appliedPromotions(
                    listOf(
                        PromotionDto(
                            type = "BLUE_MARKET",
                            buyerDiscount = BigDecimal("100"),
                            deliveryDiscount = BigDecimal.ZERO,
                            promoCode = "promoCode"
                        )
                    )
                )
                .totalMoneyAmount(BigDecimal.ZERO)
                .build(),
            nearestOutlet = NearestOutletDto("42", "55.11,66.22")
        )

        override val type = OrderShopOptionsDto::class.java

        override val jsonSource = text(
            """
                {
                      "removedByRegroupingItems": [
                          "1", "2", "3"
                      ],
                      "shopId": 42,
                      "paymentMethods": [
                        "CASH_ON_DELIVERY"
                      ],
                      "items": [
                        {
                          "bundleSettings": {
                            "quantityLimit": {
                              "minimum": 1,
                              "step": 1
                            }
                          },
                          "supplier": {
                            "id": 42,
                            "name": "name",
                            "organizations": [
                              {
                                "address": "address",
                                "contactPhone": "contactPhone",
                                "contactUrl": "contactUrl",
                                "name": "name",
                                "ogrn": "ogrn",
                                "postalAddress": "postalAddress",
                                "type": "type"
                              }
                            ],
                            "workSchedule": "workSchedule"
                          },
                          "offerId": "offer-id",
                          "offerPhone": "offer-phone",
                          "shopOfferId": {
                            "feedId": 42,
                            "offerId": "offerId"
                          },
                          "title": "title",
                          "price": 1000.0,
                          "basePrice": 1000.0,
                          "bnpl": false,
                          "count": 2,
                          "cpaUrl": "cpa-url",
                          "cpc": "cpc",
                          "modifications": [],
                          "errors": [],
                          "payload": "payload",
                          "subTotal": 0,
                          "preorder": false,
                          "restrictedAge18": false,
                          "promos": [
                            {
                              "type": "UNKNOWN",
                              "buyerDiscount": 10,
                              "deliveryDiscount": 10,
                              "isPickupPromocode": true,
                              "marketPromoId": "marketPromoId",
                              "promoCode": "promoCode"
                            }
                          ],
                          "buyerPriceNominal": 0,
                          "wareMd5": "persistent-offer-id",
                          "feeShow": "fee-show",
                          "cartShowInfo": "cartShowInfo",
                          "manufactCountries": [
                            {
                              "id": 42,
                              "name": "name",
                              "nameAccusative": "nameAccusative",
                              "type": "type"
                            }
                          ],
                          "supplierDescription": "supplierDescription",
                          "skuLink": "skuLink",
                          "sku": {
                            "id": "sku-id"
                          },
                          "skuType": "market",
                          "shopSku": "shopSku",
                          "delivery": {
                            "deliveryPartnerTypes": [
                              "YANDEX_MARKET"
                            ],
                            "isEda": false,
                            "isExpress": false
                          },
                          "warnings": [
                            {
                              "code": "code",
                              "message": "message"
                            }
                          ],
                          "categoryId": 42,
                          "label": "label",
                          "relatedItemLabel": "relatedItemLabel",
                          "vendorId": 42,
                          "warehouseId" : 1,
                          "fulfilmentWarehouseId" : 42,
                          "pricedropPromoEnabled": false,
                          "modelId": "modelId"
                        }
                      ],
                      "cheapestDeliveryOptions": [],
                      "deliveryOptions": [
                        {
                          "id": "id",
                          "__type": "service",
                          "deliveryPartnerType": "YANDEX_MARKET",
                          "deliveryPointSupportedAPI": "UNKNOWN",
                          "deliveryServiceId": 1,
                          "features": [],
                          "title": "title",
                          "userReceived": true,
                          "price": 1.1,
                          "marketBranded": false,
                          "isTryingAvailable": false,
                          "estimated": false,
                          "currency": "UNKNOWN",
                          "beginDate": "2018-06-22",
                          "endDate": "2018-06-22",
                          "paymentMethods": [
                            "CASH_ON_DELIVERY"
                          ],
                          "hiddenPaymentMethods": [
                            {
                              "value": "CASH_ON_DELIVERY",
                              "hiddenReason": "MUID"
                            }
                          ],
                          "outlets": [
                            {
                              "id": "0",
                              "name": "name",
                              "address": {
                                "regionId": 42,
                                "country": "country",
                                "city": "city",
                                "street": "street",
                                "district": "district",
                                "house": "house",
                                "block": "block",
                                "room": "room",
                                "geoLocation": "10.0,20.0",
                                "profileId": 0
                              },
                              "notes": "notes",
                              "phones": [
                                "phone"
                              ],
                              "deliveryOptionId": "delivery-option-id",
                              "deliveryDate": "2018-06-22",
                              "endDeliveryDate": "2018-06-22",
                              "shopId": 0,
                              "price": 1.1,
                              "currency": "UNKNOWN",
                              "workSchedulesV2": [
                                {
                                  "daysFrom": "start-day",
                                  "daysTill": "end-day",
                                  "from": "start-time",
                                  "till": "end-time",
                                  "breaks": [
                                    {
                                      "from": "start-time",
                                      "till": "end-time"
                                    }
                                  ]
                                }
                              ],
                              "paymentMethods": [
                                "CARD_ON_DELIVERY"
                              ],
                              "outletPurposes": [],
                              "daily": false,
                              "isMarketBranded": false,
                              "aroundTheClock": false,
                              "storagePeriod" : 1,
                              "isMarketPostamat":false,
                              "isMarketPickupPoint":false,
                              "isTryingAvailable": false
                            }
                          ],
                          "outlet": {
                            "id": "0",
                            "name": "name",
                            "address": {
                              "regionId": 42,
                              "country": "country",
                              "city": "city",
                              "street": "street",
                              "district": "district",
                              "house": "house",
                              "block": "block",
                              "room": "room",
                              "geoLocation": "10.0,20.0",
                              "profileId": 0
                            },
                            "notes": "notes",
                            "phones": [
                              "phone"
                            ],
                            "deliveryOptionId": "delivery-option-id",
                            "deliveryDate": "2018-06-22",
                            "endDeliveryDate": "2018-06-22",
                            "shopId": 0,
                            "price": 1.1,
                            "currency": "UNKNOWN",
                            "workSchedulesV2": [
                              {
                                "daysFrom": "start-day",
                                "daysTill": "end-day",
                                "from": "start-time",
                                "till": "end-time",
                                "breaks": [
                                  {
                                    "from": "start-time",
                                    "till": "end-time"
                                  }
                                ]
                              }
                            ],
                            "paymentMethods": [
                              "CARD_ON_DELIVERY"
                            ],
                            "outletPurposes": [],
                            "daily": false,
                            "isMarketBranded": false,
                            "aroundTheClock": false,
                            "storagePeriod" : 1,
                            "isMarketPostamat":false,
                            "isMarketPickupPoint":false,
                            "isTryingAvailable":false
                          }
                        }
                      ],
                      "errors": [],
                      "warnings":[],
                      "modifications": [
                        "COUNT"
                      ],
                      "summary": {
                        "baseAmount": 0,
                        "discountAmount": 0,
                        "totalAmount": 0,
                        "totalMoneyAmount": 0,
                        "weight": 0,
                        "delivery": {
                          "isFree": false,
                          "price": 0,
                          "leftToFree": 0,
                          "freeThreshold": 0,
                          "discountType": "discountType",
                          "freeDeliveryStatus" : "freeDeliveryStatus",
                          "freeDeliveryReason" : "freeDeliveryReason"
                        },
                        "isPickupPromocodeSuitable": false,
                        "promoCodeDiscount": 0,
                        "promoDiscount": 0,
                        "coinDiscount": 0,
                        "promos": [
                            {
                                "type": "BLUE_MARKET",
                                "buyerDiscount": 100,
                                "deliveryDiscount": 0,
                                "promoCode": "promoCode"
                            }
                        ]
                      },
                      "coinInfo": {
                        "unusedCoinIds": [
                          "42"
                        ],
                        "coinErrors": [
                          {
                            "coinId": "42",
                            "code": "code",
                            "message": "message"
                          }
                        ],
                        "allCoins": [
                          {
                            "title": "title",
                            "subtitle": "subtitle",
                            "coinType": "coinType",
                            "nominal": 500.0,
                            "description": "description",
                            "images": {
                              "standard": "standard",
                              "alt": "alt"
                            },
                            "backgroundColor": "backgroundColor",
                            "activationToken": "activationToken",
                            "id": "id",
                            "endDate": "endDate",
                            "creationDate": "creationDate",
                            "status": "status",
                            "reason": "ORDER",
                            "reasonParam": "12345",
                            "coinRestrictions": [],
                            "bonusLink": []
                          }
                        ]
                      },
                      "promos": [
                        {
                          "type": "UNKNOWN",
                          "buyerDiscount": 10,
                          "deliveryDiscount": 10,
                          "isPickupPromocode": true,
                          "marketPromoId": "marketPromoId",
                          "promoCode": "promoCode"
                        }
                      ],
                      "validFeatures": [
                        "TEST_FEATURE"
                      ],
                      "nearestOutlet": {
                        "id" : "42",
                        "gps" : "55.11,66.22"
                      },
                      "rgb": "BLUE",
                      "status": "UNKNOWN",
                      "substatus": "UNKNOWN",
                      "parcelInfo": "w:5;p:5;pc:RUR;tp:5;tpc:RUR;d:10x20x30;ct:1/2/3;wh:145;ffwh:145;"
                }
            """.trimIndent()
        )
    }
}
