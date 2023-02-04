# -*- coding: utf-8 -*-
__all__ = []

import functools

import util

true, false = True, False

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

register('cloud_referal',
{
  "contracts": [
    {
      "firm": 123,
      "_rules_constants": {
        "flags": {
          "is_cloud": false
        }
      },
      "ctype": "SPENDABLE",
      "tag": "cloud_referal",
      "services": {
        "mandatory": [
          693
        ]
      },
      "_params": {
        "enable_setting_attributes": 1,
        "enable_validating_attributes": 1
      }
    }
  ],
  "thirdparty_processing": [
    {
      "service_ids": [
        693
      ],
      "pipeline": [
        "GetId",
        "PayoutReadyDtUnit",
        "PartnerUnit",
        {
          "params": {
            "contract_filters": [
              {
                "filter": {
                  "name": "service"
                }
              },
              {
                "filter": {
                  "name": "partner"
                }
              },
              {
                "filter": {
                  "name": "transaction_dt"
                }
              },
              {
                "filter": {
                  "params": {
                    "contract_type": "SPENDABLE"
                  },
                  "name": "contract_type"
                }
              }
            ]
          },
          "unit": "ContractUnit"
        },
        "AmountUnit",
        "OEBSOrgID"
      ]
    }
  ],
  "close_month": [
    {
      "month_close_generator": "SpendablePartnerProductGenerator",
      "contract_tag": "cloud_referal"
    }
  ]
})
