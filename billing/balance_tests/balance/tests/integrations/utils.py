# coding: utf-8

__author__ = 'nurlykhan'

import copy
import json
import allure

import attr
import pytest
from balance.balance_objects import Context
from balance.tests.conftest import FakeTrustActivator
from btestlib.constants import *
from btestlib import reporter

from datetime import datetime, timedelta
from decimal import Decimal as D

from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data import simpleapi_defaults
from btestlib.matchers import contains_dicts_with_entries
from cashmachines.data.constants import CMNds, NDS_BY_NAME
from simpleapi.common.payment_methods import YandexAccountWithdraw

log = reporter.logger()


def create_context(service, contract, test_input, person_type, service_firm_mapper):
    service_firm_value = None
    if service_firm_mapper:
        service_firm_value = service_firm_mapper.get_firm_to_service(service.id)
    firm_id = service_firm_value or (contract['firm'] if contract.get('firm') else contract['firm_id'])
    firm = get_firm_by_id(firm_id)
    contract_services = set(contract['services'].get('mandatory', []))
    contract_services.add(service.id)
    currency = getattr(Currencies, contract['currency'], None) if 'currency' in contract else Currencies.RUB
    paysys = get_paysys_by_firm_and_person(firm, person_type)
    if paysys is None:
        # имхо такой способ нахождения пэйсиса корректнее, но не стал сразу менять, чтобы ничего не развалить
        paysys = get_paysys_by_firm_and_person(firm, person_type, currency=currency)
    return Context().new(
        name=(service.name + "_CONTEXT"),
        service=service,
        person_type=PersonTypes.PersonType(code=person_type),
        firm=firm,
        currency=currency,
        payment_currency=test_input.payment_currency,
        contract_type=ContractSubtype[contract['ctype']],
        contract_services=contract_services,
        tpt_payment_type=test_input.payment_method,
        tpt_paysys_type_cc=test_input.tpt_paysys_type_cc,  # TODO: from where to take?
        special_contract_params=contract.get('special_contract_params'),
        nds=test_input.nds,
        invoice_type=test_input.invoice_type,
        paysys=paysys,
        paysys_wo_nds=paysys,
        params=contract.get('_params'),
        pad_type_id=test_input.pad_type_id,
    )


def get_paysys_by_firm_and_person(firm, person_type, currency=None):
    if currency is None:
        query = 'SELECT * FROM bo.t_paysys WHERE firm_id=:firm_id and cc=:person_type'
        params = {'firm_id': firm.id, 'person_type': person_type}
    else:
        query = 'SELECT * FROM bo.t_paysys WHERE firm_id=:firm_id and category=:person_type and currency=:currency'
        params = {'firm_id': firm.id, 'person_type': person_type, 'currency': currency.char_code}
    result = db.balance().execute(query, params)
    if not result:
        return None

    result = result[0]
    id = result['id']
    name = result['name']
    instant = result['instant']
    currency = getattr(Currencies, result['iso_currency'], None)

    return Paysys(id=id, currency=currency, firm=firm, name=name, instant=instant)


def get_invoice_eid_from_service(contract_id, service):
    invoice_id, invoice_eid, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(
        contract_id,
        service=service
    )
    return invoice_id, invoice_eid


def get_main_product_id_by_query(query, context):
    product_mapping_config = steps.CommonPartnerSteps.get_product_mapping_config(context.service)
    product_cfg = query.split('.')
    main_product_id = product_mapping_config[product_cfg[0]][context.payment_currency.iso_code][product_cfg[1]]
    return main_product_id


def get_allowed_person_types(person):
    if not isinstance(person, dict):
        if isinstance(person, set):
            person = list(person)
        if isinstance(person, str):
            person = [person]
        return person

    person_type = person.get('type')
    if not person_type:
        return []
    if not isinstance(person_type, dict):
        return [person_type]
    person_types_list = PERSON_TYPES_LIST

    mandatory = person_type.get('mandatory')
    if mandatory:
        if not isinstance(mandatory, list):
            mandatory = [mandatory]
        person_types_list = mandatory

    forbidden = person_type.get('forbidden')
    if forbidden:
        if not isinstance(forbidden, list):
            forbidden = [forbidden]
        for forbidden_type in forbidden:
            if forbidden_type in person_types_list:
                person_types_list.remove(forbidden_type)

    return person_types_list


def get_firm_by_id(firm_id):
    if isinstance(firm_id, list):
        firm = FIRM_BY_ID.get(firm_id[0])
    else:
        firm = FIRM_BY_ID.get(firm_id)

    if firm:
        return firm

    firm = steps.CommonSteps.get_firm_by_id(firm_id)
    return Firms.Firm(id=firm_id, name=firm['title'],
                      inn=firm['inn'], oebs_org_id=firm['oebs_org_id'])


def get_page_info_by_service_id(service_id):
    query = 'SELECT * FROM bo.t_partner_page WHERE service_id=:service_id'
    query_params = {'service_id': service_id}
    result = db.balance().execute(query, query_params)
    if not result:
        query = 'SELECT * FROM bo.t_page_data WHERE service_id=:service_id'
        query_params = {'service_id': service_id}
        result = db.balance().execute(query, query_params)[0]
        return Pages.Page(id=result['page_id'], desc=result['DESC'], payment_type=result['payment_type'])
    else:
        result = result[0]
        page_id = result['page_id']
        query = 'SELECT * FROM bo.t_page_data WHERE page_id=:page_id'
        query_params = {'page_id': page_id}
        desc = db.balance().execute(query, query_params)[0]['DESC']
        return Pages.Page(id=page_id, desc=desc, payment_type=result['payment_type'])


def get_service_by_id(service_id):
    service = SERVICE_BY_ID.get(service_id)
    if service:
        return service

    details = steps.CommonSteps.get_service_by_id(service_id)
    if not details:
        return None
    return Services.Service(id=service_id, name=details['name'], token=details['token'])


def get_row_by_config_id(config_id):
    query = "SELECT * FROM bo.t_integrations_configuration WHERE id=:id"
    query_params = {'id': config_id}
    result = db.balance().execute(query, query_params)
    if result:
        return result[0]
    return result


def get_row_by_configuration_cc(configuration_cc):
    query = "SELECT * FROM bo.t_integrations_configuration WHERE cc=:cc"
    query_params = {'cc': configuration_cc}
    result = db.balance().execute(query, query_params)
    if result:
        return result[0]
    return result


def get_all_configs():
    query = "SELECT * FROM bo.t_integrations_configuration WHERE JSON_EXISTS(scheme, '$.tests')"
    result = db.balance().execute(query)
    return result


def create_contract(context, partner_integration_params, start_dt, is_postpay=1, selfemployed=None):
    additional_params = dict(start_dt=start_dt)
    if selfemployed is not None:
        additional_params['selfemployed'] = selfemployed

    client_id, person_id, contract_id, _ = steps.ContractSteps. \
        create_partner_contract(context,
                                additional_params=additional_params,
                                partner_integration_params=copy.deepcopy(partner_integration_params),
                                is_postpay=is_postpay
                                )
    return client_id, person_id, contract_id


# проверяем schema_name на trust_pricing_without_partner, нужно для сервисов у которых service fee None
def get_amounts_and_nds_by_service_schema(service_id, amounts, fiscal_nds_list):
    schema = steps.CommonSteps.get_trust_service_by_id(service_id)
    if schema and schema['schema_name'] == 'trust_pricing_without_partner':
        # TODO: reassert what to do with scheme_name (prices=[None])
        amounts = [None]
        fiscal_nds_list = [None]
    return amounts, fiscal_nds_list


def get_partner_integration_params(config):
    if ('integration_cc' in config) and ('configuration_cc' in config):
        return {
            'link_integration_to_client': 1,
            'link_integration_to_client_args': {
                'integration_cc': config['integration_cc'],
                'configuration_cc': config['configuration_cc'],
            },
            'set_integration_to_contract': 1,
            'set_integration_to_contract_params': {
                'integration_cc': config['integration_cc'],
            }
        }
    else:
        return None
