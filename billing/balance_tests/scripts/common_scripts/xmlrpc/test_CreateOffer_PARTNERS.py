# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime

import pytest

import balance.balance_api as api
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Managers, Nds
from btestlib.data import defaults
from btestlib.matchers import has_entries_casted
from temp.igogor.balance_objects import Contexts

PASSPORT_ID = defaults.PASSPORT_UID
MANAGER = Managers.SOME_MANAGER

CONTEXT = Contexts.PARTNERS_DEFAULT

contract_start_dt = datetime.datetime.fromordinal(datetime.datetime.today().toordinal()).replace(
    day=1) - datetime.timedelta(days=200)


def create_client_person(person_type='ur'):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type, {'is-partner': '1'})
    return client_id, person_id


def expected_data_preparation(client_id, person_id, person_type, contract_id, contract_external_id,
                              additional_params={}):
    start_dt = additional_params['start_dt'] if additional_params.has_key(
        'start_dt') else utils.Date.nullify_time_of_date(datetime.datetime.today())
    service_start_dt = additional_params['service_start_dt'] if additional_params.has_key(
        'service_start_dt') else utils.Date.nullify_time_of_date(datetime.datetime.today())

    # для физика дефолтный ндс = 0, для юрика = 18
    if additional_params.has_key('nds'):
        nds = additional_params['nds']
    elif person_type == 'ph':
        nds = Nds.ZERO
    else:
        nds = Nds.DEFAULT

    expected_data = {
        'client_id': client_id,
        'contract2_id': contract_id,
        'currency': additional_params['currency'].iso_num_code if additional_params.has_key(
            'currency') else CONTEXT.currency.iso_num_code,
        'contract_type': 9,
        'dt': start_dt.date().isoformat(),
        'firm': additional_params['firm_id'] if additional_params.has_key('firm_id') else CONTEXT.firm.id,
        'is_cancelled': '',
        'is_faxed': '',
        # 'is_signed': utils.Date.nullify_time_of_date(datetime.datetime.today()).date().isoformat(),
        'manager_code': CONTEXT.manager.code,
        'nds': nds,
        'payment_type': additional_params['payment_type'] if additional_params.has_key('payment_type') else 1,
        'pay_to': 1,
        'memo': u'Договор создан автоматически',
        'person_id': person_id,
        'external_id': additional_params['external_id'] if additional_params.has_key(
            'external_id') else contract_external_id,
        'type': 'PARTNERS',
        'market_api_pct': additional_params['market_api_pct'] if additional_params.has_key(
            'market_api_pct') else CONTEXT.default_market_api_pct,
        'test_mode': additional_params['test_mode'] if additional_params.has_key('test_mode') else 0,
        'partner_pct': additional_params['partner_pct'] if additional_params.has_key(
            'partner_pct') else CONTEXT.default_partner_pct,
        'reward_type': additional_params['reward_type'] if additional_params.has_key(
            'reward_type') else CONTEXT.default_reward_type,
        'search_forms': additional_params['search_forms'] if additional_params.has_key(
            'search_forms') else CONTEXT.default_search_forms,
        'open_date': additional_params['open_date'] if additional_params.has_key(
            'open_date') else CONTEXT.default_open_date,
        'service_start_dt': service_start_dt.date().isoformat(),
    }
    return expected_data


@reporter.feature(Features.XMLRPC)
@pytest.mark.parametrize('person_type, additional_params',
                         [
                             pytest.mark.smoke(('ur', {})),
                             # ('ur', {'currency': Currencies.USD}),
                         ],
                         ids=[
                             'CreateOffer with mandatory params only with ur',
                             # 'CreateOffer ',
                         ]
                         )
def test_check_create_offer_partners(person_type, additional_params):
    # создаем клиента и плательщика
    client_id, person_id = create_client_person(person_type)

    # создаем договор
    params = CONTEXT.create_offer_params
    params.update({'client_id': client_id, 'person_id': person_id})
    params.update(additional_params)
    contract_id, contract_eid = steps.ContractSteps.create_offer(params)

    # забираем данные по договору
    contract_data = api.medium().GetPartnerContracts({'ExternalID': contract_eid})[0]['Contract']

    expected_data = expected_data_preparation(client_id, person_id, person_type,
                                              contract_id, contract_eid, additional_params=additional_params)

    # сравниваем платеж с шаблоном
    utils.check_that(contract_data, has_entries_casted(expected_data),
                     'Сравниваем данные по договору с шаблоном')
