# -*- coding: utf-8 -*-

import copy
import datetime
from decimal import Decimal as D
from . import steps
from temp.igogor.balance_objects import Products, Services
from balance import balance_steps
from jsonrpc import dispatcher
from .. import common_defaults
from btestlib import utils
from balance import balance_db as db
from balance import balance_api as api
from btestlib.data.partner_contexts import ZAXI_RU_CONTEXT, TAXI_RU_CONTEXT, CLOUD_RU_CONTEXT, \
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, \
    CORP_TAXI_BY_CONTEXT_GENERAL_MIGRATED, DRIVE_B2B_CONTEXT, TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT, \
    TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT, TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT

to_iso = utils.Date.date_to_iso_format

CONTEXT = steps.CONTEXT
COMPLETIONS = D('99.99')
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
END_DT = datetime.datetime(year=2025, month=1, day=1)
START_DT = datetime.datetime(year=2020, month=1, day=1)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
YESTERDAY = datetime.datetime.now() - datetime.timedelta(days=2)
TOMORROW = datetime.datetime.now() + datetime.timedelta(days=1)
FUTURE = datetime.datetime.now() + datetime.timedelta(days=5)
QTY = D('50')


@dispatcher.add_method
def test_request_old_pa_zapravki(login=None):
    context = ZAXI_RU_CONTEXT.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'заправочный договор', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=0, additional_params=additional_contract_params)
    request_id = create_request(None, client_id, contract_id, None)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_taxi(login=None):
    context = TAXI_RU_CONTEXT.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'таксишный договор', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=0, additional_params=additional_contract_params)

    request_id = create_request(Services.TAXI_111.id, client_id, contract_id, None, 510064)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_cloud(login=None):
    context = CLOUD_RU_CONTEXT.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'облачный договор', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=0, additional_params=additional_contract_params)
    request_id = create_request(context.service.id, client_id, contract_id, Products.CLOUD_TEST.id)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_cloud_offer(login=None):
    context = CLOUD_RU_CONTEXT.new(person_params=common_defaults.FIXED_UR_PARAMS, is_offer=True)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'облачная оферта', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=0, additional_params=additional_contract_params)

    request_id = create_request(context.service.id, client_id, contract_id, Products.CLOUD_TEST.id)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_corp_taxi_ru(login=None):
    context = CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'корповый ru договор', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=0, additional_params=additional_contract_params)
    request_id = create_request(context.service.id, client_id, contract_id, None, 509966)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_corp_taxi_kz(login='yb-atst-user-36'):
    context = CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED.new(person_params=common_defaults.FIXED_KZU_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'корповый kz договор', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=0, additional_params=additional_contract_params)
    request_id = create_request(context.service.id, client_id, contract_id, None, 511326)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_corp_taxi_by(login=None):
    context = CORP_TAXI_BY_CONTEXT_GENERAL_MIGRATED.new(person_params=common_defaults.FIXED_BYU_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'корповый by договор', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=0, additional_params=additional_contract_params)
    request_id = create_request(context.service.id, client_id, contract_id, None, 511459)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_drive_b2b_prepay(login=None):
    context = DRIVE_B2B_CONTEXT.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'драйв б2б предоплатный договор', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=0, additional_params=additional_contract_params)
    request_id = create_request(context.service.id, client_id, contract_id, None)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_drive_b2b_postpay(login=None):
    context = DRIVE_B2B_CONTEXT.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'драйв б2б постоплатный договор', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=1, additional_params=additional_contract_params)
    request_id = create_request(context.service.id, client_id, contract_id, None)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_taxi_cameroon_postpay(login=None):
    context = TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT.new(person_params=common_defaults.FIXED_EU_YT_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'mlu договор', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=1, additional_params=additional_contract_params)
    request_id = create_request(context.service.id, client_id, contract_id, None, 511445)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_taxi_congo_postpay(login=None):
    context = TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT.new(person_params=common_defaults.FIXED_EU_YT_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'mlu договор', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=1, additional_params=additional_contract_params)
    request_id = create_request(context.service.id, client_id, contract_id, None, 511445)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_taxi_algeria_postpay(login=None):
    context = TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT.new(person_params=common_defaults.FIXED_EU_YT_PARAMS)
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, context.person_params)
    additional_contract_params = {'external_id': 'mlu договор', 'start_dt': START_DT}
    client_id, person_id, contract_id, contract_eid = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            is_postpay=1, additional_params=additional_contract_params)
    request_id = create_request(context.service.id, client_id, contract_id, None, 511445)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


def create_request(service_id, client_id, contract_id, product_id=None, service_code=None):
    if product_id:
        service_order_id = balance_steps.OrderSteps.next_id(service_id)
        balance_steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                                service_id=service_id)
    else:
        if service_code:
            service_order_id = \
                db.balance().execute('select service_order_id from t_order where contract_id = :contract_id '
                                     'and service_code=:service_code',
                                     {'contract_id': contract_id, 'service_code':service_code})[0]['service_order_id']
        elif service_id:
            service_order_id = db.balance().execute('select service_order_id from t_order where service_id = :service_id '
                                                    'and client_id = :client_id',
                                 {'service_id': service_id, 'client_id': client_id})[0]['service_order_id']
        else:
            service_order_id = api.medium().GetOrdersInfo({'ContractID': contract_id})[0]['ServiceOrderID']
            service_id = db.balance().execute('select service_id from t_order where service_order_id = :service_order_id',
                                     {'service_order_id': service_order_id})[0]['service_id']

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                    'BeginDT': datetime.datetime.now()}]
    request_id = balance_steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': datetime.datetime.now(),
                                                              'InvoiceDesireType': 'charge_note'})
    return request_id
