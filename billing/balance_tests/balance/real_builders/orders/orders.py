# -*- coding: utf-8 -*-

import copy
import json
import datetime
from decimal import Decimal as D
from . import steps
from temp.igogor.balance_objects import Contexts, ContractCommissionType, PersonTypes, Currencies, Paysyses, Products, Services
from btestlib.constants import ContractCommissionType, Currencies, Services, Collateral
from btestlib.constants import Firms, PersonTypes, ContractPaymentType
from balance import balance_steps
from jsonrpc import dispatcher
from .. import common_defaults
from btestlib import utils
from balance import balance_db as db
from balance import balance_api as api
from btestlib.data.partner_contexts import ZAXI_RU_CONTEXT, TAXI_RU_CONTEXT


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

FIX_DISCOUNT_POLICY = 8
DISCOUNT_PCT = 11


@dispatcher.add_method
def test_client_empty_order(login=None, fixed_dt=False):
    client_id, _, _, _, service_id, service_order_id, order_id, _, request_id = steps.create_base_request()
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    if fixed_dt:
        db.balance().execute("update t_order set dt = :dt where id = :id", {'dt': datetime.datetime(2021,01,01),
                                                                            'id': order_id})
    return client_id, service_id, service_order_id, order_id, request_id


@dispatcher.add_method
def test_request_market_with_ph(login=None):
    context = Contexts.MARKET_RUB_CONTEXT.new(person_type=PersonTypes.PH, paysys=Paysyses.BANK_PH_RUB,
                                               person_params=common_defaults.FIXED_PH_PARAMS)
    client_id, _, _, _, service_id, service_order_id, order_id, _, request_id = steps.create_base_request(context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_request_market_with_ur(login=None):
    context = Contexts.MARKET_RUB_CONTEXT.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, _, _, service_id, service_order_id, order_id, _, request_id = steps.create_base_request(context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_request_toloka_with_usu(login=None):
    context = Contexts.TOLOKA_FISH_USD_CONTEXT.new(person_type=PersonTypes.USU,
                                                   person_params=common_defaults.FIXED_USU_PARAMS)
    client_id, _, _, _, service_id, service_order_id, order_id, _, request_id = steps.create_base_request(context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_request_practicum_with_usp(login=None):
    context = Contexts.PRACTICUM_US_YT_UR.new(person_type=PersonTypes.USP,
                                              person_params=common_defaults.FIXED_USP_PARAMS,
                                              product=Products.PRACTICUM_511328)
    client_id, _, _, _, service_id, service_order_id, order_id, _, request_id = steps.create_base_request(context=context)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id


@dispatcher.add_method
def test_client_empty_order_with_ph(login=None):
    client_id = balance_steps.ClientSteps.create({'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, params=common_defaults.FIXED_PH_PARAMS)
    _, _, _, _, service_id, service_order_id, order_id, _, request_id = \
        steps.create_base_request(client_id=client_id, person_id=person_id)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, service_id, service_order_id, order_id, request_id


@dispatcher.add_method
def test_client_3_orders_with_ph(login=None):
    client_id = balance_steps.ClientSteps.create({'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, params=common_defaults.FIXED_PH_PARAMS)
    _, _, _, _, service_id, service_order_id, order_id, _, request_id = \
        steps.create_base_request(client_id=client_id, person_id=person_id, order_amount=3)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, service_id, service_order_id, order_id, request_id


@dispatcher.add_method
def test_client_3_orders_with_ur(login=None):
    client_id = balance_steps.ClientSteps.create({'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=common_defaults.FIXED_UR_PARAMS)
    _, _, _, _, service_id, service_order_id, order_id, _, request_id = \
        steps.create_base_request(client_id=client_id, person_id=person_id, order_amount=3)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, service_id, service_order_id, order_id, request_id


@dispatcher.add_method
def test_request_direct_with_sw_ur(login=None):
    client_id = balance_steps.ClientSteps.create({'NAME': common_defaults.CLIENT_NAME})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.SW_UR.code, params=common_defaults.FIXED_SW_UR_PARAMS)
    _, _, _, _, service_id, service_order_id, order_id, _, request_id = \
        steps.create_base_request(client_id=client_id, person_id=person_id)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, service_id, service_order_id, order_id, request_id


@dispatcher.add_method
def test_client_empty_order_no_person(login=None):
    client_id, _, _, _, service_id, service_order_id, order_id, _, request_id = steps.create_base_request(with_person=False)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, service_id, service_order_id, order_id, request_id



@dispatcher.add_method
def test_client_empty_order_three_persons(login=None):
    client_id, _, _, _, service_id, service_order_id, order_id, _, request_id = steps.create_base_request(with_person=False)
    person_params_1 = copy.deepcopy(common_defaults.FIXED_UR_PARAMS)
    person_params_1.update({u'name': u'ООО Плательщик 1'})
    person_id_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=person_params_1)
    person_params_2 = copy.deepcopy(common_defaults.FIXED_UR_PARAMS)
    person_params_2.update({u'name': u'ООО Плательщик 2'})
    person_id_2 = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=person_params_2)
    person_params_3 = copy.deepcopy(common_defaults.FIXED_UR_PARAMS)
    person_params_3.update({u'name': u'ООО Плательщик 3'})
    person_id_3 = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=person_params_3)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, service_id, service_order_id, order_id, request_id, person_id_1, person_id_2, person_id_3


@dispatcher.add_method
def test_client_campaigns_turned_off_invoice_order():
    client_id, _, person_id, _, service_id, service_order_id, order_id, _, request_id = steps.create_base_request()
    invoice_id = steps.create_base_invoice(request_id, person_id, turn_on=False, need_campaigns=True,
                                           service_order_id=service_order_id)
    return client_id, service_id, service_order_id, invoice_id, order_id


@dispatcher.add_method
def test_client_campaigns_order():
    client_id, _, person_id, _, service_id, service_order_id, order_id, _, request_id = steps.create_base_request()
    invoice_id = steps.create_base_invoice(request_id, person_id, turn_on=True, need_campaigns=True,
                                           service_order_id=service_order_id)
    return client_id, service_id, service_order_id, invoice_id, order_id


@dispatcher.add_method
def test_agency_empty_order():
    _, agency_id , _, _, service_id, service_order_id, order_id, _, _ = steps.create_base_request(need_agency=1)
    return agency_id, service_id, service_order_id, order_id


@dispatcher.add_method
def test_agency_multiple_clients_order(login=None):
    client_id_1, agency_id , _, _, service_id, service_order_id_1, order_id_1, _, _ = \
        steps.create_base_request(need_agency=1, client_name='client name 1')
    db.balance().execute("update t_order set dt = :dt where id = :id", {'dt': datetime.datetime(2021,03,01), 'id': order_id_1})
    client_id_2, agency_id , _, _, service_id, service_order_id_2, order_id_2, _, _ = \
        steps.create_base_request(agency_id=agency_id, client_name='client name 2')
    db.balance().execute("update t_order set dt = :dt where id = :id", {'dt': datetime.datetime(2021,03,02), 'id': order_id_2})

    data = balance_steps.api.medium().GetPassportByLogin(0, 'yb-orders-subclient')
    balance_steps.ClientSteps.link(client_id_2, 'yb-orders-subclient')
    balance_steps.ClientSteps.link(agency_id, login)
    return agency_id, service_id, service_order_id_1, service_order_id_2, order_id_1, order_id_2

# заказ на маркет без договора на фл с ограничением способов оплаты при больших суммах
@dispatcher.add_method
def test_client_ph_market_order_max_sum():
    client_id = balance_steps.ClientSteps.create()
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code)
    service_id=11
    orders_list = []
    service_order_id = balance_steps.OrderSteps.next_id(service_id)
    balance_steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=512943)
    orders_list.append(
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 10000000, 'BeginDT': datetime.datetime.now()})
    request_id = balance_steps.RequestSteps.create(client_id, orders_list, {'InvoiceDesireDT': datetime.datetime.now()})
    return client_id, request_id

# создаем два заказа, чтобы убедиться в фильтрации по сервисам
@dispatcher.add_method
def test_client_market_and_direct_order():
    context = Contexts.MARKET_RUB_CONTEXT.new(firm=Firms.MARKET_111,
                                              person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, person_id, _, market_service_id, market_service_order_id, _, _, _ = \
        steps.create_base_request(context=context)
    _, _, _, _, direct_service_id, direct_service_order_id, _, _, _ = steps.\
        create_base_request(client_id=client_id, person_id=person_id, context=CONTEXT)
    return client_id, market_service_id, market_service_order_id, direct_service_id, direct_service_order_id


# создаем два заказа со счетами, один включен, другой нет, для проверки фильтра "Включение"
@dispatcher.add_method
def test_client_turned_on_invoice_order():
    client_id, _, person_id, _, service_id, service_order_id_1, _, _, request_id_1 = steps.create_base_request()
    _, _, _, _, _, service_order_id_2, _, _, request_id_2 = steps.create_base_request(client_id=client_id,
                                                                                      person_id=person_id)
    invoice_id_1 = steps.create_base_invoice(request_id_1, person_id, turn_on=True)
    invoice_id_2 = steps.create_base_invoice(request_id_2, person_id)
    return client_id, service_id, service_order_id_1, service_order_id_2, invoice_id_1, invoice_id_2


# заказ без реквеста
@dispatcher.add_method
def test_agency_no_request_order():
    context = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, agency_id, _, _, service_id, service_order_id, order_id, _, _ = \
        steps.create_base_request(no_request=True, need_agency=1, context=context)
    db.balance().execute('''UPDATE t_order SET memo = 'Текст заметки' WHERE id =:order_id''', {'order_id': order_id})
    return client_id, service_id, service_order_id, order_id, agency_id


# заказ с зачисленным и открученным, без акта
@dispatcher.add_method
def test_client_consume_compl_and_empty_order():
    client_id, _, person_id, _, service_id, service_order_id_1, order_id_1, _, request_id_1 = \
        steps.create_base_request()
    _, _, _, _, _, service_order_id_2, order_id_2, _, _ = \
        steps.create_base_request(client_id=client_id, person_id=person_id)
    invoice_id = steps.create_base_invoice(request_id_1, person_id, turn_on=True)
    balance_steps.CampaignsSteps.do_campaigns(CONTEXT.product.service.id, service_order_id_1,
                                              {'Bucks': COMPLETIONS, 'Money': 0}, 0, COMPLETIONS_DT)
    return client_id, service_id, service_order_id_1, order_id_1, invoice_id


# заказ с зачисленным и открученным, с актом, фиксированная скидка
@dispatcher.add_method
def test_client_fix_discount():
    context = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(contract_type=ContractCommissionType.SW_OPT_CLIENT,
                                                      person_params=common_defaults.FIXED_SW_UR_PARAMS)
    contract_params = {'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                       'DISCOUNT_POLICY_TYPE': FIX_DISCOUNT_POLICY,
                       'CONTRACT_DISCOUNT': str(DISCOUNT_PCT),
                       'DISCOUNT_FIXED': 20,
                       'CURRENCY': 978
                       }

    client_id, _, person_id, contract_id, service_id, service_order_id, order_id, _, request_id = \
        steps.create_base_request(contract_params=contract_params, context=context)
    invoice_id = steps.create_base_invoice(request_id, person_id, contract_id=contract_id,
                                           turn_on=True, context=context)
    balance_steps.CampaignsSteps.do_campaigns(CONTEXT.product.service.id, service_order_id,
                                              {'Bucks': COMPLETIONS, 'Money': 0}, 0, COMPLETIONS_DT)
    balance_steps.ActsSteps.generate(client_id)
    return client_id, service_id, service_order_id, order_id


# заказ с зачисленным и открученным, с актом, фиксированная скидка
@dispatcher.add_method
def test_client_part_compl(login=None):
    client_id, _, person_id, _, service_id, service_order_id, order_id, _, request_id_1 = \
        steps.create_base_request()
    invoice_id = steps.create_base_invoice(request_id_1, person_id, turn_on=True)
    balance_steps.CampaignsSteps.do_campaigns(CONTEXT.product.service.id, service_order_id,
                                              {'Bucks': COMPLETIONS, 'Money': 0}, 0, COMPLETIONS_DT)
    balance_steps.ActsSteps.generate(client_id)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, service_id, service_order_id, order_id


# заказ с большим количеством недовыставленных счетов
@dispatcher.add_method
def test_requests_pagination():
    client_id, _, _, _, _, _, order_id, order_info, _ = steps.create_base_request()
    for i in range(30):
        balance_steps.RequestSteps.create(client_id, [order_info], additional_params=dict(InvoiceDesireDT=INVOICE_DT))
    return client_id, order_id


# заказ с большим количеством неоплаченных счетов
def test_unpaid_invoices_pagination():
    client_id, _, person_id, _, _, _, order_id, order_info, _ = steps.create_base_request()
    for i in range(30):
        request_id = balance_steps.RequestSteps.create(client_id, [order_info],
                                                       additional_params=dict(InvoiceDesireDT=INVOICE_DT))
        invoice_id = steps.create_base_invoice(request_id, person_id)
    return client_id, order_id


# заказ без названия и менеджера
@dispatcher.add_method
def test_no_manager_no_name_order():
    client_id, _, person_id, _, _, _, order_id, order_info, _ = steps.create_base_request(order_params={'Text': None},
                                                                                          no_manager=True,
                                                                                          no_request=True)
    return client_id, order_id


# заказ с агентством без договора с сублиентом-нерезом
@dispatcher.add_method
def test_request_for_agency_no_contract_with_nonrez():
    context = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(contract_type=ContractCommissionType.PR_AGENCY,
                                                    person_params=common_defaults.FIXED_UR_PARAMS,
                                                    firm=Firms.YANDEX_1)
    agency_id = balance_steps.ClientSteps.create({'NAME': common_defaults.AGENCY_NAME, 'IS_AGENCY': 1})
    subclient_id = balance_steps.ClientSteps.create_sub_client_non_resident(Currencies.USD.char_code,
                                                                            params={'NAME': u'Нерезидент'})
    person_id = balance_steps.PersonSteps.create(agency_id, context.person_type.code, context.person_params)

    _, _, _, _, _, _, _, _, request_id = \
        steps.create_base_request(client_id=subclient_id, agency_id=agency_id, person_id=person_id,
                                  need_agency=True, qty=D('50'), context=context)
    return agency_id, request_id

# заказ с агентством без договора
@dispatcher.add_method
def test_request_for_agency_no_contract():
    context = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(contract_type=ContractCommissionType.PR_AGENCY,
                                                    person_params=common_defaults.FIXED_UR_PARAMS,
                                                    firm=Firms.YANDEX_1)
    subclient_id = balance_steps.ClientSteps.create({'NAME': u'Я субклиентище'})

    _, agency_id, _, _, _, _, _, _, request_id = \
        steps.create_base_request(client_id=subclient_id, need_agency=True, qty=D('50'), context=context)

    return agency_id, request_id

# заказ с агентством c договором
@dispatcher.add_method
def test_request_for_agency():
    context = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(contract_type=ContractCommissionType.PR_AGENCY,
                                                    person_params=common_defaults.FIXED_UR_PARAMS,
                                                    firm=Firms.YANDEX_1)
    agency_id = balance_steps.ClientSteps.create({'NAME': common_defaults.AGENCY_NAME, 'IS_AGENCY': 1})
    person_id = balance_steps.PersonSteps.create(agency_id, context.person_type.code, context.person_params)
    subclient_id = balance_steps.ClientSteps.create({'NAME': u'Я не агентство, честно'})
    contract_id, _ = balance_steps.ContractSteps.create_contract('opt_agency',
                                                                 {'FINISH_DT': to_iso(END_DT),
                                                                  'IS_SIGNED': to_iso(START_DT),
                                                                  'DT': to_iso(START_DT),
                                                                  'PAYMENT_TYPE': '3',
                                                                  'DISCOUNT_POLICY_TYPE': 8,
                                                                  'PAYMENT_TERM': '10',
                                                                  'CREDIT_TYPE': '2',
                                                                  'CREDIT_LIMIT_SINGLE': '10000',
                                                                  'SERVICES': [context.service.id],
                                                                  'CURRENCY': str(Currencies.RUB.num_code),
                                                                  'FIRM': context.firm.id,
                                                                  'EXTERNAL_ID': 'договор на агентство',
                                                                  'CLIENT_ID': agency_id,
                                                                  'PERSON_ID': person_id,
                                                                  })
    _, agency_id, _, _, _, _, _, _, request_id = \
    steps.create_base_request(agency_id=agency_id, client_id=subclient_id, qty=D('50'), context=context)

    return agency_id, request_id


# заказ с агентством c договором с субклиентом-нерезом
@dispatcher.add_method
def test_request_for_agency_with_nonrez():
    context = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(contract_type=ContractCommissionType.PR_AGENCY,
                                                    person_params=common_defaults.FIXED_UR_PARAMS,
                                                    firm=Firms.YANDEX_1)
    agency_id = balance_steps.ClientSteps.create({'NAME': common_defaults.AGENCY_NAME, 'IS_AGENCY': 1})
    person_id = balance_steps.PersonSteps.create(agency_id, context.person_type.code, context.person_params)
    subclient_id = balance_steps.ClientSteps.create({'NAME': u'Ya subclient, chestno'})
    query = "UPDATE t_client SET FULLNAME = 'Subclient_nerez', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 WHERE ID = :client_id"
    query_params = {'client_id': subclient_id}
    db.balance().execute(query, query_params)
    contract_id, _ = balance_steps.ContractSteps.create_contract('comm_post',
                                                                 {'FINISH_DT': to_iso(END_DT),
                                                                  'IS_SIGNED': to_iso(START_DT),
                                                                  'DT': to_iso(START_DT),
                                                                  'SERVICES': [context.service.id],
                                                                  'CURRENCY': str(Currencies.RUB.num_code),
                                                                  'FIRM': context.firm.id,
                                                                  'EXTERNAL_ID': 'договор с субклиентом-нерезом',
                                                                  'CLIENT_ID': agency_id,
                                                                  'PERSON_ID': person_id,
                                                                  'NON_RESIDENT_CLIENTS': 1,
                                                                  'REPAYMENT_ON_CONSUME': 0,
                                                                  'PERSONAL_ACCOUNT': 1,
                                                                  'LIFT_CREDIT_ON_PAYMENT': 1,
                                                                  'PERSONAL_ACCOUNT_FICTIVE': 1
                                                                  })
    _, agency_id, _, _, _, _, _, _, request_id = \
    steps.create_base_request(agency_id=agency_id, client_id=subclient_id, qty=D('50'), context=context)

    return agency_id, request_id


#заказ на агентство с субклиентом с индивидуальным лимитом
@dispatcher.add_method
def test_request_for_agency_with_ind_limits():
    context = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(contract_type=ContractCommissionType.PR_AGENCY,
                                                    person_params=common_defaults.FIXED_UR_PARAMS,
                                                    firm=Firms.YANDEX_1)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(START_DT+utils.relativedelta(years=5)),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': D('5700'),
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'договор с допами на инд.лимиты',
                       'SERVICES': [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                                    Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                                    Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
                       }

    agency_id, person_id, contract_id, contract_eid = steps.create_base_contract(contract_params, context=context,
                                                                                 is_agency=1)

    # создаем первого клиента и допник на индивидуальный кредитный лимит
    subclient_id_1 = balance_steps.ClientSteps.create(params={'NAME': u'Я субклиент'})
    credit_limits = [{"id": "1",
                      "num": "{0}".format(subclient_id_1),
                      "client": "{0}".format(subclient_id_1),
                      "client_limit": "1000",
                      "client_payment_term": "45",
                      "client_credit_type": "1000",
                      "client_limit_currency": "RUR"}]

    collateral_params = {'CONTRACT2_ID': contract_id,
                         'CLIENT_LIMITS': str(json.dumps(credit_limits)),
                         'DT': utils.Date.date_to_iso_format(START_DT+utils.relativedelta(days=2)),
                         'IS_SIGNED': utils.Date.date_to_iso_format(START_DT+utils.relativedelta(days=2))}
    balance_steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, collateral_params)

    # создаем второго клиента и допник на индивидуальный кредитный лимит с другой суммой
    subclient_id_2 = balance_steps.ClientSteps.create(params={'NAME': u'Я субклиент'})
    credit_limits = [{"id": "1",
                      "num": "{0}".format(subclient_id_2),
                      "client": "{0}".format(subclient_id_2),
                      "client_limit": "1500",
                      "client_payment_term": "45",
                      "client_credit_type": "1000",
                      "client_limit_currency": "RUR"}]

    collateral_params = {'CONTRACT2_ID': contract_id,
                         'CLIENT_LIMITS': str(json.dumps(credit_limits)),
                         'DT': utils.Date.date_to_iso_format(START_DT + utils.relativedelta(days=1)),
                         'IS_SIGNED': utils.Date.date_to_iso_format(START_DT + utils.relativedelta(days=1))}
    balance_steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, collateral_params)

    request_id_1 = steps.create_base_request(agency_id=agency_id, client_id=subclient_id_1, qty=D('50'), context=context)
    request_id_2 = steps.create_base_request(agency_id=agency_id, client_id=subclient_id_2, qty=D('50'), context=context)

    subclient_id = balance_steps.ClientSteps.create({'NAME': u'Я не агентство, честно'})
    request_id_3 = steps.create_base_request(agency_id=agency_id, client_id=subclient_id, qty=D('50'), context=context)

    return agency_id, request_id_1[-1], request_id_2[-1], request_id_3[-1]


# заказ на клиента с договором
@dispatcher.add_method
def test_request_with_contract_client(login=None):
    context = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(contract_type=ContractCommissionType.NO_AGENCY,
                                                    person_params=common_defaults.FIXED_UR_PARAMS,
                                                    firm=Firms.YANDEX_1)
    contract_params = {'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'DT': to_iso(START_DT),
                       'PAYMENT_TYPE': '2',
                       'SERVICES': [context.service.id],
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'йа договорчик',
                       }
    client_id, _, person_id, contract_id, _, _, _, _, request_id = \
        steps.create_base_request(contract_params=contract_params, context=context)
    person_id_new = balance_steps.PersonSteps.create(client_id, context.person_type.code, params=common_defaults.FIXED_UR_PARAMS)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id, person_id, contract_id, person_id_new



# заказ с новым ЛС
@dispatcher.add_method
def test_request_fictive_pa(login=None):
    context = CONTEXT.new(person_type=PersonTypes.PH,
                          paysys=Paysyses.BANK_PH_RUB,
                          person_params=common_defaults.FIXED_PH_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'IS_FIXED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'SERVICES': [context.service.id],
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'UNILATERAL': 1,
                       'EXTERNAL_ID': 'кредитный договорушка'
                       }

    client_id, _, person_id, contract_id, _, _, _, _, request_id = \
        steps.create_base_request(contract_params=contract_params, context=context)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id

@dispatcher.add_method
def test_request_fictive_pa_ur(login=None):
    context = CONTEXT.new(person_type=PersonTypes.UR,
                          paysys=Paysyses.BANK_UR_RUB,
                          person_params=common_defaults.FIXED_UR_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'IS_FIXED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'SERVICES': [context.service.id],
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'UNILATERAL': 1,
                       'EXTERNAL_ID': 'кредитный договорушка'
                       }

    client_id, _, person_id, contract_id, _, _, _, _, request_id = \
        steps.create_base_request(contract_params=contract_params, context=context)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


# заказ со старым ЛС
@dispatcher.add_method
def test_request_old_pa_Yandex_Inc(login=None):
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_INC_4,
                                                   person_type=PersonTypes.USU,
                                                   paysys=Paysyses.BANK_US_UR_USD,
                                                   contract_type=ContractCommissionType.USA_OPT_CLIENT,
                                                   person_params=common_defaults.FIXED_USU_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': D('123123'),
                       'SERVICES': [context.service.id],
                       'PERSONAL_ACCOUNT': 1,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.USD.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'контрактушка'
                       }

    client_id, _, person_id, contract_id, _, _, _, _, request_id = \
        steps.create_base_request(contract_params=contract_params, context=context)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_old_pa_Yandex_Europe_AG(login=None):
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7,
                                                   person_type=PersonTypes.SW_UR,
                                                   paysys=Paysyses.BANK_SW_UR_USD,
                                                   contract_type=ContractCommissionType.SW_OPT_CLIENT,
                                                   person_params=common_defaults.FIXED_SW_UR_PARAMS)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(END_DT),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': D('123123'),
                       'SERVICES': [context.service.id],
                       'PERSONAL_ACCOUNT': 1,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.USD.num_code),
                       'FIRM': context.firm.id,
                       'EXTERNAL_ID': 'контрактушка'
                       }

    client_id, _, person_id, contract_id, _, _, _, _, request_id = \
        steps.create_base_request(contract_params=contract_params, context=context)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


@dispatcher.add_method
def test_request_ph_with_overdraft(login=None):
    context = CONTEXT.new(person_type=PersonTypes.PH,
                          paysys=Paysyses.BANK_PH_RUB,
                          person_params=common_defaults.FIXED_PH_PARAMS)
    client_id, _, person_id, contract_id, _, _, _, _, request_id = \
        steps.create_base_request(context=context)
    balance_steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, limit=123123.12)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id

@dispatcher.add_method
def test_request_ur_with_overdraft(login=None):
    context = CONTEXT.new(person_type=PersonTypes.UR,
                          paysys=Paysyses.BANK_UR_RUB,
                          person_params=common_defaults.FIXED_UR_PARAMS)
    client_id, _, person_id, contract_id, _, _, _, _, request_id = \
        steps.create_base_request(context=context)
    balance_steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, limit=123123.12)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, request_id

# превышение лимита овердрафта
@dispatcher.add_method
def test_request_ph_with_overdraft_under_limit():
    client_id, _, person_id, contract_id, _, _, _, _, request_id = steps.create_base_request(qty=10000)
    balance_steps.OverdraftSteps.set_force_overdraft(client_id, CONTEXT.service.id, limit=1000)
    return client_id, request_id


# Реквест с фиксированной агентской скидкой (ДОДЕЛАТЬ!!!)
@dispatcher.add_method
def test_request_fix_agency_discount(login=None):
    client_id = balance_steps.ClientSteps.create({'NAME': common_defaults.AGENCY_NAME, 'IS_AGENCY': 1})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.SW_YT.code, common_defaults.FIXED_SW_YT_PARAMS)
    service_id = 70
    contract_id, _ = balance_steps.ContractSteps.create_contract('shv_agent',
                                                         {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                          'CONTRACT_DISCOUNT': 19.26,
                                                          'MANAGER_UID': 98700241,
                                                          'MANAGER_CODE': 20453,
                                                          'DISCOUNT_POLICY_TYPE': 8,
                                                          'DT': to_iso(START_DT),
                                                          'FINISH_DT': to_iso(END_DT),
                                                          'IS_SIGNED': to_iso(START_DT),
                                                          'SERVICES': [service_id],
                                                          'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 25,
                                                          'FIRM': 7,
                                                          'CURRENCY': 840,
                                                          'PAYMENT_TYPE': 2,
                                                          'COMMISSION': 23,
                                                          'EXTERNAL_ID': 'договорчик со скидкой'
                                                          })
    orders_list = []
    service_order_id = balance_steps.OrderSteps.next_id(service_id)
    balance_steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=508705)
    orders_list.append(
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100000, 'BeginDT': datetime.datetime.now()})
    request_id = balance_steps.RequestSteps.create(client_id, orders_list,
                                           {'InvoiceDesireDT': datetime.datetime.now()})
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, contract_id, request_id


# Реквест с клиентской разовой скидкой
@dispatcher.add_method
def test_request_onetime_client_discount(login=None):
    client_id = balance_steps.ClientSteps.create()
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, common_defaults.FIXED_PH_PARAMS)
    service_id = 37
    orders_list = []
    service_order_id = balance_steps.OrderSteps.next_id(service_id)
    balance_steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=502987)
    orders_list.append(
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 10})
    request_id = balance_steps.RequestSteps.create(client_id, orders_list,)
    invoice_id, _, _ = balance_steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_PH_RUB.id)
    balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id, request_id, invoice_id

#заказ с qty=100000, проверка ограничения способов оплаты
@dispatcher.add_method
def test_client_order_max_sum(login=None):
    client_id, _, _, _, service_id, service_order_id, order_id, _, request_id = steps.create_base_request(qty=100000)
    if login:
        balance_steps.ClientSteps.link(client_id, login)

    return client_id, service_id, service_order_id, order_id, request_id
