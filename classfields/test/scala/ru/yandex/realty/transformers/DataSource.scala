package ru.yandex.realty.transformers

import play.api.libs.json.{JsObject, Json}

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 07.06.17
  */
trait DataSource {

  val FilterStateSellApartment: JsObject =
    Json.parse("""
      | {
      |            "type": "SELL",
      |            "category": "APARTMENT",
      |            "location": {
      |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
      |                "longitude": 30.38843346,
      |                "latitude": 60.01175308,
      |                "country": 225,
      |                "rgid": 417899
      |            },
      |            "buildingType": "MONOLIT",
      |            "apartments": true,
      |            "builtYear": 2010,
      |            "newFlat": false,
      |            "lift": true,
      |            "rubbishChute": true,
      |            "alarm": true,
      |            "passBy": true,
      |            "parkingType": "SECURE",
      |            "floors": [ 3 ],
      |            "floorsTotal": 10,
      |            "roomsTotalApartment": "4",
      |            "area": 200,
      |            "rooms": [ 35, 35, 35, 35 ],
      |            "livingSpace": 140,
      |            "kitchenSpace": 30,
      |            "ceilingHeight": 2,
      |            "bathroomUnit": "SEPARATED",
      |            "balcony": "BALCONY_LOGGIA",
      |            "windowView": [ "YARD", "STREET" ],
      |            "floorCovering": "LAMINATED_FLOORING_BOARD",
      |            "renovation": "EURO",
      |            "phone": true,
      |            "internet": true,
      |            "roomFurniture": true,
      |            "kitchenFurniture": true,
      |            "buildInTech": true,
      |            "aircondition": true,
      |            "refrigerator": true,
      |            "price": 10000000,
      |            "currency": "RUR",
      |            "haggle": true,
      |            "mortgage": true,
      |            "dealType": "DIRECT",
      |            "apartment": "10",
      |            "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
      |            "photo": [
      |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f"
      |            ],
      |            "siteId": {
      |                "name": "ЖК «Орбита»",
      |                "id": "17723"
      |            },
      |            "curatedFlatPlan": {
      |                "url": "",
      |                "auto": true,
      |                "removed": false
      |            },
      |            "videoReviewUrl" : "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
      |            "onlineShowPossible" : true
      |        }
    """.stripMargin).as[JsObject]

  val OfferDataSellApartment: JsObject = Json
    .parse(
      """
      |{
      |            "common": {
      |                "propertyType": "LIVING",
      |                "offerType": "SELL",
      |                "category": "APARTMENT",
      |                "haggle": true,
      |                "mortgage": true
      |            },
      |            "buildingDescription": {
      |                "buildingType": "MONOLIT",
      |                "builtYear": 2010,
      |                "lift": true,
      |                "rubbishChute": true,
      |                "alarm": true,
      |                "passBy": true,
      |                "parkingType": "SECURE",
      |                "floorsTotal": 10,
      |                "ceilingHeight": 2,
      |                "buildingName": "ЖК «Орбита»",
      |                "siteId": 17723
      |            },
      |            "objectInformation": {
      |                "apartments": true,
      |                "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
      |                "studio": false,
      |                "area": {
      |                    "unit": "SQ_M",
      |                    "value": 200
      |                },
      |                "livingSpace": {
      |                    "unit": "SQ_M",
      |                    "value": 140
      |                },
      |                "kitchenSpace": {
      |                    "unit": "SQ_M",
      |                    "value": 30
      |                },
      |                "rooms": [ {
      |                    "unit": "SQ_M",
      |                    "value": 35
      |                }, {
      |                    "unit": "SQ_M",
      |                    "value": 35
      |                }, {
      |                    "unit": "SQ_M",
      |                    "value": 35
      |                }, {
      |                    "unit": "SQ_M",
      |                    "value": 35
      |                } ],
      |                "renovation": "EURO",
      |                "images": [ {
      |                    "active": true,
      |                    "url": "https://avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/orig"
      |                } ]
      |            },
      |            "livingPremises": {
      |                "newFlat": false,
      |                "openPlan": false,
      |                "rooms": 4,
      |                "bathroomUnit": "SEPARATED",
      |                "balcony": "BALCONY_LOGGIA",
      |                "windowView": "YARD_STREET",
      |                "floorCovering": "LAMINATED_FLOORING_BOARD",
      |                "phone": true,
      |                "internet": true,
      |                "refrigerator": true,
      |                "roomFurniture": true,
      |                "kitchenFurniture": true,
      |                "buildInTech": true,
      |                "floor": [ 3 ],
      |                "aircondition": true,
      |                "curatedFlatPlan": {
      |                    "url": "",
      |                    "auto": true,
      |                    "removed": false
      |                }
      |            },
      |            "price": {
      |                "value": 10000000,
      |                "currency": "RUB",
      |                "dealStatus": "SALE"
      |            },
      |            "location": {
      |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
      |                "latitude": 60.01175308,
      |                "longitude": 30.38843346,
      |                "country": "Россия",
      |                "rgid": 417899,
      |                "apartment": "10"
      |            },
      |            "remoteReview" : {
      |                "videoReviewUrl" : "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
      |                "onlineShowPossible" : true
      |            }
      |        }
    """.stripMargin
    )
    .as[JsObject]

  val FilterStateSellRooms: JsObject = Json
    .parse("""
       |{
       |            "type": "SELL",
       |            "category": "ROOMS",
       |            "location": {
       |                "address": "Россия, Москва, Архангельский переулок, 3с1",
       |                "latitude": 55.76076889,
       |                "longitude": 37.63727188,
       |                "country": 225,
       |                "rgid": 193389
       |            },
       |            "buildingType": "BRICK",
       |            "apartments": true,
       |            "builtYear": 2010,
       |            "lift": false,
       |            "rubbishChute": true,
       |            "alarm": false,
       |            "passBy": false,
       |            "parkingType": "SECURE",
       |            "floors": [ 5 ],
       |            "floorsTotal": 9,
       |            "roomsOffered": "2",
       |            "roomsTotal": "4",
       |            "area": 120,
       |            "rooms": [ 30, 30 ],
       |            "kitchenSpace": 20,
       |            "ceilingHeight": 3,
       |            "bathroomUnit": "MATCHED",
       |            "balcony": "LOGGIA",
       |            "windowView": [ "YARD" ],
       |            "floorCovering": "LINOLEUM",
       |            "renovation": "NORMAL",
       |            "phone": true,
       |            "internet": false,
       |            "roomFurniture": false,
       |            "kitchenFurniture": true,
       |            "buildInTech": false,
       |            "price": 1400000,
       |            "currency": "RUR",
       |            "haggle": true,
       |            "mortgage": false,
       |            "dealType": "DIRECT",
       |            "apartment": "10",
       |            "description": "Хорошая комната, в хорошей квартире, в хорошем дома и в хорошем городе",
       |            "photo": [ "https://avatars.mdst.yandex.net/get-realty/2991/add.148200205360507f14b2562/orig" ]
       |        }
    """.stripMargin)
    .as[JsObject]

  val OfferDataSellRooms: JsObject = Json
    .parse(
      """
      |{
      |            "common": {
      |                "propertyType": "LIVING",
      |                "offerType": "SELL",
      |                "category": "ROOMS",
      |                "haggle": true,
      |                "mortgage": false
      |            },
      |            "buildingDescription": {
      |                "buildingType": "BRICK",
      |                "builtYear": 2010,
      |                "lift": false,
      |                "rubbishChute": true,
      |                "alarm": false,
      |                "passBy": false,
      |                "parkingType": "SECURE",
      |                "floorsTotal": 9,
      |                "ceilingHeight": 3
      |            },
      |            "objectInformation": {
      |                "apartments": true,
      |                "description": "Хорошая комната, в хорошей квартире, в хорошем дома и в хорошем городе",
      |                "area": {
      |                    "unit": "SQ_M",
      |                    "value": 120
      |                },
      |                "kitchenSpace": {
      |                    "unit": "SQ_M",
      |                    "value": 20
      |                },
      |                "rooms": [ {
      |                    "unit": "SQ_M",
      |                    "value": 30
      |                }, {
      |                    "unit": "SQ_M",
      |                    "value": 30
      |                } ],
      |                "renovation": "NORMAL",
      |                "images": [ {
      |                    "active": true,
      |                    "url": "https://avatars.mdst.yandex.net/get-realty/2991/add.148200205360507f14b2562/orig"
      |                } ]
      |            },
      |            "price": {
      |                "value": 1400000,
      |                "currency": "RUB",
      |                "dealStatus": "SALE"
      |            },
      |            "location": {
      |                "address": "Россия, Москва, Архангельский переулок, 3с1",
      |                "latitude": 55.76076889,
      |                "longitude": 37.63727188,
      |                "country": "Россия",
      |                "rgid": 193389,
      |                "apartment": "10"
      |            },
      |            "livingPremises": {
      |                "floor": [ 5 ],
      |                "roomsOffered": 2,
      |                "rooms": 4,
      |                "bathroomUnit": "MATCHED",
      |                "balcony": "LOGGIA",
      |                "windowView": "YARD",
      |                "floorCovering": "LINOLEUM",
      |                "phone": true,
      |                "internet": false,
      |                "roomFurniture": false,
      |                "kitchenFurniture": true,
      |                "buildInTech": false
      |            }
      |        }
    """.stripMargin
    )
    .as[JsObject]

  val FilterStateSellGarage: JsObject = Json
    .parse("""{
             |            "type": "SELL",
             |            "category": "GARAGE",
             |            "location": {
             |                "address": "Россия, Московская область, Орехово-Зуевский район",
             |                "latitude": 55.64774704,
             |                "longitude": 38.90790939,
             |                "country": 225,
             |                "rgid": 587679
             |            },
             |            "lotArea": 100,
             |            "lotAreaUnit": "SOTKA",
             |            "lotType": "IGS",
             |            "houseArea": 150,
             |            "houseFloors": 2,
             |            "shower": "OUTSIDE",
             |            "toilet": "OUTSIDE",
             |            "pmg": false,
             |            "kitchen": false,
             |            "heatingSupply": true,
             |            "waterSupply": true,
             |            "sewerageSupply": false,
             |            "electricitySupply": false,
             |            "gasSupply": true,
             |            "billiard": true,
             |            "garage": {
             |                "type": "GARAGE",
             |                "ownership": "PRIVATE",
             |                "name": "ГСК 1"
             |            },
             |            "sauna": false,
             |            "pool": false,
             |            "price": 13000000,
             |            "currency": "RUR",
             |            "haggle": true,
             |            "mortgage": true,
             |            "dealType": "ALTERNATIVE",
             |            "cadastralNumber": "",
             |            "description": "Хороший дом, хороший участок, хорошие соседи",
             |            "photo": [ "https://avatars.mdst.yandex.net/get-realty/2957/add.14820022776942c4ff31ea0/orig" ]
             |        }
    """.stripMargin)
    .as[JsObject]

  val OfferDataSellGarage: JsObject = Json
    .parse(
      """
      {
        |  "price": {
        |    "value": 3000000,
        |    "currency": "RUB"
        |  },
        |  "location": {
        |    "address": "Донской район",
        |    "latitude": 55.69625473,
        |    "longitude": 37.60534286,
        |    "country": "Россия",
        |    "rgid": 587795
        |  },
        |  "commercialPremises": {
        |    "fireAlarm": true
        |  },
        |  "garage": {
        |    "supplies": {
        |      "carWash": true,
        |      "inspectionPit": true,
        |      "autoRepair": true,
        |      "electricity": true,
        |      "heating": true,
        |      "water": true
        |    },
        |    "name": "ГСК 5",
        |    "type": "GARAGE",
        |    "ownership": "PRIVATE"
        |  },
        |  "objectInformation": {
        |    "description": "Сам бы жил, да деньги нужны.",
        |    "area": {
        |      "unit": "SQ_M",
        |      "value": 15
        |    },
        |    "images": [
        |      {
        |        "active": true,
        |        "url": "https://avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/orig"
        |      },
        |      {
        |        "active": true,
        |        "url": "https://avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/orig"
        |      },
        |      {
        |        "active": true,
        |        "url": "https://avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/orig"
        |      }
        |    ]
        |  },
        |  "buildingDescription": {
        |    "buildingType": "BRICK",
        |    "parkingType": "UNDERGROUND",
        |    "automaticGates": true,
        |    "cctv": true,
        |    "accessControlSystem": true,
        |    "security": true,
        |    "twentyFourSeven": true
        |  },
        |  "remoteReview": {
        |    "onlineShowPossible": false
        |  },
        |  "common": {
        |    "offerType": "SELL",
        |    "category": "GARAGE",
        |    "haggle": true,
        |    "imageOrderChangeAllowed": true,
        |    "propertyType": "LIVING"
        |  }
        |}
    """.stripMargin
    )
    .as[JsObject]

  val FilterStateSellHouse: JsObject = Json
    .parse("""
      | {
      |            "type": "SELL",
      |            "category": "HOUSE",
      |            "location": {
      |                "address": "Россия, Московская область, Орехово-Зуевский район",
      |                "latitude": 55.64774704,
      |                "longitude": 38.90790939,
      |                "country": 225,
      |                "rgid": 587679
      |            },
      |            "lotArea": 100,
      |            "lotAreaUnit": "SOTKA",
      |            "lotType": "IGS",
      |            "houseArea": 150,
      |            "houseFloors": 2,
      |            "shower": "OUTSIDE",
      |            "toilet": "OUTSIDE",
      |            "pmg": false,
      |            "kitchen": false,
      |            "heatingSupply": true,
      |            "waterSupply": true,
      |            "sewerageSupply": false,
      |            "electricitySupply": false,
      |            "gasSupply": true,
      |            "billiard": true,
      |            "sauna": false,
      |            "pool": false,
      |            "price": 13000000,
      |            "currency": "RUR",
      |            "haggle": true,
      |            "mortgage": true,
      |            "dealType": "ALTERNATIVE",
      |            "cadastralNumber": "",
      |            "description": "Хороший дом, хороший участок, хорошие соседи",
      |            "photo": [ "https://avatars.mdst.yandex.net/get-realty/2957/add.14820022776942c4ff31ea0/orig" ]
      |        }
    """.stripMargin)
    .as[JsObject]

  val OfferDataSellHouse: JsObject = Json
    .parse(
      """
      | {
      |            "common": {
      |                "propertyType": "LIVING",
      |                "offerType": "SELL",
      |                "category": "HOUSE",
      |                "haggle": true,
      |                "mortgage": true
      |            },
      |            "objectInformation": {
      |                "lotArea": {
      |                    "unit": "SOTKA",
      |                    "value": 100
      |                },
      |                "lotType": "IGS",
      |                "area": {
      |                    "unit": "SQ_M",
      |                    "value": 150
      |                },
      |                "description": "Хороший дом, хороший участок, хорошие соседи",
      |                "images": [ {
      |                    "active": true,
      |                    "url": "https://avatars.mdst.yandex.net/get-realty/2957/add.14820022776942c4ff31ea0/orig"
      |                } ]
      |            },
      |            "buildingDescription": {
      |                "floorsTotal": 2,
      |                "cadastralNumber": ""
      |            },
      |            "residentialRealEstate": {
      |                "shower": "OUTSIDE",
      |                "toilet": "OUTSIDE",
      |                "pmg": false,
      |                "kitchen": false,
      |                "heatingSupply": true,
      |                "waterSupply": true,
      |                "sewerageSupply": false,
      |                "electricitySupply": false,
      |                "gasSupply": true,
      |                "billiard": true,
      |                "sauna": false,
      |                "pool": false
      |            },
      |            "price": {
      |                "value": 13000000,
      |                "currency": "RUB",
      |                "dealStatus": "COUNTERSALE"
      |            },
      |            "location": {
      |                "address": "Россия, Московская область, Орехово-Зуевский район",
      |                "latitude": 55.64774704,
      |                "longitude": 38.90790939,
      |                "country": "Россия",
      |                "rgid": 587679
      |            }
      |        }
    """.stripMargin
    )
    .as[JsObject]

  val OfferDataVosSellApartment: JsObject = Json
    .parse(
      """
      |{
      |            "status": "OK",
      |            "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
      |            "offer": {
      |                "telephones": [ ],
      |                "photoCount": 2,
      |                "showDuration": 90,
      |                "type": "realty",
      |                "createTime": "2016-12-13 19:27:25",
      |                "updateTime": "2016-12-18 00:02:50",
      |                "id": "7788901307321249025",
      |                "status": "active",
      |                "payable": false,
      |                "payed": false,
      |                "endOfShow": "2017-03-18 00:02:50"
      |            },
      |            "photo": [
      |                {
      |                    "url": "https://avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/orig"
      |                },
      |                {
      |                    "url": "https://avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/orig",
      |                    "tag": "plan"
      |                }
      |            ],
      |            "specific": {
      |                "country": "Россия",
      |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
      |                "rgid": 417899,
      |                "latitude": 60.01175308,
      |                "longitude": 30.38843346,
      |                "builtYear": 2010,
      |                "ceilingHeight": 2,
      |                "alarm": true,
      |                "haggle": true,
      |                "internet": true,
      |                "kitchenFurniture": true,
      |                "lift": true,
      |                "mortgage": true,
      |                "newFlat": false,
      |                "openPlan": false,
      |                "passBy": true,
      |                "phone": true,
      |                "refrigerator": true,
      |                "roomFurniture": true,
      |                "rubbishChute": true,
      |                "studio": false,
      |                "apartments": true,
      |                "aircondition": true,
      |                "buildInTech": true,
      |                "areaUnit": "SQ_M",
      |                "areaValue": 200,
      |                "balcony": "BALCONY_LOGGIA",
      |                "buildingType": "MONOLIT",
      |                "category": "APARTMENT",
      |                "currency": "RUB",
      |                "bathroomUnit": "SEPARATED",
      |                "floorCovering": "LAMINATED_FLOORING_BOARD",
      |                "floors": [
      |                    3
      |                ],
      |                "floorsTotal": 10,
      |                "kitchenSpaceUnit": "SQ_M",
      |                "kitchenSpaceValue": 30,
      |                "livingSpaceUnit": "SQ_M",
      |                "livingSpaceValue": 140,
      |                "offerType": "SELL",
      |                "payedAdv": false,
      |                "notForAgents": false,
      |                "price": 10000000,
      |                "propertyType": "LIVING",
      |                "rooms": [
      |                    {
      |                        "unit": "SQ_M",
      |                        "value": 35
      |                    },
      |                    {
      |                        "unit": "SQ_M",
      |                        "value": 35
      |                    },
      |                    {
      |                        "unit": "SQ_M",
      |                        "value": 35
      |                    },
      |                    {
      |                        "unit": "SQ_M",
      |                        "value": 35
      |                    }
      |                ],
      |                "share": false,
      |                "roomsNumber": 4,
      |                "windowView": "YARD_STREET",
      |                "renovation": "EURO",
      |                "parkingType": "SECURE",
      |                "dealType": "DIRECT",
      |                "dealStatus": "SALE",
      |                "buildingName": "ЖК «Орбита»",
      |                "cadastralNumber": "",
      |                "apartment": "10",
      |                "siteId": 17723,
      |                "curatedFlatPlan": {
      |                    "url": "",
      |                    "auto": true,
      |                    "removed": false
      |                }
      |            },
      |            "user": {
      |                "createTime": "2016-08-16 18:49:14",
      |                "updateTime": "2016-12-13 19:27:25",
      |                "vertical": "realty",
      |                "id": "uid_4003764933",
      |                "login": "4003764933",
      |                "phones": [
      |                    "+79651007850"
      |                ],
      |                "status": "active",
      |                "type": 0,
      |                "licenseAgreement": false,
      |                "name": "st"
      |            }
      |        }
    """.stripMargin
    )
    .as[JsObject]

  val FilterStateEditSellApartment: JsObject =
    Json.parse("""
      | {
      |            "type": "SELL",
      |            "category": "APARTMENT",
      |            "location": {
      |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
      |                "longitude": 30.38843346,
      |                "latitude": 60.01175308,
      |                "country": 225,
      |                "rgid": 417899
      |            },
      |            "buildingType": "MONOLIT",
      |            "apartments": true,
      |            "builtYear": 2010,
      |            "newFlat": false,
      |            "lift": true,
      |            "rubbishChute": true,
      |            "alarm": true,
      |            "passBy": true,
      |            "parkingType": "SECURE",
      |            "floors": [ 3 ],
      |            "floorsTotal": 10,
      |            "roomsTotalApartment": "4",
      |            "area": 200,
      |            "rooms": [ 35, 35, 35, 35 ],
      |            "livingSpace": 140,
      |            "kitchenSpace": 30,
      |            "ceilingHeight": 2,
      |            "bathroomUnit": "SEPARATED",
      |            "balcony": "BALCONY_LOGGIA",
      |            "windowView": [ "YARD", "STREET" ],
      |            "floorCovering": "LAMINATED_FLOORING_BOARD",
      |            "renovation": "EURO",
      |            "phone": true,
      |            "internet": true,
      |            "roomFurniture": true,
      |            "kitchenFurniture": true,
      |            "buildInTech": true,
      |            "aircondition": true,
      |            "refrigerator": true,
      |            "price": 10000000,
      |            "currency": "RUR",
      |            "haggle": true,
      |            "mortgage": true,
      |            "dealStatus": "SALE",
      |            "apartment": "10",
      |            "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
      |            "photo": [
      |                {
      |                    "path": "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
      |                    "tag": "",
      |                    "sizes": [
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/app_snippet_large",
      |                            "variant": "app_snippet_large"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/app_snippet_middle",
      |                            "variant": "app_snippet_middle"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/app_snippet_small",
      |                            "variant": "app_snippet_small"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/app_snippet_mini",
      |                            "variant": "app_snippet_mini"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/app_large",
      |                            "variant": "app_large"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/app_middle",
      |                            "variant": "app_middle"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/cosmic",
      |                            "variant": "cosmic"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/alike",
      |                            "variant": "alike"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/large",
      |                            "variant": "large"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/main",
      |                            "variant": "main"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f/minicard",
      |                            "variant": "minicard"
      |                        }
      |                    ]
      |                },
      |                {
      |                    "path": "avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b",
      |                    "tag": "plan",
      |                    "sizes": [
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/app_snippet_large",
      |                            "variant": "app_snippet_large"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/app_snippet_middle",
      |                            "variant": "app_snippet_middle"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/app_snippet_small",
      |                            "variant": "app_snippet_small"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/app_snippet_mini",
      |                            "variant": "app_snippet_mini"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/app_large",
      |                            "variant": "app_large"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/app_middle",
      |                            "variant": "app_middle"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/cosmic",
      |                            "variant": "cosmic"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/alike",
      |                            "variant": "alike"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/large",
      |                            "variant": "large"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/main",
      |                            "variant": "main"
      |                        },
      |                        {
      |                            "path": "//avatars.mdst.yandex.net/get-realty/2935/add.15657901556847e73cca35b/minicard",
      |                            "variant": "minicard"
      |                        }
      |                    ]
      |                }
      |            ],
      |            "siteId": {
      |                "name": "ЖК «Орбита»",
      |                "id": "17723"
      |            },
      |            "curatedFlatPlan": {
      |                "url": "",
      |                "auto": true,
      |                "removed": false
      |            }
      |        }
    """.stripMargin).as[JsObject]

  val OfferDataVosSellRoom: JsObject = Json
    .parse(
      """
      | {
      |            "status": "OK",
      |            "description": "Хорошая комната, в хорошей квартире, в хорошем дома и в хорошем городе",
      |            "offer": {
      |                "telephones": [ ],
      |                "photoCount": 1,
      |                "showDuration": 30,
      |                "type": "realty",
      |                "createTime": "2016-12-17 22:14:24",
      |                "updateTime": "2016-12-18 00:04:14",
      |                "id": "4631355743148785409",
      |                "status": "active",
      |                "payable": false,
      |                "payed": false,
      |                "endOfShow": "2017-01-17 00:04:14"
      |            },
      |            "photo": [
      |                {
      |                    "url": "https://avatars.mdst.yandex.net/get-realty/2991/add.148200205360507f14b2562/orig"
      |                }
      |            ],
      |            "specific": {
      |                "country": "Россия",
      |                "address": "Россия, Москва, Архангельский переулок, 3с1",
      |                "rgid": 193389,
      |                "latitude": 55.76076889,
      |                "longitude": 37.63727188,
      |                "builtYear": 2010,
      |                "ceilingHeight": 3,
      |                "alarm": false,
      |                "haggle": true,
      |                "internet": false,
      |                "kitchenFurniture": true,
      |                "lift": false,
      |                "mortgage": false,
      |                "passBy": false,
      |                "phone": true,
      |                "roomFurniture": false,
      |                "rubbishChute": true,
      |                "apartments": true,
      |                "buildInTech": false,
      |                "areaUnit": "SQ_M",
      |                "areaValue": 120,
      |                "balcony": "LOGGIA",
      |                "buildingType": "BRICK",
      |                "category": "ROOMS",
      |                "currency": "RUB",
      |                "bathroomUnit": "MATCHED",
      |                "floorCovering": "LINOLEUM",
      |                "floors": [
      |                    5
      |                ],
      |                "floorsTotal": 9,
      |                "kitchenSpaceUnit": "SQ_M",
      |                "kitchenSpaceValue": 20,
      |                "offerType": "SELL",
      |                "payedAdv": false,
      |                "notForAgents": false,
      |                "price": 1400000,
      |                "propertyType": "LIVING",
      |                "rooms": [
      |                    {
      |                        "unit": "SQ_M",
      |                        "value": 30
      |                    },
      |                    {
      |                        "unit": "SQ_M",
      |                        "value": 30
      |                    }
      |                ],
      |                "share": false,
      |                "roomsNumber": 4,
      |                "roomsOffered": 2,
      |                "windowView": "YARD",
      |                "renovation": "NORMAL",
      |                "parkingType": "SECURE",
      |                "dealType": "DIRECT",
      |                "dealStatus": "SALE",
      |                "apartment": "10",
      |                "cadastralNumber": ""
      |            },
      |            "user": {
      |                "createTime": "2016-08-16 18:49:14",
      |                "updateTime": "2016-12-13 19:27:25",
      |                "vertical": "realty",
      |                "id": "uid_4003764933",
      |                "login": "4003764933",
      |                "phones": [
      |                    "+79651007850"
      |                ],
      |                "status": "active",
      |                "type": 0,
      |                "licenseAgreement": false,
      |                "name": "st"
      |            }
      |        }
    """.stripMargin
    )
    .as[JsObject]

  val OfferDataSellGarage2 =
    Json
      .parse("""{
               |  "status": "OK",
               |  "description": "Сам бы жил, да деньги нужны.",
               |  "offer": {
               |    "type": "realty",
               |    "showDuration": 120,
               |    "photoCount": 3,
               |    "telephones": [],
               |    "redirectPhones": true,
               |    "redirectPhonesState": {
               |      "status": "ENABLED"
               |    },
               |    "createTime": "2021-06-22 12:50:31",
               |    "updateTime": "2021-06-28 15:17:23",
               |    "id": "4841101740110335745",
               |    "status": "active",
               |    "endOfShow": "2021-10-26 15:17:23",
               |    "timezone": "+0300",
               |    "idxErrors": [],
               |    "payable": false,
               |    "payed": false,
               |    "unknown_product": {
               |      "id": "f05c13bcd17447b0abdd6ec4d433e53d",
               |      "start": "2021-06-22T10:53:42.751Z",
               |      "end": "2021-07-22T10:53:42.751Z",
               |      "scheduledEnd": "2021-07-22T10:53:42.751Z",
               |      "status": "active",
               |      "source": "manual",
               |      "packageProducts": []
               |    },
               |    "origin": {},
               |    "isFromFeed": false
               |  },
               |  "photo": [
               |    {
               |      "url": "https://avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/orig",
               |      "tag": "",
               |      "isExternal": false
               |    },
               |    {
               |      "url": "https://avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/orig",
               |      "tag": "",
               |      "isExternal": false
               |    },
               |    {
               |      "url": "https://avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/orig",
               |      "tag": "",
               |      "isExternal": false
               |    }
               |  ],
               |  "virtualTours": [],
               |  "specific": {
               |    "rgid": 587795,
               |    "country": "Россия",
               |    "address": "Донской район",
               |    "latitude": 55.69625473,
               |    "longitude": 37.60534286,
               |    "unifiedLocation": {
               |      "rgid": 193299,
               |      "region": "Москва",
               |      "localityName": "Москва",
               |      "subLocalityName": "Донской район",
               |      "shortAddress": "Донской район",
               |      "metroLineColor": [
               |        "ffa8af"
               |      ],
               |      "subjectFederationId": 1,
               |      "metro": [
               |        {
               |          "name": "Верхние Котлы",
               |          "timeOnFoot": 14,
               |          "timeOnTransport": 8,
               |          "geoId": 152942
               |        },
               |        {
               |          "name": "Тульская",
               |          "timeOnFoot": 16,
               |          "timeOnTransport": 12,
               |          "geoId": 20426
               |        },
               |        {
               |          "name": "Крымская",
               |          "timeOnFoot": 19,
               |          "timeOnTransport": 13,
               |          "geoId": 152943
               |        },
               |        {
               |          "name": "Нагатинская",
               |          "timeOnFoot": 21,
               |          "timeOnTransport": 8,
               |          "geoId": 20427
               |        }
               |      ]
               |    },
               |    "offerType": "SELL",
               |    "category": "GARAGE",
               |    "areaUnit": "SQ_M",
               |    "areaValue": 15,
               |    "buildingType": "BRICK",
               |    "currency": "RUB",
               |    "garage": {
               |      "name": "ГСК 5",
               |      "type": "PARKING_PLACE",
               |      "ownership": "PRIVATE"
               |    },
               |    "electricitySupply": true,
               |    "haggle": true,
               |    "heatingSupply": true,
               |    "waterSupply": true,
               |    "fireAlarm": true,
               |    "security": true,
               |    "accessControlSystem": true,
               |    "twentyFourSeven": true,
               |    "automaticGates": true,
               |    "cctv": true,
               |    "inspectionPit": true,
               |    "carWash": true,
               |    "autoRepair": true,
               |    "floors": [],
               |    "payedAdv": false,
               |    "imageOrderChangeAllowed": true,
               |    "notForAgents": false,
               |    "share": false,
               |    "onlineShowPossible": true,
               |    "videoReviewUrl": "",
               |    "price": 3000000,
               |    "propertyType": "LIVING",
               |    "parkingType": "UNDERGROUND",
               |    "trustedOfferInfo": {
               |      "isFullTrustedOwner": false,
               |      "ownerTrustedStatus": "NOT_LINKED_MOSRU",
               |      "isCadastrPersonMatched": false
               |    }
               |  },
               |  "user": {
               |    "createTime": "2020-02-28 15:26:58",
               |    "updateTime": "2020-07-10 11:49:33",
               |    "vertical": "realty",
               |    "id": "uid_4037530165",
               |    "login": "4037530165",
               |    "status": "active",
               |    "type": 0,
               |    "licenseAgreement": false,
               |    "paymentType": "NATURAL_PERSON",
               |    "redirectPhones": true,
               |    "capaUser": false,
               |    "reseller": false,
               |    "mosRuStatus": "NOT_PROCESSED",
               |    "mosRuAvailable": false,
               |    "name": "Никита",
               |    "email": "nnnnnnn@yandex.ru",
               |    "phones": [
               |      "+88005553535"
               |    ]
               |  }
               |}""".stripMargin)
      .as[JsObject]

  val FilterStateEditSellGarage: JsObject =
    Json
      .parse("""{
               |  "imageOrderChangeAllowed" : true,
               |  "price" : 3000000,
               |  "haggle" : true,
               |  "location" : {
               |    "address" : "Донской район",
               |    "latitude" : 55.69625473,
               |    "longitude" : 37.60534286,
               |    "country" : 225,
               |    "rgid" : 587795
               |  },
               |  "automaticGates" : true,
               |  "fireAlarm" : true,
               |  "garageOwnership" : "PRIVATE",
               |  "cctv" : true,
               |  "type" : "SELL",
               |  "garageType" : "PARKING_PLACE",
               |  "accessControlSystem" : true,
               |  "security" : true,
               |  "garageHeatingSupply" : true,
               |  "onlineShowPossible" : true,
               |  "garageName" : "ГСК 5",
               |  "currency" : "RUR",
               |  "description" : "Сам бы жил, да деньги нужны.",
               |  "parkingType" : "UNDERGROUND",
               |  "category" : "GARAGE",
               |  "carWash" : true,
               |  "photo" : [ {
               |    "path" : "avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c",
               |    "tag" : "",
               |    "sizes" : [ {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/app_snippet_large",
               |      "variant" : "app_snippet_large"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/app_snippet_middle",
               |      "variant" : "app_snippet_middle"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/app_snippet_small",
               |      "variant" : "app_snippet_small"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/app_snippet_mini",
               |      "variant" : "app_snippet_mini"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/app_large",
               |      "variant" : "app_large"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/app_middle",
               |      "variant" : "app_middle"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/cosmic",
               |      "variant" : "cosmic"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/alike",
               |      "variant" : "alike"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/large",
               |      "variant" : "large"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/main",
               |      "variant" : "main"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/minicard",
               |      "variant" : "minicard"
               |    } ]
               |  }, {
               |    "path" : "avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941",
               |    "tag" : "",
               |    "sizes" : [ {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/app_snippet_large",
               |      "variant" : "app_snippet_large"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/app_snippet_middle",
               |      "variant" : "app_snippet_middle"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/app_snippet_small",
               |      "variant" : "app_snippet_small"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/app_snippet_mini",
               |      "variant" : "app_snippet_mini"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/app_large",
               |      "variant" : "app_large"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/app_middle",
               |      "variant" : "app_middle"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/cosmic",
               |      "variant" : "cosmic"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/alike",
               |      "variant" : "alike"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/large",
               |      "variant" : "large"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/main",
               |      "variant" : "main"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/minicard",
               |      "variant" : "minicard"
               |    } ]
               |  }, {
               |    "path" : "avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc",
               |    "tag" : "",
               |    "sizes" : [ {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/app_snippet_large",
               |      "variant" : "app_snippet_large"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/app_snippet_middle",
               |      "variant" : "app_snippet_middle"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/app_snippet_small",
               |      "variant" : "app_snippet_small"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/app_snippet_mini",
               |      "variant" : "app_snippet_mini"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/app_large",
               |      "variant" : "app_large"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/app_middle",
               |      "variant" : "app_middle"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/cosmic",
               |      "variant" : "cosmic"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/alike",
               |      "variant" : "alike"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/large",
               |      "variant" : "large"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/main",
               |      "variant" : "main"
               |    }, {
               |      "path" : "//avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/minicard",
               |      "variant" : "minicard"
               |    } ]
               |  } ],
               |  "buildingType" : "BRICK",
               |  "area" : 15,
               |  "autoRepair" : true,
               |  "garageWaterSupply" : true,
               |  "twentyFourSeven" : true,
               |  "inspectionPit" : true,
               |  "garageElectricitySupply" : true
               |}
               |""".stripMargin)
      .as[JsObject]

  val FilterStateEditSellGarage2: JsObject =
    Json.parse("""
                 |{
                 |  "imageOrderChangeAllowed": true,
                 |  "garageWaterSupply": true,
                 |  "price": 3000000,
                 |  "location": {
                 |    "address": "Донской район",
                 |    "latitude": 55.69625473,
                 |    "longitude": 37.60534286,
                 |    "country": 225,
                 |    "rgid": 587795
                 |  },
                 |  "automaticGates": true,
                 |  "fireAlarm": true,
                 |  "garageOwnership": "PRIVATE",
                 |  "cctv": true,
                 |  "garageType": "GARAGE",
                 |  "type": "SELL",
                 |  "haggle": true,
                 |  "accessControlSystem": true,
                 |  "security": true,
                 |  "onlineShowPossible": false,
                 |  "garageName": "ГСК 5",
                 |  "currency": "RUR",
                 |  "description": "Сам бы жил, да деньги нужны.",
                 |  "parkingType": "UNDERGROUND",
                 |  "category": "GARAGE",
                 |  "carWash": true,
                 |  "photo": [
                 |    "https://avatars.mdst.yandex.net/get-realty/3019/add.1624355387182f0997a320c/orig",
                 |    "https://avatars.mdst.yandex.net/get-realty/2991/add.162435538715322206ba941/orig",
                 |    "https://avatars.mdst.yandex.net/get-realty/3019/add.16243553871835e003e18bc/orig"
                 |  ],
                 |  "buildingType": "BRICK",
                 |  "garageHeatingSupply": true,
                 |  "area": 15,
                 |  "autoRepair": true,
                 |  "garageElectricitySupply": true,
                 |  "twentyFourSeven": true,
                 |  "inspectionPit": true
                 |}
         """.stripMargin).as[JsObject]
}
