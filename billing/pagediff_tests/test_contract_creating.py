# coding: utf-8

import datetime
import uuid

import pytest

import balance.balance_steps as steps
import balance.balance_web as web
import btestlib.constants as constant
import btestlib.pagediff as pagediff
import btestlib.reporter as reporter
from balance.features import Features
from btestlib.data import person_defaults
from btestlib import utils
import filters

START_DT = utils.Date.date_to_iso_format(datetime.datetime.now())

pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.CONTRACT),
    pytest.mark.tickets('BALANCE-25225','BALANCE-23441','BALANCE-25117','BALANCE-23442')
]

def create_client_and_person(person_type, contract_spec_params):
    client_id = steps.ClientSteps.create()
    client_name = steps.ClientSteps.get_client_name(client_id)
    person_id = steps.PersonSteps.create(client_id, person_type, name_type=person_defaults.NameType.DEFAULT)

    params = {'CLIENT_ID': client_id, 'PERSON_ID': person_id, 'DT': START_DT, 'IS_SIGNED': START_DT}
    params.update(contract_spec_params)
    dynamic_values = [client_id, person_id, client_name]

    return params, dynamic_values


def check_diff(dynamic_values, contract_id, external_id, unique_name):
    dynamic_values += [contract_id, external_id]
    page_html = pagediff.get_page(url=web.AdminInterface.ContractEditPage.url(contract_id=contract_id))
    pagediff.pagediff(unique_name=unique_name, page_html=page_html,
                      filters=filters.contract_edit_filters(dynamic_values=dynamic_values))


@pytest.mark.parametrize('unique_name, contract_type,  person_type, contract_spec_params', [
    pytest.mark.skip(('taxi_ua', 'no_agency', 'ua', {'SERVICES': [constant.Services.TAXI_111.id, constant.Services.TAXI.id, constant.Services.TAXI_128.id],
                                    'PAYMENT_TYPE': constant.ContractPaymentType.PREPAY,
                                    'FIRM': constant.Firms.TAXI_UA_23.id,
                                    'COUNTRY': constant.Regions.UA.id,
                                    'PERSONAL_ACCOUNT': 1}), reason = u'отключили Украину' ),
    # ('addapter_general', 'no_agency', 'ur', {'SERVICES': [constant.Services.ADAPTER_DEVELOPER.id, constant.Services.ADAPTER_RITEILER.id],
    #                                          'PAYMENT_TYPE': constant.ContractPaymentType.POSTPAY,
    #                                          'PERSONAL_ACCOUNT': 1,
    #                                          'PARTNER_CREDIT': 1,
    #                                          'CREDIT_TYPE': 2})  ##BALANCE-23442
], ids=lambda unique_name, contract_type, person_type, contract_spec_params: unique_name)
def test_contract_creating_general(unique_name, contract_type, person_type, contract_spec_params):
    params, dynamic_values = create_client_and_person(person_type, contract_spec_params)

    contract_id, external_id = steps.ContractSteps.create_contract_new(contract_type, params)
    check_diff(dynamic_values, contract_id, external_id, unique_name)


@pytest.mark.parametrize('unique_name, contract_type,  person_type, contract_spec_params', [
    ('universal_distr_empty_sup4', 'universal_distr_empty', 'ur', {'SUPPLEMENTS': [4]}),  ##BALANCE-23441
    ('universal_distr_empty_sup5', 'universal_distr_empty', 'ur', {'SUPPLEMENTS': [5]})  ##BALANCE-23441
], ids=lambda unique_name, contract_type, person_type, contract_spec_params: unique_name)
def test_contract_creating_distribution(unique_name, contract_type, person_type, contract_spec_params):
    params, dynamic_values = create_client_and_person(person_type, contract_spec_params)
    tag_id = steps.DistributionSteps.create_distr_tag(params['CLIENT_ID'])
    params['DISTRIBUTION_TAG'] = tag_id

    contract_id, external_id = steps.ContractSteps.create_contract(contract_type, params)
    check_diff(dynamic_values, contract_id, external_id, unique_name)

@pytest.mark.parametrize('unique_name, person_type, oferta_params', [
    # ('oferta_market', 'ur', {'currency': constant.Currencies.RUB.char_code,
    #                            'firm_id': constant.Firms.MARKET_111.id,
    #                            'manager_uid': constant.Managers.SOME_MANAGER.uid,
    #                            'payment_term': 10,
    #                            'personal_account':1,
    #                            'payment_type': constant.ContractPaymentType.POSTPAY,
    #                            'commission_pct': 3,
    #                            'services': [constant.Services.NEW_MARKET.id],
    #                            'start_dt': START_DT}),   ##BALANCE-25225
    ('oferta_cloud', 'ur',    {'currency': constant.Currencies.RUB.char_code,
                               'firm_id': constant.Firms.CLOUD_112.id,
                               'manager_uid': constant.Managers.SOME_MANAGER.uid,
                               'payment_term': 10,
                               'payment_type': constant.ContractPaymentType.POSTPAY,
                               'projects': [str(uuid.uuid4())],
                               'services': [constant.Services.CLOUD_143.id],
                               'start_dt': START_DT})  ##BALANCE-25117
], ids=lambda unique_name, person_type, oferta_params: unique_name)
def test_contract_oferta_market(unique_name, person_type, oferta_params):
    client_id = steps.ClientSteps.create()
    client_name = steps.ClientSteps.get_client_name(client_id)
    person_id = steps.PersonSteps.create(client_id, person_type, name_type=person_defaults.NameType.DEFAULT)
    contract_id, contract_eid = steps.ContractSteps.create_offer(dict(oferta_params, **{'client_id':client_id, 'person_id':person_id}))
    dynamic_values = [client_id, person_id, client_name]
    if 'projects' in oferta_params.keys():
        dynamic_values += oferta_params['projects']
    check_diff(dynamic_values, contract_id, contract_eid, unique_name)

