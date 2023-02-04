# -*- coding: utf-8 -*-
__all__ = []

import functools

import util

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

register(
    "ph",
    {
        "contracts": [
            {
                "partner_contract": {
                    "attributes": "common"
                },
                "firm": 114,
                "ctype": "GENERAL",
                "tag": "health_payments_ph",
                "services": {
                    "mandatory": [
                        723
                    ]
                },
                "currency": "RUB",
                "payment_type": 3,
                "partner_credit": 1,
                "personal_account": 1,
                "_params": {
                    "enable_setting_attributes": 1,
                    "enable_validating_attributes": 1
                }
            }
        ],
        "thirdparty_processing": [
            {
                "service_ids": [
                    723
                ],
                "pipeline": [
                    {"chain_alias": "BaseFilters"},
                    "SetInternal",
                    "ForcePartner",
                    {"if":  {"ir.payment_type":  {"$eq":  "new_promocode"}},
                     "then": {
                         "unit": "ChangeFieldsUnit",
                         "params": {"product_id": 511806}
                     },
                     "else": {
                         "unit": "ProductMappingUnit"
                     }
                     },
                    "ContractUnit",
                    {"if": {"ir.fiscal_nds": {"$in": ["nds_0", "nds_none"]}},
                     "then": {
                          "unit": "PersonalAccountUnit",
                          "params": {"service_code": "YANDEX_SERVICE_WO_VAT"}},
                     "else": {
                         "unit": "PersonalAccountUnit"
                        }
                    },
                    "AmountUnit",
                    "OEBSOrgID"
                ]
            }
        ],
        "close_month": [
            {
                "contract_tag": "health_payments_ph",
                "month_close_generator": "RevPartnerGenerator"
            }
        ]
    }
)
