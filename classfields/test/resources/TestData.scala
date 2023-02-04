package common.clients.moderation.test.resources

object TestData {

  val instancesJson =
    """{
      |  "total": 2,
      |  "page": {
      |    "size": 10,
      |    "number": 0
      |  },
      |  "values": [
      |    {
      |      "id": "CAESJQgBEgwIATIINzE0MTM0MzQaEzE1MTQ2OTM1NjUtZDQ5ZGNhMGM=",
      |      "externalId": {
      |        "user": {
      |          "type": "auto_ru",
      |          "userId": "auto_ru_71413434",
      |          "autoruId": "71413434"
      |        },
      |        "objectId": "1514693565-d49dca0c"
      |      },
      |      "essentials": {
      |        "type": "autoru",
      |        "autoru": {
      |          "description": "",
      |          "timestampCreate": 1650283538000,
      |          "category": "CARS",
      |          "section": "USED",
      |          "phones": {
      |            "73635373839": ""
      |          },
      |          "offerUserName": "id71413434",
      |          "ip": "2a02:6b8:0:520:844a:c5fd:375f:1269",
      |          "address": "",
      |          "geobaseId": 213,
      |          "geobaseIds": [
      |            213,
      |            1,
      |            3,
      |            225,
      |            10001,
      |            10000
      |          ],
      |          "sellerType": "PRIVATE",
      |          "mileage": 42222,
      |          "condition": "EXCELLENT",
      |          "ptsOwnerCount": "ONE",
      |          "ptsOwnersExactCount": 1,
      |          "isPtsOriginal": true,
      |          "vin": "YEY63636373373737",
      |          "customHouseState": "CLEARED",
      |          "year": 1994,
      |          "mark": "AUDI",
      |          "model": "80",
      |          "horsePower": 140,
      |          "bodyType": "SEDAN",
      |          "engineVolume": 1984,
      |          "engineType": "GASOLINE",
      |          "gearType": "FRONT",
      |          "doorsCount": 4,
      |          "steeringWheel": "LEFT",
      |          "transmissionType": "MECHANICAL",
      |          "superGen": "7878108",
      |          "colorHex": "ffffff",
      |          "photos": [
      |            {
      |              "id": "65744-371a22d8f09578b99ca8b21711034d86",
      |              "cvHash": "M6DA586DE21C86FBD",
      |              "picaInfo": {
      |                "partitionId": "a_71413434",
      |                "srcUrl": "http://avatars-int.mdst.yandex.net/get-autoru-orig/65744/371a22d8f09578b99ca8b21711034d86/1200x900",
      |                "metaVersion": 4
      |              },
      |              "deleted": false,
      |              "namespace": "autoru-orig",
      |              "photoType": "PHOTO_TYPE_UNKNOWN",
      |              "uploadTime": 1650283518315,
      |              "checkType": "PHOTO_CHECK_TYPE_UNKNOWN"
      |            }
      |          ],
      |          "moderationPhotos": [],
      |          "priceInfo": {
      |            "price": 155000,
      |            "currency": "RUB",
      |            "discountPrice": 155000
      |          },
      |          "originalPriceInfo": {
      |            "price": 155000,
      |            "currency": "RUB",
      |            "discountPrice": 155000
      |          },
      |          "isPlacedForFree": false,
      |          "platform": "IOS",
      |          "source": "AUTO_RU",
      |          "isCallCenter": false,
      |          "actualizeTime": 1650283975241,
      |          "predictPrice": {
      |            "currency": "RUR",
      |            "from": 138000,
      |            "to": 172000
      |          },
      |          "licensePlate": "Е737ОО736",
      |          "vinResolution": {
      |            "updated": 1650283907478
      |          },
      |          "priceMismatchDownRatioPercent": 112,
      |          "hasLicensePlateOnPhotos": true,
      |          "photosLicensePlate": "IN37",
      |          "notRegisteredInRussia": false,
      |          "vinResolutionStatus": "UNKNOWN",
      |          "nameplateFront": "",
      |          "complectationId": "0",
      |          "chatOnly": false,
      |          "panoramaStatus": "NO_PANORAMA",
      |          "autoruExpert": false,
      |          "additionalPhotos": [],
      |          "email": "Heh@yd.hd",
      |          "offerWasActive": true,
      |          "resellerStatusAccepted": false,
      |          "colorName": "Белый"
      |        }
      |      },
      |      "signals": [],
      |      "createTime": 1650283552413,
      |      "essentialsUpdateTime": 1650283997431,
      |      "updateTime": 1650283997431,
      |      "opinion": {
      |        "type": "unknown",
      |        "details": {
      |          "type": "autoru",
      |          "autoru": {
      |            "isBanByInheritance": false,
      |            "isFromReseller": false
      |          }
      |        },
      |        "warnDetailedReasons": []
      |      },
      |      "opinions": {
      |        "values": [
      |          {
      |            "domain": {
      |              "type": "autoru",
      |              "value": "DEFAULT_AUTORU"
      |            },
      |            "opinion": {
      |              "type": "unknown",
      |              "details": {
      |                "type": "autoru",
      |                "autoru": {
      |                  "isBanByInheritance": false,
      |                  "isFromReseller": false
      |                }
      |              },
      |              "warnDetailedReasons": []
      |            }
      |          }
      |        ]
      |      },
      |      "context": {
      |        "visibility": "VISIBLE"
      |      },
      |      "metadata": [
      |        {
      |          "timestamp": "1650283550727",
      |          "geobaseIp": {
      |            "ipInfos": [
      |              {
      |                "ip": "2a02:6b8:0:520:844a:c5fd:375f:1269",
      |                "isVpn": false,
      |                "isProxy": false,
      |                "isHosting": false,
      |                "isTor": false,
      |                "isYandexTurbo": false,
      |                "isYandexStaff": true,
      |                "isYandexNet": true
      |              }
      |            ]
      |          }
      |        }
      |      ],
      |      "signalMap": [],
      |      "switchOffMap": []
      |    },
      |    {
      |      "id": "CAESIggBEgkIASoFMjAxMDEaEzE1MTQ2OTM1NjQtN2I0OGEyYzA=",
      |      "externalId": {
      |        "user": {
      |          "type": "dealer",
      |          "userId": "dealer_20101",
      |          "dealerId": "20101"
      |        },
      |        "objectId": "1514693564-7b48a2c0"
      |      },
      |      "essentials": {
      |        "type": "autoru",
      |        "autoru": {
      |          "description": "",
      |          "timestampCreate": 1650283220000,
      |          "category": "CARS",
      |          "section": "NEW",
      |          "phones": {
      |            "74959999999": ""
      |          },
      |          "offerUserName": "САЛОНА НЕ СУЩЕСТВУЕТ СОВСЕМ!",
      |          "ip": "::1",
      |          "address": "улица Льва Толстого, 12с1",
      |          "geobaseId": 213,
      |          "geobaseIds": [
      |            213,
      |            1,
      |            3,
      |            225,
      |            10001,
      |            10000
      |          ],
      |          "sellerType": "COMMERCIAL",
      |          "mileage": 0,
      |          "condition": "EXCELLENT",
      |          "customHouseState": "CLEARED",
      |          "year": 2010,
      |          "mark": "CHERY",
      |          "model": "BONUS",
      |          "horsePower": 109,
      |          "bodyType": "SEDAN",
      |          "engineVolume": 1497,
      |          "engineType": "GASOLINE",
      |          "gearType": "FRONT",
      |          "doorsCount": 4,
      |          "steeringWheel": "LEFT",
      |          "transmissionType": "MECHANICAL",
      |          "superGen": "7353103",
      |          "colorHex": "34ba2b",
      |          "photos": [],
      |          "moderationPhotos": [],
      |          "priceInfo": {
      |            "price": 650000,
      |            "currency": "RUB",
      |            "maxDiscount": 20000,
      |            "recommendedPrice": 389999,
      |            "discountPrice": 630000,
      |            "ratioOfDiscountPriceToRecommended": 162,
      |            "ratioOfMaxDiscountToPrice": 3,
      |            "ratioOfPriceToRecommended": 167
      |          },
      |          "originalPriceInfo": {
      |            "price": 650000,
      |            "currency": "RUB",
      |            "maxDiscount": 20000,
      |            "recommendedPrice": 389999,
      |            "discountPrice": 630000,
      |            "ratioOfDiscountPriceToRecommended": 162,
      |            "ratioOfMaxDiscountToPrice": 3,
      |            "ratioOfPriceToRecommended": 167
      |          },
      |          "isPlacedForFree": true,
      |          "platform": "PLATFORM_UNKNOWN",
      |          "source": "SOURCE_UNKNOWN",
      |          "isCallCenter": false,
      |          "notRegisteredInRussia": false,
      |          "nameplateFront": "",
      |          "complectationId": "7826547",
      |          "chatOnly": false,
      |          "panoramaStatus": "NO_PANORAMA",
      |          "autoruExpert": false,
      |          "additionalPhotos": [],
      |          "offerWasActive": true,
      |          "resellerStatusAccepted": false,
      |          "colorName": "Зеленый"
      |        }
      |      },
      |      "signals": [
      |        {
      |          "type": "hobo",
      |          "domain": {
      |            "type": "dealers_autoru",
      |            "value": "DEFAULT_DEALERS_AUTORU"
      |          },
      |          "key": "inherited_DEALERS_AUTORU_hobo_LOYALTY_DEALERS",
      |          "source": {
      |            "type": "manual",
      |            "userId": "avlyalin",
      |            "serviceInheritedFrom": "DEALERS_AUTORU"
      |          },
      |          "timestamp": 1645118932826,
      |          "info": "test",
      |          "checkType": "LOYALTY_DEALERS",
      |          "task": {
      |            "queue": "AUTO_RU_LOYALTY_DEALERS",
      |            "key": "37c8c6fa96b48f2fa42d8de63d4489f2"
      |          },
      |          "taskResult": "none",
      |          "reasons": [],
      |          "detailedReasons": [],
      |          "labels": [
      |            ""
      |          ],
      |          "allowResultAfter": 1643025402253,
      |          "auxInfo": [
      |            {
      |              "type": "hobo_task_info",
      |              "hoboTaskInfoForAutoruLoyaltyDealerPeriodId": ""
      |            }
      |          ],
      |          "finishTime": 1645118918742
      |        },
      |        {
      |          "type": "ban",
      |          "domain": {
      |            "type": "dealers_autoru",
      |            "value": "DEFAULT_DEALERS_AUTORU"
      |          },
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_ADDRESS",
      |          "source": {
      |            "type": "manual",
      |            "userId": "s-grigoryev",
      |            "serviceInheritedFrom": "DEALERS_AUTORU"
      |          },
      |          "timestamp": 1595581890004,
      |          "reasons": [
      |            "WRONG_ADDRESS"
      |          ],
      |          "detailedReasons": [
      |            {
      |              "reason": "WRONG_ADDRESS"
      |            }
      |          ],
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "awethon"
      |            },
      |            "timestamp": 1643626319734,
      |            "comment": "test"
      |          },
      |          "labels": [],
      |          "auxInfo": []
      |        },
      |        {
      |          "type": "ban",
      |          "domain": {
      |            "type": "dealers_autoru",
      |            "value": "DEFAULT_DEALERS_AUTORU"
      |          },
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_NOT_VERIFIED",
      |          "source": {
      |            "type": "manual",
      |            "userId": "dcversus",
      |            "serviceInheritedFrom": "DEALERS_AUTORU"
      |          },
      |          "timestamp": 1585839986066,
      |          "reasons": [
      |            "NOT_VERIFIED"
      |          ],
      |          "detailedReasons": [
      |            {
      |              "reason": "NOT_VERIFIED"
      |            }
      |          ],
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "dinoskova"
      |            },
      |            "timestamp": 1643626319375,
      |            "comment": ""
      |          },
      |          "labels": [],
      |          "auxInfo": []
      |        },
      |        {
      |          "type": "hobo",
      |          "domain": {
      |            "type": "dealers_autoru",
      |            "value": "DEFAULT_DEALERS_AUTORU"
      |          },
      |          "key": "inherited_DEALERS_AUTORU_hobo_PREMODERATION_DEALER",
      |          "source": {
      |            "type": "automatic",
      |            "application": "MODERATION",
      |            "serviceInheritedFrom": "DEALERS_AUTORU"
      |          },
      |          "timestamp": 1597667171183,
      |          "checkType": "PREMODERATION_DEALER",
      |          "task": {
      |            "queue": "AUTO_RU_PREMODERATION_DEALER",
      |            "key": "aed7dc9dca8262550e48f7f00d6d31a4"
      |          },
      |          "taskResult": "none",
      |          "reasons": [],
      |          "detailedReasons": [],
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "sernovikov"
      |            },
      |            "timestamp": 1637591821884,
      |            "comment": ""
      |          },
      |          "labels": [],
      |          "allowResultAfter": 1595852464543,
      |          "auxInfo": [],
      |          "finishTime": 1597667156971
      |        },
      |        {
      |          "type": "ban",
      |          "domain": {
      |            "type": "dealers_autoru",
      |            "value": "DEFAULT_DEALERS_AUTORU"
      |          },
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_NO_ANSWER",
      |          "source": {
      |            "type": "manual",
      |            "userId": "s-grigoryev",
      |            "serviceInheritedFrom": "DEALERS_AUTORU"
      |          },
      |          "timestamp": 1595581890004,
      |          "reasons": [
      |            "NO_ANSWER"
      |          ],
      |          "detailedReasons": [
      |            {
      |              "reason": "NO_ANSWER"
      |            }
      |          ],
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "dinoskova"
      |            },
      |            "timestamp": 1643626319135,
      |            "comment": ""
      |          },
      |          "labels": [],
      |          "auxInfo": []
      |        },
      |        {
      |          "type": "ban",
      |          "domain": {
      |            "type": "dealers_autoru",
      |            "value": "DEFAULT_DEALERS_AUTORU"
      |          },
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_PRESENCE",
      |          "source": {
      |            "type": "manual",
      |            "userId": "s-grigoryev",
      |            "serviceInheritedFrom": "DEALERS_AUTORU"
      |          },
      |          "timestamp": 1595581890004,
      |          "reasons": [
      |            "WRONG_PRESENCE"
      |          ],
      |          "detailedReasons": [
      |            {
      |              "reason": "WRONG_PRESENCE"
      |            }
      |          ],
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "dinoskova"
      |            },
      |            "timestamp": 1643626318893,
      |            "comment": ""
      |          },
      |          "labels": [],
      |          "auxInfo": []
      |        },
      |        {
      |          "type": "indexer_error",
      |          "domain": {
      |            "type": "dealers_autoru",
      |            "value": "DEFAULT_DEALERS_AUTORU"
      |          },
      |          "key": "inherited_DEALERS_AUTORU_automatic_INDEXER_index_error",
      |          "source": {
      |            "type": "automatic",
      |            "application": "INDEXER",
      |            "serviceInheritedFrom": "DEALERS_AUTORU"
      |          },
      |          "timestamp": 1611594776116,
      |          "reasons": [
      |            "NOT_VERIFIED"
      |          ],
      |          "detailedReasons": [
      |            {
      |              "reason": "NOT_VERIFIED"
      |            }
      |          ],
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "19565983"
      |            },
      |            "timestamp": 1643303938709,
      |            "comment": "User 19565983 switch off signal for dealer 20101"
      |          },
      |          "labels": [],
      |          "auxInfo": []
      |        },
      |        {
      |          "type": "warn",
      |          "domain": {
      |            "type": "autoru",
      |            "value": "DEFAULT_AUTORU"
      |          },
      |          "key": "automatic_MODERATION_warn_LICENSE_PLATE_MISMATCH",
      |          "source": {
      |            "type": "automatic",
      |            "application": "MODERATION",
      |            "tag": "licensePlateMismatch"
      |          },
      |          "timestamp": 1650283355037,
      |          "weight": 1,
      |          "reasons": [
      |            "LICENSE_PLATE_MISMATCH"
      |          ],
      |          "detailedReasons": [
      |            {
      |              "reason": "LICENSE_PLATE_MISMATCH"
      |            }
      |          ],
      |          "labels": [],
      |          "auxInfo": []
      |        },
      |        {
      |          "type": "ban",
      |          "domain": {
      |            "type": "dealers_autoru",
      |            "value": "DEFAULT_DEALERS_AUTORU"
      |          },
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_AD_PARAMETERS",
      |          "source": {
      |            "type": "manual",
      |            "userId": "s-grigoryev",
      |            "serviceInheritedFrom": "DEALERS_AUTORU"
      |          },
      |          "timestamp": 1595581645024,
      |          "reasons": [
      |            "WRONG_AD_PARAMETERS"
      |          ],
      |          "detailedReasons": [
      |            {
      |              "reason": "WRONG_AD_PARAMETERS"
      |            }
      |          ],
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "mcsim-gr"
      |            },
      |            "timestamp": 1643626319614,
      |            "comment": ""
      |          },
      |          "labels": [],
      |          "auxInfo": []
      |        }
      |      ],
      |      "createTime": 1650283349993,
      |      "essentialsUpdateTime": 1650283349993,
      |      "updateTime": 1650283355037,
      |      "opinion": {
      |        "type": "unknown",
      |        "details": {
      |          "type": "autoru",
      |          "autoru": {
      |            "isBanByInheritance": false,
      |            "isFromReseller": false
      |          }
      |        },
      |        "warnDetailedReasons": [
      |          {
      |            "reason": "LICENSE_PLATE_MISMATCH"
      |          }
      |        ]
      |      },
      |      "opinions": {
      |        "values": [
      |          {
      |            "domain": {
      |              "type": "autoru",
      |              "value": "DEFAULT_AUTORU"
      |            },
      |            "opinion": {
      |              "type": "unknown",
      |              "details": {
      |                "type": "autoru",
      |                "autoru": {
      |                  "isBanByInheritance": false,
      |                  "isFromReseller": false
      |                }
      |              },
      |              "warnDetailedReasons": [
      |                {
      |                  "reason": "LICENSE_PLATE_MISMATCH"
      |                }
      |              ]
      |            }
      |          }
      |        ]
      |      },
      |      "context": {
      |        "visibility": "DELETED",
      |        "updateTime": 1650283349993
      |      },
      |      "metadata": [
      |        {
      |          "timestamp": "1650283248677",
      |          "geobaseIp": {
      |            "ipInfos": [
      |              {
      |                "ip": "0:0:0:0:0:0:0:1",
      |                "isVpn": false,
      |                "isProxy": false,
      |                "isHosting": false,
      |                "isTor": false,
      |                "isYandexTurbo": false,
      |                "isYandexStaff": false,
      |                "isYandexNet": false
      |              }
      |            ]
      |          }
      |        }
      |      ],
      |      "signalMap": [
      |        {
      |          "key": "inherited_DEALERS_AUTORU_hobo_LOYALTY_DEALERS",
      |          "signal": {
      |            "type": "hobo",
      |            "domain": {
      |              "type": "dealers_autoru",
      |              "value": "DEFAULT_DEALERS_AUTORU"
      |            },
      |            "key": "inherited_DEALERS_AUTORU_hobo_LOYALTY_DEALERS",
      |            "source": {
      |              "type": "manual",
      |              "userId": "avlyalin",
      |              "serviceInheritedFrom": "DEALERS_AUTORU"
      |            },
      |            "timestamp": 1645118932826,
      |            "info": "test",
      |            "checkType": "LOYALTY_DEALERS",
      |            "task": {
      |              "queue": "AUTO_RU_LOYALTY_DEALERS",
      |              "key": "37c8c6fa96b48f2fa42d8de63d4489f2"
      |            },
      |            "taskResult": "none",
      |            "reasons": [],
      |            "detailedReasons": [],
      |            "labels": [
      |              ""
      |            ],
      |            "allowResultAfter": 1643025402253,
      |            "auxInfo": [
      |              {
      |                "type": "hobo_task_info",
      |                "hoboTaskInfoForAutoruLoyaltyDealerPeriodId": ""
      |              }
      |            ],
      |            "finishTime": 1645118918742
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_ADDRESS",
      |          "signal": {
      |            "type": "ban",
      |            "domain": {
      |              "type": "dealers_autoru",
      |              "value": "DEFAULT_DEALERS_AUTORU"
      |            },
      |            "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_ADDRESS",
      |            "source": {
      |              "type": "manual",
      |              "userId": "s-grigoryev",
      |              "serviceInheritedFrom": "DEALERS_AUTORU"
      |            },
      |            "timestamp": 1595581890004,
      |            "reasons": [
      |              "WRONG_ADDRESS"
      |            ],
      |            "detailedReasons": [
      |              {
      |                "reason": "WRONG_ADDRESS"
      |              }
      |            ],
      |            "labels": [],
      |            "auxInfo": []
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_NOT_VERIFIED",
      |          "signal": {
      |            "type": "ban",
      |            "domain": {
      |              "type": "dealers_autoru",
      |              "value": "DEFAULT_DEALERS_AUTORU"
      |            },
      |            "key": "inherited_DEALERS_AUTORU_manual_ban_NOT_VERIFIED",
      |            "source": {
      |              "type": "manual",
      |              "userId": "dcversus",
      |              "serviceInheritedFrom": "DEALERS_AUTORU"
      |            },
      |            "timestamp": 1585839986066,
      |            "reasons": [
      |              "NOT_VERIFIED"
      |            ],
      |            "detailedReasons": [
      |              {
      |                "reason": "NOT_VERIFIED"
      |              }
      |            ],
      |            "labels": [],
      |            "auxInfo": []
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_hobo_PREMODERATION_DEALER",
      |          "signal": {
      |            "type": "hobo",
      |            "domain": {
      |              "type": "dealers_autoru",
      |              "value": "DEFAULT_DEALERS_AUTORU"
      |            },
      |            "key": "inherited_DEALERS_AUTORU_hobo_PREMODERATION_DEALER",
      |            "source": {
      |              "type": "automatic",
      |              "application": "MODERATION",
      |              "serviceInheritedFrom": "DEALERS_AUTORU"
      |            },
      |            "timestamp": 1597667171183,
      |            "checkType": "PREMODERATION_DEALER",
      |            "task": {
      |              "queue": "AUTO_RU_PREMODERATION_DEALER",
      |              "key": "aed7dc9dca8262550e48f7f00d6d31a4"
      |            },
      |            "taskResult": "none",
      |            "reasons": [],
      |            "detailedReasons": [],
      |            "labels": [],
      |            "allowResultAfter": 1595852464543,
      |            "auxInfo": [],
      |            "finishTime": 1597667156971
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_NO_ANSWER",
      |          "signal": {
      |            "type": "ban",
      |            "domain": {
      |              "type": "dealers_autoru",
      |              "value": "DEFAULT_DEALERS_AUTORU"
      |            },
      |            "key": "inherited_DEALERS_AUTORU_manual_ban_NO_ANSWER",
      |            "source": {
      |              "type": "manual",
      |              "userId": "s-grigoryev",
      |              "serviceInheritedFrom": "DEALERS_AUTORU"
      |            },
      |            "timestamp": 1595581890004,
      |            "reasons": [
      |              "NO_ANSWER"
      |            ],
      |            "detailedReasons": [
      |              {
      |                "reason": "NO_ANSWER"
      |              }
      |            ],
      |            "labels": [],
      |            "auxInfo": []
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_PRESENCE",
      |          "signal": {
      |            "type": "ban",
      |            "domain": {
      |              "type": "dealers_autoru",
      |              "value": "DEFAULT_DEALERS_AUTORU"
      |            },
      |            "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_PRESENCE",
      |            "source": {
      |              "type": "manual",
      |              "userId": "s-grigoryev",
      |              "serviceInheritedFrom": "DEALERS_AUTORU"
      |            },
      |            "timestamp": 1595581890004,
      |            "reasons": [
      |              "WRONG_PRESENCE"
      |            ],
      |            "detailedReasons": [
      |              {
      |                "reason": "WRONG_PRESENCE"
      |              }
      |            ],
      |            "labels": [],
      |            "auxInfo": []
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_automatic_INDEXER_index_error",
      |          "signal": {
      |            "type": "indexer_error",
      |            "domain": {
      |              "type": "dealers_autoru",
      |              "value": "DEFAULT_DEALERS_AUTORU"
      |            },
      |            "key": "inherited_DEALERS_AUTORU_automatic_INDEXER_index_error",
      |            "source": {
      |              "type": "automatic",
      |              "application": "INDEXER",
      |              "serviceInheritedFrom": "DEALERS_AUTORU"
      |            },
      |            "timestamp": 1611594776116,
      |            "reasons": [
      |              "NOT_VERIFIED"
      |            ],
      |            "detailedReasons": [
      |              {
      |                "reason": "NOT_VERIFIED"
      |              }
      |            ],
      |            "labels": [],
      |            "auxInfo": []
      |          }
      |        },
      |        {
      |          "key": "automatic_MODERATION_warn_LICENSE_PLATE_MISMATCH",
      |          "signal": {
      |            "type": "warn",
      |            "domain": {
      |              "type": "autoru",
      |              "value": "DEFAULT_AUTORU"
      |            },
      |            "key": "automatic_MODERATION_warn_LICENSE_PLATE_MISMATCH",
      |            "source": {
      |              "type": "automatic",
      |              "application": "MODERATION",
      |              "tag": "licensePlateMismatch"
      |            },
      |            "timestamp": 1650283355037,
      |            "weight": 1,
      |            "reasons": [
      |              "LICENSE_PLATE_MISMATCH"
      |            ],
      |            "detailedReasons": [
      |              {
      |                "reason": "LICENSE_PLATE_MISMATCH"
      |              }
      |            ],
      |            "labels": [],
      |            "auxInfo": []
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_AD_PARAMETERS",
      |          "signal": {
      |            "type": "ban",
      |            "domain": {
      |              "type": "dealers_autoru",
      |              "value": "DEFAULT_DEALERS_AUTORU"
      |            },
      |            "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_AD_PARAMETERS",
      |            "source": {
      |              "type": "manual",
      |              "userId": "s-grigoryev",
      |              "serviceInheritedFrom": "DEALERS_AUTORU"
      |            },
      |            "timestamp": 1595581645024,
      |            "reasons": [
      |              "WRONG_AD_PARAMETERS"
      |            ],
      |            "detailedReasons": [
      |              {
      |                "reason": "WRONG_AD_PARAMETERS"
      |              }
      |            ],
      |            "labels": [],
      |            "auxInfo": []
      |          }
      |        }
      |      ],
      |      "switchOffMap": [
      |        {
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_ADDRESS",
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "awethon"
      |            },
      |            "timestamp": 1643626319734,
      |            "comment": "test"
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_NOT_VERIFIED",
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "dinoskova"
      |            },
      |            "timestamp": 1643626319375,
      |            "comment": ""
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_hobo_PREMODERATION_DEALER",
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "sernovikov"
      |            },
      |            "timestamp": 1637591821884,
      |            "comment": ""
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_NO_ANSWER",
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "dinoskova"
      |            },
      |            "timestamp": 1643626319135,
      |            "comment": ""
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_PRESENCE",
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "dinoskova"
      |            },
      |            "timestamp": 1643626318893,
      |            "comment": ""
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_automatic_INDEXER_index_error",
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "19565983"
      |            },
      |            "timestamp": 1643303938709,
      |            "comment": "User 19565983 switch off signal for dealer 20101"
      |          }
      |        },
      |        {
      |          "key": "inherited_DEALERS_AUTORU_manual_ban_WRONG_AD_PARAMETERS",
      |          "switchOff": {
      |            "source": {
      |              "type": "manual",
      |              "userId": "mcsim-gr"
      |            },
      |            "timestamp": 1643626319614,
      |            "comment": ""
      |          }
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

}
