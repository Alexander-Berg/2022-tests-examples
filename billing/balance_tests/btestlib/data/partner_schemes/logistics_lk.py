# -*- coding: utf-8 -*-
__all__ = []

import functools

from btestlib.constants import Firms, Services, ContractPaymentType, ContractSubtype, PersonTypes

import util

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

_tag = 'logistics_lk_ph'

register(
    "ph",
    {
        "contracts": [
            {
                "partner_contract": {
                    "attributes": "common"
                },
                "firm": Firms.LOGISTICS_130.id,
                "person": {"type": PersonTypes.PH.code},
                "ctype": ContractSubtype.GENERAL.name,
                "tag": _tag,
                "services": {
                    "mandatory": [
                        Services.LOGISTICS_LK.id
                    ]
                },
                "currency": "RUB",
                "payment_type": ContractPaymentType.PREPAY,
                "personal_account": 1,
                "_params": {
                    "enable_setting_attributes": 1,
                    "enable_validating_attributes": 1
                }
            }
        ],
        "close_month": [
            {
                "contract_tag": _tag,
                "month_close_generator": {
                    "name": "RevPartnerGenerator",
                    "reverse_partners_processor": "ReversePartnersUnifiedAccountOEBSComplsDefaultProcessor",
                },
                "completions_funcs": {
                    str(Services.LOGISTICS_LK.id): {
                        "name": "partner_oebs_completions",
                        "params": {
                            "service_ids": [Services.LOGISTICS_LK.id]
                        }
                    }
                }
            }
        ]
    }
)
