tariffs = [
  {
    "client_access": False,

    "name": "\u0422\u0435\u0441\u0442\u043e\u0432\u044b\u0439",
    "weight": 10.0,
    "cc": "testpogoda_free_trial",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {
          "ban_reason": 118.0,
          "unban_reason": 119.0,
          "days": 30.0,
          "period_mask": ""
        },
        "unit": "TemporaryActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testpogoda_hits_daily",
          "limit": "5000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000018.0,
    "_id": {
      "$oid": "5afaaf0a15073bb902824ea5"
    },
    "description": "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, 5 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0444\u0430\u043a\u0442\u0443 \u0438 \u043f\u0440\u043e\u0433\u043d\u043e\u0437\u0430\u043c \u0432 \u0442\u0435\u0447\u0435\u043d\u0438\u0435 30 \u0434\u043d\u0435\u0439. \u0414\u043b\u044f \u043f\u0440\u043e\u0434\u043b\u0435\u043d\u0438\u044f \u043e\u0442\u043f\u0440\u0430\u0432\u044c\u0442\u0435 \u0437\u0430\u044f\u0432\u043a\u0443 \u043d\u0430 \u043f\u043e\u0447\u0442\u0443 api-weather@support.yandex.ru"
  },
  {
    "client_access": True,

    "name": "\u041f\u043e\u0433\u043e\u0434\u0430 \u043d\u0430 \u0432\u0430\u0448\u0435\u043c \u0441\u0430\u0439\u0442\u0435",
    "weight": 20.0,
    "cc": "testpogoda_free_info",
    "description": "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u043e\u0439 \u0440\u0443\u0447\u043a\u0435 \u0441 \u0441\u043e\u043a\u0440\u0430\u0449\u0435\u043d\u043d\u044b\u043c \u043d\u0430\u0431\u043e\u0440\u043e\u043c \u0434\u0430\u043d\u043d\u044b\u0445.",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testpogoda_hits_daily",
          "limit": "50"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000018.0,
    "_id": {
      "$oid": "5afaaf0a15073bb902824ea6"
    },
    "info_for_table": {
      "month_payment": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 40.0,
        "value": "0"
      },
      "requests_per_day_limit": {
        "_type": "number",
        "_weight": 20.0,
        "value": 50.0
      },
      "request_price": {
        "_type": "string",
        "_weight": 30.0,
        "value": "-"
      },
      "month_payed_requests": {
        "_type": "string",
        "_weight": 10.0,
        "value": "-"
      }
    }
  },
  {
    "client_access": False,

    "name": "\u0414\u043b\u044f \u041c\u0435\u0434\u0438\u0430",
    "cc": "testpogoda_free_media",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testpogoda_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000018.0,
    "_id": {
      "$oid": "5afaaf0a15073bb902824ea7"
    },
    "description": "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0444\u0430\u043a\u0442\u0443 \u0438 \u043f\u0440\u043e\u0433\u043d\u043e\u0437\u0430\u043c."
  },
  {
    "info_for_table": {
      "month_payment": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 40.0,
        "value": "150000"
      },
      "requests_per_day_limit": {
        "_type": "string",
        "_weight": 20.0,
        "value": "-"
      },
      "request_price": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 30.0,
        "value": "0.02"
      },
      "month_payed_requests": {
        "_type": "number",
        "_weight": 10.0,
        "value": "2000000"
      }
    },
    "_id": {
      "$oid": "5b0d1ce215073bb90282500c"
    },
    "name": "\u041e\u0441\u043d\u043e\u0432\u043d\u043e\u0439",
    "weight": 30.0,
    "cc": "testpogoda_main_2018",
    "tarifficator_config": [
      {
        "params": {},
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testpogoda_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "509189",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "2000000",
          "product_value": "0.02",
          "quantum": "1",
          "truncate_period_mask": "0 0 * * * *",
          "subscription_product_id": "509188",
          "period_mask": "0 0 x * * *"
        },
        "unit": "PostpaySubscribePeriodicallyRangeConsumerUnit"
      },
      {
        "params": {
          "ban_reason": 122.0,
          "product_id": "509188",
          "time_zone": "+0300",
          "product_value": "150000",
          "truncate_period_mask": "0 0 * * * *",
          "autocharge_personal_account": True,
          "period_mask": "0 0 x * * *",
          "unban_reason": 123.0
        },
        "unit": "PrepayPeriodicallyUnit"
      }
    ],
    "contractless": False,
    "service_id": 10000018.0,
    "client_access": False,
    "personal_account": {
      "product": "509188",
      "default_replenishment_amount": "150000"
    },
    "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e 2 \u043c\u043b\u043d. \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446, \u0434\u0430\u043b\u0435\u0435 2 \u043a\u043e\u043f. \u0437\u0430 \u0437\u0430\u043f\u0440\u043e\u0441. \u0414\u0435\u0439\u0441\u0442\u0432\u0443\u0435\u0442 \u043c\u0435\u0441\u044f\u0446 \u0441 \u043c\u043e\u043c\u0435\u043d\u0442\u0430 \u043e\u043f\u043b\u0430\u0442\u044b."
  },
  {
    "client_access": True,
    "name": "\u0422\u0435\u0441\u0442\u043e\u0432\u044b\u0439",
    "cc": "testspeechkitcloud_trial",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {
          "ban_reason": 32.0,
          "unban_reason": 35.0,
          "days": 30.0,
          "period_mask": ""
        },
        "unit": "TemporaryActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f04"
    },
    "description": "\u041f\u0440\u043e\u0431\u043d\u044b\u0439 \u043f\u0435\u0440\u0438\u043e\u0434 \u043d\u0430 30 \u0434\u043d\u0435\u0439"
  },
  #   # APIKEYS-890
  # {
  #   "client_access": True,
  #   "name": "\u041d\u0435\u043a\u043e\u043c\u043c\u0435\u0440\u0447\u0435\u0441\u043a\u0438\u0439",
  #   "cc": "testspeechkitcloud_custom",
  #   "next_tariff_immediately": True,
  #   "tarifficator_config": [
  #       {
  #           "params": {
  #               "limit_id": "testspeechkitcloud_voice_unit_daily",
  #               "limit": "custom"
  #           },
  #           "unit": "StaticLimitsUnit"
  #       },
  #       {
  #           "params": {
  #               "limit_id": "testspeechkitcloud_tts_unit_daily",
  #               "limit": "custom"
  #           },
  #           "unit": "StaticLimitsUnit"
  #       },
  #       {
  #           "params": {
  #               "limit_id": "testspeechkitcloud_ner_unit_daily",
  #               "limit": "custom"
  #           },
  #           "unit": "StaticLimitsUnit"
  #       }
  #   ],
  #   "contractless": True,
  #   "service_id": 10000020.0,
  #   "_id": {
  #     "$oid": "5afd7a1215073bb902824f05"
  #   },
  #   "description": ""
  # },
  #   # APIKEYS-890
  # {
  #   "client_access": True,
  #   "name": "\u041d\u0435\u043a\u043e\u043c\u043c\u0435\u0440\u0447\u0435\u0441\u043a\u0438\u0439 \u0431\u0435\u0437\u043b\u0438\u043c\u0438\u0442",
  #   "cc": "testspeechkitcloud_custom_unlimited",
  #   "next_tariff_immediately": True,
  #   "tarifficator_config": [
  #     {
  #       "params": {
  #         "limit_id": "testspeechkitcloud_voice_unit_daily",
  #         "limit": "custom"
  #       },
  #       "unit": "StaticLimitsUnit"
  #     },
  #     {
  #       "params": {
  #         "limit_id": "testspeechkitcloud_tts_unit_daily",
  #         "limit": "custom"
  #       },
  #       "unit": "StaticLimitsUnit"
  #     },
  #     {
  #       "params": {
  #         "limit_id": "testspeechkitcloud_ner_unit_daily",
  #         "limit": "custom"
  #       },
  #       "unit": "StaticLimitsUnit"
  #     }
  #   ],
  #   "contractless": True,
  #   "service_id": 10000020.0,
  #   "_id": {
  #     "$oid": "5afd7a1215073bb902824f06"
  #   },
  #   "description": ""
  # },
  {
    "name": "\u041a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u0438\u0439 (2016)",
    "cc": "testspeechkitcloud_client",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1.0,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "400",
          "quantum": "1",
          "scope": "range0"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "skip_when_range_out": True,
          "range_to": 1000.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "400",
          "quantum": "1000",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.40",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f07"
    },
    "description": ""
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Gold (2016)",
    "cc": "testspeechkitcloud_partner_gold",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1.0,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "200",
          "quantum": "1",
          "scope": "range0"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "skip_when_range_out": True,
          "range_to": 1000.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "200",
          "quantum": "1000",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.20",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f09"
    },
    "description": ""
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Silver (2016)",
    "cc": "testspeechkitcloud_partner_silver",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1.0,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "300",
          "quantum": "1",
          "scope": "range0"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "skip_when_range_out": True,
          "range_to": 1000.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "300",
          "quantum": "1000",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.30",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f08"
    },
    "description": ""
  },
  {
    "name": "\u041a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u0438\u0439 (201702)",
    "cc": "testspeechkitcloud_client_201702",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "200",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.20",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f0a"
    },
    "description": ""
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Silver (201702)",
    "cc": "testspeechkitcloud_partner_silver_201702",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "150",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.15",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f0b"
    },
    "description": ""
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Gold (201702)",
    "cc": "testspeechkitcloud_partner_gold_201702",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "100",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.10",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f0c"
    },
    "description": ""
  },
  {
    "name": "\u041a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u0438\u0439",
    "cc": "testspeechkitcloud_client_201705",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "200",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.20",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f0d"
    },
    "description": ""
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Silver",
    "cc": "testspeechkitcloud_partner_silver_201705",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "150",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.15",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f0e"
    },
    "description": ""
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Gold",
    "cc": "testspeechkitcloud_partner_gold_201705",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "100",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.10",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f0f"
    },
    "description": ""
  },
  {
    "info_for_table": {
      "per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "200"
      }
    },
    "client_access": True,
    "name": "\u0411\u0440\u043e\u043d\u0437\u043e\u0432\u044b\u0439",
    "weight": 10.0,
    "cc": "testspeechkitcloud_bronze_contractless_2018",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "limit_statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": False,
          "skip_when_range_out": True,
          "transit_yesterday_stat": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "0",
          "product_value": "200",
          "quantum": "1000",
          "unban_reason": 111.0,
          "true_prepay": True,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "share_state": {
            "products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat": "str(D(state_get('products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat')) + D(state_get('products__507905__TodayPrepayStatisticRangeConsumerUnit__consumed_stat')))",
            "products__507905__TodayPrepayStatisticRangeConsumerUnit__consumed_stat_dt": "state_get('products__507905__TodayPrepayStatisticRangeConsumerUnit__consumed_stat_dt')"
          },
          "condition": "D(state_get('products__507905__consumed', 0)) >= 500000",
          "next_tariff": "testspeechkitcloud_silver_contractless_2018"
        },
        "unit": "NextTariffSwitchEventUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f10"
    },
    "personal_account": {
      "product": "507905",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "500"
    },
    "description": ""
  },
  {
    "info_for_table": {
      "per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "150"
      }
    },
    "client_access": False,
    "name": "\u0421\u0435\u0440\u0435\u0431\u0440\u044f\u043d\u044b\u0439",
    "weight": 20.0,
    "cc": "testspeechkitcloud_silver_contractless_2018",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "limit_statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": False,
          "skip_when_range_out": True,
          "transit_yesterday_stat": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "0",
          "product_value": "150",
          "quantum": "1000",
          "unban_reason": 111.0,
          "true_prepay": True,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "share_state": {
            "products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat": "str(D(state_get('products__507905__TodayPrepayStatisticRangeConsumerUnit__uncountable_stat')) + D(state_get('products__507905__TodayPrepayStatisticRangeConsumerUnit__consumed_stat')))",
            "products__507905__TodayPrepayStatisticRangeConsumerUnit__consumed_stat_dt": "state_get('products__507905__TodayPrepayStatisticRangeConsumerUnit__consumed_stat_dt')"
          },
          "condition": "D(state_get('products__507905__consumed', 0)) >= 900000",
          "next_tariff": "testspeechkitcloud_gold_contractless_2018"
        },
        "unit": "NextTariffSwitchEventUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f11"
    },
    "personal_account": {
      "product": "507905",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "500"
    },
    "description": ""
  },
  {
    "info_for_table": {
      "per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "100"
      }
    },
    "client_access": False,
    "name": "\u0417\u043e\u043b\u043e\u0442\u043e\u0439",
    "weight": 30.0,
    "cc": "testspeechkitcloud_gold_contractless_2018",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "testspeechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "limit_statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": False,
          "skip_when_range_out": True,
          "transit_yesterday_stat": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "0",
          "product_value": "100",
          "quantum": "1000",
          "unban_reason": 111.0,
          "true_prepay": True,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      },
      None
    ],
    "contractless": True,
    "service_id": 10000020.0,
    "_id": {
      "$oid": "5afd7a1215073bb902824f12"
    },
    "personal_account": {
      "product": "507905",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "500"
    },
    "description": ""
  },
  {
    "name": "\u041a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u0438\u0439",
    "cc": "speechkitjsapi_client_201702",
    "description": "",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "508286",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "200",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "508286",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.20",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 21.0,
    "_id": {
      "$oid": "5afedbe0280be620eb27ebf6"
    },
    "info_for_table": {
      "request_price_over_minpay": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 30.0,
        "value": "0.2"
      },
      "minimal_month_payment": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 10.0,
        "value": "200"
      },
      "month_payed_requests": {
        "_type": "number",
        "_weight": 20.0,
        "value": 1000.0
      }
    }
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Silver",
    "cc": "speechkitjsapi_partner_silver_201702",
    "description": "",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "508286",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "150",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "508286",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.15",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 21.0,
    "_id": {
      "$oid": "5afedbe0280be620eb27ebf7"
    },
    "info_for_table": {
      "request_price_over_minpay": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 30.0,
        "value": "0.15"
      },
      "minimal_month_payment": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 10.0,
        "value": "150"
      },
      "month_payed_requests": {
        "_type": "number",
        "_weight": 20.0,
        "value": 1000.0
      }
    }
  },
  #   # APIKEYS-890
  # {
  #   "client_access": False,
  #   "name": "\u041d\u0435\u043a\u043e\u043c\u043c\u0435\u0440\u0447\u0435\u0441\u043a\u0438\u0439",
  #   "cc": "speechkitjsapi_custom",
  #   "tarifficator_config": [
  #       {
  #           "params": {
  #               "limit_id": "speechkitjsapi_voice_unit_daily",
  #               "limit": "custom"
  #           },
  #           "unit": "StaticLimitsUnit"
  #       },
  #       {
  #           "params": {
  #               "limit_id": "speechkitjsapi_tts_unit_daily",
  #               "limit": "custom"
  #           },
  #           "unit": "StaticLimitsUnit"
  #       },
  #       {
  #           "params": {
  #               "limit_id": "speechkitjsapi_ner_unit_daily",
  #               "limit": "custom"
  #           },
  #           "unit": "StaticLimitsUnit"
  #       }
  #   ],
  #   "contractless": True,
  #   "service_id": 21.0,
  #   "_id": {
  #     "$oid": "5afedbe0280be620eb27ebf4"
  #   },
  #   "description": ""
  # },
  {
    "client_access": True,
    "name": "\u0422\u0435\u0441\u0442\u043e\u0432\u044b\u0439",
    "cc": "speechkitjsapi_trial",
    "tarifficator_config": [
      {
        "params": {
          "ban_reason": 32.0,
          "unban_reason": 35.0,
          "days": 30.0,
          "period_mask": ""
        },
        "unit": "TemporaryActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_voice_unit_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_tts_unit_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_ner_unit_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 21.0,
    "_id": {
      "$oid": "5afedbe0280be620eb27ebf3"
    },
    "description": "\u041f\u0440\u043e\u0431\u043d\u044b\u0439 \u043f\u0435\u0440\u0438\u043e\u0434 \u043d\u0430 30 \u0434\u043d\u0435\u0439"
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Gold",
    "cc": "speechkitjsapi_partner_gold_201702",
    "description": "",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "508286",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "100",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "508286",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.10",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 21.0,
    "_id": {
      "$oid": "5afedbe0280be620eb27ebf8"
    },
    "info_for_table": {
      "request_price_over_minpay": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 30.0,
        "value": "0.1"
      },
      "minimal_month_payment": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 10.0,
        "value": "100"
      },
      "month_payed_requests": {
        "_type": "number",
        "_weight": 20.0,
        "value": 1000.0
      }
    }
  },
  {
    "client_access": False,
    "name": "\u0411\u0435\u0437\u043b\u0438\u043c\u0438\u0442\u043d\u044b\u0439",
    "cc": "speechkitjsapi_custom_unlimited",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {
          "limit_id": "speechkitjsapi_voice_unit_daily",
          "limit": "custom"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_tts_unit_daily",
          "limit": "custom"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitjsapi_ner_unit_daily",
          "limit": "custom"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 21.0,
    "_id": {
      "$oid": "5afedbe0280be620eb27ebf5"
    },
    "description": ""
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "4000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "16944000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.10"
      }
    },
    "name": "\u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 370.0,
    "cc": "routingmatrix_4000000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16944000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "16944000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "4000000",
          "product_value": "4.10",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9491"
    },
    "description": "16 944 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.10 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "client_access": False,
    "name": "\u0422\u0430\u0440\u0438\u0444 \u0432\u043d\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430",
    "cc": "routingmatrix_custom",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "custom"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9484"
    },
    "description": ""
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "100000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1500000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "11"
      }
    },
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 330.0,
    "cc": "routingmatrix_100000_yearprepay_plus_2017",
    "tarifficator_config": [
      {\
        "params": {
          "needle_credited": "1500000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "1500000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9489"
    },
    "description": "1 500 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "client_access": False,
    "name": "\u041f\u0440\u043e\u0431\u043d\u044b\u0439",
    "cc": "routingmatrix_trial",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {
          "ban_reason": 124.0,
          "unban_reason": 125.0,
          "days": 14.0,
          "period_mask": ""
        },
        "unit": "TemporaryActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "10000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9483"
    },
    "description": "\u0431\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, 14 \u0434\u043d\u0435\u0439, 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432, 100000 \u044f\u0447\u0435\u0435\u043a, 1000 \u0437\u0430\u043a\u0430\u0437\u043e\u0432"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "50000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1350000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "17"
      }
    },
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 325.0,
    "cc": "routingmatrix_50000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1350000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "1350000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9488"
    },
    "description": "1 350 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "300000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "2292000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "8"
      }
    },
    "name": "\u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 340.0,
    "cc": "routingmatrix_300000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2292000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "2292000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "300000",
          "product_value": "8",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb948b"
    },
    "description": "2 292 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 8 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "7044000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.40"
      }
    },
    "name": "\u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 355.0,
    "cc": "routingmatrix_1500000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "7044000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "7044000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1500000",
          "product_value": "4.40",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb948e"
    },
    "description": "7 044 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.40 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "2000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "9024000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.30"
      }
    },
    "name": "\u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 360.0,
    "cc": "routingmatrix_2000000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "9024000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "9024000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "2000000",
          "product_value": "4.30",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb948f"
    },
    "description": "9 024 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.30 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "25000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1100000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "24"
      }
    },
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 320.0,
    "cc": "routingmatrix_25000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1100000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "1100000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9487"
    },
    "description": "1 100 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "10000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "860000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "36"
      }
    },
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 315.0,
    "cc": "routingmatrix_10000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "860000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "860000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9486"
    },
    "description": "860 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "200000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1896000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "9.50"
      }
    },
    "name": "\u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 335.0,
    "cc": "routingmatrix_200000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1896000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "1896000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "200000",
          "product_value": "9.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb948a"
    },
    "description": "1 896 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 9.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "client_access": False,
    "name": "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u044b\u0439",
    "cc": "routingmatrix_free",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "10000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9482"
    },
    "description": "1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432, 100000 \u044f\u0447\u0435\u0435\u043a, 1000 \u0437\u0430\u043a\u0430\u0437\u043e\u0432"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "5064000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.50"
      }
    },
    "name": "\u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 350.0,
    "cc": "routingmatrix_1000000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "5064000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "5064000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000000",
          "product_value": "4.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb948d"
    },
    "description": "5 064 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "3084000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "5.20"
      }
    },
    "name": "\u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 345.0,
    "cc": "routingmatrix_500000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "3084000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "3084000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "500000",
          "product_value": "5.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb948c"
    },
    "description": "3 084 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 5.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "620000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "120"
      }
    },
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 310.0,
    "cc": "routingmatrix_1000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "620000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "620000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9485"
    },
    "description": "620 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "3000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "12984000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.20"
      }
    },
    "name": "\u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 365.0,
    "cc": "routingmatrix_3000000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12984000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "12984000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "3000000",
          "product_value": "4.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9490"
    },
    "description": "12 984 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "120000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "120"
      }
    },
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 210.0,
    "cc": "routingmatrix_1000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "120000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "120000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9493"
    },
    "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "100000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1000000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "11"
      }
    },
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 230.0,
    "cc": "routingmatrix_100000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1000000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "1000000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9497"
    },
    "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "4564000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.50"
      }
    },
    "name": "\u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 250.0,
    "cc": "routingmatrix_1000000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "4564000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "4564000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000000",
          "product_value": "4.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb949b"
    },
    "description": "4 564 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "_group": "common",
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "120000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "120"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 110.0,
    "cc": "staticmaps_1000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508902",
          "time_zone": "+0300",
          "product_value": "120000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "hits",
          "product_id": "508901",
          "statistic_aggregator": "hits",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadeed"
    },
    "personal_account": {
      "product": "508902",
      "firm_id": 1.0,
      "default_paysys": 1001.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "120000"
    },
    "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "3000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "12484000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.20"
      }
    },
    "name": "\u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 265.0,
    "cc": "routingmatrix_3000000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12484000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "12484000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "3000000",
          "product_value": "4.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb949e"
    },
    "description": "12 484 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "200000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1396000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "9.50"
      }
    },
    "name": "\u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 235.0,
    "cc": "routingmatrix_200000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1396000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "1396000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "200000",
          "product_value": "9.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9498"
    },
    "description": "1 396 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 9.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "10000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "360000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "36"
      }
    },
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 215.0,
    "cc": "routingmatrix_10000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "360000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "360000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9494"
    },
    "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "2584000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "5.20"
      }
    },
    "name": "\u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 245.0,
    "cc": "staticmaps_500k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2584000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "2584000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "500000",
          "product_value": "5.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadef9"
    },
    "description": "2 584 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 5.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "2000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "8524000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.30"
      }
    },
    "name": "\u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 260.0,
    "cc": "routingmatrix_2000000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "8524000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "8524000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "2000000",
          "product_value": "4.30",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb949d"
    },
    "description": "8 524 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.30 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "4000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "16444000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.10"
      }
    },
    "name": "\u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 270.0,
    "cc": "staticmaps_4b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16444000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "16444000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "4000000",
          "product_value": "4.10",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadefe"
    },
    "description": "16 444 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.10 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 215.0,
    "cc": "staticmaps_10k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "360000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "360000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadef3"
    },
    "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "4000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "16444000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.10"
      }
    },
    "name": "\u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 270.0,
    "cc": "routingmatrix_4000000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16444000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "16444000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "4000000",
          "product_value": "4.10",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb949f"
    },
    "description": "16 444 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.10 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "5000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "20404000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4"
      }
    },
    "name": "\u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 275.0,
    "cc": "staticmaps_5b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20404000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "20404000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "5000000",
          "product_value": "4",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadeff"
    },
    "description": "20 404 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "5000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "20404000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4"
      }
    },
    "name": "\u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 275.0,
    "cc": "routingmatrix_5000000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20404000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "20404000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "5000000",
          "product_value": "4",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb94a0"
    },
    "description": "20 404 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 220.0,
    "cc": "staticmaps_25k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "600000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "600000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadef4"
    },
    "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "_group": "common",
      "payed_daily_limit": {
        "_type": "number",
        "value": "25000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "600000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "24"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 130.0,
    "cc": "staticmaps_25000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508902",
          "time_zone": "+0300",
          "product_value": "600000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "hits",
          "product_id": "508901",
          "statistic_aggregator": "hits",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadeef"
    },
    "personal_account": {
      "product": "508902",
      "firm_id": 1.0,
      "default_paysys": 1001.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "600000"
    },
    "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "_group": "common",
      "payed_daily_limit": {
        "_type": "number",
        "value": "100000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1000000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "11"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 150.0,
    "cc": "staticmaps_100000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508902",
          "time_zone": "+0300",
          "product_value": "1000000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "hits",
          "product_id": "508901",
          "statistic_aggregator": "hits",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadef1"
    },
    "personal_account": {
      "product": "508902",
      "firm_id": 1.0,
      "default_paysys": 1001.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "1000000"
    },
    "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "6544000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.40"
      }
    },
    "name": "\u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 255.0,
    "cc": "staticmaps_1.5b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "6544000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "6544000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1500000",
          "product_value": "4.40",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadefb"
    },
    "description": "6 544 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.40 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "3000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "12484000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.20"
      }
    },
    "name": "\u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 265.0,
    "cc": "staticmaps_3b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12484000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "12484000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "3000000",
          "product_value": "4.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadefd"
    },
    "description": "12 484 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "300000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1792000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "8"
      }
    },
    "name": "\u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 240.0,
    "cc": "routingmatrix_300000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1792000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "1792000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "300000",
          "product_value": "8",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9499"
    },
    "description": "1 792 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 8 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 210.0,
    "cc": "staticmaps_1000_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "120000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "120000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadef2"
    },
    "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "6544000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.40"
      }
    },
    "name": "\u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 255.0,
    "cc": "routingmatrix_1500000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "6544000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "6544000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1500000",
          "product_value": "4.40",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb949c"
    },
    "description": "6 544 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.40 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 225.0,
    "cc": "staticmaps_50k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "850000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "850000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadef5"
    },
    "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "50000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "850000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "17"
      }
    },
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 225.0,
    "cc": "routingmatrix_50000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "850000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "850000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9496"
    },
    "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "5000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "20904000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4"
      }
    },
    "name": "\u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 (\u0441 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435\u043c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432)",
    "weight": 375.0,
    "cc": "routingmatrix_5000000_yearprepay_plus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20904000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "20904000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "5000000",
          "product_value": "4",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9492"
    },
    "description": "20 904 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "25000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "600000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "24"
      }
    },
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 220.0,
    "cc": "routingmatrix_25000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "600000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "600000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb9495"
    },
    "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "_group": "common",
      "payed_daily_limit": {
        "_type": "number",
        "value": "50000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "850000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "17"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 140.0,
    "cc": "staticmaps_50000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508902",
          "time_zone": "+0300",
          "product_value": "850000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "hits",
          "product_id": "508901",
          "statistic_aggregator": "hits",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadef0"
    },
    "personal_account": {
      "product": "508902",
      "firm_id": 1.0,
      "default_paysys": 1001.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "850000"
    },
    "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "2584000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "5.20"
      }
    },
    "name": "\u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 245.0,
    "cc": "routingmatrix_500000_yearprepay_minus_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2584000",
          "product_id": "508685"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "routingmatrix_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508685",
          "time_zone": "+0300",
          "product_value": "2584000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508686",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "500000",
          "product_value": "5.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508685"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 31.0,
    "_id": {
      "$oid": "5afedbe1eb20f2657adb949a"
    },
    "description": "2 584 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 5.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 230.0,
    "cc": "staticmaps_100k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1000000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "1000000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadef6"
    },
    "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "300000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1792000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "8"
      }
    },
    "name": "\u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 240.0,
    "cc": "staticmaps_300k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1792000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "1792000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "300000",
          "product_value": "8",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadef8"
    },
    "description": "1 792 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 8 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "_group": "common",
      "payed_daily_limit": {
        "_type": "number",
        "value": "10000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "360000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "36"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 120.0,
    "cc": "staticmaps_10000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508902",
          "time_zone": "+0300",
          "product_value": "360000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "hits",
          "product_id": "508901",
          "statistic_aggregator": "hits",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadeee"
    },
    "personal_account": {
      "product": "508902",
      "firm_id": 1.0,
      "default_paysys": 1001.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "360000"
    },
    "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "client_access": False,
    "name": "\u041f\u0440\u043e\u0431\u043d\u044b\u0439",
    "cc": "staticmaps_trial",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {
          "ban_reason": 124.0,
          "unban_reason": 125.0,
          "days": 14.0,
          "period_mask": ""
        },
        "unit": "TemporaryActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadeec"
    },
    "description": "\u0431\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, 14 \u0434\u043d\u0435\u0439, 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0434\u0435\u043d\u044c"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "4564000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.50"
      }
    },
    "name": "\u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 250.0,
    "cc": "staticmaps_1b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "4564000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "4564000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000000",
          "product_value": "4.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadefa"
    },
    "description": "4 564 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "200000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1396000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "9.50"
      }
    },
    "name": "\u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 235.0,
    "cc": "staticmaps_200k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1396000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "1396000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "200000",
          "product_value": "9.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadef7"
    },
    "description": "1 396 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 9.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "2000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "8524000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.30"
      }
    },
    "name": "\u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 260.0,
    "cc": "staticmaps_2b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "8524000",
          "product_id": "508208"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "staticmaps_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508208",
          "time_zone": "+0300",
          "product_value": "8524000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508207",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "2000000",
          "product_value": "4.30",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508208"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 16.0,
    "_id": {
      "$oid": "5afedbe1de92aa3840aadefc"
    },
    "description": "8 524 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.30 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "client_access": True,
    "name": "\u0411\u0435\u0437\u043b\u0438\u043c\u0438\u0442\u043d\u044b\u0439",
    "cc": "speechkitcloud_custom_unlimited",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "custom"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "custom"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "custom"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9bed5"
    },
    "description": ""
  },
  {
    "name": "\u041a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u0438\u0439 (2016)",
    "cc": "speechkitcloud_client",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1.0,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "400",
          "quantum": "1",
          "scope": "range0"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "skip_when_range_out": True,
          "range_to": 1000.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "400",
          "quantum": "1000",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.40",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9bed6"
    },
    "description": ""
  },
  {
    "client_access": True,
    "name": "\u0422\u0435\u0441\u0442\u043e\u0432\u044b\u0439",
    "cc": "speechkitcloud_trial",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {
          "ban_reason": 32.0,
          "unban_reason": 35.0,
          "days": 30.0,
          "period_mask": ""
        },
        "unit": "TemporaryActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9bed3"
    },
    "description": "\u041f\u0440\u043e\u0431\u043d\u044b\u0439 \u043f\u0435\u0440\u0438\u043e\u0434 \u043d\u0430 30 \u0434\u043d\u0435\u0439"
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Silver (2016)",
    "cc": "speechkitcloud_partner_silver",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1.0,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "300",
          "quantum": "1",
          "scope": "range0"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "skip_when_range_out": True,
          "range_to": 1000.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "300",
          "quantum": "1000",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.3",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9bed7"
    },
    "description": ""
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Silver (201702)",
    "cc": "speechkitcloud_partner_silver_201702",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "150",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.15",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9beda"
    },
    "description": ""
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Gold (2016)",
    "cc": "speechkitcloud_partner_gold",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1.0,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "200",
          "quantum": "1",
          "scope": "range0"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "skip_when_range_out": True,
          "range_to": 1000.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "200",
          "quantum": "1000",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.2",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9bed8"
    },
    "description": ""
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Silver",
    "cc": "speechkitcloud_partner_silver_201705",
    "description": "",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "150",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.15",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9bedd"
    },
    "info_for_table": {
      "request_price_over_minpay": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 30.0,
        "value": "0.15"
      },
      "minimal_month_payment": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 10.0,
        "value": "150"
      },
      "month_payed_requests": {
        "_type": "number",
        "_weight": 20.0,
        "value": 1000.0
      }
    }
  },
  {
    "name": "\u041a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u0438\u0439",
    "cc": "speechkitcloud_client_201705",
    "description": "",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "200",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.20",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9bedc"
    },
    "info_for_table": {
      "request_price_over_minpay": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 30.0,
        "value": "0.2"
      },
      "minimal_month_payment": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 10.0,
        "value": "200"
      },
      "month_payed_requests": {
        "_type": "number",
        "_weight": 20.0,
        "value": 1000.0
      }
    }
  },
  {
    "name": "\u041a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u0438\u0439 (201702)",
    "cc": "speechkitcloud_client_201702",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "200",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.20",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9bed9"
    },
    "description": ""
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Gold (201702)",
    "cc": "speechkitcloud_partner_gold_201702",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "100",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.10",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9bedb"
    },
    "description": ""
  },
  {
    "name": "\u041f\u0430\u0440\u0442\u043d\u0435\u0440\u0441\u043a\u0438\u0439 Gold",
    "cc": "speechkitcloud_partner_gold_201705",
    "description": "",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit + 1",
          "skip_when_range_out": True,
          "range_to": 1001.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "100",
          "quantum": "1001",
          "scope": "range1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "voice_unit + tts_unit + ner_unit",
          "include_range_from": True,
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 1001.0,
          "product_value": "0.10",
          "quantum": "1",
          "scope": "range2"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9bede"
    },
    "info_for_table": {
      "request_price_over_minpay": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 30.0,
        "value": "0.1"
      },
      "minimal_month_payment": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 10.0,
        "value": "100"
      },
      "month_payed_requests": {
        "_type": "number",
        "_weight": 20.0,
        "value": 1000.0
      }
    }
  },
  {
    "name": "5000-\u0444\u0438\u043a\u0441",
    "cc": "speechkitcloud_5000_unlimited_2018",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_voice_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_tts_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "speechkitcloud_ner_unit_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507905",
          "statistic_aggregator": "1",
          "range_to": 1.0,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": 0.0,
          "product_value": "5000",
          "quantum": "1"
        },
        "unit": "MonthlyStatisticRangePerDayConsumerUnit"
      }
    ],
    "service_id": 20.0,
    "_id": {
      "$oid": "5afedbe277f062fbdec9bedf"
    },
    "description": ""
  },
  {
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_10k_yearprepay_2017",
    "description": "860 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "860000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "860000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c567011d"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 10000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "860000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "36"
      }
    }
  },
  {
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_1000_yearprepay_2017",
    "description": "620 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "620000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "620000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c567011c"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 1000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "620000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "120"
      }
    }
  },
  {
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_50k_yearprepay_2017",
    "description": "1 350 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1350000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "1350000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c567011f"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 50000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1350000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "17"
      }
    }
  },
  {
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_100k_yearprepay_2017",
    "description": "1 500 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1500000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "1500000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c5670120"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 100000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1500000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "11"
      }
    }
  },
  {
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_25k_yearprepay_2017",
    "description": "1 100 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u044f \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1100000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "1100000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c567011e"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 25000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1100000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "24"
      }
    }
  },
  {
    "name": "\u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_200k_yearprepay_2017",
    "description": "1 896 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 9,5 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1896000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "1896000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "200000",
          "product_value": "9.5",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c5670121"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 200000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1896000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "9.5"
      }
    }
  },
  {
    "name": "\u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_300k_yearprepay_2017",
    "description": "2 292 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 8 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2292000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "2292000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "300000",
          "product_value": "8",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c5670122"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 300000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "3000000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "8"
      }
    }
  },
  {
    "name": "\u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_500k_yearprepay_2017",
    "description": "3 084 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 5,20 \u0440\u0443\u0431\u043b\u044f \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "3084000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "3084000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "500000",
          "product_value": "5.2",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c5670123"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 500000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "3084000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "5.2"
      }
    }
  },
  {
    "name": "\u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_5b_yearprepay_2017",
    "description": "20 904 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4 \u0440\u0443\u0431\u043b\u044f \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20904000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "20904000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "5000000",
          "product_value": "4",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c5670129"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 5000000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "20904000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4"
      }
    }
  },
  {
    "name": "\u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_4b_yearprepay_2017",
    "description": "16 944 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4,10 \u0440\u0443\u0431\u043b\u044f \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16944000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "16944000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "4000000",
          "product_value": "4.1",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c5670128"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 4000000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "16944000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.1"
      }
    }
  },
  {
    "name": "\u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_1b_yearprepay_2017",
    "description": "5 064 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4,50 \u0440\u0443\u0431\u043b\u044f \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "5064000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "5064000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000000",
          "product_value": "4.5",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c5670124"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 1000000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "5064000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.5"
      }
    }
  },
  {
    "name": "\u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_3b_yearprepay_2017",
    "description": "12 984 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4,20 \u0440\u0443\u0431\u043b\u044f \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12984000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "12984000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "3000000",
          "product_value": "4.2",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c5670127"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 3000000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "12984000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.2"
      }
    }
  },
  {
    "name": "\u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_2b_yearprepay_2017",
    "description": "9 024 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4,30 \u0440\u0443\u0431\u043b\u044f \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "9024000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "9024000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "2000000",
          "product_value": "4.3",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c5670126"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 2000000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "9024000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.3"
      }
    }
  },
  {
    "name": "\u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimapsplus_1.5b_yearprepay_2017",
    "description": "7 044 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4,40 \u0440\u0443\u0431\u043b\u044f \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "7044000",
          "product_id": "508494"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508494",
          "time_zone": "+0300",
          "product_value": "7044000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508493",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1500000",
          "product_value": "4.4",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508494"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 28.0,
    "_id": {
      "$oid": "5afedbe2c7222da1c5670125"
    },
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": 1500000.0
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "7044000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.4"
      }
    }
  },
  {
    "info_for_table": {
      "_group": "common",
      "payed_daily_limit": {
        "_type": "number",
        "value": "25000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "600000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "24"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 130.0,
    "cc": "city_25000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508905",
          "time_zone": "+0300",
          "product_value": "600000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "hits",
          "product_id": "508904",
          "statistic_aggregator": "hits",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f4235d"
    },
    "personal_account": {
      "product": "508905",
      "firm_id": 1.0,
      "default_paysys": 1001.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "600000"
    },
    "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "client_access": False,
    "name": "\u041f\u0440\u043e\u0431\u043d\u044b\u0439 \u043d\u0430 14 \u0434\u043d\u0435\u0439",
    "cc": "city_trial",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {
          "ban_reason": 124.0,
          "unban_reason": 125.0,
          "days": 14.0,
          "period_mask": ""
        },
        "unit": "TemporaryActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "500"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f4235a"
    },
    "description": "\u0431\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, 14 \u0434\u043d\u0435\u0439, 500 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0434\u0435\u043d\u044c"
  },
  {
    "info_for_table": {
      "_group": "common",
      "payed_daily_limit": {
        "_type": "number",
        "value": "10000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "360000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "36"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 120.0,
    "cc": "city_10000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508905",
          "time_zone": "+0300",
          "product_value": "360000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "hits",
          "product_id": "508904",
          "statistic_aggregator": "hits",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f4235c"
    },
    "personal_account": {
      "product": "508905",
      "firm_id": 1.0,
      "default_paysys": 1001.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "360000"
    },
    "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "client_access": False,
    "name": "\u041f\u0440\u043e\u0431\u043d\u044b\u0439",
    "cc": "city_free_2017",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "500"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f42359"
    },
    "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e 500 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0440\u043e\u043a \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f \u0442\u0430\u0440\u0438\u0444\u0430 - 14 \u0434\u043d\u0435\u0439 \u0441 \u0434\u0430\u0442\u044b \u0430\u043a\u0442\u0438\u0432\u0430\u0446\u0438\u0438."
  },
  {
    "info_for_table": {
      "_group": "common",
      "payed_daily_limit": {
        "_type": "number",
        "value": "50000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "850000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "17"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 140.0,
    "cc": "city_50000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508905",
          "time_zone": "+0300",
          "product_value": "850000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "hits",
          "product_id": "508904",
          "statistic_aggregator": "hits",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f4235e"
    },
    "personal_account": {
      "product": "508905",
      "firm_id": 1.0,
      "default_paysys": 1001.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "850000"
    },
    "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "_group": "common",
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "120000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "120"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 110.0,
    "cc": "city_1000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508905",
          "time_zone": "+0300",
          "product_value": "120000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "hits",
          "product_id": "508904",
          "statistic_aggregator": "hits",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f4235b"
    },
    "personal_account": {
      "product": "508905",
      "firm_id": 1.0,
      "default_paysys": 1001.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "120000"
    },
    "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 215.0,
    "cc": "city_10k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "360000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "360000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f42361"
    },
    "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 220.0,
    "cc": "city_25k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "600000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "600000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f42362"
    },
    "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "_group": "common",
      "payed_daily_limit": {
        "_type": "number",
        "value": "100000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1000000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "11"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 150.0,
    "cc": "city_100000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508905",
          "time_zone": "+0300",
          "product_value": "1000000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "hits",
          "product_id": "508904",
          "statistic_aggregator": "hits",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f4235f"
    },
    "personal_account": {
      "product": "508905",
      "firm_id": 1.0,
      "default_paysys": 1001.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "1000000"
    },
    "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 225.0,
    "cc": "city_50k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "850000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "850000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f42363"
    },
    "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "200000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1396000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "9.50"
      }
    },
    "name": "\u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 235.0,
    "cc": "city_200k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1396000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "1396000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "200000",
          "product_value": "9.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f42365"
    },
    "description": "1 396 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 9.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 230.0,
    "cc": "city_100k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1000000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "1000000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f42364"
    },
    "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 210.0,
    "cc": "city_1000_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "120000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "120000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f42360"
    },
    "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "300000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1792000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "8"
      }
    },
    "name": "\u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 240.0,
    "cc": "city_300k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1792000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "1792000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "300000",
          "product_value": "8",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f42366"
    },
    "description": "1 792 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 8 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "2584000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "5.20"
      }
    },
    "name": "\u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 245.0,
    "cc": "city_500k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2584000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "2584000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "500000",
          "product_value": "5.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f42367"
    },
    "description": "2 584 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 5.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "6544000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.40"
      }
    },
    "name": "\u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 255.0,
    "cc": "city_1.5b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "6544000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "6544000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1500000",
          "product_value": "4.40",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f42369"
    },
    "description": "6 544 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.40 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "3000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "12484000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.20"
      }
    },
    "name": "\u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 265.0,
    "cc": "city_3b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12484000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "12484000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "3000000",
          "product_value": "4.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f4236b"
    },
    "description": "12 484 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "4000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "16444000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.10"
      }
    },
    "name": "\u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 270.0,
    "cc": "city_4b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16444000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "16444000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "4000000",
          "product_value": "4.10",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f4236c"
    },
    "description": "16 444 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.10 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "4564000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.50"
      }
    },
    "name": "\u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 250.0,
    "cc": "city_1b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "4564000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "4564000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000000",
          "product_value": "4.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f42368"
    },
    "description": "4 564 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "2000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "8524000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.30"
      }
    },
    "name": "\u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 260.0,
    "cc": "city_2b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "8524000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "8524000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "2000000",
          "product_value": "4.30",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f4236a"
    },
    "description": "8 524 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.30 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "5000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "20404000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4"
      }
    },
    "name": "\u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 275.0,
    "cc": "city_5b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20404000",
          "product_id": "508210"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "city_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 47.0,
          "product_id": "508210",
          "time_zone": "+0300",
          "product_value": "20404000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 50.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508209",
          "statistic_aggregator": "hits",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "5000000",
          "product_value": "4",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508210"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 12.0,
    "_id": {
      "$oid": "5afedbe24ec4c3fc96f4236d"
    },
    "description": "20 404 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "questionnaire_id": "3412",
    "client_access": True,
    "name": "\u0411\u0430\u0437\u043e\u0432\u044b\u0439 \u0434\u043e\u0441\u0442\u0443\u043f",
    "weight": 10.0,
    "cc": "market_api_client_base",
    "description": "",
    "tarifficator_config": [
      {
        "params": {
          "limit_id": "market_light_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "market_heavy_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "market_special_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 15.0,
    "_id": {
      "$oid": "5afedbe2d216b43a15eee34c"
    },
    "personal_account": {
      "product": "508202",
      "firm_id": 111.0,
      "default_paysys": 11101001.0
    },
    "period_close": {
      "mask": "* * * * * *",
      "time_zone": "+0300"
    },
    "info_for_table": {
      "days31_payment": {
        "currency": "RUR",
        "_type": "money",
        "value": "0"
      },
      "hits_limit": {
        "_type": "number",
        "value": 100.0
      }
    }
  },
  {
    "questionnaire_id": "3412",
    "client_access": True,
    "name": "\u0412\u0435\u043d\u0434\u043e\u0440 \u043c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439",
    "weight": 20.0,
    "cc": "market_vendor_mini",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "PersonalAccountActivatorUnit"
      },
      {
        "params": {
          "limit_id": "market_light_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "market_heavy_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "market_special_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 104.0,
          "product_id": "508202",
          "several_days": 31.0,
          "time_zone": "+0300",
          "product_value": "20000",
          "unban_reason": 106.0,
          "autocharge_personal_account": True
        },
        "unit": "DailyPrepaySeveralDaysUnit"
      }
    ],
    "contractless": True,
    "service_id": 15.0,
    "_id": {
      "$oid": "5afedbe2d216b43a15eee350"
    },
    "personal_account": {
      "product": "508202",
      "firm_id": 111.0,
      "default_paysys": 11101001.0,
      "default_paysys_by_person_type": {
        "ph": 11101001.0,
        "ur": 11101003.0
      },
      "default_replenishment_amount": "20000"
    },
    "description": ""
  },
  {
    "questionnaire_id": "3412",
    "client_access": True,
    "name": "\u041c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439",
    "weight": 30.0,
    "cc": "market_api_client_maxi",
    "description": "",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "PersonalAccountActivatorUnit"
      },
      {
        "params": {
          "limit_id": "market_light_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "market_heavy_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "market_special_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 104.0,
          "product_id": "508202",
          "several_days": 31.0,
          "time_zone": "+0300",
          "product_value": "150000",
          "unban_reason": 106.0,
          "autocharge_personal_account": True
        },
        "unit": "DailyPrepaySeveralDaysUnit"
      }
    ],
    "contractless": True,
    "service_id": 15.0,
    "_id": {
      "$oid": "5afedbe2d216b43a15eee34d"
    },
    "personal_account": {
      "product": "508202",
      "firm_id": 111.0,
      "default_paysys": 11101001.0,
      "default_paysys_by_person_type": {
        "ph": 11101001.0,
        "ur": 11101003.0
      },
      "default_replenishment_amount": "150000"
    },
    "info_for_table": {
      "days31_payment": {
        "currency": "RUR",
        "_type": "money",
        "value": "150000"
      },
      "hits_limit": {
        "_type": "number",
        "value": 150000.0
      }
    }
  },
  {
    "questionnaire_id": "3412",
    "client_access": True,
    "name": "\u0412\u0435\u043d\u0434\u043e\u0440 \u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439",
    "weight": 30.0,
    "cc": "market_vendor_maxi",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "PersonalAccountActivatorUnit"
      },
      {
        "params": {
          "limit_id": "market_light_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "market_heavy_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "market_special_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 104.0,
          "product_id": "508202",
          "several_days": 31.0,
          "time_zone": "+0300",
          "product_value": "150000",
          "unban_reason": 106.0,
          "autocharge_personal_account": True
        },
        "unit": "DailyPrepaySeveralDaysUnit"
      }
    ],
    "contractless": True,
    "service_id": 15.0,
    "_id": {
      "$oid": "5afedbe2d216b43a15eee34f"
    },
    "personal_account": {
      "product": "508202",
      "firm_id": 111.0,
      "default_paysys": 11101001.0,
      "default_paysys_by_person_type": {
        "ph": 11101001.0,
        "ur": 11101003.0
      },
      "default_replenishment_amount": "150000"
    },
    "description": ""
  },
  {
    "questionnaire_id": "3412",
    "client_access": True,
    "name": "\u041c\u0438\u043d\u0438\u043c\u0430\u043b\u044c\u043d\u044b\u0439",
    "weight": 20.0,
    "cc": "market_api_client_mini",
    "description": "",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "PersonalAccountActivatorUnit"
      },
      {
        "params": {
          "limit_id": "market_light_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "market_heavy_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "limit_id": "market_special_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 104.0,
          "product_id": "508202",
          "several_days": 31.0,
          "time_zone": "+0300",
          "product_value": "20000",
          "unban_reason": 106.0,
          "autocharge_personal_account": True
        },
        "unit": "DailyPrepaySeveralDaysUnit"
      }
    ],
    "contractless": True,
    "service_id": 15.0,
    "_id": {
      "$oid": "5afedbe2d216b43a15eee34e"
    },
    "personal_account": {
      "product": "508202",
      "firm_id": 111.0,
      "default_paysys": 11101001.0,
      "default_paysys_by_person_type": {
        "ph": 11101001.0,
        "ur": 11101003.0
      },
      "default_replenishment_amount": "20000"
    },
    "info_for_table": {
      "days31_payment": {
        "currency": "RUR",
        "_type": "money",
        "value": "20000"
      },
      "hits_limit": {
        "_type": "number",
        "value": 11000.0
      }
    }
  },
  {
    "client_access": True,
    "name": "\u041f\u043e\u0433\u043e\u0434\u0430 \u043d\u0430 \u0432\u0430\u0448\u0435\u043c \u0441\u0430\u0439\u0442\u0435",
    "weight": 20.0,
    "cc": "pogoda_free_info",
    "description": "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u043e\u0439 \u0440\u0443\u0447\u043a\u0435 \u0441 \u0441\u043e\u043a\u0440\u0430\u0449\u0435\u043d\u043d\u044b\u043c \u043d\u0430\u0431\u043e\u0440\u043e\u043c \u0434\u0430\u043d\u043d\u044b\u0445.",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "pogoda_hits_daily",
          "limit": "50"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 18.0,
    "_id": {
      "$oid": "5afedbe2fd5c3e1e0a18f372"
    },
    "info_for_table": {
      "month_payment": {
        "currency": "RUR",
        "_type": "money",
        "_weight": 40.0,
        "value": "0"
      },
      "requests_per_day_limit": {
        "_type": "number",
        "_weight": 20.0,
        "value": 50.0
      },
      "request_price": {
        "_type": "string",
        "_weight": 30.0,
        "value": "-"
      },
      "month_payed_requests": {
        "_type": "string",
        "_weight": 10.0,
        "value": "-"
      }
    }
  },
  {
    "client_access": False,
    "name": "\u0414\u043b\u044f \u041c\u0435\u0434\u0438\u0430",
    "cc": "pogoda_free_media",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "pogoda_hits_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 18.0,
    "_id": {
      "$oid": "5afedbe2fd5c3e1e0a18f373"
    },
    "description": "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0444\u0430\u043a\u0442\u0443 \u0438 \u043f\u0440\u043e\u0433\u043d\u043e\u0437\u0430\u043c."
  },
  {
    "client_access": False,
    "name": "\u0422\u0435\u0441\u0442\u043e\u0432\u044b\u0439",
    "weight": 10.0,
    "cc": "pogoda_free_trial",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {
          "ban_reason": 118.0,
          "unban_reason": 119.0,
          "days": 30.0,
          "period_mask": ""
        },
        "unit": "TemporaryActivatorUnit"
      },
      {
        "params": {
          "limit_id": "pogoda_hits_daily",
          "limit": "5000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 18.0,
    "_id": {
      "$oid": "5afedbe2fd5c3e1e0a18f371"
    },
    "description": "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, 5 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0414\u043e\u0441\u0442\u0443\u043f \u043a \u0444\u0430\u043a\u0442\u0443 \u0438 \u043f\u0440\u043e\u0433\u043d\u043e\u0437\u0430\u043c \u0432 \u0442\u0435\u0447\u0435\u043d\u0438\u0435 30 \u0434\u043d\u0435\u0439. \u0414\u043b\u044f \u043f\u0440\u043e\u0434\u043b\u0435\u043d\u0438\u044f \u043e\u0442\u043f\u0440\u0430\u0432\u044c\u0442\u0435 \u0437\u0430\u044f\u0432\u043a\u0443 \u043d\u0430 \u043f\u043e\u0447\u0442\u0443 api-weather@support.yandex.ru"
  },
  {
    "cc": "apimaps_100000_noban_noprepay",
    "service_id": 3.0,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507906",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": 100000.0,
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      }
    ],
    "_id": {
      "$oid": "5afef24b15073bb902824f13"
    },
    "name": "\u041f\u0430\u043a\u0435\u0442 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0434\u0435\u043d\u044c + 11 \u0440\u0443\u0431 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432"
  },
  {
    "cc": "apimaps_100000_ban_prepay",
    "service_id": 3.0,
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1500000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "100000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1500000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "_id": {
      "$oid": "5afef24b15073bb902824f14"
    },
    "name": "\u041f\u0430\u043a\u0435\u0442 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0434\u0435\u043d\u044c, \u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439"
  },
  {
    "client_access": False,
    "name": "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u044b\u0439",
    "cc": "apimaps_free",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f15"
    },
    "description": "1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0434\u0435\u043d\u044c"
  },
  {
    "client_access": False,
    "name": "\u041f\u0440\u043e\u0431\u043d\u044b\u0439",
    "cc": "apimaps_trial",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {
          "ban_reason": 124.0,
          "unban_reason": 125.0,
          "days": 14.0,
          "period_mask": ""
        },
        "unit": "TemporaryActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f16"
    },
    "description": "\u0431\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, 14 \u0434\u043d\u0435\u0439, 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0434\u0435\u043d\u044c"
  },
  {
    "client_access": False,
    "name": "\u0422\u0430\u0440\u0438\u0444 \u0432\u043d\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430",
    "cc": "apimaps_custom",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "custom"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f17"
    },
    "description": ""
  },
  {
    "info_for_table": {
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "120"
      },
      "_group": "common",
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "120000"
      },
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 110.0,
    "cc": "apimaps_1000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508899",
          "time_zone": "+0300",
          "product_value": "120000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "total",
          "product_id": "508896",
          "statistic_aggregator": "total",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f18"
    },
    "personal_account": {
      "product": "508899",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "120000"
    },
    "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "24"
      },
      "_group": "common",
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "600000"
      },
      "payed_daily_limit": {
        "_type": "number",
        "value": "25000"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 130.0,
    "cc": "apimaps_25000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508899",
          "time_zone": "+0300",
          "product_value": "600000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "total",
          "product_id": "508896",
          "statistic_aggregator": "total",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f1a"
    },
    "personal_account": {
      "product": "508899",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "600000"
    },
    "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "11"
      },
      "_group": "common",
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1000000"
      },
      "payed_daily_limit": {
        "_type": "number",
        "value": "100000"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 150.0,
    "cc": "apimaps_100000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508899",
          "time_zone": "+0300",
          "product_value": "1000000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "total",
          "product_id": "508896",
          "statistic_aggregator": "total",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f1c"
    },
    "personal_account": {
      "product": "508899",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "1000000"
    },
    "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "36"
      },
      "_group": "common",
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "360000"
      },
      "payed_daily_limit": {
        "_type": "number",
        "value": "10000"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 120.0,
    "cc": "apimaps_10000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508899",
          "time_zone": "+0300",
          "product_value": "360000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "total",
          "product_id": "508896",
          "statistic_aggregator": "total",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f19"
    },
    "personal_account": {
      "product": "508899",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "360000"
    },
    "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "17"
      },
      "_group": "common",
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "850000"
      },
      "payed_daily_limit": {
        "_type": "number",
        "value": "50000"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 140.0,
    "cc": "apimaps_50000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508899",
          "time_zone": "+0300",
          "product_value": "850000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "total",
          "product_id": "508896",
          "statistic_aggregator": "total",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f1b"
    },
    "personal_account": {
      "product": "508899",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "850000"
    },
    "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 210.0,
    "cc": "apimaps_1000_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "120000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "120000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f22"
    },
    "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 225.0,
    "cc": "apimaps_50k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "850000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "850000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f25"
    },
    "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 220.0,
    "cc": "apimaps_25k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "600000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "600000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f24"
    },
    "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "4564000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.50"
      }
    },
    "name": "\u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 250.0,
    "cc": "apimaps_1b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "4564000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "4564000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000000",
          "product_value": "4.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f2a"
    },
    "description": "4 564 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "2584000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "5.20"
      }
    },
    "name": "\u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 245.0,
    "cc": "apimaps_500k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2584000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "2584000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "500000",
          "product_value": "5.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f29"
    },
    "description": "2 584 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 5.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "200000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1396000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "9.50"
      }
    },
    "name": "\u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 235.0,
    "cc": "apimaps_200k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1396000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1396000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "200000",
          "product_value": "9.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f27"
    },
    "description": "1 396 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 9.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 230.0,
    "cc": "apimaps_100k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1000000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1000000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f26"
    },
    "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "6544000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.40"
      }
    },
    "name": "\u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 255.0,
    "cc": "apimaps_1.5b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "6544000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "6544000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1500000",
          "product_value": "4.40",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f2b"
    },
    "description": "6 544 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.40 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 215.0,
    "cc": "apimaps_10k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "360000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "360000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f23"
    },
    "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "2000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "8524000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.30"
      }
    },
    "name": "\u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 260.0,
    "cc": "apimaps_2b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "8524000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "8524000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "2000000",
          "product_value": "4.30",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f2c"
    },
    "description": "8 524 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.30 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "300000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1792000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "8"
      }
    },
    "name": "\u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 240.0,
    "cc": "apimaps_300k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1792000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1792000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "300000",
          "product_value": "8",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f28"
    },
    "description": "1 792 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 8 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "4000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "16444000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.10"
      }
    },
    "name": "\u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 270.0,
    "cc": "apimaps_4b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16444000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "16444000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "4000000",
          "product_value": "4.10",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f2e"
    },
    "description": "16 444 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.10 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "5000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "20404000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4"
      }
    },
    "name": "\u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 275.0,
    "cc": "apimaps_5b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20404000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "20404000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "5000000",
          "product_value": "4",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f2f"
    },
    "description": "20 404 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "3000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "12484000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.20"
      }
    },
    "name": "\u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 265.0,
    "cc": "apimaps_3b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12484000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "12484000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "3000000",
          "product_value": "4.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f2d"
    },
    "description": "12 484 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_10k_yearprepay_ban_minus_2018",
    "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "360000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "10000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "360000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f31"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_100k_yearprepay_ban_minus_2018",
    "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1000000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "100000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1000000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f34"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_25k_yearprepay_ban_minus_2018",
    "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "600000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "25000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "600000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f32"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_1000_yearprepay_ban_minus_2018",
    "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "120000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "120000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f30"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_50k_yearprepay_ban_minus_2018",
    "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "850000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "50000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "850000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f33"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_4b_yearprepay_ban_minus_2018",
    "description": "16 444 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16444000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "4000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "16444000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f3c"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_5b_yearprepay_ban_minus_2018",
    "description": "20 404 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20404000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "5000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "20404000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f3d"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_300k_yearprepay_ban_minus_2018",
    "description": "1 792 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1792000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "300000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1792000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f36"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_2b_yearprepay_ban_minus_2018",
    "description": "8 524 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "8524000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "2000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "8524000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f3a"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_200k_yearprepay_ban_minus_2018",
    "description": "1 396 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1396000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "200000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1396000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f35"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_3b_yearprepay_ban_minus_2018",
    "description": "12 484 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12484000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "3000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "12484000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f3b"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_500k_yearprepay_ban_minus_2018",
    "description": "2 584 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2584000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "500000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "2584000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f37"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_1b_yearprepay_ban_minus_2018",
    "description": "4 564 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "4564000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "1000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "4564000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f38"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_1.5b_yearprepay_ban_minus_2018",
    "description": "6 544 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "6544000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "1500000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "6544000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f39"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_10k_yearprepay_ban_plus_2018",
    "description": "860 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "860000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "10000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "860000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f3f"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_50k_yearprepay_ban_plus_2018",
    "description": "1 350 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1350000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "50000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1350000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f41"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_25k_yearprepay_ban_plus_2018",
    "description": "1 100 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1100000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "25000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1100000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f40"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_1000_yearprepay_ban_plus_2018",
    "description": "620 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "620000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "620000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f3e"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_4b_yearprepay_ban_plus_2018",
    "description": "16 944 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16944000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "4000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "16944000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f4a"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_5b_yearprepay_ban_plus_2018",
    "description": "20 904 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20904000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "5000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "20904000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f4b"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_1.5b_yearprepay_ban_plus_2018",
    "description": "7 044 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "7044000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "1500000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "7044000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f47"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_300k_yearprepay_ban_plus_2018",
    "description": "2 292 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2292000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "300000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "2292000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f44"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_200k_yearprepay_ban_plus_2018",
    "description": "1 896 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1896000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "200000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1896000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f43"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_100k_yearprepay_ban_plus_2018",
    "description": "1 500 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1500000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "100000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1500000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f42"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_2b_yearprepay_ban_plus_2018",
    "description": "9 024 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "9024000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "2000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "9024000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f48"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_500k_yearprepay_ban_plus_2018",
    "description": "3 084 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "3084000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "500000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "3084000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f45"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_3b_yearprepay_ban_plus_2018",
    "description": "12 984 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12984000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "3000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "12984000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f49"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "apimaps_1b_yearprepay_ban_plus_2018",
    "description": "5 064 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "5064000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "1000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "5064000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f46"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_1000_yearprepay_noban_plus_2018",
    "description": "620 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "620000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "620000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f4c"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_500k_yearprepay_noban_plus_2018",
    "description": "3 084 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 5.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "3084000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "3084000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "500000",
          "product_value": "5.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f53"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_10k_yearprepay_noban_plus_2018",
    "description": "860 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "860000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "860000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f4d"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_100k_yearprepay_noban_plus_2018",
    "description": "1 500 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1500000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1500000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f50"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_300k_yearprepay_noban_plus_2018",
    "description": "2 292 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 8 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2292000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "2292000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "300000",
          "product_value": "8",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f52"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_1b_yearprepay_noban_plus_2018",
    "description": "5 064 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "5064000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "5064000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000000",
          "product_value": "4.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f54"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_1.5b_yearprepay_noban_plus_2018",
    "description": "7 044 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.40 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "7044000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "7044000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1500000",
          "product_value": "4.40",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f55"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_50k_yearprepay_noban_plus_2018",
    "description": "1 350 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1350000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1350000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f4f"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_25k_yearprepay_noban_plus_2018",
    "description": "1 100 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1100000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1100000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f4e"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_200k_yearprepay_noban_plus_2018",
    "description": "1 896 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 9.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1896000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1896000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "200000",
          "product_value": "9.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f51"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_3b_yearprepay_noban_plus_2018",
    "description": "12 984 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12984000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "12984000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "3000000",
          "product_value": "4.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f57"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_5b_yearprepay_noban_plus_2018",
    "description": "20 904 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20904000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "20904000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "5000000",
          "product_value": "4",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f59"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_4b_yearprepay_noban_plus_2018",
    "description": "16 944 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.10 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16944000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "16944000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "4000000",
          "product_value": "4.10",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f58"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "apimaps_2b_yearprepay_noban_plus_2018",
    "description": "9 024 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.30 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "9024000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "apimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "9024000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "2000000",
          "product_value": "4.30",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 3.0,
    "_id": {
      "$oid": "5afef24b15073bb902824f56"
    },
    "info_for_table": None
  },
  {
    "cc": "testapimaps_100000_noban_noprepay",
    "service_id": 10000003.0,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "product_id": "507906",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": 100000.0,
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      }
    ],
    "_id": {
      "$oid": "5b01575c15073bb902824f5a"
    },
    "name": "\u041f\u0430\u043a\u0435\u0442 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0434\u0435\u043d\u044c + 11 \u0440\u0443\u0431 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432"
  },
  {
    "client_access": False,
    "name": "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u044b\u0439",
    "cc": "testapimaps_free",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f5c"
    },
    "description": "1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0434\u0435\u043d\u044c"
  },
  {
    "cc": "testapimaps_100000_ban_prepay",
    "service_id": 10000003.0,
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1500000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "100000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1500000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "_id": {
      "$oid": "5b01575c15073bb902824f5b"
    },
    "name": "\u041f\u0430\u043a\u0435\u0442 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0434\u0435\u043d\u044c, \u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439"
  },
  {
    "client_access": False,
    "name": "\u041f\u0440\u043e\u0431\u043d\u044b\u0439",
    "cc": "testapimaps_trial",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {
          "ban_reason": 124.0,
          "unban_reason": 125.0,
          "days": 14.0,
          "period_mask": ""
        },
        "unit": "TemporaryActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f5d"
    },
    "description": "\u0431\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, 14 \u0434\u043d\u0435\u0439, 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0434\u0435\u043d\u044c"
  },
  {
    "client_access": False,
    "name": "\u0422\u0430\u0440\u0438\u0444 \u0432\u043d\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430",
    "cc": "testapimaps_custom",
    "next_tariff_immediately": True,
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "custom"
        },
        "unit": "StaticLimitsUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f5e"
    },
    "description": ""
  },
  {
    "info_for_table": {
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "120"
      },
      "_group": "common",
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "120000"
      },
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 110.0,
    "cc": "testapimaps_1000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508899",
          "time_zone": "+0300",
          "product_value": "120000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "total",
          "product_id": "508896",
          "statistic_aggregator": "total",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f5f"
    },
    "personal_account": {
      "product": "508899",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "120000"
    },
    "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "24"
      },
      "_group": "common",
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "600000"
      },
      "payed_daily_limit": {
        "_type": "number",
        "value": "25000"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 130.0,
    "cc": "testapimaps_25000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508899",
          "time_zone": "+0300",
          "product_value": "600000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "total",
          "product_id": "508896",
          "statistic_aggregator": "total",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f61"
    },
    "personal_account": {
      "product": "508899",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "600000"
    },
    "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "17"
      },
      "_group": "common",
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "850000"
      },
      "payed_daily_limit": {
        "_type": "number",
        "value": "50000"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 140.0,
    "cc": "testapimaps_50000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508899",
          "time_zone": "+0300",
          "product_value": "850000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "total",
          "product_id": "508896",
          "statistic_aggregator": "total",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f62"
    },
    "personal_account": {
      "product": "508899",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "850000"
    },
    "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "11"
      },
      "_group": "common",
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1000000"
      },
      "payed_daily_limit": {
        "_type": "number",
        "value": "100000"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 150.0,
    "cc": "testapimaps_100000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508899",
          "time_zone": "+0300",
          "product_value": "1000000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "total",
          "product_id": "508896",
          "statistic_aggregator": "total",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f63"
    },
    "personal_account": {
      "product": "508899",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "1000000"
    },
    "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "36"
      },
      "_group": "common",
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "360000"
      },
      "payed_daily_limit": {
        "_type": "number",
        "value": "10000"
      }
    },
    "client_access": True,
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "weight": 120.0,
    "cc": "testapimaps_10000_yearprepay_contractless",
    "tarifficator_config": [
      {
        "params": {

        },
        "unit": "UnconditionalActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 110.0,
          "product_id": "508899",
          "time_zone": "+0300",
          "product_value": "360000",
          "truncate_period_mask": "0 0 * * * *",
          "unban_reason": 111.0,
          "period_mask": "0 0 x x * *",
          "autocharge_personal_account": True
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "ban_reason": 117.0,
          "limit_statistic_aggregator": "total",
          "product_id": "508896",
          "statistic_aggregator": "total",
          "include_range_from": False,
          "skip_when_range_out": True,
          "round_method": "ceil",
          "time_zone": "+0300",
          "precision": "1",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000",
          "unban_reason": 111.0,
          "autocharge_personal_account": True
        },
        "unit": "TodayPrepayStatisticRangeConsumerUnit"
      }
    ],
    "contractless": True,
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f60"
    },
    "personal_account": {
      "product": "508899",
      "firm_id": 1.0,
      "default_paysys": 1128.0,
      "default_paysys_by_person_type": {
        "ph": 1128.0,
        "ur": 1117.0
      },
      "default_replenishment_amount": "360000"
    },
    "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 210.0,
    "cc": "testapimaps_1000_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "120000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "120000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f69"
    },
    "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 215.0,
    "cc": "testapimaps_10k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "360000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "360000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f6a"
    },
    "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 230.0,
    "cc": "testapimaps_100k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1000000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1000000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f6d"
    },
    "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "4564000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.50"
      }
    },
    "name": "\u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 250.0,
    "cc": "testapimaps_1b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "4564000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "4564000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000000",
          "product_value": "4.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f71"
    },
    "description": "4 564 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 225.0,
    "cc": "testapimaps_50k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "850000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "850000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f6c"
    },
    "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "200000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1396000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "9.50"
      }
    },
    "name": "\u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 235.0,
    "cc": "testapimaps_200k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1396000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1396000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "200000",
          "product_value": "9.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f6e"
    },
    "description": "1 396 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 9.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "1500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "6544000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.40"
      }
    },
    "name": "\u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 255.0,
    "cc": "testapimaps_1.5b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "6544000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "6544000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1500000",
          "product_value": "4.40",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f72"
    },
    "description": "6 544 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.40 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "300000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "1792000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "8"
      }
    },
    "name": "\u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 240.0,
    "cc": "testapimaps_300k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1792000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1792000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "300000",
          "product_value": "8",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f6f"
    },
    "description": "1 792 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 8 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": None,
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 220.0,
    "cc": "testapimaps_25k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "600000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "600000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f6b"
    },
    "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "500000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "2584000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "5.20"
      }
    },
    "name": "\u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 245.0,
    "cc": "testapimaps_500k_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2584000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "2584000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "500000",
          "product_value": "5.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f70"
    },
    "description": "2 584 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 5.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "4000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "16444000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.10"
      }
    },
    "name": "\u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 270.0,
    "cc": "testapimaps_4b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16444000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "16444000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "4000000",
          "product_value": "4.10",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f75"
    },
    "description": "16 444 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.10 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "2000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "8524000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.30"
      }
    },
    "name": "\u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 260.0,
    "cc": "testapimaps_2b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "8524000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "8524000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "2000000",
          "product_value": "4.30",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f73"
    },
    "description": "8 524 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.30 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "3000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "12484000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4.20"
      }
    },
    "name": "\u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 265.0,
    "cc": "testapimaps_3b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12484000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "12484000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "3000000",
          "product_value": "4.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f74"
    },
    "description": "12 484 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "info_for_table": {
      "payed_daily_limit": {
        "_type": "number",
        "value": "5000000"
      },
      "year_subscribe": {
        "currency": "RUR",
        "_type": "money",
        "value": "20404000"
      },
      "overhead_per_1000_cost": {
        "currency": "RUR",
        "_type": "money",
        "value": "4"
      }
    },
    "name": "\u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
    "weight": 275.0,
    "cc": "testapimaps_5b_yearprepay_2017",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20404000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "20404000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "5000000",
          "product_value": "4",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f76"
    },
    "description": "20 404 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
  },
  {
    "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_1000_yearprepay_ban_minus_2018",
    "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "120000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "120000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f77"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_10k_yearprepay_ban_minus_2018",
    "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "360000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "10000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "360000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f78"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_100k_yearprepay_ban_minus_2018",
    "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1000000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "100000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1000000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f7b"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_50k_yearprepay_ban_minus_2018",
    "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "850000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "50000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "850000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f7a"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_300k_yearprepay_ban_minus_2018",
    "description": "1 792 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1792000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "300000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1792000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f7d"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_25k_yearprepay_ban_minus_2018",
    "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "600000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "25000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "600000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f79"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_3b_yearprepay_ban_minus_2018",
    "description": "12 484 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12484000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "3000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "12484000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f82"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_1b_yearprepay_ban_minus_2018",
    "description": "4 564 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "4564000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "1000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "4564000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f7f"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_500k_yearprepay_ban_minus_2018",
    "description": "2 584 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2584000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "500000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "2584000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f7e"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_200k_yearprepay_ban_minus_2018",
    "description": "1 396 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1396000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "200000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1396000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f7c"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_1.5b_yearprepay_ban_minus_2018",
    "description": "6 544 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "6544000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "1500000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "6544000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f80"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_2b_yearprepay_ban_minus_2018",
    "description": "8 524 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "8524000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "2000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "8524000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f81"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_5b_yearprepay_ban_minus_2018",
    "description": "20 404 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20404000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "5000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "20404000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f84"
    },
    "info_for_table": None
  },
  {
    "name": "\u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_4b_yearprepay_ban_minus_2018",
    "description": "16 444 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16444000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "4000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "16444000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f83"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_1000_yearprepay_ban_plus_2018",
    "description": "620 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "620000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "1000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "620000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f85"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_25k_yearprepay_ban_plus_2018",
    "description": "1 100 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1100000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "25000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1100000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f87"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_100k_yearprepay_ban_plus_2018",
    "description": "1 500 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1500000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "100000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1500000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f89"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_500k_yearprepay_ban_plus_2018",
    "description": "3 084 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "3084000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "500000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "3084000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f8c"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_200k_yearprepay_ban_plus_2018",
    "description": "1 896 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1896000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "200000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1896000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f8a"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_10k_yearprepay_ban_plus_2018",
    "description": "860 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "860000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "10000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "860000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f86"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_300k_yearprepay_ban_plus_2018",
    "description": "2 292 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2292000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "300000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "2292000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f8b"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_1b_yearprepay_ban_plus_2018",
    "description": "5 064 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "5064000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "1000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "5064000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f8d"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_1.5b_yearprepay_ban_plus_2018",
    "description": "7 044 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "7044000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "1500000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "7044000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f8e"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_50k_yearprepay_ban_plus_2018",
    "description": "1 350 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1350000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "50000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1350000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f88"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_3b_yearprepay_ban_plus_2018",
    "description": "12 984 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12984000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "3000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "12984000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f90"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_2b_yearprepay_ban_plus_2018",
    "description": "9 024 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "9024000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "2000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "9024000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f8f"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_4b_yearprepay_ban_plus_2018",
    "description": "16 944 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16944000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "4000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "16944000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f91"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0434\u043e\u043a\u0443\u043f\u043a\u0438 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432",
    "cc": "testapimaps_5b_yearprepay_ban_plus_2018",
    "description": "20 904 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u0431\u0435\u0437 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20904000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "5000000"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "20904000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f92"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_1000_yearprepay_noban_plus_2018",
    "description": "620 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "620000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "620000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000",
          "product_value": "120",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f93"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_3b_yearprepay_noban_plus_2018",
    "description": "12 984 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "12984000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "12984000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "3000000",
          "product_value": "4.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f9e"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_25k_yearprepay_noban_plus_2018",
    "description": "1 100 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1100000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1100000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "25000",
          "product_value": "24",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f95"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_50k_yearprepay_noban_plus_2018",
    "description": "1 350 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1350000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1350000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "50000",
          "product_value": "17",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f96"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_1.5b_yearprepay_noban_plus_2018",
    "description": "7 044 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.40 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "7044000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "7044000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1500000",
          "product_value": "4.40",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f9c"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_10k_yearprepay_noban_plus_2018",
    "description": "860 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "860000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "860000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "10000",
          "product_value": "36",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f94"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_500k_yearprepay_noban_plus_2018",
    "description": "3 084 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 5.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "3084000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "3084000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "500000",
          "product_value": "5.20",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f9a"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_1b_yearprepay_noban_plus_2018",
    "description": "5 064 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "5064000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "5064000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "1000000",
          "product_value": "4.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f9b"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_2b_yearprepay_noban_plus_2018",
    "description": "9 024 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.30 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "9024000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "9024000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "2000000",
          "product_value": "4.30",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f9d"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_100k_yearprepay_noban_plus_2018",
    "description": "1 500 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1500000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1500000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "100000",
          "product_value": "11",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f97"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_200k_yearprepay_noban_plus_2018",
    "description": "1 896 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 9.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "1896000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "1896000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "200000",
          "product_value": "9.50",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f98"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_300k_yearprepay_noban_plus_2018",
    "description": "2 292 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 8 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "2292000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "2292000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "300000",
          "product_value": "8",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f99"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_4b_yearprepay_noban_plus_2018",
    "description": "16 944 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.10 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "16944000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "16944000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "4000000",
          "product_value": "4.10",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824f9f"
    },
    "info_for_table": None
  },
  {
    "name": "\u0441\u043f\u0435\u0446\u0438\u0430\u043b\u044c\u043d\u044b\u0439, \u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438",
    "cc": "testapimaps_5b_yearprepay_noban_plus_2018",
    "description": "20 904 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
    "tarifficator_config": [
      {
        "params": {
          "needle_credited": "20904000",
          "product_id": "508206"
        },
        "unit": "CreditedActivatorUnit"
      },
      {
        "params": {
          "limit_id": "testapimaps_total_daily",
          "limit": "-1"
        },
        "unit": "StaticLimitsUnit"
      },
      {
        "params": {
          "ban_reason": 1.0,
          "product_id": "508206",
          "time_zone": "+0300",
          "product_value": "20904000",
          "truncate_period_mask": "0 0 * * * *",
          "period_mask": "0 0 x x * *",
          "unban_reason": 18.0
        },
        "unit": "PrepayPeriodicallyUnit"
      },
      {
        "params": {
          "product_id": "508229",
          "statistic_aggregator": "total",
          "round_method": "ceil",
          "precision": "1",
          "time_zone": "+0300",
          "range_from": "5000000",
          "product_value": "4",
          "quantum": "1000"
        },
        "unit": "DailyStatisticRangeConsumerUnit"
      },
      {
        "params": {
          "days_before_next_consume": "30",
          "products_filter": [
            "508206"
          ]
        },
        "unit": "BillDateEventUnit"
      }
    ],
    "service_id": 10000003.0,
    "_id": {
      "$oid": "5b01575c15073bb902824fa0"
    },
    "info_for_table": None
  },
    {
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce19"
        },
        "name": "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u044b\u0439",
        "cc": "mapkit_free",
        "next_tariff_immediately": True,
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "1000"
                },
                "unit": "StaticLimitsUnit"
            }
        ],
        "contractless": True,
        "service_id": 33.0,
        "client_access": False,
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0411\u0435\u0437 \u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u0438\u044f \u043f\u043e \u0441\u0440\u043e\u043a\u0443 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f."
    },
    {
        "client_access": False,
        "name": "\u041d\u0435\u043a\u043e\u043c\u043c\u0435\u0440\u0447\u0435\u0441\u043a\u0438\u0439",
        "cc": "mapkit_custom",
        "next_tariff_immediately": True,
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "custom"
                },
                "unit": "StaticLimitsUnit"
            }
        ],
        "contractless": True,
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce1a"
        }
    },
    {
        "name": "100 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446",
        "weight": 100.0,
        "cc": "mapkit_100_per_device_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u0435\u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0434\u043b\u044f 100 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "100000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "100000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "devices",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "100",
                    "product_value": "125",
                    "quantum": "1"
                },
                "unit": "MonthlyStatisticRangePerDayConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce1b"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "string",
                "_weight": 2.0,
                "value": "-"
            },
            "overhead_device_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 5.0,
                "value": "125"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "100000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "number",
                "_weight": 4.0,
                "value": "100"
            }
        }
    },
    {
        "name": "200 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446",
        "weight": 110.0,
        "cc": "mapkit_200_per_device_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u0435\u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0434\u043b\u044f 200 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "200000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "200000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "devices",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "200",
                    "product_value": "125",
                    "quantum": "1"
                },
                "unit": "MonthlyStatisticRangePerDayConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce1c"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "string",
                "_weight": 2.0,
                "value": "-"
            },
            "overhead_device_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 5.0,
                "value": "125"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "200000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "number",
                "_weight": 4.0,
                "value": "200"
            }
        }
    },
    {
        "name": "500 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446",
        "weight": 120.0,
        "cc": "mapkit_500_per_device_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u0435\u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0434\u043b\u044f 500 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "450000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "450000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "devices",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "500",
                    "product_value": "113",
                    "quantum": "1"
                },
                "unit": "MonthlyStatisticRangePerDayConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce1d"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "string",
                "_weight": 2.0,
                "value": "-"
            },
            "overhead_device_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 5.0,
                "value": "113"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "450000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "number",
                "_weight": 4.0,
                "value": "500"
            }
        }
    },
    {
        "name": "1 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446",
        "weight": 130.0,
        "cc": "mapkit_1000_per_device_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u0435\u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0434\u043b\u044f 1 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "900000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "900000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "devices",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "1000",
                    "product_value": "113",
                    "quantum": "1"
                },
                "unit": "MonthlyStatisticRangePerDayConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce1e"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "string",
                "_weight": 2.0,
                "value": "-"
            },
            "overhead_device_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 5.0,
                "value": "113"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "900000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "number",
                "_weight": 4.0,
                "value": "1000"
            }
        }
    },
    {
        "name": "2 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446",
        "weight": 140.0,
        "cc": "mapkit_2000_per_device_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u0435\u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0434\u043b\u044f 2 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "1700000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "1700000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "devices",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "2000",
                    "product_value": "106",
                    "quantum": "1"
                },
                "unit": "MonthlyStatisticRangePerDayConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce1f"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "string",
                "_weight": 2.0,
                "value": "-"
            },
            "overhead_device_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 5.0,
                "value": "106"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "1700000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "number",
                "_weight": 4.0,
                "value": "2000"
            }
        }
    },
    {
        "name": "3 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446",
        "weight": 150.0,
        "cc": "mapkit_3000_per_device_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u0435\u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0434\u043b\u044f 3 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "2550000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "2550000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "devices",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "3000",
                    "product_value": "106",
                    "quantum": "1"
                },
                "unit": "MonthlyStatisticRangePerDayConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce20"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "string",
                "_weight": 2.0,
                "value": "-"
            },
            "overhead_device_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 5.0,
                "value": "106"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "2550000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "number",
                "_weight": 4.0,
                "value": "3000"
            }
        }
    },
    {
        "name": "4 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446",
        "weight": 160.0,
        "cc": "mapkit_4000_per_device_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u0435\u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0434\u043b\u044f 4 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "3400000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "3400000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "devices",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "4000",
                    "product_value": "106",
                    "quantum": "1"
                },
                "unit": "MonthlyStatisticRangePerDayConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce21"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "string",
                "_weight": 2.0,
                "value": "-"
            },
            "overhead_device_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 5.0,
                "value": "106"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "3400000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "number",
                "_weight": 4.0,
                "value": "4000"
            }
        }
    },
    {
        "name": "5 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446",
        "weight": 170.0,
        "cc": "mapkit_5000_per_device_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u0435\u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0434\u043b\u044f 5 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "4250000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "4250000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "devices",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "5000",
                    "product_value": "106",
                    "quantum": "1"
                },
                "unit": "MonthlyStatisticRangePerDayConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce22"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "string",
                "_weight": 2.0,
                "value": "-"
            },
            "overhead_device_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 5.0,
                "value": "106"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "4250000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "number",
                "_weight": 4.0,
                "value": "5000"
            }
        }
    },
    {
        "name": "7 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446",
        "weight": 180.0,
        "cc": "mapkit_7000_per_device_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u0435\u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0434\u043b\u044f 7 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "5600000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "5600000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "devices",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "7000",
                    "product_value": "100",
                    "quantum": "1"
                },
                "unit": "MonthlyStatisticRangePerDayConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce23"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "string",
                "_weight": 2.0,
                "value": "-"
            },
            "overhead_device_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 5.0,
                "value": "100"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "5600000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "number",
                "_weight": 4.0,
                "value": "7000"
            }
        }
    },
    {
        "name": "10 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446",
        "weight": 190.0,
        "cc": "mapkit_10000_per_device_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043d\u0435\u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u043d\u043e\u0435 \u043a\u043e\u043b\u0438\u0447\u0435\u0441\u0442\u0432\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0434\u043b\u044f 10 000 \u0434\u0435\u0432\u0430\u0439\u0441\u043e\u0432 \u0432 \u043c\u0435\u0441\u044f\u0446.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "8000000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "8000000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "devices",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "10000",
                    "product_value": "100",
                    "quantum": "1"
                },
                "unit": "MonthlyStatisticRangePerDayConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce24"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "string",
                "_weight": 2.0,
                "value": "-"
            },
            "overhead_device_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 5.0,
                "value": "100"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "8000000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "number",
                "_weight": 4.0,
                "value": "10000"
            }
        }
    },
    {
        "name": "\u0434\u043e 1 000",
        "weight": 200.0,
        "cc": "mapkit_1000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "120000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "120000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "1000",
                    "product_value": "120",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce25"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "1000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "120000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "120"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 10 000",
        "weight": 210.0,
        "cc": "mapkit_10000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "360000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "360000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "10000",
                    "product_value": "36",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce26"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "10000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "360000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "36"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 25 000",
        "weight": 220.0,
        "cc": "mapkit_25000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "600000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "600000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "25000",
                    "product_value": "24",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce27"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "25000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "600000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "24"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 50 000",
        "weight": 230.0,
        "cc": "mapkit_50000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "850000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "850000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "50000",
                    "product_value": "17",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce28"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "50000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "850000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "17"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 100 000",
        "weight": 240.0,
        "cc": "mapkit_100000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "1000000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "1000000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "100000",
                    "product_value": "11",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce29"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "100000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "1000000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "11"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 200 000",
        "weight": 250.0,
        "cc": "mapkit_200000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "1396000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "1396000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "200000",
                    "product_value": "9.50",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce2a"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "200000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "1396000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "9.50"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 300 000",
        "weight": 260.0,
        "cc": "mapkit_300000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "1792000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "1792000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "300000",
                    "product_value": "8",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce2b"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "300000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "1792000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "8"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 500 000",
        "weight": 270.0,
        "cc": "mapkit_500000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "2584000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "2584000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "500000",
                    "product_value": "5.20",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce2c"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "500000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "2584000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "5.20"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 1 000 000",
        "weight": 280.0,
        "cc": "mapkit_1000000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "4564000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "4564000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "1000000",
                    "product_value": "4.50",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce2d"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "1000000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "4564000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "4.50"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 1 500 000",
        "weight": 290.0,
        "cc": "mapkit_1500000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "6544000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "6544000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "1500000",
                    "product_value": "4.40",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce2e"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "1500000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "6544000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "4.40"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 2 000 000",
        "weight": 300.0,
        "cc": "mapkit_2000000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "8524000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "8524000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "2000000",
                    "product_value": "4.30",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce2f"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "2000000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "8524000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "4.30"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 3 000 000",
        "weight": 310.0,
        "cc": "mapkit_3000000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "12484000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "12484000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "3000000",
                    "product_value": "4.20",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce30"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "3000000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "12484000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "4.20"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 4 000 000",
        "weight": 320.0,
        "cc": "mapkit_4000000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "16444000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "16444000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "4000000",
                    "product_value": "4.10",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce31"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "4000000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "16444000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "4.10"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 5 000 000",
        "weight": 330.0,
        "cc": "mapkit_5000000_yearprepay_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "20404000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "20404000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "5000000",
                    "product_value": "4",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce32"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "5000000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "20404000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 3.0,
                "value": "4"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 1 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 205.0,
        "cc": "mapkit_1000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "120000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "1000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "120000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce33"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "1000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "120000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 10 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 215.0,
        "cc": "mapkit_10000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "360000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "10000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "360000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce34"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "10000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "360000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 25 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 225.0,
        "cc": "mapkit_25000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "600000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "25000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "600000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce35"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "25000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "600000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 50 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 235.0,
        "cc": "mapkit_50000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "850000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "50000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "850000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce36"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "50000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "850000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 100 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 245.0,
        "cc": "mapkit_100000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "1000000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "100000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "1000000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce37"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "100000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "1000000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 200 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 255.0,
        "cc": "mapkit_200000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "1396000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "200000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "1396000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce38"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "200000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "1396000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 500 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 275.0,
        "cc": "mapkit_500000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "2584000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "500000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "2584000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce3a"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "500000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "2584000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 300 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 265.0,
        "cc": "mapkit_300000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "1792000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "300000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "1792000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce39"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "300000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "1792000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 1 000 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 285.0,
        "cc": "mapkit_1000000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "4564000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "1000000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "4564000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce3b"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "1000000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "4564000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 1 500 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 295.0,
        "cc": "mapkit_1500000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "6544000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "1500000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "6544000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce3c"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "1500000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "6544000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 2 000 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 305.0,
        "cc": "mapkit_2000000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "8524000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "2000000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "8524000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce3d"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "2000000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "8524000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 3 000 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 315.0,
        "cc": "mapkit_3000000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "12484000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "3000000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "12484000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce3e"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "3000000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "12484000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 4 000 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 325.0,
        "cc": "mapkit_4000000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "16444000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "4000000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "16444000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce3f"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "4000000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "16444000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "name": "\u0434\u043e 5 000 000 (\u0431\u0435\u0437 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u0439)",
        "weight": 335.0,
        "cc": "mapkit_5000000_yearprepay_ban_2018",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443.",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "20404000",
                    "product_id": "509134"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "mapkit_total_daily",
                    "limit": "5000000"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "20404000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "30",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 33.0,
        "_id": {
            "$oid": "5b1a9064ad5dc8bfcba0ce40"
        },
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "_weight": 2.0,
                "value": "5000000"
            },
            "overhead_device_cost": {
                "_type": "string",
                "_weight": 5.0,
                "value": "-"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "_weight": 1.0,
                "value": "20404000"
            },
            "overhead_per_1000_cost": {
                "_type": "string",
                "_weight": 3.0,
                "value": "-"
            },
            "payed_devices_limit": {
                "_type": "string",
                "_weight": 4.0,
                "value": "-"
            }
        }
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "2000000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "8524000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "4.30"
            }
        },
        "name": "\u0434\u043e 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_2000000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "8524000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "8524000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "2000000",
                    "product_value": "4.30",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ff9"
        },
        "description": "8 524 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 2 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.30 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "client_access": False,
        "name": "\u0422\u0430\u0440\u0438\u0444 \u0432\u043d\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430",
        "cc": "staticmaps_custom",
        "next_tariff_immediately": True,
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "staticmaps_hits_daily",
                    "limit": "custom"
                },
                "unit": "StaticLimitsUnit"
            }
        ],
        "contractless": True,
        "service_id": 16.0,
        "_id": {
            "$oid": "5b29e6910252ac82f8e81f7b"
        }
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "50000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "850000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "17"
            }
        },
        "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_50000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "850000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "850000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "50000",
                    "product_value": "17",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ff2"
        },
        "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 17 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "_id": {
            "$oid": "5b20e934ad5dc8bfcba0ce96"
        },
        "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 \u0441\u043e \u0441\u043a\u0438\u0434\u043a\u043e\u0439 15%",
        "weight": 155.0,
        "cc": "apimaps_100000_yearprepay_contractless_15_discount",
        "description": "850 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 9.35 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "apimaps_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "condition": "state_get('products__508899__next_consume_date') and state_get('products__508899__next_consume_date') <= state_get('now')",
                    "next_tariff": "apimaps_100000_yearprepay_contractless"
                },
                "unit": "NextTariffSwitchEventUnit"
            },
            {
                "params": {
                    "ban_reason": 110.0,
                    "product_id": "508899",
                    "time_zone": "+0300",
                    "product_value": "850000",
                    "truncate_period_mask": "0 0 * * * *",
                    "autocharge_personal_account": True,
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 111.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "ban_reason": 117.0,
                    "limit_statistic_aggregator": "total",
                    "product_id": "508896",
                    "statistic_aggregator": "total",
                    "include_range_from": False,
                    "skip_when_range_out": True,
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "100000",
                    "product_value": "9.35",
                    "quantum": "1000",
                    "autocharge_personal_account": True,
                    "unban_reason": 111.0
                },
                "unit": "TodayPrepayStatisticRangeConsumerUnit"
            }
        ],
        "contractless": True,
        "service_id": 3.0,
        "client_access": True,
        "personal_account": {
            "product": "508899",
            "firm_id": 1.0,
            "default_paysys": 1128.0,
            "default_paysys_by_person_type": {
                "ph": 1128.0,
                "ur": 1117.0
            },
            "default_replenishment_amount": "850000"
        },
        "info_for_table": {
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "9.35"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "850000"
            },
            "_group": "special_discount",
            "payed_daily_limit": {
                "_type": "number",
                "value": "100000"
            }
        }
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "300000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "1792000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "8"
            }
        },
        "name": "\u0434\u043e 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_300000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "1792000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "1792000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "300000",
                    "product_value": "8",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ff5"
        },
        "description": "1 792 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 300 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 8 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "_id": {
            "$oid": "5b1925acad5dc8bfcba0cce8"
        },
        "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 \u0441\u043e \u0441\u043a\u0438\u0434\u043a\u043e\u0439 15%",
        "weight": 115.0,
        "cc": "testapimaps_1000_yearprepay_contractless_15_discount",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 102 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u043e\u0444\u0435\u0440\u0442\u0435.",
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testapimaps_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 110.0,
                    "product_id": "508899",
                    "time_zone": "+0300",
                    "product_value": "102000",
                    "truncate_period_mask": "0 0 * * * *",
                    "autocharge_personal_account": True,
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 111.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "ban_reason": 117.0,
                    "limit_statistic_aggregator": "total",
                    "product_id": "508896",
                    "statistic_aggregator": "total",
                    "include_range_from": False,
                    "skip_when_range_out": True,
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "1000",
                    "product_value": "102",
                    "quantum": "1000",
                    "autocharge_personal_account": True,
                    "unban_reason": 111.0
                },
                "unit": "TodayPrepayStatisticRangeConsumerUnit"
            }
        ],
        "contractless": True,
        "service_id": 10000003.0,
        "client_access": True,
        "personal_account": {
            "product": "508899",
            "firm_id": 1.0,
            "default_paysys": 1128.0,
            "default_paysys_by_person_type": {
                "ph": 1128.0,
                "ur": 1117.0
            },
            "default_replenishment_amount": "102000"
        },
        "info_for_table": {
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "102"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "102000"
            },
            "_group": "special_discount",
            "payed_daily_limit": {
                "_type": "number",
                "value": "1000"
            }
        }
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "25000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "600000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "24"
            }
        },
        "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_25000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "600000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "600000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "25000",
                    "product_value": "24",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ff1"
        },
        "description": "600 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 24 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "_id": {
            "$oid": "5b1925acad5dc8bfcba0cce9"
        },
        "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 \u0441\u043e \u0441\u043a\u0438\u0434\u043a\u043e\u0439 15%",
        "weight": 125.0,
        "cc": "testapimaps_10000_yearprepay_contractless_15_discount",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 306 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u043e\u0444\u0435\u0440\u0442\u0435.",
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testapimaps_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 110.0,
                    "product_id": "508899",
                    "time_zone": "+0300",
                    "product_value": "306000",
                    "truncate_period_mask": "0 0 * * * *",
                    "autocharge_personal_account": True,
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 111.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "ban_reason": 117.0,
                    "limit_statistic_aggregator": "total",
                    "product_id": "508896",
                    "statistic_aggregator": "total",
                    "include_range_from": False,
                    "skip_when_range_out": True,
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "10000",
                    "product_value": "30.6",
                    "quantum": "1000",
                    "autocharge_personal_account": True,
                    "unban_reason": 111.0
                },
                "unit": "TodayPrepayStatisticRangeConsumerUnit"
            }
        ],
        "contractless": True,
        "service_id": 10000003.0,
        "client_access": True,
        "personal_account": {
            "product": "508899",
            "firm_id": 1.0,
            "default_paysys": 1128.0,
            "default_paysys_by_person_type": {
                "ph": 1128.0,
                "ur": 1117.0
            },
            "default_replenishment_amount": "306000"
        },
        "info_for_table": {
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "30.6"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "306000"
            },
            "_group": "special_discount",
            "payed_daily_limit": {
                "_type": "number",
                "value": "10000"
            }
        }
    },
    {
        "_id": {
            "$oid": "5afd7a1215073bb902824f05"
        },
        "name": "\u041d\u0435\u043a\u043e\u043c\u043c\u0435\u0440\u0447\u0435\u0441\u043a\u0438\u0439",
        "cc": "testspeechkitcloud_custom",
        "next_tariff_immediately": True,
        "tarifficator_config": [],
        "contractless": True,
        "service_id": 10000020.0,
        "client_access": True,
        "description": ""
    },
    {
        "_id": {
            "$oid": "5b20e934ad5dc8bfcba0ce95"
        },
        "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 \u0441\u043e \u0441\u043a\u0438\u0434\u043a\u043e\u0439 15%",
        "weight": 145.0,
        "cc": "apimaps_50000_yearprepay_contractless_15_discount",
        "description": "722 500 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 14.45 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "apimaps_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "condition": "state_get('products__508899__next_consume_date') and state_get('products__508899__next_consume_date') <= state_get('now')",
                    "next_tariff": "apimaps_50000_yearprepay_contractless"
                },
                "unit": "NextTariffSwitchEventUnit"
            },
            {
                "params": {
                    "ban_reason": 110.0,
                    "product_id": "508899",
                    "time_zone": "+0300",
                    "product_value": "722500",
                    "truncate_period_mask": "0 0 * * * *",
                    "autocharge_personal_account": True,
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 111.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "ban_reason": 117.0,
                    "limit_statistic_aggregator": "total",
                    "product_id": "508896",
                    "statistic_aggregator": "total",
                    "include_range_from": False,
                    "skip_when_range_out": True,
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "50000",
                    "product_value": "14.45",
                    "quantum": "1000",
                    "autocharge_personal_account": True,
                    "unban_reason": 111.0
                },
                "unit": "TodayPrepayStatisticRangeConsumerUnit"
            }
        ],
        "contractless": True,
        "service_id": 3.0,
        "client_access": True,
        "personal_account": {
            "product": "508899",
            "firm_id": 1.0,
            "default_paysys": 1128.0,
            "default_paysys_by_person_type": {
                "ph": 1128.0,
                "ur": 1117.0
            },
            "default_replenishment_amount": "722500"
        },
        "info_for_table": {
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "14.45"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "722500"
            },
            "_group": "special_discount",
            "payed_daily_limit": {
                "_type": "number",
                "value": "50000"
            }
        }
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "1000000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "4564000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "4.50"
            }
        },
        "name": "\u0434\u043e 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_1000000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "4564000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "4564000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "1000000",
                    "product_value": "4.50",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ff7"
        },
        "description": "4 564 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "_id": {
            "$oid": "5b20e934ad5dc8bfcba0ce94"
        },
        "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 \u0441\u043e \u0441\u043a\u0438\u0434\u043a\u043e\u0439 15%",
        "weight": 135.0,
        "cc": "apimaps_25000_yearprepay_contractless_15_discount",
        "description": "510 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 20.4 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "apimaps_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "condition": "state_get('products__508899__next_consume_date') and state_get('products__508899__next_consume_date') <= state_get('now')",
                    "next_tariff": "apimaps_25000_yearprepay_contractless"
                },
                "unit": "NextTariffSwitchEventUnit"
            },
            {
                "params": {
                    "ban_reason": 110.0,
                    "product_id": "508899",
                    "time_zone": "+0300",
                    "product_value": "510000",
                    "truncate_period_mask": "0 0 * * * *",
                    "autocharge_personal_account": True,
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 111.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "ban_reason": 117.0,
                    "limit_statistic_aggregator": "total",
                    "product_id": "508896",
                    "statistic_aggregator": "total",
                    "include_range_from": False,
                    "skip_when_range_out": True,
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "25000",
                    "product_value": "20.4",
                    "quantum": "1000",
                    "autocharge_personal_account": True,
                    "unban_reason": 111.0
                },
                "unit": "TodayPrepayStatisticRangeConsumerUnit"
            }
        ],
        "contractless": True,
        "service_id": 3.0,
        "client_access": True,
        "personal_account": {
            "product": "508899",
            "firm_id": 1.0,
            "default_paysys": 1128.0,
            "default_paysys_by_person_type": {
                "ph": 1128.0,
                "ur": 1117.0
            },
            "default_replenishment_amount": "510000"
        },
        "info_for_table": {
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "20.4"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "510000"
            },
            "_group": "special_discount",
            "payed_daily_limit": {
                "_type": "number",
                "value": "25000"
            }
        }
    },
    {
        "_id": {
            "$oid": "5b1925acad5dc8bfcba0ccec"
        },
        "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 \u0441\u043e \u0441\u043a\u0438\u0434\u043a\u043e\u0439 15%",
        "weight": 155.0,
        "cc": "testapimaps_100000_yearprepay_contractless_15_discount",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 850 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u043e\u0444\u0435\u0440\u0442\u0435.",
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testapimaps_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 110.0,
                    "product_id": "508899",
                    "time_zone": "+0300",
                    "product_value": "850000",
                    "truncate_period_mask": "0 0 * * * *",
                    "autocharge_personal_account": True,
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 111.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "ban_reason": 117.0,
                    "limit_statistic_aggregator": "total",
                    "product_id": "508896",
                    "statistic_aggregator": "total",
                    "include_range_from": False,
                    "skip_when_range_out": True,
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "100000",
                    "product_value": "9.35",
                    "quantum": "1000",
                    "autocharge_personal_account": True,
                    "unban_reason": 111.0
                },
                "unit": "TodayPrepayStatisticRangeConsumerUnit"
            }
        ],
        "contractless": True,
        "service_id": 10000003.0,
        "client_access": True,
        "personal_account": {
            "product": "508899",
            "firm_id": 1.0,
            "default_paysys": 1128.0,
            "default_paysys_by_person_type": {
                "ph": 1128.0,
                "ur": 1117.0
            },
            "default_replenishment_amount": "850000"
        },
        "info_for_table": {
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "9.35"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "850000"
            },
            "_group": "special_discount",
            "payed_daily_limit": {
                "_type": "number",
                "value": "100000"
            }
        }
    },
    {
        "_id": {
            "$oid": "5b20e934ad5dc8bfcba0ce92"
        },
        "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 \u0441\u043e \u0441\u043a\u0438\u0434\u043a\u043e\u0439 15%",
        "weight": 115.0,
        "cc": "apimaps_1000_yearprepay_contractless_15_discount",
        "description": "102 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 102 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "apimaps_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "condition": "state_get('products__508899__next_consume_date') and state_get('products__508899__next_consume_date') <= state_get('now')",
                    "next_tariff": "apimaps_1000_yearprepay_contractless"
                },
                "unit": "NextTariffSwitchEventUnit"
            },
            {
                "params": {
                    "ban_reason": 110.0,
                    "product_id": "508899",
                    "time_zone": "+0300",
                    "product_value": "102000",
                    "truncate_period_mask": "0 0 * * * *",
                    "autocharge_personal_account": True,
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 111.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "ban_reason": 117.0,
                    "limit_statistic_aggregator": "total",
                    "product_id": "508896",
                    "statistic_aggregator": "total",
                    "include_range_from": False,
                    "skip_when_range_out": True,
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "1000",
                    "product_value": "102",
                    "quantum": "1000",
                    "autocharge_personal_account": True,
                    "unban_reason": 111.0
                },
                "unit": "TodayPrepayStatisticRangeConsumerUnit"
            }
        ],
        "contractless": True,
        "service_id": 3.0,
        "client_access": True,
        "personal_account": {
            "product": "508899",
            "firm_id": 1.0,
            "default_paysys": 1128.0,
            "default_paysys_by_person_type": {
                "ph": 1128.0,
                "ur": 1117.0
            },
            "default_replenishment_amount": "102000"
        },
        "info_for_table": {
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "102"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "102000"
            },
            "_group": "special_discount",
            "payed_daily_limit": {
                "_type": "number",
                "value": "1000"
            }
        }
    },
    {
        "_id": {
            "$oid": "5afedbe0280be620eb27ebf4"
        },
        "name": "\u041d\u0435\u043a\u043e\u043c\u043c\u0435\u0440\u0447\u0435\u0441\u043a\u0438\u0439",
        "cc": "speechkitjsapi_custom",
        "tarifficator_config": [],
        "contractless": True,
        "service_id": 21.0,
        "client_access": False,
        "description": ""
    },
    {
        "_id": {
            "$oid": "5b1925acad5dc8bfcba0cceb"
        },
        "name": "\u0434\u043e 50 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 \u0441\u043e \u0441\u043a\u0438\u0434\u043a\u043e\u0439 15%",
        "weight": 145.0,
        "cc": "testapimaps_50000_yearprepay_contractless_15_discount",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 722 500 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u043e\u0444\u0435\u0440\u0442\u0435.",
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testapimaps_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 110.0,
                    "product_id": "508899",
                    "time_zone": "+0300",
                    "product_value": "722500",
                    "truncate_period_mask": "0 0 * * * *",
                    "autocharge_personal_account": True,
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 111.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "ban_reason": 117.0,
                    "limit_statistic_aggregator": "total",
                    "product_id": "508896",
                    "statistic_aggregator": "total",
                    "include_range_from": False,
                    "skip_when_range_out": True,
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "50000",
                    "product_value": "14.45",
                    "quantum": "1000",
                    "autocharge_personal_account": True,
                    "unban_reason": 111.0
                },
                "unit": "TodayPrepayStatisticRangeConsumerUnit"
            }
        ],
        "contractless": True,
        "service_id": 10000003.0,
        "client_access": True,
        "personal_account": {
            "product": "508899",
            "firm_id": 1.0,
            "default_paysys": 1128.0,
            "default_paysys_by_person_type": {
                "ph": 1128.0,
                "ur": 1117.0
            },
            "default_replenishment_amount": "722500"
        },
        "info_for_table": {
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "14.45"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "722500"
            },
            "_group": "special_discount",
            "payed_daily_limit": {
                "_type": "number",
                "value": "50000"
            }
        }
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "100000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "1000000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "11"
            }
        },
        "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_100000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "1000000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "1000000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "100000",
                    "product_value": "11",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ff3"
        },
        "description": "1 000 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 11 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "4000000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "16444000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "4.10"
            }
        },
        "name": "\u0434\u043e 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_4000000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "16444000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "16444000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "4000000",
                    "product_value": "4.10",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ffb"
        },
        "description": "16 444 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 4 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.10 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "1500000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "6544000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "4.40"
            }
        },
        "name": "\u0434\u043e 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_1500000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "6544000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "6544000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "1500000",
                    "product_value": "4.40",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ff8"
        },
        "description": "6 544 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.40 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "_id": {
            "$oid": "5b1925acad5dc8bfcba0ccea"
        },
        "name": "\u0434\u043e 25 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 \u0441\u043e \u0441\u043a\u0438\u0434\u043a\u043e\u0439 15%",
        "weight": 135.0,
        "cc": "testapimaps_25000_yearprepay_contractless_15_discount",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 510 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u043e\u0444\u0435\u0440\u0442\u0435.",
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testapimaps_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 110.0,
                    "product_id": "508899",
                    "time_zone": "+0300",
                    "product_value": "510000",
                    "truncate_period_mask": "0 0 * * * *",
                    "autocharge_personal_account": True,
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 111.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "ban_reason": 117.0,
                    "limit_statistic_aggregator": "total",
                    "product_id": "508896",
                    "statistic_aggregator": "total",
                    "include_range_from": False,
                    "skip_when_range_out": True,
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "25000",
                    "product_value": "20.4",
                    "quantum": "1000",
                    "autocharge_personal_account": True,
                    "unban_reason": 111.0
                },
                "unit": "TodayPrepayStatisticRangeConsumerUnit"
            }
        ],
        "contractless": True,
        "service_id": 10000003.0,
        "client_access": True,
        "personal_account": {
            "product": "508899",
            "firm_id": 1.0,
            "default_paysys": 1128.0,
            "default_paysys_by_person_type": {
                "ph": 1128.0,
                "ur": 1117.0
            },
            "default_replenishment_amount": "510000"
        },
        "info_for_table": {
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "20.4"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "510000"
            },
            "_group": "special_discount",
            "payed_daily_limit": {
                "_type": "number",
                "value": "25000"
            }
        }
    },
    {
        "_id": {
            "$oid": "5b1925acad5dc8bfcba0cced"
        },
        "name": "\u0434\u043e 100 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 \u0441\u043e \u0441\u043a\u0438\u0434\u043a\u043e\u0439 15% (\u0442\u0435\u0441\u0442 \u043f\u0435\u0440\u0435\u0445\u043e\u0434\u0430)",
        "weight": 155.0,
        "cc": "testapimaps_100000_yearprepay_contractless_15_discount_transit",
        "description": "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u043f\u043e \u0446\u0435\u043d\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0434\u043e 850 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438. \u0421\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0441\u0432\u044b\u0448\u0435 \u043f\u043e\u0434\u043f\u0438\u0441\u043a\u0438 \u0440\u0430\u0441\u0441\u0447\u0438\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u0437\u0430 \u043a\u0430\u0436\u0434\u0443\u044e \u043d\u0435\u043f\u043e\u043b\u043d\u0443\u044e 1000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432. \u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e \u043e\u0444\u0435\u0440\u0442\u0435.",
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testapimaps_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "condition": "state_get('products__508899__next_consume_date') and state_get('products__508899__next_consume_date') >= state_get('now')",
                    "next_tariff": "testapimaps_100000_yearprepay_contractless"
                },
                "unit": "NextTariffSwitchEventUnit"
            },
            {
                "params": {
                    "ban_reason": 110.0,
                    "product_id": "508899",
                    "time_zone": "+0300",
                    "product_value": "850000",
                    "truncate_period_mask": "0 0 * * * *",
                    "autocharge_personal_account": True,
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 111.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "ban_reason": 117.0,
                    "limit_statistic_aggregator": "total",
                    "product_id": "508896",
                    "statistic_aggregator": "total",
                    "include_range_from": False,
                    "skip_when_range_out": True,
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "100000",
                    "product_value": "9.35",
                    "quantum": "1000",
                    "autocharge_personal_account": True,
                    "unban_reason": 111.0
                },
                "unit": "TodayPrepayStatisticRangeConsumerUnit"
            }
        ],
        "contractless": True,
        "service_id": 10000003.0,
        "client_access": True,
        "personal_account": {
            "product": "508899",
            "firm_id": 1.0,
            "default_paysys": 1128.0,
            "default_paysys_by_person_type": {
                "ph": 1128.0,
                "ur": 1117.0
            },
            "default_replenishment_amount": "850000"
        },
        "info_for_table": {
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "9.35"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "850000"
            },
            "_group": "special_discount",
            "payed_daily_limit": {
                "_type": "number",
                "value": "100000"
            }
        }
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "10000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "360000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "36"
            }
        },
        "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_10000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "360000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "360000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "10000",
                    "product_value": "36",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ff0"
        },
        "description": "360 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 36 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "5000000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "20404000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "4"
            }
        },
        "name": "\u0434\u043e 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_5000000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "20404000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "20404000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "5000000",
                    "product_value": "4",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ffc"
        },
        "description": "20 404 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 5 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "1000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "120000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "120"
            }
        },
        "name": "\u0434\u043e 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_1000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "120000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "120000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "1000",
                    "product_value": "120",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824fef"
        },
        "description": "120 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 120 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "client_access": False,
        "name": "\u041d\u0435\u043a\u043e\u043c\u043c\u0435\u0440\u0447\u0435\u0441\u043a\u0438\u0439",
        "cc": "testmapkit_custom",
        "next_tariff_immediately": True,
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "custom"
                },
                "unit": "StaticLimitsUnit"
            }
        ],
        "contractless": True,
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824fee"
        }
    },
    {
        "_id": {
            "$oid": "5b20e934ad5dc8bfcba0ce93"
        },
        "name": "\u0434\u043e 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438 \u0441\u043e \u0441\u043a\u0438\u0434\u043a\u043e\u0439 15%",
        "weight": 125.0,
        "cc": "apimaps_10000_yearprepay_contractless_15_discount",
        "description": "306 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 10 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 30.6 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f",
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "apimaps_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "condition": "state_get('products__508899__next_consume_date') and state_get('products__508899__next_consume_date') <= state_get('now')",
                    "next_tariff": "apimaps_10000_yearprepay_contractless"
                },
                "unit": "NextTariffSwitchEventUnit"
            },
            {
                "params": {
                    "ban_reason": 110.0,
                    "product_id": "508899",
                    "time_zone": "+0300",
                    "product_value": "306000",
                    "truncate_period_mask": "0 0 * * * *",
                    "autocharge_personal_account": True,
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 111.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "ban_reason": 117.0,
                    "limit_statistic_aggregator": "total",
                    "product_id": "508896",
                    "statistic_aggregator": "total",
                    "include_range_from": False,
                    "skip_when_range_out": True,
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "10000",
                    "product_value": "30.6",
                    "quantum": "1000",
                    "autocharge_personal_account": True,
                    "unban_reason": 111.0
                },
                "unit": "TodayPrepayStatisticRangeConsumerUnit"
            }
        ],
        "contractless": True,
        "service_id": 3.0,
        "client_access": True,
        "personal_account": {
            "product": "508899",
            "firm_id": 1.0,
            "default_paysys": 1128.0,
            "default_paysys_by_person_type": {
                "ph": 1128.0,
                "ur": 1117.0
            },
            "default_replenishment_amount": "306000"
        },
        "info_for_table": {
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "30.6"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "306000"
            },
            "_group": "special_discount",
            "payed_daily_limit": {
                "_type": "number",
                "value": "10000"
            }
        }
    },
    {
        "_id": {
            "$oid": "5afd7a1215073bb902824f06"
        },
        "name": "\u041d\u0435\u043a\u043e\u043c\u043c\u0435\u0440\u0447\u0435\u0441\u043a\u0438\u0439 \u0431\u0435\u0437\u043b\u0438\u043c\u0438\u0442",
        "cc": "testspeechkitcloud_custom_unlimited",
        "next_tariff_immediately": True,
        "tarifficator_config": [
            {
                "params": {
                    "limit_id": "testspeechkitcloud_voice_unit_daily",
                    "limit": "custom"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "limit_id": "testspeechkitcloud_tts_unit_daily",
                    "limit": "custom"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "limit_id": "testspeechkitcloud_ner_unit_daily",
                    "limit": "custom"
                },
                "unit": "StaticLimitsUnit"
            }
        ],
        "contractless": True,
        "service_id": 10000020.0,
        "client_access": True,
        "description": ""
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "500000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "2584000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "5.20"
            }
        },
        "name": "\u0434\u043e 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_500000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "2584000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "2584000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "500000",
                    "product_value": "5.20",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ff6"
        },
        "description": "2 584 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 500 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 5.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "3000000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "12484000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "4.20"
            }
        },
        "name": "\u0434\u043e 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_3000000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "12484000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "12484000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "3000000",
                    "product_value": "4.20",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ffa"
        },
        "description": "12 484 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 3 000 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 4.20 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "client_access": False,
        "name": "\u0422\u0430\u0440\u0438\u0444 \u0432\u043d\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430",
        "cc": "city_custom",
        "next_tariff_immediately": True,
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "city_hits_daily",
                    "limit": "custom"
                },
                "unit": "StaticLimitsUnit"
            }
        ],
        "contractless": True,
        "service_id": 12.0,
        "_id": {
            "$oid": "5b29e68a0252ac82f8e81f66"
        }
    },
    {
        "info_for_table": {
            "payed_daily_limit": {
                "_type": "number",
                "value": "200000"
            },
            "year_subscribe": {
                "currency": "RUR",
                "_type": "money",
                "value": "1396000"
            },
            "overhead_per_1000_cost": {
                "currency": "RUR",
                "_type": "money",
                "value": "9.50"
            }
        },
        "name": "\u0434\u043e 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, \u043f\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0443",
        "cc": "testmapkit_200000_yearprepay_2018",
        "tarifficator_config": [
            {
                "params": {
                    "needle_credited": "1396000",
                    "product_id": "508206"
                },
                "unit": "CreditedActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "-1"
                },
                "unit": "StaticLimitsUnit"
            },
            {
                "params": {
                    "ban_reason": 1.0,
                    "product_id": "509134",
                    "time_zone": "+0300",
                    "product_value": "1396000",
                    "truncate_period_mask": "0 0 * * * *",
                    "period_mask": "0 0 x x * *",
                    "unban_reason": 18.0
                },
                "unit": "PrepayPeriodicallyUnit"
            },
            {
                "params": {
                    "product_id": "509133",
                    "statistic_aggregator": "total",
                    "round_method": "ceil",
                    "time_zone": "+0300",
                    "precision": "1",
                    "range_from": "200000",
                    "product_value": "9.50",
                    "quantum": "1000"
                },
                "unit": "DailyStatisticRangeConsumerUnit"
            },
            {
                "params": {
                    "days_before_next_consume": "15",
                    "products_filter": [
                        "509134"
                    ]
                },
                "unit": "BillDateEventUnit"
            }
        ],
        "service_id": 10000033.0,
        "_id": {
            "$oid": "5b0bdb5215073bb902824ff4"
        },
        "description": "1 396 000 \u0440\u0443\u0431\u043b\u0435\u0439 \u043f\u0440\u0435\u0434\u043e\u043b\u043f\u0430\u0442\u0430 \u0432 \u0433\u043e\u0434 \u0437\u0430 200 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438, 9.50 \u0440\u0443\u0431\u043b\u0435\u0439 \u0437\u0430 1 000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d\u0438\u044f"
    },
    {
        "_id": {
            "$oid": "5b0bdb5215073bb902824fed"
        },
        "name": "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u044b\u0439",
        "cc": "testmapkit_free",
        "next_tariff_immediately": True,
        "tarifficator_config": [
            {
                "params": {},
                "unit": "UnconditionalActivatorUnit"
            },
            {
                "params": {
                    "limit_id": "testmapkit_total_daily",
                    "limit": "25000"
                },
                "unit": "StaticLimitsUnit"
            }
        ],
        "contractless": True,
        "service_id": 10000033.0,
        "client_access": False,
        "description": "25000 \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432 \u0432 \u0434\u0435\u043d\u044c"
    }
]