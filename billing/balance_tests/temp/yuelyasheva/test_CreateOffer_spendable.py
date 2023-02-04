# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime

import hamcrest
import pytest

import balance.balance_api as api
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Managers, Currencies, Nds
from btestlib.data import defaults
from btestlib.data.defaults import GeneralPartnerContractDefaults as GenDefParams
from btestlib.data.defaults import SpendableContractDefaults as SpendableDefParams
from btestlib.data.defaults import convert_params_for_create_offer
from btestlib.matchers import has_entries_casted

PASSPORT_ID = defaults.PASSPORT_UID
MANAGER = Managers.SOME_MANAGER

contract_start_dt = datetime.datetime.fromordinal(datetime.datetime.today().toordinal()).replace(
    day=1) - datetime.timedelta(days=200)


def create_client_person(person_type='ur', is_partner='1', is_tinkoff = 0):
    person_params = {'is-partner': is_partner}
    if is_tinkoff:
        person_params.update({'bik': '044525974', 'account': '40802810800000041990'})
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type, person_params)
    return client_id, person_id


def create_params_for_create_offer(client_id, person_id, params, additional_params=None):
    return convert_params_for_create_offer(client_id, person_id, params, additional_params, manager=MANAGER)


def expected_data_preparation(client_id, person_id, contract_id, contract_external_id, params, additional_params,
                              pay_to=1):
    currency = additional_params['currency'].iso_num_code if additional_params.has_key('currency') else params[
        'currency'].iso_num_code
    start_dt = additional_params['start_dt'] if additional_params.has_key(
        'start_dt') else utils.Date.nullify_time_of_date(datetime.datetime.today())
    firm = additional_params['firm_id'] if additional_params.has_key('firm_id') else params['firm_id']
    nds = additional_params['nds'] if additional_params.has_key('nds') else params['nds']
    payment_type = additional_params['payment_type'] if additional_params.has_key('payment_type') else 1

    expected_data = {
        'client_id': client_id,
        'contract2_id': contract_id,
        'currency': currency,
        'dt': start_dt.date().isoformat(),
        'firm': firm,
        'is_cancelled': '',
        'is_faxed': '',
        'is_signed': utils.Date.nullify_time_of_date(datetime.datetime.today()).date().isoformat(),
        'is_offer': 1,
        'manager_code': MANAGER.code,
        'nds': nds,
        'payment_type': payment_type,
        'pay_to': pay_to if pay_to else 1,
        'person_id': person_id,
        'services': dict.fromkeys([str(params['services'][0])], 1),
        'type': 'SPENDABLE',
    }

    if params.has_key('region'):
        expected_data.update({'region': params['region']})
    if params.has_key('country'):
        expected_data.update({'country': params['country'].id})
    if additional_params.has_key('link_contract_id'):
        expected_data.update({'link_contract_id': additional_params['link_contract_id']})
    return expected_data


@reporter.feature(Features.XMLRPC)
@pytest.mark.parametrize('additional_params',
                         [
                             pytest.mark.smoke(({'nds': Nds.DEFAULT})),
                             ({'nds': Nds.NOT_RESIDENT}),
                             ({'start_dt': contract_start_dt}),
                             ({'external_id': 'Test_Оферта+' + str(datetime.datetime.now())}),
                             ({'payment_type': 1}),
                             ({'payment_type': 2})
                         ],
                         ids=[
                             'CreateOffer market spendable check nds = 18',
                             'CreateOffer market spendable check nds = 0',
                             'CreateOffer market spendable with start_dt',
                             'CreateOffer market spendable with special external id',
                             'CreateOffer market spendable payment_type = 1',
                             'CreateOffer market spendable payment_type = 2',
                         ]
                         )
def test_check_spendable_create_offer_market(additional_params):
    client_id, person_id = create_client_person(SpendableDefParams.MARKET_MARKETING['PERSON_TYPE'])

    params = create_params_for_create_offer(client_id, person_id,
                                            SpendableDefParams.MARKET_MARKETING['CONTRACT_PARAMS'].copy(),
                                            additional_params=additional_params)

    # создаем договор
    contract_id, contract_eid = steps.ContractSteps.create_offer(params)

    # забираем данные по договору
    contract_data = api.medium().GetPartnerContracts({'ExternalID': contract_eid})[0]['Contract']

    expected_data = expected_data_preparation(client_id, person_id, contract_id, contract_eid,
                                              SpendableDefParams.MARKET_MARKETING['CONTRACT_PARAMS'].copy(),
                                              additional_params=additional_params)

    # сравниваем платеж с шаблоном
    utils.check_that(contract_data, has_entries_casted(expected_data),
                     'Сравниваем данные по договору с шаблоном')


@reporter.feature(Features.XMLRPC)
@pytest.mark.parametrize('additional_params',
                         [({'is_offer': 1})
                          ],
                         ids=[
                             'CreateOffer adaptor 2 0 spendable offer = 1'
                         ]
                         )
def test_check_spendable_create_offer_adaptor_2_0(additional_params):
    client_id, person_id = create_client_person(SpendableDefParams.ADDAPPTER_2_0['PERSON_TYPE'])

    params = create_params_for_create_offer(client_id, person_id,
                                            SpendableDefParams.ADDAPPTER_2_0['CONTRACT_PARAMS'].copy(),
                                            additional_params=additional_params)

    # создаем договор
    contract_id, contract_eid = steps.ContractSteps.create_offer(params)

    # забираем данные по договору
    contract_data = api.medium().GetPartnerContracts({'ExternalID': contract_eid})[0]['Contract']

    expected_data = expected_data_preparation(client_id, person_id, contract_id, contract_eid,
                                              SpendableDefParams.ADDAPPTER_2_0['CONTRACT_PARAMS'].copy(),
                                              additional_params=additional_params, pay_to=2)

    # сравниваем платеж с шаблоном
    utils.check_that(contract_data, has_entries_casted(expected_data),
                     'Сравниваем данные по договору с шаблоном')



@reporter.feature(Features.XMLRPC)
@pytest.mark.parametrize('additional_params',
                         [
                             pytest.mark.smoke(({})),
                             ({'currency': Currencies.USD}),
                         ],
                         ids=[
                             'CreateOffer taxi coroba RUB',
                             'CreateOffer taxi coroba USD',
                         ]
                         )
def test_check_spendable_create_offer_coroba(additional_params):
    client_id, person_id = create_client_person(SpendableDefParams.TAXI_COROBA['PERSON_TYPE'])

    params = create_params_for_create_offer(client_id, person_id,
                                            SpendableDefParams.TAXI_COROBA['CONTRACT_PARAMS'].copy(),
                                            additional_params=additional_params.copy())

    # создаем договор
    contract_id, contract_eid = steps.ContractSteps.create_offer(params)

    # забираем данные по договору
    contract_data = api.medium().GetPartnerContracts({'ExternalID': contract_eid})[0]['Contract']

    expected_data = expected_data_preparation(client_id, person_id, contract_id, contract_eid,
                                              SpendableDefParams.TAXI_COROBA['CONTRACT_PARAMS'].copy(),
                                              additional_params=additional_params, pay_to=5)

    # сравниваем платеж с шаблоном
    utils.check_that(contract_data, has_entries_casted(expected_data),
                     'Сравниваем данные по договору с шаблоном')


@reporter.feature(Features.XMLRPC)
@pytest.mark.parametrize('is_linked_contract_needed',
                         [
                             pytest.mark.smoke((1)),
                             (0),
                         ],
                         ids=[
                             'CreateOffer taxi corp with link contract id',
                             'CreateOffer taxi corp wo link contract id',
                         ]
                         )
def test_check_spendable_create_offer_corp_taxi(is_linked_contract_needed):
    client_id, person_id = create_client_person(SpendableDefParams.TAXI_CORP['PERSON_TYPE'])

    params = create_params_for_create_offer(client_id, person_id,
                                            SpendableDefParams.TAXI_CORP['CONTRACT_PARAMS'].copy(),
                                            )

    additional_params = {}
    if is_linked_contract_needed:
        general_contract_id, _ = steps.CommonPartnerSteps.create_gerenal_partner_person_and_contract(client_id,
                                                                                                     GenDefParams.YANDEX_TAXI)
        params.update({'link_contract_id': general_contract_id})
        additional_params = {'link_contract_id': general_contract_id}

    # создаем договор
    contract_id, contract_eid = steps.ContractSteps.create_offer(params)

    # забираем данные по договору
    contract_data = api.medium().GetPartnerContracts({'ExternalID': contract_eid})[0]['Contract']

    expected_data = expected_data_preparation(client_id, person_id, contract_id, contract_eid,
                                              SpendableDefParams.TAXI_CORP['CONTRACT_PARAMS'].copy(),
                                              additional_params=additional_params)

    # сравниваем платеж с шаблоном
    utils.check_that(contract_data, has_entries_casted(expected_data),
                     'Сравниваем данные по договору с шаблоном')


@reporter.feature(Features.XMLRPC)
@pytest.mark.parametrize('spendable_data, general_data, is_tinkoff',
                         [
                             pytest.mark.smoke((SpendableDefParams.TAXI_DONATE_RUSSIA, GenDefParams.YANDEX_TAXI, 0)),
                             (SpendableDefParams.TAXI_DONATE_KZT, GenDefParams.YANDEX_TAXI_BV, 0),
                             (SpendableDefParams.TAXI_DONATE_UA, GenDefParams.YANDEX_TAXI_UKRAINE, 0),
                             (SpendableDefParams.TAXI_DONATE_RUSSIA, GenDefParams.YANDEX_TAXI, 1)
                         ],
                         ids=[
                             'CreateOffer taxi donate LLC',
                             'CreateOffer taxi donate TAXI BV',
                             'CreateOffer taxi donate TAXI UKR',
                             'CreateOffer taxi donate LLC with pay_to = tinkoff',
                         ]
                         )
def test_check_spendable_create_offer_taxi_donate(spendable_data, general_data, is_tinkoff):
    client_id, person_id = create_client_person(spendable_data['PERSON_TYPE'], is_tinkoff=is_tinkoff)

    params = create_params_for_create_offer(client_id, person_id,
                                            spendable_data['CONTRACT_PARAMS'].copy())

    general_contract_id, _ = steps.CommonPartnerSteps.create_gerenal_partner_person_and_contract(client_id,
                                                                                                 general_data)

    params.update({'link_contract_id': general_contract_id, 'offer_confirmation_type': 'no'})
    additional_params = {'link_contract_id': general_contract_id}

    # создаем договор
    contract_id, contract_eid = steps.ContractSteps.create_offer(params)

    # забираем данные по договору
    contract_data = api.medium().GetPartnerContracts({'ExternalID': contract_eid})[0]['Contract']

    expected_data = expected_data_preparation(client_id, person_id, contract_id, contract_eid,
                                              spendable_data['CONTRACT_PARAMS'],
                                              additional_params=additional_params, pay_to=5 if is_tinkoff else 1)

    # сравниваем платеж с шаблоном
    utils.check_that(contract_data, has_entries_casted(expected_data),
                     'Сравниваем данные по договору с шаблоном')


@reporter.feature(Features.XMLRPC)
def test_check_spendable_create_offer_telemedicine_donate():
    client_id, person_id = create_client_person(SpendableDefParams.TELEMEDICINE_DONATE['PERSON_TYPE'])

    params = create_params_for_create_offer(client_id, person_id,
                                            SpendableDefParams.TELEMEDICINE_DONATE['CONTRACT_PARAMS'].copy())

    general_contract_id, _ = steps.CommonPartnerSteps.create_gerenal_partner_person_and_contract(client_id,
                                                                                                 GenDefParams.TELEMEDICINE)

    params.update({'link_contract_id': general_contract_id})
    additional_params = {'link_contract_id': general_contract_id}

    # создаем договор
    contract_id, contract_eid = steps.ContractSteps.create_offer(params)

    # забираем данные по договору
    contract_data = api.medium().GetPartnerContracts({'ExternalID': contract_eid})[0]['Contract']

    expected_data = expected_data_preparation(client_id, person_id, contract_id, contract_eid,
                                              SpendableDefParams.TELEMEDICINE_DONATE['CONTRACT_PARAMS'],
                                              additional_params=additional_params)

    # сравниваем платеж с шаблоном
    utils.check_that(contract_data, has_entries_casted(expected_data),
                     'Сравниваем данные по договору с шаблоном')


def test_create_offer_invalid_person():
    client_id, person_id = create_client_person(is_partner='0')
    params = create_params_for_create_offer(client_id, person_id,
                                            SpendableDefParams.MARKET_MARKETING['CONTRACT_PARAMS'].copy())
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(params)
    utils.check_that(steps.CommonSteps.get_exception_code(exc.value, 'msg'),
                     hamcrest.equal_to('Invalid parameter for function: PERSON. '
                                       'Contract_type and person.is_partner are incompatible.'))


def test_create_offer_wo_nds():
    client_id, person_id = create_client_person()
    params = create_params_for_create_offer(client_id, person_id,
                                            SpendableDefParams.MARKET_MARKETING['CONTRACT_PARAMS'].copy())
    params.pop('nds')
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(params)
    utils.check_that(steps.CommonSteps.get_exception_code(exc.value, 'msg'),
                     hamcrest.equal_to(
                         'Invalid parameter for function: NDS. Nds is mandatory for spendable and distribution contracts.'))


def test_create_offer_wo_ctype_for_corp_taxi():
    client_id, person_id = create_client_person(SpendableDefParams.TAXI_CORP['PERSON_TYPE'])
    params = create_params_for_create_offer(client_id, person_id,
                                            SpendableDefParams.TAXI_CORP['CONTRACT_PARAMS'].copy())
    params.pop('ctype')
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(params)
    utils.check_that(steps.CommonSteps.get_exception_code(exc.value, 'msg'),
                     hamcrest.equal_to(
                         'Invalid parameter for function: CTYPE. Required for 135 service'))


def test_create_offer_with_ctype_for_coroba():
    client_id, person_id = create_client_person(SpendableDefParams.TAXI_COROBA['PERSON_TYPE'])
    params = create_params_for_create_offer(client_id, person_id,
                                            SpendableDefParams.TAXI_COROBA['CONTRACT_PARAMS'].copy(),
                                            additional_params={'ctype': 'SPENDABLE'})
    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(params)
    utils.check_that(steps.CommonSteps.get_exception_code(exc.value, 'msg'),
                     hamcrest.equal_to(
                         'Invalid parameter for function: CTYPE. Allowed only for 135 service'))
