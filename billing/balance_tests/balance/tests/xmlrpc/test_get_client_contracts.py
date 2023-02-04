# coding: utf-8
__author__ = 'a-vasin'

import json
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_inanyorder, not_, has_key, has_item, all_of, has_entry, equal_to

import balance.balance_db as db
import balance.balance_steps as steps
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance.features import Features
from btestlib.constants import ContractSubtype, Currencies, NdsNew, Firms, Services
from btestlib.data.defaults import ContractDefaults
from btestlib.matchers import contains_dicts_with_entries
from temp.igogor.balance_objects import Contexts

pytestmark = [
    reporter.feature(Features.PARTNER, Features.XMLRPC, Features.CONTRACT, Features.SPENDABLE,
                     Features.GET_CLIENT_CONTRACTS)
]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)
FINISH_DT = START_DT + relativedelta(years=1)

FIRM_YANDEX_TAXI = Firms.TAXI_13
FIRM_YANDEX_TAXI_UKRAINE = Firms.TAXI_UA_23
FIRM_YANDEX_TAXI_BV = Firms.TAXI_BV_22

EXPECTED_DIRECT_POSTPAY = {
    'CREDIT_LIMITS': contains_dicts_with_entries(
        [{
            'ACTIVITY_TYPE': None, 'CURRENCY': 'RUB', 'LIMIT': Decimal('10000.00')
        }]),
    'CURRENCY': Currencies.RUB.char_code,
    'PAYMENT_TERM': 90, 'PAYMENT_TYPE': 3, 'SERVICES': [7]
}

EXPECTED_DIRECT_PREPAY = {
    'CURRENCY': Currencies.RUB.char_code, 'PAYMENT_TYPE': 2, 'SERVICES': [7]
}
EXPECTED_ADFOX = {
    'ADFOX_PRODUCTS': [{'DT': START_DT, 'IS_ACTIVE': 1,
                        'PRODUCTS': [
                            {'PRODUCT_ID': 505176, 'SCALE_CODE': 'adfox_sites_requests_test_scale'},
                            {'PRODUCT_ID': 504402, 'SCALE_CODE': 'adfox_mobile_default_test_scale'},
                            {'PRODUCT_ID': 505173, 'SCALE_CODE': 'adfox_mobile_test_scale'}]}],
    'CURRENCY': Currencies.RUB.char_code, 'PAYMENT_TYPE': 3, 'SERVICES': [102], 'VIP_CLIENT': 0
}
EXPECTED_TAXI_POSTPAY = {
    'COUNTRY': 225, 'CURRENCY': Currencies.RUB.char_code, 'PAYMENT_TYPE': 3,
    'SERVICES': contains_inanyorder(128, 124, 111, 125, 605), 'NDS_FOR_RECEIPT': 18
}
EXPECTED_TAXI_PRE = {
    'COUNTRY': 225, 'CURRENCY': Currencies.RUB.char_code, 'PAYMENT_TYPE': 2,
    'SERVICES': contains_inanyorder(128, 124, 111, 125, 605), 'NDS_FOR_RECEIPT': 18
}
EXPECTED_TAXI_POSTPAY_SNG = {
    'COUNTRY': 159, 'CURRENCY': Currencies.USD.char_code,
    'PARTNER_COMMISSION_PCT2': Decimal('3.5'), 'PAYMENT_TYPE': 3,
    'SERVICES': contains_inanyorder(128, 124, 111)
}


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('contract_type, service, firm, currency, nds, person_type',
                         [
                             ('spendable_corp_clients', Services.TAXI_CORP_PARTNERS, FIRM_YANDEX_TAXI, Currencies.RUB,
                              NdsNew.DEFAULT, 'ur'),
                             ('spendable_corp_clients', Services.TAXI_CORP_PARTNERS, FIRM_YANDEX_TAXI_UKRAINE, Currencies.UAH,
                              NdsNew.UKRAINE, 'ua'),
                             ('spendable_corp_clients', Services.TAXI_CORP_PARTNERS, FIRM_YANDEX_TAXI_BV, Currencies.USD,
                              NdsNew.NOT_RESIDENT,
                              'eu_yt')
                         ],
                         ids=lambda contract_type, contract_type_number, firm, currency, nds,
                                    person_type: '{}-{}'.format(contract_type, Firms.name(firm)))
def test_get_client_contracts_spendable(contract_type, service, firm, currency, nds, person_type):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type)
    contract_id, external_id = steps.ContractSteps.create_contract(contract_type,
                                                                   {
                                                                       'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                       'DT': START_DT,
                                                                       'IS_SIGNED': START_DT.isoformat(),
                                                                       'FIRM': firm.id,
                                                                       'CURRENCY': currency.iso_num_code,
                                                                       'NDS': str(nds.nds_id),
                                                                       'SERVICES': [service.id]
                                                                   })

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.SPENDABLE)
    expected_contracts_info = [create_expected_spendable_contract_info(person_id, contract_id,
                                                                       external_id, currency, nds, service)]

    utils.check_that(contracts_info, contains_dicts_with_entries(expected_contracts_info),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
@pytest.mark.smoke
def test_get_client_contracts_donate():
    PAY_TO = 5
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    link_contract_id, _ = steps.ContractSteps.create_contract('taxi_pre',
                                                              {'CLIENT_ID': client_id, 'PERSON_ID': person_id})
    contract_id, external_id = steps.ContractSteps.create_contract('spendable_taxi_donate',
                                                                   {
                                                                       'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                       'DT': START_DT,
                                                                       'IS_SIGNED': START_DT.isoformat(),
                                                                       'LINK_CONTRACT_ID': link_contract_id,
                                                                       'SERVICES': [Services.TAXI_DONATE.id],
                                                                       'PAY_TO': PAY_TO
                                                                   })

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.SPENDABLE)
    expected_contracts_info = [create_expected_spendable_contract_info(person_id, contract_id, external_id,
                                                                       Currencies.RUB, NdsNew.DEFAULT,
                                                                       Services.TAXI_DONATE, pay_to=PAY_TO)]

    utils.check_that(contracts_info, contains_dicts_with_entries(expected_contracts_info),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('contract_type, person_type, expected_values',
                         [
                             # Такси постоплата СНГ
                             ('taxi_postpay_sng', 'eu_yt', EXPECTED_TAXI_POSTPAY_SNG),

                             # Такси предоплата
                             ('taxi_pre', 'ur', EXPECTED_TAXI_PRE),

                             # Такси предоплата
                             ('taxi_postpay', 'ur', EXPECTED_TAXI_POSTPAY),

                             # adfox
                             ('adfox_all_products', 'ur', EXPECTED_ADFOX),

                             # Директ предоплата
                             ('direct_prepay', 'ur', EXPECTED_DIRECT_PREPAY),

                             # Директ постоплата
                             pytest.mark.smoke(('direct_postpay', 'ur', EXPECTED_DIRECT_POSTPAY))

                             #
                         ], ids=lambda contract_type, person_type, expected_values: contract_type)
def test_get_client_contracts_general(contract_type, person_type, expected_values):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type)
    contract_id, external_id = steps.ContractSteps.create_contract(contract_type,
                                                                   {
                                                                       'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                       'DT': START_DT,
                                                                       'FINISH_DT': FINISH_DT,
                                                                       'IS_SIGNED': START_DT.isoformat(),
                                                                       'MANAGER_CODE': ContractDefaults.MANAGER_CODE,
                                                                   })

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)

    expected_contracts_info = [create_expected_general_contract_info(person_id, contract_id, external_id)]
    expected_contracts_info[0].update(expected_values)

    utils.check_that(contracts_info, contains_dicts_with_entries(expected_contracts_info),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
def test_two_general_contracts():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT,
        'FINISH_DT': FINISH_DT,
        'IS_SIGNED': START_DT.isoformat()
    }

    taxi_prepay_contract_id, taxi_prepay_external_id = steps.ContractSteps.create_contract('taxi_pre', contract_params)
    direct_prepay_contract_id, direct_prepay_external_id = steps.ContractSteps.create_contract('direct_prepay',
                                                                                             contract_params)

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)

    expected_contracts_info = [
        create_expected_general_contract_info(person_id, taxi_prepay_contract_id, taxi_prepay_external_id),
        create_expected_general_contract_info(person_id, direct_prepay_contract_id, direct_prepay_external_id)]
    expected_contracts_info[0].update(EXPECTED_TAXI_PRE)
    expected_contracts_info[1].update(EXPECTED_DIRECT_PREPAY)

    utils.check_that(contracts_info, contains_dicts_with_entries(expected_contracts_info),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
def test_ended_contract():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT,
        'FINISH_DT': START_DT,
        'IS_SIGNED': START_DT.isoformat()
    }
    steps.ContractSteps.create_contract('taxi_pre', contract_params)

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL, signed=0)

    expected_values = [{
        'IS_SIGNED': 1,
        'IS_ACTIVE': 0
    }]

    utils.check_that(contracts_info, contains_dicts_with_entries(expected_values),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
def test_annul_contract():
    contract_type = 'opt_agency_post'

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT,
        'FINISH_DT': START_DT + relativedelta(months=6),
        'IS_SIGNED': START_DT.isoformat()
    }
    contract_id, contract_eid = steps.ContractSteps.create_contract(contract_type, contract_params)

    contract_params.update({'IS_CANCELLED': START_DT.isoformat(),
                            'ID': contract_id,
                            'EXTERNAL_ID': contract_eid})
    steps.ContractSteps.create_contract(contract_type, contract_params)

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL, signed=0)

    expected_values = [{
        'IS_CANCELLED': 1,
        'IS_ACTIVE': 0,
        'IS_SIGNED': 1
    }]

    utils.check_that(contracts_info, contains_dicts_with_entries(expected_values),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
def test_suspended_contract():
    contract_type = 'taxi_pre'

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT,
        'IS_SIGNED': START_DT.isoformat()
    }
    contract_id, _ = steps.ContractSteps.create_contract(contract_type, contract_params)

    contract_params.update({'IS_SUSPENDED': START_DT.isoformat(),
                            'ID': contract_id})
    steps.ContractSteps.create_contract(contract_type, contract_params)

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL, signed=0)

    expected_values = [{
        'IS_SIGNED': 1,
        'IS_ACTIVE': 0,
        'IS_SUSPENDED': 1
    }]

    utils.check_that(contracts_info, contains_dicts_with_entries(expected_values),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


# по мотивам задач BALANCE-26480, BALANCE-26714
@reporter.feature(Features.TO_UNIT)
def test_deactivated_contract():
    contract_type = 'contract_offer_taxi_pre'

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT,
        'IS_SIGNED': START_DT.isoformat(),
        'SERVICES': [Services.TAXI_111.id]
    }

    contract_id, _ = steps.ContractSteps.create_contract(contract_type, contract_params)
    contract_params.update({'IS_SUSPENDED': START_DT.isoformat(),
                            'IS_DEACTIVATED': 1,
                            'ID': contract_id})
    steps.ContractSteps.create_contract(contract_type, contract_params)
    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL, signed=0)
    expected_values = [{
        'IS_SIGNED': 1,
        'IS_ACTIVE': 0,
        'IS_SUSPENDED': 1,
        'IS_DEACTIVATED': 1
    }]

    utils.check_that(contracts_info, contains_dicts_with_entries(expected_values),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
def test_unsigned_contract():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT
    }

    steps.ContractSteps.create_contract('taxi_pre', contract_params, remove_params=['IS_SIGNED'])

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL, signed=0)

    expected_values = [{
        'IS_SIGNED': 0,
        'IS_ACTIVE': 0
    }]

    utils.check_that(contracts_info, contains_dicts_with_entries(expected_values),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
def test_faxed_contract():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT,
        'IS_FAXED': START_DT.isoformat()
    }
    steps.ContractSteps.create_contract('taxi_pre', contract_params, remove_params=['IS_SIGNED'])

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)

    expected_values = [{
        'IS_FAXED': 1,
        'IS_ACTIVE': 1,
        'IS_SIGNED': 0
    }]

    utils.check_that(contracts_info, contains_dicts_with_entries(expected_values),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
def test_contract_without_finish_dt():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT
    }
    steps.ContractSteps.create_contract('taxi_pre', contract_params)

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)

    utils.check_that(contracts_info, not_(has_item(has_key('FINISH_DT'))),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
def test_get_client_contracts_parameter_dt():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT,
        'FINISH_DT': FINISH_DT,
        'IS_SIGNED': START_DT.isoformat()
    }

    steps.ContractSteps.create_contract('taxi_pre', contract_params)

    contracts_info_start_dt = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL, dt=START_DT)
    contracts_info_after_finish_dt = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL,
                                                                            dt=FINISH_DT + relativedelta(months=1),
                                                                            signed=0)

    expected_values_start_dt = [{'IS_ACTIVE': 1}]
    expected_values_after_finish_dt = [{'IS_ACTIVE': 0}]

    utils.check_that(contracts_info_start_dt, contains_dicts_with_entries(expected_values_start_dt),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')
    utils.check_that(contracts_info_after_finish_dt, contains_dicts_with_entries(expected_values_after_finish_dt),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
def test_get_client_contracts_parameter_signed():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT
    }

    steps.ContractSteps.create_contract('taxi_pre', contract_params, remove_params=['IS_SIGNED'])
    steps.ContractSteps.create_contract('taxi_pre', contract_params)

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL, signed=0)
    expected_contracts_info = [{'IS_SIGNED': 1}, {'IS_SIGNED': 0}]

    utils.check_that(contracts_info, contains_dicts_with_entries(expected_contracts_info),
                     u'Проверяем, что метод вернул ожидаемую информацию о договоре')


@reporter.feature(Features.TO_UNIT)
def test_apikeys_tariff_params():

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_cc = 'apimaps'
    tariff = 'apikeys_apimaps_10k_yearprepay_2017'

    apikeys_tariffs = get_apikeys_tariffs(tariff, service_cc)

    params = {'CLIENT_ID': client_id,
              'PERSON_ID': person_id,
              'DT': START_DT,
              'FINISH_DT': FINISH_DT,
              'APIKEYS_TARIFFS': apikeys_tariffs}

    contract_id, _ = steps.ContractSteps.create_contract('no_agency_apikeys_post', params)

    contracts_info = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)
    apikeys_params = contracts_info[0]['APIKEYS_TARIFFS']
    expected_params = [{'DT': START_DT,
                        'IS_ACTIVE': 1,
                        'TARIFFS': {'apikeys_{}'.format(service_cc): tariff}}]
    utils.check_that(apikeys_params, equal_to(expected_params))

# -------------------
# Utils

def create_expected_netting_matcher(services, netting_pct):
    if Services.TAXI_111 not in services and Services.TAXI_128 not in services:
        return contains_inanyorder(all_of(
            not_(has_key('NETTING')),
            not_(has_key('NETTING_PCT'))
        ))

    if netting_pct:
        return contains_dicts_with_entries([{
            'NETTING': 1,
            'NETTING_PCT': netting_pct
        }])

    return contains_inanyorder(all_of(
        has_entry('NETTING', 0),
        not_(has_key('NETTING_PCT'))
    ))


def create_netting_contract(context, services, netting_pct):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'FIRM': str(context.firm.id),
        'NDS_FOR_RECEIPT': str(context.nds.nds_id),
        'COUNTRY': str(context.region.id),
        'CURRENCY': context.currency.num_code,
        'SERVICES': [service.id for service in services]
    }

    if netting_pct is not None:
        params.update({'NETTING_PCT': netting_pct, 'NETTING': 1})

    contract_id, _ = steps.ContractSteps.create_contract('taxi_pre', params)

    return client_id, contract_id


def create_expected_basic_signed_contract_info(person_id, contract_id, external_id, service=None, contract_type=None):
    info = {'IS_SUSPENDED': 0, 'IS_ACTIVE': 1, 'IS_SIGNED': 1, 'ID': contract_id, 'PERSON_ID': person_id,
            'IS_FAXED': 0, 'IS_CANCELLED': 0, 'EXTERNAL_ID': external_id, 'DT': START_DT}

    if service:
        info['SERVICES'] = [service.id]
    if contract_type:
        info['CONTRACT_TYPE'] = contract_type

    return info


def create_expected_spendable_contract_info(person_id, contract_id, external_id, currency, nds, service, pay_to=0):
    expected_contract_info = create_expected_basic_signed_contract_info(person_id, contract_id, external_id, service)
    expected_contract_info.update({'NDS': nds.nds_id, 'CURRENCY': currency.char_code})
    if pay_to:
        expected_contract_info.update({'PAY_TO': pay_to})
    return expected_contract_info


def create_expected_general_contract_info(person_id, contract_id, external_id):
    expected_contract_info = create_expected_basic_signed_contract_info(person_id, contract_id, external_id,
                                                                        contract_type=0)
    expected_contract_info.update({'MANAGER_CODE': ContractDefaults.MANAGER_CODE, 'FINISH_DT': FINISH_DT})
    return expected_contract_info

def get_apikeys_control():
    apikeys_control = []
    for group in db.get_apikeys_tariff_groups():
        apikeys_control.append({'group_id': group['id'],
                                'group_cc': group['cc'],
                                'group_name': group['name'],
                                'member': '',
                                'id': group['id']})
    return apikeys_control

def get_apikeys_tariffs(tariff, service_cc):
    apikeys_control = get_apikeys_control()
    services = {item['cc']: item['id'] for item in db.get_apikeys_tariff_groups()}
    groups = {item['cc']: item['id'] for item in
              db.get_apikeys_tariff_group_by_service(Services.APIKEYS.id)}
    apikeys_tariff_id = db.get_apikeys_tariff_by_cc(tariff)
    for item in apikeys_control:
        try:
            item['id'] = int(services[item['group_cc']])
            item['group_id'] = groups[item['group_cc']]
            if item['group_cc'] == 'apikeys_{}'.format(service_cc):
                item['member'] = apikeys_tariff_id[0]['id']
        except KeyError as ke:
            print '....... [DEBUG] ...... Service {} not found'.format(ke.message)

    apikeys_tariffs = json.dumps(apikeys_control)
    return apikeys_tariffs
