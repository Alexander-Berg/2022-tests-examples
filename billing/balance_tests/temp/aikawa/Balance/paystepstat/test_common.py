# coding=utf-8

import datetime
import pytest
import pprint
import hamcrest

import balance.balance_db as db
from balance import balance_steps as steps
import btestlib.utils as utils
from btestlib.constants import Firms, Services, Products
import btestlib.matchers as matchers

from maintenance.check_settings import SERVICES_WITH_RESTRICT_CLIENT_FLAG, SERVICES_ALLOWED_AGENCY_WITHOUT_CONTRACT_FLAG, SERVICES_WITH_CONTRACT_NEEDED_FLAG

DT = datetime.datetime.now()

SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'
# CONTRACT_TYPE = 'no_agency'

HALF_YEAR_AFTER_NOW_ISO = utils.Date.date_to_iso_format(DT + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=180))
YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=365))

DIRECT = Services.DIRECT.id
VENDORS = Services.VENDORS.id
RABOTA = Services.RABOTA.id
TOLOKA = Services.TOLOKA.id
MARKET = Services.MARKET.id
ADFOX = Services.ADFOX.id
KUPIBILET = Services.KUPIBILET.id
MEDIASELLING = Services.BAYAN.id
BAYAN = Services.MEDIA_BANNERS.id
MEDIA = Services.MEDIA_70.id
GEO = Services.GEO.id
APIKEYS = Services.APIKEYS.id
DSP = Services.DSP.id
MEDIABANNERS = Services.MEDIA_BANNERS_167.id
TRANSLATE = Services.TRANSLATE.id
CLOUD = Services.CLOUD_143.id
PUBLIC = Services.PUBLIC.id
DOSTAVKA = Services.DOSTAVKA.id
BANKI = Services.BANKI.id
OFD = Services.OFD.id
SPAMDEF = Services.SPAMDEF.id


YANDEX_FIRM = Firms.YANDEX_1.id
MARKET_FIRM = Firms.MARKET_111.id
VERTICAL_FIRM = Firms.VERTICAL_12.id
SERVICES_AG = Firms.SERVICES_AG_16.id
YANDEX_TURKEY_8 = Firms.YANDEX_TURKEY_8.id

VENDORS_PRODUCT = Products.VENDOR.id
DIRECT_PRODUCT = Products.DIRECT_FISH.id
RABOTA_PRODUCT = Products.RABOTA.id
TOLOKA_PRODUCT = Products.TOLOKA.id
MARKET_PRODUCT = Products.MARKET.id
ADFOX_PRODUCT = Products.ADFOX.id
KUPIBILET_PRODUCT = Products.KUPIBILET.id
MEDIASELLING_PRODUCT = Products.MEDIA.id

SERVICE_PRODUCT_MAP = {VENDORS: VENDORS_PRODUCT,
                       DIRECT: DIRECT_PRODUCT,
                       RABOTA: RABOTA_PRODUCT,
                       TOLOKA: TOLOKA_PRODUCT,
                       MARKET: MARKET_PRODUCT,
                       ADFOX: ADFOX_PRODUCT,
                       KUPIBILET: KUPIBILET_PRODUCT,
                       MEDIASELLING: MEDIASELLING_PRODUCT
                       }

SERVICE_FIRM_MAP = {VENDORS: MARKET_FIRM,
                    DIRECT: YANDEX_FIRM,
                    RABOTA: VERTICAL_FIRM,
                    TOLOKA: SERVICES_AG,
                    MARKET: MARKET_FIRM,
                    ADFOX: YANDEX_FIRM,
                    KUPIBILET: SERVICES_AG,
                    MEDIASELLING: MARKET_FIRM}


def build_pcps(pcps_params):
    base_pcps = {}
    print pcps_params
    if pcps_params.get('contract_id', False):
        contract = db.get_contract_by_id(pcps_params['contract_id'])[0]
        base_pcps['contract'] = {'client_id': contract['client_id'],
                                 'external_id': contract['external_id'],
                                 'id': contract['id'],
                                 'person_id': contract['person_id']}
    elif pcps_params['contract_id'] is None:
        base_pcps['contract'] = None
    assert base_pcps
    return base_pcps


# Описываем сервисы, продукты, тип плательщиков и фирмы и признак того, что такому сочетанию доступно выставление без оферты
SERVICES_PARAMS = [

    # Казахи-резиденты
    # {'service_id': DIRECT, 'person_type': 'kzu', 'is_offer_available': True},
    # {'service_id': DIRECT, 'person_type': 'kzp', 'is_offer_available': True},

    # Казахи-нерезиденты
    # {'service_id': DIRECT, 'person_type': 'yt_kzu', 'is_offer_available': False},
    # {'service_id': DIRECT, 'person_type': 'yt_kzp', 'is_offer_available': False},
    # {'service_id': MARKET, 'person_type': 'yt_kzu', 'is_offer_available': True},
    # {'service_id': MARKET, 'person_type': 'yt_kzp', 'is_offer_available': True},

    # Турки, Директ+Медийка
    {'service_id': DIRECT, 'person_type': 'tru', 'is_offer_available': True, 'contract_type': 'TR_OPT_CLIENT', 'firm_id': YANDEX_TURKEY_8},
    {'service_id': DIRECT, 'person_type': 'tru', 'is_offer_available': True, 'contract_type': 'TR_OPT_CLIENT', 'firm_id': YANDEX_TURKEY_8},
    {'service_id': MARKET, 'person_type': 'tru', 'is_offer_available': True, 'contract_type': 'TR_OPT_CLIENT', 'firm_id': YANDEX_TURKEY_8},
    {'service_id': MARKET, 'person_type': 'tru', 'is_offer_available': True, 'contract_type': 'TR_OPT_CLIENT', 'firm_id': YANDEX_TURKEY_8},
]


def check_pcps(pcps, list_of_pcps):
    pcp_list = pcps['pcp_list']
    expected_pcps = []
    for pcps in list_of_pcps:
        expected_pcps.append(build_pcps(pcps))
    utils.check_that(pcp_list, matchers.contains_dicts_with_entries(expected_pcps))


def check_pcps_does_not_exist(pcps, list_of_pcps):
    pcp_list = pcps['pcp_list']
    expected_pcps = []
    for pcps in list_of_pcps:
        expected_pcps.append(build_pcps(pcps))
    utils.check_that(pcp_list, hamcrest.not_(matchers.contains_dicts_with_entries(expected_pcps)))


def create_contract(client_id, person_id, service_id, contract_type, firm_id):
    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                       'SERVICES': [service_id],
                       'PAYMENT_TYPE': 2,
                       'FIRM': firm_id
                       }

    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params)
    return contract_id

def create_request(client_id, service_id, contract_id):
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    steps.OrderSteps.create(client_id=client_id, product_id=SERVICE_PRODUCT_MAP[service_id], service_id=service_id,
                            service_order_id=service_order_id, params={'AgencyID': None, 'ContractID': contract_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': DT}
    ]
    return steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

def test_subclient_non_resident_contract_is_not_available():
    print steps.ClientSteps.create_sub_client_non_resident('1')
    pass

@pytest.mark.parametrize('service_params', SERVICES_PARAMS)
@pytest.mark.parametrize('with_active_contract', [
    True,
    False
])
# есть признак restrict_client, оферта доступна
def test_restrict_client_offer_available(service_params, with_active_contract):
    service_id = service_params['service_id']
    if service_id not in SERVICES_WITH_RESTRICT_CLIENT_FLAG or not service_params['is_offer_available']:
        return
    else:
        contract_id = None
        client_id = steps.ClientSteps.create()
        person_id = steps.PersonSteps.create(client_id, service_params['person_type'])

        if with_active_contract:
            contract_id = create_contract(client_id, person_id, service_id, contract_type=service_params['contract_type'], firm_id=service_params['firm_id'])

        request_id = create_request(client_id, service_id, contract_id)

        request_choices = steps.RequestSteps.get_request_choices(request_id)
        # если есть активный договор
        if with_active_contract:
            # по договору выставиться можно
            check_pcps(request_choices, [{'contract_id': contract_id}])
            # без договора выставиться нельзя
            check_pcps_does_not_exist(request_choices, [{'contract_id': None}])
        else:
            # без договора выставиться можно
            check_pcps(request_choices, [{'contract_id': None}])

@pytest.mark.parametrize('service_params', SERVICES_PARAMS)
@pytest.mark.parametrize('with_active_contract', [
    True,
    False
])
# нет признака restrict_client, оферта недоступна
def test_not_restrict_client_offer_non_available(service_params, with_active_contract):
    service_id = service_params['service_id']
    if service_id in SERVICES_WITH_RESTRICT_CLIENT_FLAG or service_params['is_offer_available']:
        return
    else:
        contract_id = None
        client_id = steps.ClientSteps.create()
        person_id = steps.PersonSteps.create(client_id, service_params['person_type'])

        if with_active_contract:
            contract_id = create_contract(client_id, person_id, service_id)

        request_id = create_request(client_id, service_id, contract_id)

        request_choices = steps.RequestSteps.get_request_choices(request_id)
        # если есть активный договор
        if with_active_contract:
            # по договору выставиться можно
            check_pcps(request_choices, [{'contract_id': contract_id}])
            # без договора выставиться нельзя
            check_pcps_does_not_exist(request_choices, [{'contract_id': None}])
        else:
            # без договора выставиться можно
            check_pcps(request_choices, [{'contract_id': None}])
