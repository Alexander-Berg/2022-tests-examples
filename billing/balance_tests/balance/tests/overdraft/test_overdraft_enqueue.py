# -*- coding: utf-8 -*-

import datetime

import hamcrest
import pytest
from enum import Enum

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.constants import Firms, Services, PersonTypes, Paysyses, Products, ContractPaymentType, \
    ContractCreditType, Regions, Currencies, Export
from balance.features import Features

DT_NOW = datetime.datetime.now()
DT_6_MONTHS_AGO = utils.Date.shift_date(utils.Date.first_day_of_month(DT_NOW), months=-6, days=-1)
DT_5_MONTHS_AGO = utils.Date.shift_date(utils.Date.first_day_of_month(DT_NOW), months=-5)
DT_DAY_AGO = utils.Date.to_iso(utils.Date.shift_date(DT_NOW, days=-1))
DT_BEFORE_2007 = utils.Date.shift_date(datetime.datetime.now().replace(year=2007, day=1, month=1), days=-1)
DT_BEFORE_2007_iso = utils.Date.to_iso(DT_BEFORE_2007)

HALF_YEAR_AFTER_NOW_ISO = utils.Date.to_iso(utils.Date.shift_date(DT_NOW, months=6))
HALF_YEAR_BEFORE_NOW_ISO = utils.Date.to_iso(utils.Date.shift_date(DT_NOW, months=-6))
YEAR_BEFORE_NOW_ISO = utils.Date.to_iso(utils.Date.shift_date(DT_NOW, years=-1))

PERSON_PAYSYS_MAP = {PersonTypes.PH: Paysyses.BANK_PH_RUB,
                     PersonTypes.USU: Paysyses.BANK_US_UR_USD}

SERVICE_PRODUCT_MAP = {Services.DIRECT: Products.DIRECT_FISH,
                       Services.MARKET: Products.MARKET}

SERVICE_FIRM_OVERDRAFT_AVAILABLE = {Services.DIRECT: [Firms.YANDEX_1],
                                    Services.MARKET: [Firms.MARKET_111, Firms.YANDEX_1]}

OVERDRAFT_LIMIT = 1000
QTY = 10
PAYMENT_SUM = 300
CONTRACT_TYPE_NO_AGENCY = 'no_agency'

ERROR_MESSAGE_HEADER = 'Decline in overdraft for service_id={service_id} due reason "{message}"'


class ErrorMessage(Enum):
    CLIENT_CHECK = 'client has non CLIENT categories: AGENCY'
    NOT_ACCEPTABLE_REGION_CHECK = 'Client doesn\'t have resident payers in acceptable regions'
    BAN_CLIENT_CHECK = 'Banned or suspected client {client_id}'
    NO_RESIDENT_PAYERS = 'Client doesn\'t have resident payers'
    POSTPAY_CONTRACT = 'Postpay contract {contract_id} for service_id={service_id}'
    MANY_REGIONS_CHECK = 'Client has payers in more than one region: {region_ids}'
    RESIDENT_CHECK = 'Client has nonresident payers'
    WRONG_REGION_CHECK = 'Client has payers in forbidden region'
    ACT_CHECK = 'no act in needed previous months'
    INVOICE_CHECK = 'Expired pay invoices: {invoice_ids}'


DEFAULT_EXPORT_PARAMS = {'classname': 'Client',
                         'input': 'None',
                         'type': 'OVERDRAFT',
                         'priority': '0',
                         'state': '1',
                         'error': 'None',
                         'reason': 'None'}


def format_message(message_type, service_id, client_id=None, contract_id=None, region_ids=None, invoice_ids=None):
    error_tail = message_type.value.format(service_id=service_id, client_id=client_id,
                                           contract_id=contract_id, region_ids=region_ids,
                                           invoice_ids=invoice_ids)
    result_message = ERROR_MESSAGE_HEADER.format(service_id=service_id, message=error_tail)
    return result_message


def create_invoice_with_act(service, client_id, product, person_id, paysys, dt, contract_id=None, payment_sum=None,
                            completion_qty=None, count_of_acts=1):
    with reporter.step(u"Создаем счет с актами"):
        ORDER_QTY = QTY
        COMPLETION_QTY = completion_qty if completion_qty is not None else ORDER_QTY

        service_order_id = steps.OrderSteps.next_id(service_id=service.id)
        steps.OrderSteps.create(client_id=client_id, product_id=product.id, service_id=service.id,
                                service_order_id=service_order_id)
        orders_list = [
            {'ServiceID': service.id, 'ServiceOrderID': service_order_id, 'Qty': ORDER_QTY, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(InvoiceDesireDT=dt))
        credit = 1 if contract_id is not None else 0

        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys.id,
                                                     credit=credit, contract_id=contract_id, overdraft=0)
        steps.InvoiceSteps.pay(invoice_id, payment_sum=payment_sum)
        acts_list = []
        for x in range(count_of_acts):
            steps.CampaignsSteps.do_campaigns(service.id, service_order_id, {product.type.code: COMPLETION_QTY}, 0, dt)
            act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
            acts_list.append(act_id)
            COMPLETION_QTY += COMPLETION_QTY * 0.5
    return invoice_id, acts_list


def preset_overdraft(client_id):
    with reporter.step(u"Устанавливает клиенту {} ненулевой овердрафт по всем парам сервис/фирма".format(client_id)):
        for service, firms in SERVICE_FIRM_OVERDRAFT_AVAILABLE.iteritems():
            for firm in firms:
                # todo-blubimov здесь ничего не ломается из-за того что мы для украины передаем RUR ?
                steps.OverdraftSteps.set_force_overdraft(client_id=client_id, service_id=service.id, limit=1000,
                                                         firm_id=firm.id, currency=Currencies.RUB.char_code)


def check_export_result(client_id, error_message_type, is_error_occurred, service, contract_id=None, region_ids=None,
                        invoice_ids=None):
    export = steps.ExportSteps.get_export_data(client_id, Export.Classname.CLIENT, Export.Type.OVERDRAFT)
    utils.check_that(export, hamcrest.has_entries(DEFAULT_EXPORT_PARAMS))
    export_output = steps.ExportSteps.get_export_output(client_id, Export.Classname.CLIENT, Export.Type.OVERDRAFT)
    reporter.log('export_output: {}'.format(export_output))
    expected_result_message = format_message(error_message_type, service_id=service.id, client_id=client_id,
                                             contract_id=contract_id,
                                             region_ids=region_ids, invoice_ids=invoice_ids)
    if is_error_occurred:
        utils.check_that(export_output, hamcrest.contains_string(expected_result_message))
    else:
        utils.check_that(export_output, hamcrest.not_(hamcrest.contains_string(expected_result_message)))

    overdraft_limits = steps.OverdraftSteps.get_limit(client_id, service.id)
    reporter.log('overdraft_limits: {}'.format(overdraft_limits))

    if is_error_occurred:
        for overdraft_limit in overdraft_limits:
            utils.check_that(overdraft_limit, hamcrest.has_entries({'overdraft_limit': 0, 'iso_currency': None}))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('data',
                         [
                             {'client_ban_params': None,
                              'error_message_type': ErrorMessage.CLIENT_CHECK,
                              'is_error_occurred': False,
                              'test_name': 'client'},

                             # {'client_params': {'IS_AGENCY': 1},
                             #  'client_ban_params': None,
                             #  'error_message_type': ErrorMessage.CLIENT_CHECK,
                             #  'is_error_occurred': True,
                             #  'test_name': 'agency'},

                             {'is_with_brand_agency': True,
                              'client_ban_params': None,
                              'error_message_type': ErrorMessage.CLIENT_CHECK,
                              'is_error_occurred': True,
                              'test_name': 'agency_in_brand'},

                             {'client_ban_params': {'manual_suspect': 1},
                              'error_message_type': ErrorMessage.BAN_CLIENT_CHECK,
                              'is_error_occurred': True,
                              'test_name': 'manual suspect'},

                             {'client_ban_params': {'overdraft_ban': 1},
                              'error_message_type': ErrorMessage.BAN_CLIENT_CHECK,
                              'is_error_occurred': True,
                              'test_name': 'overdraft ban'},

                             {'client_ban_params': {'deny_overdraft': 1},
                              'error_message_type': ErrorMessage.BAN_CLIENT_CHECK,
                              'is_error_occurred': True,
                              'test_name': 'deny overdraft'},
                         ],
                         ids=lambda data: data['test_name'])
def test_overdraft_enqueue_client_check(data):
    client_id = steps.ClientSteps.create(data.get('client_params'))

    if data['is_error_occurred']:
        preset_overdraft(client_id)

    ban_params = data['client_ban_params']

    if ban_params is not None:
        for param_name, param_value in ban_params.iteritems():
            if param_name == 'deny_overdraft':
                steps.CommonSteps.set_extprops('Client', client_id, param_name, {'value_num': param_value})
            else:
                db.balance().execute('UPDATE t_client SET {param_name} = :param_value '
                                     'WHERE id = :client_id'.format(param_name=param_name),
                                     {'param_value': param_value, 'client_id': client_id})

    if data.get('is_with_brand_agency', None):
        steps.ClientSteps.create_client_brand_contract(client_id, {'IS_AGENCY': 1})

    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)

    check_export_result(client_id=client_id, error_message_type=data['error_message_type'],
                        is_error_occurred=data['is_error_occurred'], service=Services.DIRECT)

    check_export_result(client_id=client_id, error_message_type=data['error_message_type'],
                        is_error_occurred=data['is_error_occurred'], service=Services.MARKET)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('service', [Services.DIRECT, Services.MARKET], ids=lambda service: str(service.id))
@pytest.mark.parametrize('data',
                         [
                             {'additional_contract_params': {},
                              'error_message_type': ErrorMessage.POSTPAY_CONTRACT,
                              'is_error_occurred': True,
                              'test_name': 'postpay'},

                             {'additional_contract_params': {'FINISH_DT': DT_DAY_AGO},
                              'error_message_type': ErrorMessage.POSTPAY_CONTRACT,
                              'is_error_occurred': False,
                              'test_name': 'not active'},

                             {'additional_contract_params': {'PAYMENT_TYPE': ContractPaymentType.PREPAY},
                              'error_message_type': ErrorMessage.POSTPAY_CONTRACT,
                              'is_error_occurred': False,
                              'test_name': 'prepay'},

                             {'additional_contract_params': {'another_service': True},
                              'error_message_type': ErrorMessage.POSTPAY_CONTRACT,
                              'is_error_occurred': False,
                              'test_name': 'another service'},
                         ],
                         ids=lambda data: data['test_name'])
def test_overdraft_enqueue_contract_check(data, service):
    client_id = steps.ClientSteps.create()
    if data['is_error_occurred']:
        preset_overdraft(client_id)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)
    firm = Firms.YANDEX_1 if service == Services.DIRECT else Firms.MARKET_111
    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'SERVICES': [service.id],
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'FIRM': firm.id,
                       'PAYMENT_TERM': 10,
                       'CREDIT_TYPE': ContractCreditType.BY_TERM,
                       'CREDIT_LIMIT_SINGLE': 10000
                       }
    # меняем сервис в договоре на директ, если в параметрах пришел маркет, и наоборот
    if data['additional_contract_params'].get('another_service', False):
        another_service = Services.MARKET if service == Services.DIRECT else Services.DIRECT
        another_firm = Firms.MARKET_111 if service == Services.DIRECT else Firms.YANDEX_1
        data['additional_contract_params']['SERVICES'] = [another_service.id]
        data['additional_contract_params']['FIRM'] = another_firm.id

    contract_params.update(data['additional_contract_params'])

    contract_id, _ = steps.ContractSteps.create_contract(CONTRACT_TYPE_NO_AGENCY, contract_params)

    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)

    check_export_result(client_id=client_id, error_message_type=data['error_message_type'],
                        is_error_occurred=data['is_error_occurred'], service=service, contract_id=contract_id)


@reporter.feature(Features.TO_UNIT)
# @pytest.mark.parametrize('service_list', [(Services.DIRECT, Services.MARKET)],
#                          ids=lambda service_list: 'service{}-service{}'.format(service_list[0].id,
#                                                                                service_list[1].id))
@pytest.mark.parametrize('data',
                         [

                             # # доступен овердрафт на директ для белорусов резидентов
                             # {'services': [Services.DIRECT],
                             #  'person_list': [{'type': PersonTypes.BYU, 'hidden': 0}],
                             #  'error_message_type': ErrorMessage.NOT_ACCEPTABLE_REGION_CHECK,
                             #  'is_error_occurred': False,
                             #  'check_descr': 'acceptable_region_BEL'},

                             # недоступен овердрафт на маркет для белорусов резидентов
                             {'services': [Services.MARKET],
                              'person_list': [{'type': PersonTypes.BYU, 'hidden': 0}],
                              'error_message_type': ErrorMessage.NOT_ACCEPTABLE_REGION_CHECK,
                              'is_error_occurred': True,
                              'check_descr': 'acceptable_region_BEL'},

                             # # доступен овердрафт на директ для казахов резидентов
                             # {'services': [Services.DIRECT],
                             #  'person_list': [{'type': PersonTypes.KZU, 'hidden': 0}],
                             #  'error_message_type': ErrorMessage.NOT_ACCEPTABLE_REGION_CHECK,
                             #  'is_error_occurred': False,
                             #  'check_descr': 'acceptable_region_KZ'},

                             # недоступен овердрафт на маркет для казахов резидентов
                             {'services': [Services.MARKET],
                              'person_list': [{'type': PersonTypes.KZU, 'hidden': 0}],
                              'error_message_type': ErrorMessage.NOT_ACCEPTABLE_REGION_CHECK,
                              'is_error_occurred': True,
                              'check_descr': 'acceptable_region_KZ'},

                             {'services': [Services.MARKET, Services.DIRECT],
                              'person_list': [{'type': PersonTypes.AM_UR}],
                              'error_message_type': ErrorMessage.NOT_ACCEPTABLE_REGION_CHECK,
                              'is_error_occurred': True,
                              'check_descr': 'wrong_region_AM_UR'},

                             # {'services': [Services.MARKET, Services.DIRECT],
                             #  'person_list': [{'type': PersonTypes.TRU}],
                             #  'error_message_type': ErrorMessage.NOT_ACCEPTABLE_REGION_CHECK,
                             #  'is_error_occurred': True,
                             #  'check_descr': 'wrong_region_TRU'},

                             {'services': [Services.MARKET, Services.DIRECT],
                              'person_list': [{'type': PersonTypes.USU, 'hidden': 0}],
                              'error_message_type': ErrorMessage.NOT_ACCEPTABLE_REGION_CHECK,
                              'is_error_occurred': True,
                              'check_descr': 'wrong_region_US'},

                             {'services': [Services.MARKET, Services.DIRECT],
                              'person_list': [{'type': PersonTypes.SW_UR, 'hidden': 0}],
                              'error_message_type': ErrorMessage.NOT_ACCEPTABLE_REGION_CHECK,
                              'is_error_occurred': True,
                              'check_descr': 'wrong_region_SW_UR'},

                             # клиент без плательщиков резидентов
                             {'services': [Services.MARKET, Services.DIRECT],
                              'person_list': [{'type': PersonTypes.BY_YTPH, 'region': Regions.SW}],
                              'error_message_type': ErrorMessage.NO_RESIDENT_PAYERS,
                              'is_error_occurred': True,
                              'check_descr': 'many regions'},

                             # клиент со скрытым плательщиком резидентом (нужно ли?)
                             {'services': [Services.MARKET, Services.DIRECT],
                              'person_list': [{'type': PersonTypes.BY_YTPH, 'region': Regions.SW, 'hidden': 0},
                                              {'type': PersonTypes.YT, 'region': Regions.RU, 'hidden': 1}],
                              'error_message_type': ErrorMessage.MANY_REGIONS_CHECK,
                              'is_error_occurred': False,
                              'check_descr': 'hidden'},

                             {'services': [Services.MARKET, Services.DIRECT],
                              'person_list': [{'type': PersonTypes.BY_YTPH, 'region': Regions.SW, 'is_partner': 0},
                                              {'type': PersonTypes.YT, 'region': Regions.RU, 'is_partner': 1}],
                              'error_message_type': ErrorMessage.MANY_REGIONS_CHECK,
                              'is_error_occurred': False,
                              'check_descr': 'partner'},

                             {'services': [Services.MARKET, Services.DIRECT],
                              'person_list': [{'type': PersonTypes.YT, 'region': Regions.RU, 'hidden': 1}],
                              'error_message_type': ErrorMessage.RESIDENT_CHECK,
                              'is_error_occurred': False,
                              'check_descr': 'hidden'},

                             {'services': [Services.MARKET, Services.DIRECT],
                              'person_list': [{'type': PersonTypes.YT, 'region': Regions.RU, 'is_partner': 1}],
                              'error_message_type': ErrorMessage.RESIDENT_CHECK,
                              'is_error_occurred': False,
                              'check_descr': 'partner'},

                             {'services': [Services.MARKET, Services.DIRECT],
                              'person_list': [{'type': PersonTypes.UR, 'region': Regions.RU}],
                              'error_message_type': ErrorMessage.RESIDENT_CHECK,
                              'is_error_occurred': False,
                              'check_descr': 'resident'},

                             # {'services': [Services.MARKET, Services.DIRECT],
                             #  'person_list': [{'type': PersonTypes.TRU, 'hidden': 1}],
                             #  'error_message_type': ErrorMessage.WRONG_REGION_CHECK,
                             #  'is_error_occurred': False,
                             #  'check_descr': 'hidden'},

                             {'services': [Services.MARKET, Services.DIRECT],
                              'person_list': [{'type': PersonTypes.SW_UR, 'is_partner': 1}],
                              'error_message_type': ErrorMessage.WRONG_REGION_CHECK,
                              'is_error_occurred': False,
                              'check_descr': 'partner'},
                         ],
                         ids=lambda data: '{}-{}'.format(data['error_message_type'].name, data['check_descr']))
def test_overdraft_enqueue_persons_check(data):
    client_id = steps.ClientSteps.create()

    if data['is_error_occurred']:
        preset_overdraft(client_id)

    for person in data['person_list']:
        if person.get('is_partner', 0) == 1:
            person_id = steps.PersonSteps.create_partner(client_id, person['type'].code)
        else:
            person_id = steps.PersonSteps.create(client_id, person['type'].code)

        if person.get('hidden', 0) == 1:
            steps.PersonSteps.hide_person(person_id)

        steps.OverdraftSteps.export_client(client_id, with_enqueue=True)

        region_ids = ','.join(map(str, [person['region'].id for person in data['person_list'] if 'region' in person]))

        for service in data['services']:
            check_export_result(client_id=client_id, error_message_type=data['error_message_type'],
                                is_error_occurred=data['is_error_occurred'], service=service, region_ids=region_ids)


# todo-blubimov добавить имена тестов через ids аналогично примерам выше
@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('service', [Services.DIRECT, Services.MARKET], ids=lambda service: str(service.id))
@pytest.mark.parametrize('data',
                         [
                             {'act_dt': DT_6_MONTHS_AGO,
                              'person_type': PersonTypes.PH,
                              'error_message_type': ErrorMessage.ACT_CHECK,
                              'is_error_occurred': False},

                             {'act_dt': DT_5_MONTHS_AGO,
                              'person_type': PersonTypes.PH,
                              'error_message_type': ErrorMessage.ACT_CHECK,
                              'is_error_occurred': True},

                             {'act_dt': DT_6_MONTHS_AGO,
                              'person_type': PersonTypes.PH,
                              'is_act_hidden': True,
                              'error_message_type': ErrorMessage.ACT_CHECK,
                              'is_error_occurred': True},

                             {'act_dt': DT_6_MONTHS_AGO,
                              'another_service': True,
                              'person_type': PersonTypes.PH,
                              'error_message_type': ErrorMessage.ACT_CHECK,
                              'is_error_occurred': True},

                             # # скрываем плательщика, чтобы обойти предыдущую проверку
                             # {'act_dt': DT_6_MONTHS_AGO,
                             #  'person_type': PersonTypes.USU,
                             #  'hide_person': True,
                             #  'is_act_hidden': True,
                             #  'error_message_type': ErrorMessage.ACT_CHECK,
                             #  'is_error_occurred': True},
                         ]
                         )
def test_overdraft_enqueue_act_check(data, service):
    dt = data['act_dt']

    person_type = data['person_type']
    paysys = PERSON_PAYSYS_MAP[person_type]

    client_id = steps.ClientSteps.create()
    if data['is_error_occurred']:
        preset_overdraft(client_id)

    person_id = steps.PersonSteps.create(client_id, person_type.code)

    if data.get('another_service', False):
        service_for_act = Services.MARKET if service == Services.DIRECT else Services.DIRECT
    else:
        service_for_act = service

    # если надо иметь акт, но не по тому сервису
    product = SERVICE_PRODUCT_MAP[service_for_act]
    _, act_list = create_invoice_with_act(service_for_act, client_id, product, person_id, paysys, dt)

    act_id = act_list[0]

    if data.get('is_act_hidden', False):
        steps.ActsSteps.hide(act_id)

    if data.get('hide_person', False):
        steps.PersonSteps.hide_person(person_id)

    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)

    check_export_result(client_id=client_id, error_message_type=data['error_message_type'],
                        is_error_occurred=data['is_error_occurred'], service=service)


# todo-blubimov добавить имена тестов через ids аналогично примерам выше
@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('service', [Services.DIRECT, Services.MARKET])
@pytest.mark.parametrize('data',
                         [
                             # счет 6 месяцев назад, чтобы и акт создался 6 месяцев назад и проверка прошла
                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'invoice_turn_on_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': True},

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'is_invoice_extern': True,
                              'invoice_turn_on_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False},

                             {'invoice_dt': DT_BEFORE_2007,
                              'invoice_final_payment_sum': 120,
                              'invoice_turn_on_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False},

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'invoice_turn_on_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_our_fault_debt': True,
                              'is_error_occurred': False,
                              },

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'invoice_turn_on_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_our_fault_debt': True,
                              'is_our_fault_debt_hidden': True,
                              'is_error_occurred': True,
                              },

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 74,
                              'invoice_turn_on_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_our_fault_debt': True,
                              'is_another_act_with_bad_debt_not_our_fault': True,
                              'is_error_occurred': True,
                              },

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 75,
                              'invoice_turn_on_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_our_fault_debt': True,
                              'is_another_act_with_bad_debt_not_our_fault': True,
                              'is_error_occurred': False,
                              },

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'invoice_turn_on_dt': DT_NOW - datetime.timedelta(days=6),
                              # должно быть 7, но из-за возможноти проверки на сл. день, используем шесть
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False,
                              },

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 150,
                              'invoice_turn_on_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False,
                              }
                         ])
def test_overdraft_invoice_enqueue_prepay_invoice_check(data, service):
    dt = data['invoice_dt']

    client_id = steps.ClientSteps.create()
    if data['is_error_occurred']:
        preset_overdraft(client_id)

    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)

    product = SERVICE_PRODUCT_MAP[service]

    count_of_acts = 2 if data.get('is_another_act_with_bad_debt_not_our_fault', False) else 1

    invoice_id, act_list = create_invoice_with_act(service, client_id, product, person_id, Paysyses.BANK_PH_RUB,
                                                   dt, completion_qty=5, count_of_acts=count_of_acts)

    act_id = act_list[0]

    steps.InvoiceSteps.pay(invoice_id, data['invoice_final_payment_sum'] - PAYMENT_SUM)

    steps.InvoiceSteps.set_turn_on_dt(invoice_id, data['invoice_turn_on_dt'])

    # todo-blubimov а это точно нужно делать? судя по БД этот признак не используется с 2007 года
    if data.get('is_invoice_extern', False):
        steps.InvoiceSteps.make_invoice_extern(invoice_id)

    if data.get('is_our_fault_debt', False):
        steps.OverdraftSteps.set_payment_term_dt(invoice_id, dt - datetime.timedelta(days=1))
        # Признаем плохой долг по счету
        steps.BadDebtSteps.make_bad_debt(invoice_id, our_fault=1)
        if data.get('is_our_fault_debt_hidden', False):
            steps.BadDebtSteps.make_bad_debt_hidden(act_id)
        if data.get('is_another_act_with_bad_debt_not_our_fault', False):
            steps.BadDebtSteps.make_not_our_fault(act_list[1])

    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)

    check_export_result(client_id=client_id, error_message_type=data['error_message_type'],
                        is_error_occurred=data['is_error_occurred'], service=service, invoice_ids=[(invoice_id,)])


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('service', [Services.DIRECT])
@pytest.mark.parametrize('data',
                         [
                             # счет 6 месяцев назад, чтобы и акт создался 6 месяцев назад и проверка прошла
                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'invoice_turn_on_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False},
                         ])
def test_overdraft_invoice_enqueue_prepay_invoice_check_3_firm(data, service):
    dt = data['invoice_dt']

    client_id = steps.ClientSteps.create()
    if data['is_error_occurred']:
        preset_overdraft(client_id)

    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)

    product = SERVICE_PRODUCT_MAP[service]

    count_of_acts = 1

    invoice_id, act_list = create_invoice_with_act(service, client_id, product, person_id, Paysyses.BANK_PH_RUB,
                                                   dt, completion_qty=5, count_of_acts=count_of_acts)

    steps.InvoiceSteps.pay(invoice_id, data['invoice_final_payment_sum'] - PAYMENT_SUM)

    invoice_id, act_list = create_invoice_with_act(service, client_id, product, person_id, Paysyses.BANK_PH_RUB,
                                                   dt, completion_qty=5, count_of_acts=count_of_acts)

    steps.InvoiceSteps.pay(invoice_id, data['invoice_final_payment_sum'] - PAYMENT_SUM)

    steps.InvoiceSteps.set_turn_on_dt(invoice_id, data['invoice_turn_on_dt'])

    db.balance().execute('''UPDATE (SELECT * FROM T_invoice WHERE id =:id) SET FIRM_ID = 3''', {'id': invoice_id})

    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)

    check_export_result(client_id=client_id, error_message_type=data['error_message_type'],
                        is_error_occurred=data['is_error_occurred'], service=service, invoice_ids=[(invoice_id,)])


# todo-blubimov добавить имена тестов через ids аналогично примерам выше
@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('service', [
    Services.DIRECT,
    Services.MARKET
])
@pytest.mark.parametrize('data',
                         [
                             # счет 6 месяцев назад, чтобы и акт создался 6 месяцев назад и проверка прошла

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'act_payment_term_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': True},

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'is_invoice_extern': True,
                              'act_payment_term_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False},

                             {'invoice_dt': DT_BEFORE_2007,
                              'invoice_final_payment_sum': 120,
                              'act_payment_term_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'additional_contract_params': {'IS_SIGNED': DT_BEFORE_2007_iso,
                                                             'DT': DT_BEFORE_2007_iso},
                              'is_error_occurred': False},

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'is_our_fault_debt': True,
                              'act_payment_term_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False},

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'is_our_fault_debt': True,
                              'is_our_fault_debt_hidden': True,
                              'act_payment_term_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': True},

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 74,
                              'is_our_fault_debt': True,
                              'is_another_act_with_bad_debt_not_our_fault': True,
                              'act_payment_term_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': True},

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 75,
                              'is_our_fault_debt': True,
                              'is_another_act_with_bad_debt_not_our_fault': True,
                              'act_payment_term_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False},

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'act_payment_term_dt': DT_NOW - datetime.timedelta(days=6),
                              # должно быть 7, но из-за возможноти проверки на сл. день, используем шесть
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False},

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 180,
                              'act_payment_term_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False},

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'act_payment_term_dt': DT_NOW - datetime.timedelta(days=8),
                              'is_act_hidden': True,
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False},

                         ]
                         )
def test_overdraft_invoice_enqueue_postpay_invoice_check(data, service):
    dt = data['invoice_dt']
    client_id = steps.ClientSteps.create()
    if data['is_error_occurred']:
        preset_overdraft(client_id)

    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)
    product = SERVICE_PRODUCT_MAP[service]

    create_invoice_with_act(service, client_id, product, person_id, Paysyses.BANK_PH_RUB, dt, completion_qty=5)

    service_for_contract = Services.MARKET
    product_for_contract_invoice = SERVICE_PRODUCT_MAP[service]
    firm_for_contract = Firms.MARKET_111

    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': DT_DAY_AGO,
                       'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                       'SERVICES': [service_for_contract.id],
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'FIRM': firm_for_contract.id,
                       'PAYMENT_TERM': 10,
                       'CREDIT_TYPE': ContractCreditType.BY_TERM,
                       'CREDIT_LIMIT_SINGLE': 10000,
                       'PERSONAL_ACCOUNT': 1,
                       'PERSONAL_ACCOUNT_FICTIVE': 0
                       }

    if data.get('additional_contract_params', None) is not None:
        contract_params.update(data['additional_contract_params'])

    count_of_acts = 2 if data.get('is_another_act_with_bad_debt_not_our_fault', False) else 1

    contract_id, _ = steps.ContractSteps.create_contract(CONTRACT_TYPE_NO_AGENCY, contract_params)
    invoice_id, act_list = create_invoice_with_act(service_for_contract, client_id, product_for_contract_invoice,
                                                   person_id, Paysyses.BANK_PH_RUB, dt, completion_qty=5,
                                                   contract_id=contract_id, payment_sum=PAYMENT_SUM,
                                                   count_of_acts=count_of_acts)

    steps.InvoiceSteps.set_dt(invoice_id, dt)
    if data.get('is_invoice_extern', None) is not None:
        steps.InvoiceSteps.make_invoice_extern(invoice_id)
    steps.InvoiceSteps.pay(invoice_id, data['invoice_final_payment_sum'] - PAYMENT_SUM)

    act_id = act_list[0]
    steps.ActsSteps.set_payment_term_dt(act_id, data['act_payment_term_dt'])
    if data.get('is_act_hidden', None) is not None:
        steps.ActsSteps.hide(act_id)
    if data.get('is_our_fault_debt', None) is not None:
        steps.OverdraftSteps.set_payment_term_dt(invoice_id, dt - datetime.timedelta(days=1))
        steps.BadDebtSteps.make_bad_debt(invoice_id, our_fault=1)
        if data.get('is_our_fault_debt_hidden', False):
            steps.BadDebtSteps.make_bad_debt_hidden(act_id)
        if data.get('is_another_act_with_bad_debt_not_our_fault', False):
            steps.BadDebtSteps.make_not_our_fault(act_list[1])

    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)

    invoice_line = [(invoice_id,) for _ in range(count_of_acts)]
    reporter.log(invoice_line)
    check_export_result(client_id=client_id, error_message_type=data['error_message_type'],
                        is_error_occurred=data['is_error_occurred'], service=service, invoice_ids=invoice_line)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('service', [
    Services.DIRECT
])
@pytest.mark.parametrize('data',
                         [
                             # счет 6 месяцев назад, чтобы и акт создался 6 месяцев назад и проверка прошла

                             {'invoice_dt': DT_6_MONTHS_AGO,
                              'invoice_final_payment_sum': 120,
                              'act_payment_term_dt': DT_NOW - datetime.timedelta(days=8),
                              'error_message_type': ErrorMessage.INVOICE_CHECK,
                              'is_error_occurred': False},

                         ]
                         )
def test_overdraft_invoice_enqueue_postpay_invoice_check_firm_3(data, service):
    dt = data['invoice_dt']
    client_id = steps.ClientSteps.create()
    if data['is_error_occurred']:
        preset_overdraft(client_id)

    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)
    product = SERVICE_PRODUCT_MAP[service]

    create_invoice_with_act(service, client_id, product, person_id, Paysyses.BANK_PH_RUB, dt, completion_qty=5)

    service_for_contract = Services.MARKET
    product_for_contract_invoice = SERVICE_PRODUCT_MAP[service]
    firm_for_contract = Firms.MARKET_111

    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': DT_DAY_AGO,
                       'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                       'SERVICES': [service_for_contract.id],
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'FIRM': firm_for_contract.id,
                       'PAYMENT_TERM': 10,
                       'CREDIT_TYPE': ContractCreditType.BY_TERM,
                       'CREDIT_LIMIT_SINGLE': 10000,
                       'PERSONAL_ACCOUNT': 1,
                       'PERSONAL_ACCOUNT_FICTIVE': 0
                       }

    if data.get('additional_contract_params', None) is not None:
        contract_params.update(data['additional_contract_params'])

    count_of_acts = 1

    contract_id, _ = steps.ContractSteps.create_contract(CONTRACT_TYPE_NO_AGENCY, contract_params)
    invoice_id, act_list = create_invoice_with_act(service_for_contract, client_id, product_for_contract_invoice,
                                                   person_id, Paysyses.BANK_PH_RUB, dt, completion_qty=5,
                                                   contract_id=contract_id, payment_sum=PAYMENT_SUM,
                                                   count_of_acts=count_of_acts)

    # steps.InvoiceSteps.set_dt(invoice_id, dt)
    #
    # steps.InvoiceSteps.pay(invoice_id, data['invoice_final_payment_sum'] - PAYMENT_SUM)

    # act_id = act_list[0]
    # steps.ActsSteps.set_payment_term_dt(act_id, data['act_payment_term_dt'])

    service_for_contract = Services.MARKET
    product_for_contract_invoice = SERVICE_PRODUCT_MAP[service]
    firm_for_contract = Firms.MARKET_111

    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': DT_DAY_AGO,
                       'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                       'SERVICES': [service_for_contract.id],
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'FIRM': firm_for_contract.id,
                       'PAYMENT_TERM': 10,
                       'CREDIT_TYPE': ContractCreditType.BY_TERM,
                       'CREDIT_LIMIT_SINGLE': 10000,
                       'PERSONAL_ACCOUNT': 1,
                       'PERSONAL_ACCOUNT_FICTIVE': 0
                       }

    if data.get('additional_contract_params', None) is not None:
        contract_params.update(data['additional_contract_params'])

    count_of_acts = 1

    contract_id, _ = steps.ContractSteps.create_contract(CONTRACT_TYPE_NO_AGENCY, contract_params)
    invoice_id, act_list = create_invoice_with_act(service_for_contract, client_id, product_for_contract_invoice,
                                                   person_id, Paysyses.BANK_PH_RUB, dt, completion_qty=5,
                                                   contract_id=contract_id, payment_sum=PAYMENT_SUM,
                                                   count_of_acts=count_of_acts)

    steps.InvoiceSteps.set_dt(invoice_id, dt)

    steps.InvoiceSteps.pay(invoice_id, data['invoice_final_payment_sum'] - PAYMENT_SUM)

    act_id = act_list[0]
    steps.ActsSteps.set_payment_term_dt(act_id, data['act_payment_term_dt'])

    db.balance().execute('''UPDATE (SELECT * FROM T_invoice WHERE id =:id) SET FIRM_ID = 3''', {'id': invoice_id})
    steps.OverdraftSteps.export_client(client_id, with_enqueue=True)

    invoice_line = [(invoice_id,) for _ in range(count_of_acts)]
    reporter.log(invoice_line)
    check_export_result(client_id=client_id, error_message_type=data['error_message_type'],
                        is_error_occurred=data['is_error_occurred'], service=service, invoice_ids=invoice_line)
