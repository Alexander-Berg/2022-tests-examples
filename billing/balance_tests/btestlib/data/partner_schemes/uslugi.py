# -*- coding: utf-8 -*-
__all__ = []

import functools

import util

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

register('default_conf',
{
  "contracts": [
    {
      "currency": "RUB",
      "ctype": "GENERAL",
      "commission": 0,
      "firm_id": 1,
      "services": {
        "mandatory": [
          710
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
        710
      ],
      "pipeline": [
        {
          "then": [],
          "else": "SkipUnit",
          "if": {
            "ir.payment.terminal_payment_method": {
              "$in": [
                "yandex_account_topup",
                "yandex_account_withdraw"
              ]
            },
            "ir.payment_type": {
              "$eq": "yandex_account"
            }
          }
        },
        {
          "then": [
            {
              "chain_alias": "BaseFilters"
            },
            "SetInternal",
            "PlusPartnerUnit",
            "PlusClientUnit",
            {
              "params": {
                "contract_filters": [
                  {
                    "filter": {
                      "params": {
                        "service_id": 703
                      },
                      "name": "custom_service"
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
                        "contract_type": "GENERAL"
                      },
                      "name": "contract_type"
                    }
                  },
                  {
                    "filter": {
                      "name": "plus_operator_contract"
                    }
                  }
                ]
              },
              "unit": "ContractUnit"
            },
            {
              "params": {
                "payment_type": "yandex_account_topup"
              },
              "unit": "ChangeFieldsUnit"
            },
            "AmountUnit",
            "OEBSOrgID"
          ],
          "if": {
            "ir.payment.terminal_payment_method": {
              "$eq": "yandex_account_topup"
            }
          }
        },
        {
          "then": [
            {
              "chain_alias": "BaseFilters"
            },
            "SetInternal",
            "ForcePartner",
            "ContractUnit",
            "PlusClientUnit",
            {
              "params": {
                "payment_type": "yandex_account_withdraw"
              },
              "unit": "ChangeFieldsUnit"
            },
            "AmountUnit",
            "OEBSOrgID"
          ],
          "if": {
            "ir.payment.terminal_payment_method": {
              "$eq": "yandex_account_withdraw"
            }
          }
        }
      ]
    }
  ],
  "close_month": []
}
)
