# -*- coding: utf-8 -*-

import datetime
import copy
from decimal import Decimal as D
from balance import balance_api as api

from . import steps
from balance import balance_steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from ..common_defaults import FIXED_PH_PARAMS, FIXED_UR_PARAMS
from btestlib.constants import Currencies, Firms, ContractCommissionType, Services, PersonTypes, Paysyses, InvoiceType
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED
from .. import common_defaults
from jsonrpc import dispatcher

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW

START_DT = datetime.datetime(year=2020, month=1, day=1)

CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                               contract_type=ContractCommissionType.NO_AGENCY)


# физик РФ без КПП, ИНН, не партнер, 1 емейл, 1 счет, не удален
@dispatcher.add_method
def test_ph_with_invoice_person():
    context = CONTEXT.new(person_type=PersonTypes.PH.code, paysys=Paysyses.BANK_PH_RUB)
    client_id = balance_steps.ClientSteps.create({'NAME': common_defaults.CLIENT_NAME})
    person_params = {'is-partner': 0,
                     'fname': u'Прасковья',
                     'lname': u'Кактотамова',
                     'mname': u'Дмитриевна',
                     'kpp': ''}
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, params=person_params)
    steps.create_base_invoice(client_id=client_id, person_id=person_id, context=context)
    return client_id, person_id

@dispatcher.add_method
def test_create_ur_replace_person(client_id):
    person_params = copy.deepcopy(FIXED_UR_PARAMS)
    person_params.update({'name': 'Плательщик на замену'})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=person_params)
    return person_id

@dispatcher.add_method
def test_create_ph_replace_person(client_id):
    person_params = copy.deepcopy(FIXED_PH_PARAMS)
    person_params.update({'fname': 'Заменёна'})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, params=person_params)
    return person_id


# юрик РФ с КПП, ИНН, партнер, несколько email, нет счета, удален
@dispatcher.add_method
def test_partner_ur_no_invoice_hidden_person():
    client_id = balance_steps.ClientSteps.create({'NAME': common_defaults.CLIENT_NAME})
    person_params = common_defaults.FIXED_UR_PARAMS
    person_params.update({'is-partner': 1,
                          'email': 'email1@ya.ru; email2@ya.ru'})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=person_params)
    balance_steps.PersonSteps.hide_person(person_id)
    return client_id, person_id


@dispatcher.add_method
def test_create_client_with_person(person_type, is_partner):
    client_id = balance_steps.ClientSteps.create({'NAME': common_defaults.CLIENT_NAME})
    if person_type == 'ur':
        params = common_defaults.FIXED_UR_PARAMS
    elif person_type == 'ph':
        params = common_defaults.FIXED_PH_PARAMS
    elif person_type == 'yt':
        params = common_defaults.FIXED_YT_PARAMS
    elif person_type == 'byu':
        params = common_defaults.FIXED_BYU_PARAMS
    elif person_type == 'byp':
        params = common_defaults.FIXED_BYP_PARAMS
    elif person_type == 'eu_yt':
        params = common_defaults.FIXED_EU_YT_PARAMS
    elif person_type == 'sw_ur':
        params = common_defaults.FIXED_SW_UR_PARAMS
    elif person_type == 'sw_yt':
        params = common_defaults.FIXED_SW_YT_PARAMS
    elif person_type == 'sw_ytph':
        params = common_defaults.FIXED_SW_YTPH_PARAMS
    elif person_type == 'hk_ytph':
        params = common_defaults.FIXED_HK_YTPH_PARAMS
    elif person_type == 'kzu':
        params = common_defaults.FIXED_KZU_PARAMS
    elif person_type == 'kzp':
        params = common_defaults.FIXED_KZP_PARAMS
    elif person_type == 'il_ur':
        params = common_defaults.FIXED_IL_UR_PARAMS
    elif person_type == 'usp':
        params = common_defaults.FIXED_USP_PARAMS
    elif person_type == 'usu':
        params = common_defaults.FIXED_USU_PARAMS
    elif person_type == 'ytph':
        params = common_defaults.FIXED_YTPH_PARAMS
    else:
        raise ValueError('Unsupported person type %s' % person_type)
    params = params.copy()
    params.update({'is-partner': is_partner})
    person_id = balance_steps.PersonSteps.create(client_id, person_type, params)
    return client_id, person_id


@dispatcher.add_method
def test_create_agency_with_ur():
    agency_id = balance_steps.ClientSteps.create({'NAME': common_defaults.AGENCY_NAME,
                                                  'IS_AGENCY': 1})
    params = common_defaults.FIXED_UR_PARAMS
    person_id = balance_steps.PersonSteps.create(agency_id, PersonTypes.UR.code, params)
    return agency_id, person_id

@dispatcher.add_method
def test_create_request_with_person(person_type='yt_kzp', login=None, context=CONTEXT):
    client_id = balance_steps.ClientSteps.create({'NAME': common_defaults.CLIENT_NAME})
    if person_type == 'ur':
        params = common_defaults.FIXED_UR_PARAMS
    elif person_type == 'ph':
        params = common_defaults.FIXED_PH_PARAMS
    elif person_type == 'yt':
        params = common_defaults.FIXED_YT_PARAMS
    elif person_type == 'byu':
        params = common_defaults.FIXED_BYU_PARAMS
    elif person_type == 'byp':
        params = common_defaults.FIXED_BYP_PARAMS
    elif person_type == 'eu_yt':
        params = common_defaults.FIXED_EU_YT_PARAMS
    elif person_type == 'sw_ur':
        params = common_defaults.FIXED_SW_UR_PARAMS
    elif person_type == 'sw_yt':
        params = common_defaults.FIXED_SW_YT_PARAMS
    elif person_type == 'sw_ytph':
        params = common_defaults.FIXED_SW_YTPH_PARAMS
    elif person_type == 'hk_ytph':
        params = common_defaults.FIXED_HK_YTPH_PARAMS
    elif person_type == 'kzu':
        params = common_defaults.FIXED_KZU_PARAMS
    elif person_type == 'kzp':
        params = common_defaults.FIXED_KZP_PARAMS
    elif person_type == 'il_ur':
        params = common_defaults.FIXED_IL_UR_PARAMS
    elif person_type == 'usp':
        params = common_defaults.FIXED_USP_PARAMS
    elif person_type == 'usu':
        params = common_defaults.FIXED_USU_PARAMS
    elif person_type == 'ytph':
        params = common_defaults.FIXED_YTPH_PARAMS
    elif person_type == 'by_ytph':
        params = common_defaults.FIXED_BY_YTPH_PARAMS
    elif person_type == 'sw_ph':
        params = common_defaults.FIXED_SW_PH_PARAMS
    else:
        raise ValueError('Unsupported person type %s' % person_type)
    params = params.copy()
    person_id = balance_steps.PersonSteps.create(client_id, person_type, params)
    _, _, _, _, _, request_id = steps.create_base_request(client_id=client_id, person_id=person_id, context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, request_id


@dispatcher.add_method
def test_person_with_edo(person_type, is_partner=0):
    client_id = balance_steps.ClientSteps.create()
    person_params = {'is-partner': is_partner}
    if person_type == PersonTypes.UR.code:
        person_params.update(common_defaults.FIXED_UR_PARAMS)
    elif person_type == PersonTypes.PH.code:
        person_params.update(common_defaults.FIXED_PH_PARAMS)
    else:
        return 'No EDO'

    person_id = balance_steps.PersonSteps.create(client_id, person_type, person_params)
    balance_steps.PersonSteps.accept_edo(person_id, Firms.YANDEX_1.id, START_DT)
    return {'client_id': client_id, 'person_id': person_id}
