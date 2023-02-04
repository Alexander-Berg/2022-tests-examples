# -*- coding: utf-8 -*-

import datetime
import hamcrest
import pytest
import copy

from simpleapi.matchers import deep_equals as de
from balance import balance_db as db
from btestlib import utils as utils
from balance import balance_steps as steps
from btestlib.constants import Services
from temp.igogor.balance_objects import Contexts, Products, Firms, ContractCommissionType, PersonTypes, Currencies
from balance.tests.paystep.non_contract.test_non_contract_depend_on_service_settings import SERVICES_WITHOUT_OFFER, \
    SERVICES_AGENCY_CONTRACT_NEEDED, \
    SERVICES_WITH_ALLOWED_AGENCY_WITHOUT_CONTRACT, OTHER_SERVICES

NOW = datetime.datetime.now()
PREVIOUS_MONTH_LAST_DAY = NOW.replace(day=1) - datetime.timedelta(days=1)
YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(NOW - datetime.timedelta(days=365))

MARKET = Contexts.MARKET_RUB_CONTEXT.new(firm=Firms.MARKET_111,
                                         contract_type=ContractCommissionType.NO_AGENCY)
KUPIBILET = MARKET.new(service=Services.KUPIBILET, firm=Firms.YANDEX_1)
RABOTA = MARKET.new(service=Services.RABOTA, firm=Firms.VERTICAL_12)
MEDIANA = MARKET.new(service=Services.MEDIANA, firm=Firms.VERTICAL_12)
VENDORS = MARKET.new(service=Services.VENDORS)
TOLOKA = MARKET.new(service=Services.TOLOKA,
                    contract_type=ContractCommissionType.SW_OPT_CLIENT,
                    firm=Firms.EUROPE_AG_7,
                    person_type=PersonTypes.SW_YT,
                    contract_currency=Currencies.USD)
SCORING = MARKET.new(service=Services.SCORING)
DOSTAVKA = MARKET.new(service=Services.DOSTAVKA)
DIRECT_TUNING = MARKET.new(service=Services.DIRECT_TUNING)
PUBLIC = MARKET.new(service=Services.PUBLIC)
APIKEYS = MARKET.new(service=Services.APIKEYS)
MEDIA_BANNERS = MARKET.new(service=Services.MEDIA_BANNERS)
BAYAN = MARKET.new(service=Services.BAYAN)
CLOUD_143 = MARKET.new(service=Services.CLOUD_143)
GEO = MARKET.new(service=Services.GEO)
TRANSLATE = MARKET.new(service=Services.TRANSLATE)
DSP = MARKET.new(service=Services.DSP)
BANKI = MARKET.new(service=Services.BANKI)
OFD = MARKET.new(service=Services.OFD)
SPAMDEF = MARKET.new(service=Services.SPAMDEF)
DIRECT = MARKET.new(service=Services.DIRECT)
MEDIA_BANNERS_167 = MARKET.new(service=Services.MEDIA_BANNERS_167)
NAVIGATOR = MARKET.new(service=Services.NAVI)
REALTY = MARKET.new(service=Services.REALTY)
CATALOG1 = VENDORS.new(service=Services.CATALOG1)
CATALOG2 = VENDORS.new(service=Services.CATALOG2)
CATALOG3 = VENDORS.new(service=Services.CATALOG3)
METRICA = VENDORS.new(service=Services.METRICA)
MEDIA = VENDORS.new(service=Services.MEDIA_70)
REALTY_KOMM = VENDORS.new(service=Services.REALTY_COMM, product=Products.REALTY_COMM)
# TOURS = VENDORS.new(service=Services.TOURS)
AUTORU = VENDORS.new(service=Services.AUTORU)
RIT = VENDORS.new(service=Services.RIT)
ADFOX = VENDORS.new(service=Services.ADFOX)
TAXI_111 = VENDORS.new(service=Services.TAXI_111)
TAXI_CORP = VENDORS.new(service=Services.TAXI_CORP)
TAXI_DONATE = VENDORS.new(service=Services.TAXI_DONATE)
CONNECT = VENDORS.new(service=Services.CONNECT)
TELEMEDICINE_DONATE = VENDORS.new(service=Services.TELEMEDICINE_DONATE)
BUSES_DONATE = VENDORS.new(service=Services.BUSES_DONATE)
QTY = 2

'''
По сервисам с признаком restrict_client = 1 при наличии активных договоров клиенты могут выставлять только по договору.
'''
RESTRICT_CLIENT_SERVICES = [KUPIBILET,
                            MARKET,
                            RABOTA,
                            VENDORS,
                            MEDIANA,
                            TOLOKA,
                            SCORING,
                            NAVIGATOR]

'''
Сервисы с признаком restrict_client = 1 партнерские и трастовые. В тестах не участвуют, мониторим признак только
'''
RESTRICT_CLIENT_SERVICES_TRUST = [RIT,
                                  ADFOX,
                                  TAXI_CORP,
                                  TAXI_DONATE,
                                  CONNECT,
                                  TAXI_111,
                                  TELEMEDICINE_DONATE,
                                  BUSES_DONATE]

'''
Все другие сервисы с признаком restrict_client = 0. При наличии договора могут выставляться и по договору и без.
'''
SERVICES_NON_RESTRICTED = [DIRECT,
                           DIRECT_TUNING,
                           GEO,
                           CATALOG1,
                           CATALOG2,
                           METRICA,
                           MEDIA,
                           REALTY_KOMM,
                           # TOURS,
                           AUTORU,
                           OFD,
                           BANKI,
                           MEDIA_BANNERS,
                           ]

'''
Не может выставляться без договора, хотя и имеет признак restrict_client = 0, потому что это запрещено в таблице оферт.
'''
SERVICES_NON_RESTRICTED_WITHOUT_OFFER = [REALTY, BAYAN]

'''
Эти сервисы нельзя указать в договоре, не знаю почему:(
'''
SERVICES_NON_RESTRICTED_NOT_IN_CONTRACT = [CATALOG3, TRANSLATE]

'''
Сервисы с признаком restrict_client = 0 партнерские и трастовые.
'''
OTHER_SERVICES_NON_RESTRICTED_PARTNER = [DSP]

'''
Если в реквесте указан хотя бы один сервис с признаком restrict_client = 1,
клиенты могут выставлять только по договору.
'''

MIXED_RESTRICT_CLIENT_SERVICES = [[RESTRICT_CLIENT_SERVICES[0], SERVICES_NON_RESTRICTED[0]]]

BASE_CONTRACT_PARAMS = {
    'DT': YEAR_BEFORE_NOW_ISO,
    'PAYMENT_TYPE': 2,
    'IS_SIGNED': YEAR_BEFORE_NOW_ISO}

'''
Если договор приостановлен, то по сервису с признаком restrict_client = 1, можно выставиться без договора
'''


@pytest.mark.parametrize('context',
                         RESTRICT_CLIENT_SERVICES + SERVICES_NON_RESTRICTED + SERVICES_NON_RESTRICTED_WITHOUT_OFFER
                         # [BAYAN]
                         )
def test_contract_is_available_depend_on_restrict_client(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    contract_params = copy.deepcopy(BASE_CONTRACT_PARAMS)
    contract_params.update({'CLIENT_ID': client_id,
                            'PERSON_ID': person_id,
                            'SERVICES': [context.service.id],
                            'FIRM': context.firm.id,
                            'CURRENCY': context.contract_currency.num_code if hasattr(context,
                                                                                      'contract_currency')
                            else Currencies.RUB.num_code
                            })
    contract_id, _ = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    # на всякий пытаюсь проверить, что сервис в договоре действительно указан
    batch_id = db.get_collaterals_by_contract(contract_id)[0]['attribute_batch_id']
    service_in_contract = [x['key_num'] for x in db.get_attributes_by_batch_id(batch_id) if
                           x['code'] == 'SERVICES']
    assert service_in_contract == [context.service.id]
    orders_list = []
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': None})
    orders_list.append(
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW})
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = steps.RequestSteps.format_request_choices(request_choices)
    without_contract_request_choices = formatted_request_choices.get('without_contract', {})
    with_contract_request_choices = formatted_request_choices.get('with_contract', {})
    restrict_client = db.get_service_by_id(context.service.id)[0]['restrict_client']
    if context in RESTRICT_CLIENT_SERVICES:
        utils.check_that(restrict_client, hamcrest.is_(1))
        utils.check_that(with_contract_request_choices, hamcrest.is_not(hamcrest.empty()))
        utils.check_that(without_contract_request_choices, hamcrest.empty())
    elif context in SERVICES_NON_RESTRICTED_WITHOUT_OFFER:
        utils.check_that(restrict_client, hamcrest.is_(0))
        utils.check_that(with_contract_request_choices, hamcrest.is_not(hamcrest.empty()))
        utils.check_that(without_contract_request_choices, hamcrest.empty())
    else:
        utils.check_that(restrict_client, hamcrest.is_(0))
        utils.check_that(with_contract_request_choices, hamcrest.is_not(hamcrest.empty()))
        utils.check_that(without_contract_request_choices, hamcrest.is_not(hamcrest.empty()))


@pytest.mark.parametrize('context_list',
                         MIXED_RESTRICT_CLIENT_SERVICES)
def test_contract_is_available_depend_on_restrict_client_mixed(context_list):
    first_context = context_list[0]
    second_context = context_list[1]
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, first_context.person_type.code)

    contract_params = copy.deepcopy(BASE_CONTRACT_PARAMS)
    contract_params.update({'CLIENT_ID': client_id,
                            'PERSON_ID': person_id,
                            'SERVICES': [first_context.service.id, second_context.service.id],
                            'FIRM': first_context.firm.id,
                            'CURRENCY': first_context.contract_currency.num_code if hasattr(first_context,
                                                                                            'contract_currency')
                            else Currencies.RUB.num_code
                            })
    contract_id, _ = steps.ContractSteps.create_contract_new(first_context.contract_type, contract_params)
    orders_list = []
    for context in context_list:
        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
        steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                                service_order_id=service_order_id, params={'AgencyID': None})
        orders_list.append(
            {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW})
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = steps.RequestSteps.format_request_choices(request_choices)
    without_contract_request_choices = formatted_request_choices.get('without_contract', {})
    with_contract_request_choices = formatted_request_choices.get('with_contract', {})
    utils.check_that(with_contract_request_choices, hamcrest.is_not(hamcrest.empty()))
    utils.check_that(without_contract_request_choices, hamcrest.empty())


@pytest.mark.parametrize('context',
                         [RESTRICT_CLIENT_SERVICES[0]])
def test_restrict_client_without_contract(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    contract_params = copy.deepcopy(BASE_CONTRACT_PARAMS)
    contract_params.update({'CLIENT_ID': client_id,
                            'PERSON_ID': person_id,
                            'SERVICES': [context.service.id],
                            'FIRM': context.firm.id,
                            'CURRENCY': context.contract_currency.num_code if hasattr(context,
                                                                                      'contract_currency')
                            else Currencies.RUB.num_code
                            })
    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    # приостанавливаем договор
    contract_params.update({'IS_SUSPENDED': YEAR_BEFORE_NOW_ISO,
                            'ID': contract_id,
                            'EXTERNAL_ID': contract_eid})
    steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    orders_list = []
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': None})
    orders_list.append(
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW})
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = steps.RequestSteps.format_request_choices(request_choices)
    without_contract_request_choices = formatted_request_choices.get('without_contract', {})
    with_contract_request_choices = formatted_request_choices.get('with_contract', {})
    restrict_client = db.get_service_by_id(context.service.id)[0]['restrict_client']
    utils.check_that(restrict_client, hamcrest.is_(1))
    utils.check_that(with_contract_request_choices, hamcrest.empty())
    utils.check_that(without_contract_request_choices, hamcrest.is_not(hamcrest.empty()))


@pytest.mark.parametrize('context', RESTRICT_CLIENT_SERVICES_TRUST)
def test_check_restrict_client_in_trust(context):
    restrict_client = db.get_service_by_id(context.service.id)[0]['restrict_client']
    utils.check_that(restrict_client, hamcrest.is_(1))


def test_service_consistency_1():
    common_services_list = [SERVICES_WITHOUT_OFFER,
                            SERVICES_AGENCY_CONTRACT_NEEDED,
                            SERVICES_WITH_ALLOWED_AGENCY_WITHOUT_CONTRACT, OTHER_SERVICES]
    common_services_ids = [set(context.service.id for context in service_list) for service_list in common_services_list]
    common_union_services = set.union(*common_services_ids)
    test_services = [RESTRICT_CLIENT_SERVICES, SERVICES_NON_RESTRICTED, OTHER_SERVICES_NON_RESTRICTED_PARTNER,
                     SERVICES_NON_RESTRICTED_WITHOUT_OFFER, SERVICES_NON_RESTRICTED_NOT_IN_CONTRACT]
    test_services_ids = [set(context.service.id for context in service_list) for service_list in test_services]
    dublicate_services = set.intersection(*test_services_ids)
    test_union_services = set.union(*test_services_ids)
    utils.check_that(dublicate_services, hamcrest.empty())
    utils.check_that(test_union_services, de.deep_equals_to(common_union_services))
