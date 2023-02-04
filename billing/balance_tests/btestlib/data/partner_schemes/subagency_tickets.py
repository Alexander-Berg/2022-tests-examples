# -*- coding: utf-8 -*-
__all__ = []

import functools

import util

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

register(
    "default",
    {
      "contracts": [
        {
          "partner_contract": {
            "attributes": "common"
          },
          "firm": 121,
          "ctype": "GENERAL",
          "tag": "rub_resident",
          "commission": 0,
          "services": {
            "mandatory": [
              325,
              326,
              327
            ]
          },
          "currency": "RUB",
          "unilateral": 1,
          "_params": {
            "enable_setting_attributes": 1,
            "enable_validating_attributes": 1
          }
        }
      ],
      "thirdparty_processing": [
        {
          "service_ids": [
            325,
            326,
            327
          ],
          "pipeline": [
            "GetId",
            "PayoutReadyDtUnit",
            "PartnerUnit",
            "ContractUnit",
            "SkipDublicateRowOnSplittedRewardByVATUnit",
            "AmountUnit",
            {
              "if": {
                "ir.is_side_payment": {
                  "$eq": 1
                }
              },
              "then": {
                "unit": "SplittedByVATOrderReward",
                "params": {
                  "commission_category_from_incoming_row_field": "extra_str_0"
                }
              },
              "else": "SplittedByVATOrderReward"
            },
            {
              "if": {
                "ir.is_copied": {
                  "$eq": 1
                }
              },
              "then": [
                {
                  "unit": "PersonalAccountUnit",
                  "params": {
                    "service_code": "YANDEX_SERVICE_WO_VAT"
                  }
                },
                "ProductMappingUnit",
                {
                  "unit": "ChangeFieldsUnit",
                  "params": {
                    "paysys_type_cc": "netting_wo_nds",
                    "payment_type": "correction_commission"
                  }
                }
              ],
              "else": "PersonalAccountUnit"
            },
            "OEBSOrgID"
          ]
        }
      ],
      "close_month": [
        {
          "contract_tag": "rub_resident",
          "month_close_generator": "RevPartnerGenerator"
        }
      ]
    }
)
