# -*- coding: utf-8 -*-
__all__ = []

import functools

import util

import copy

SCHEMES = dict()

register = functools.partial(util.register, SCHEMES)

common = {
    "contracts": [
        {
            "partner_contract": {
                "attributes": "common"
            },
            "firm": 1000,
            "ctype": "GENERAL",
            "services": {
                "mandatory": [
                    1000
                ]
            },
            "currency": "RUB",
            "personal_account": 1,
            "unilateral": 1,
            "_params": {
                "enable_setting_attributes": 1,
                "enable_validating_attributes": 1
            }
        }
    ],
    "close_month": [
        {
            "month_close_generator": "RevPartnerGenerator"
        }
    ]
}


def mk_scheme(tag, is_postpay, is_offer):
    postpay = {'payment_type': 3, 'partner_credit': 1}
    prepay = {'payment_type': 2}
    contract = {'commission': 0}
    offer = {'commission': 9, 'offer_confirmation_type': 'no'}
    sch = copy.deepcopy(common)
    sch['contracts'][0].update(postpay if is_postpay else prepay)
    sch['contracts'][0].update(offer if is_offer else contract)
    sch['contracts'][0].update({'tag': tag})
    sch['close_month'][0].update({'contract_tag': tag})
    return sch


register("postpay_contract", mk_scheme("k50_postpay_contract", is_postpay=True, is_offer=False))
register("prepay_contract", mk_scheme("k50_prepay_contract", is_postpay=False, is_offer=False))
register("postpay_offer", mk_scheme("k50_postpay_offer", is_postpay=True, is_offer=True))
register("prepay_offer", mk_scheme("k50_prepay_offer", is_postpay=False, is_offer=True))
