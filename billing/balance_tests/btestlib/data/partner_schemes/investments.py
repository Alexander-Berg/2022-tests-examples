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
            "ctype": "GENERAL",
            "firm_id": 1,
            "commission": 0,
            "services": {
                "mandatory": [
                    648
                ]
            },
            "currency": "RUB",
            "_params": {
                "enable_setting_attributes": 1,
                "enable_validating_attributes": 1
            }
        }
    ],
    "thirdparty_processing": [
        {
            "service_ids": [
                648
            ],
            "pipeline": [
                {
                    "if": {
                        "ir.payment_type": {
                            "$eq": "yandex_account"
                        },
                        "ir.payment.terminal_payment_method": {
                            "$in": [
                                "yandex_account_topup",
                                "yandex_account_withdraw"
                            ]
                        }
                    },
                    "then": [],
                    "else": "SkipUnit"
                },
                {
                    "if": {
                        "ir.payment_type": {
                            "$eq": "yandex_account"
                        },
                        "ir.payment.terminal_payment_method": {
                            "$eq": "yandex_account_topup"
                        }
                    },
                    "then": [
                        {
                            "chain_alias": "BaseFilters"
                        },
                        "SetInternal",
                        "PlusPartnerUnit",
                        "PlusClientUnit",
                        {
                            "unit": "ContractUnit",
                            "params": {
                                "contract_filters": [
                                    {
                                        "filter": {
                                            "name": "custom_service",
                                            "params": {
                                                "service_id": 703
                                            }
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
                                            "name": "contract_type",
                                            "params": {
                                                "contract_type": "GENERAL"
                                            }
                                        }
                                    },
                                    {
                                        "filter": {
                                            "name": "plus_operator_contract"
                                        }
                                    }
                                ]
                            }
                        },
                        {
                            "unit": "ChangeFieldsUnit",
                            "params": {
                                "payment_type": "yandex_account_topup"
                            }
                        },
                        "AmountUnit",
                        "OEBSOrgID"
                    ]
                },
                {
                    "if": {
                        "ir.payment_type": {
                            "$eq": "yandex_account"
                        },
                        "ir.payment.terminal_payment_method": {
                            "$eq": "yandex_account_withdraw"
                        }
                    },
                    "then": [
                        {
                            "chain_alias": "BaseFilters"
                        },
                        "SetInternal",
                        "PartnerUnit",  # "ForcePartner",
                        "ContractUnit",
                        "PlusClientUnit",
                        {
                            "unit": "ChangeFieldsUnit",
                            "params": {
                                "payment_type": "yandex_account_withdraw"
                            }
                        },
                        "AmountUnit",
                        "OEBSOrgID"
                    ]
                }
            ]
        }
    ],
    "close_month": []
}
)
