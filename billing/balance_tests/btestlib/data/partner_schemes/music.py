# -*- coding: utf-8 -*-
__all__ = []

import functools

from btestlib.constants import Firms, Services, ContractPaymentType, ContractSubtype, PersonTypes

import util

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

_tag = 'music_rub'

register(
    "rub",
    {
        "contracts": [
            {
                "partner_contract": {
                    "attributes": "common"
                },
                "firm": Firms.YANDEX_1.id,
                "person": {"type": PersonTypes.PH.code},
                "ctype": ContractSubtype.GENERAL.name,
                "tag": _tag,
                "services": {
                    "mandatory": [
                        Services.MUSIC.id, Services.MUSIC_TARIFFICATOR.id
                    ]
                },
                "currency": "RUB",
                "payment_type": ContractPaymentType.POSTPAY,
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
                    Services.MUSIC.id
                ],
                "pipeline": [
                    {"chain_alias": "BaseFilters"},
                    "PaysysTypeCCUnit",
                    "SetInternal",
                    "ProductMappingUnit",
                    "YandexPlusUnit",
                    "AmountUnit",
                    "RowPaysysCommissionUnit",
                    {
                        "unit": "RewardUnit",
                        "params": {"use_min_reward": False,
                                   "no_reward_paytypes": ["yandex_account"],
                                   "reward_is_amount": True}
                    },
                    "OEBSOrgID"
                ]
            }
        ],
        "close_month": [
            {
                "contract_tag": _tag,
                "month_close_generator": "RevPartnerGenerator",
                "completions_funcs": {
                    str(Services.MUSIC.id): {
                        "name": "thirdparty_completions_new"
                    }
                }
            }
        ]
    }
)
