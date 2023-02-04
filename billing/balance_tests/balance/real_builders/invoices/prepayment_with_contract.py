# -*- coding: utf-8 -*-

import datetime
import copy
from decimal import Decimal as D
from balance import balance_api as api
from jsonrpc import dispatcher
from balance import balance_db as db
import uuid

from . import steps
from balance import balance_steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Currencies, Firms, ContractCommissionType, Services, PersonTypes, Paysyses, InvoiceType, \
    Products, Permissions, User, Users, Export
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, ZAXI_RU_CONTEXT
from .. import common_defaults
from balance.tests.conftest import get_free_user
from dateutil.relativedelta import relativedelta

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
YESTERDAY = datetime.datetime.now() - datetime.timedelta(days=2)
TOMORROW = datetime.datetime.now() + datetime.timedelta(days=1)
FUTURE = datetime.datetime.now() + datetime.timedelta(days=5)
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW
END_DT = datetime.datetime(year=2025, month=1, day=1)
START_DT = datetime.datetime(year=2020, month=1, day=1)

CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                               contract_type=ContractCommissionType.NO_AGENCY)
QTY = D('250')
COMPLETIONS = D('99.99')

@dispatcher.add_method
def test_prepayment_direct_ur_request(login=None):
    context = CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          person_params=common_defaults.FIXED_UR_PARAMS,
                          services=[Services.DIRECT.id],
                          currency=Currencies.RUB)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, _, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_prepayment_direct_byu_request(login=None):
    context = Contexts.DIRECT_BYN_BYU_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          person_params=common_defaults.FIXED_BYU_PARAMS,
                          services=[Services.DIRECT.id],
                          currency=Currencies.BYN,
                          firm=Firms.REKLAMA_BEL_27,
                          product=Products.DIRECT_BYN)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, _, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_prepayment_market_ur_request(login=None):
    context = Contexts.MARKET_RUB_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          services=[Services.MARKET.id],
                          person_params=common_defaults.FIXED_UR_PARAMS,
                          currency=Currencies.RUB,
                          firm=Firms.MARKET_111)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, _, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_prepayment_realty_ur_request(login=None):
    context = Contexts.REALTY_RUB_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          services=[Services.REALTY.id],
                          person_params=common_defaults.FIXED_UR_PARAMS,
                          currency=Currencies.RUB,
                          firm=Firms.VERTICAL_12)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, _, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_prepayment_geo_ur_request(login=None):
    context = Contexts.GEO_RUB_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          services=[Services.GEO.id],
                          person_params=common_defaults.FIXED_UR_PARAMS,
                          currency=Currencies.RUB,
                          firm=Firms.YANDEX_1)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, _, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_prepayment_direct_sw_yt_request(login=None):
    context = Contexts.DIRECT_FISH_SW_CHF_YT_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          services=[Services.DIRECT.id],
                          person_params=common_defaults.FIXED_SW_YT_PARAMS,
                          currency=Currencies.USD,
                          firm=Firms.EUROPE_AG_7,
                          product=Products.DIRECT_USD)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, _, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_prepayment_navi_ur_request(login=None):
    context = Contexts.NAVI_RUB_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          services=[Services.NAVI.id],
                          person_params=common_defaults.FIXED_UR_PARAMS,
                          currency=Currencies.RUB,
                          firm=Firms.YANDEX_1)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, _, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_prepayment_ofd_ur_request(login=None):
    context = Contexts.OFD_RUB_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          services=[Services.OFD.id],
                          person_params=common_defaults.FIXED_UR_PARAMS,
                          currency=Currencies.RUB,
                          firm=Firms.OFD_18)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, _, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_prepayment_direct_kzu_request(login=None):
    context = Contexts.DIRECT_FISH_KZ_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          services=[Services.DIRECT.id],
                          person_params=common_defaults.FIXED_KZU_PARAMS,
                          currency=Currencies.KZT,
                          firm=Firms.KZ_25,
                          product=Products.DIRECT_KZT)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, _, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_prepayment_direct_2_ur_request(login=None):
    context = CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          person_params=common_defaults.FIXED_UR_PARAMS,
                          services=[Services.MEDIA_BANNERS.id, Services.DIRECT.id, Services.BAYAN.id],
                          currency=Currencies.RUB)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, person_id, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    person_params_2 = copy.deepcopy(common_defaults.FIXED_UR_PARAMS)
    person_params_2.update({u'name': u'ООО "Плательщик 2"'})
    person_id_2 = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=person_params_2)

    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id, person_id_2


@dispatcher.add_method
def test_prepayment_direct_ur_ph_request(login=None):
    context = CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          person_params=common_defaults.FIXED_UR_PARAMS,
                          services=[Services.MEDIA_BANNERS.id, Services.DIRECT.id, Services.BAYAN.id],
                          currency=Currencies.RUB)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, _, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, common_defaults.FIXED_PH_PARAMS)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_prepayment_market_request_on_direct_contract(login=None):
    context = Contexts.MARKET_RUB_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                          services=[Services.DIRECT.id],
                          person_params=common_defaults.FIXED_UR_PARAMS,
                          currency=Currencies.RUB,
                          firm=Firms.MARKET_111)
    qty = 50
    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 2,
                       'SERVICES': context.services,
                       'CURRENCY': str(context.currency.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договорчик'
                       }

    client_id, _, _, _, _, request_id, _ = \
        steps.create_base_request(orders_amount=1, qty=qty, contract_params=contract_params,
                                  contract_type=context.contract_type, context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id