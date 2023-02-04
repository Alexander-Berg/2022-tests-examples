# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

# from builtins import dict
from future import standard_library

standard_library.install_aliases()

import btestlib.passport_steps as passport_steps
from btestlib.constants import Users
import balance.balance_db as db
from simpleapi.common.utils import call_http_raw
from balance import balance_steps as b_steps
from btestlib import utils as utils
from hamcrest import equal_to, is_not
from btestlib.matchers import has_entries_casted
import btestlib.environments as env

from btestlib.data.snout_defaults import CommonDefaults, ContractDefaults


HEADERS = {
    'GET': {'Accept': 'application/json', 'X-Is-Admin': 'true'},
    'POST': {
        'Accept': 'application/json',
        'X-Is-Admin': 'true',
        'Content-Type': 'application/x-www-form-urlencoded',
    },
    'OPTIONS': {
        'Accept': 'application/json',
        'X-Is-Admin': 'true',
        'Content-Type': 'application/json',
    },
}
ERORR_KEYS = [u'description', u'error']
EXPECTED_RESPONSE_KEYS = [u'data', u'version']


def get_handle_url(handle, object_id, additional_params):
    url = u'{base_url}/{handle}'.format(base_url=env.balance_env().snout_url, handle=handle)
    if object_id:
        url = url.format(object_id=object_id)
    if additional_params:
        for param, value in additional_params.items():
            url += u'&{param}={value}'.format(param=param, value=value)
    return url


def get_error_data(error, object_id):
    utils.check_that(sorted(error.keys()), equal_to(ERORR_KEYS))
    error['description'] = error['description'].format(object_id=object_id)
    return error


def pull_handle(handle, user=None, method='GET', object_id=None, handle_params=None,
                call_params=None, custom_headers=None, json_data=None):
    session = passport_steps.auth_session(user=user or Users.YB_ADM)

    method_url = get_handle_url(handle, object_id, handle_params)
    headers = HEADERS[method].copy()
    if custom_headers:
        headers.update(custom_headers)
    response, result = call_http_raw(method_url, method=method, headers=headers,
                                     cookies=dict(Session_id=session.cookies['Session_id'],
                                                  sessionid2=session.cookies['sessionid2']),
                                     params=call_params,
                                     json_data=json_data)
    return response, result


def pull_handle_and_check_result(handle, object_id=None, additional_params=None, status_code=200,
                                 expected_error=None, custom_headers=None, json_data=None, method='GET',
                                 user=None):
    status, data = pull_handle(handle, object_id=object_id, handle_params=additional_params,
                               custom_headers=custom_headers, json_data=json_data, method=method, user=user)

    utils.check_that(status.status_code, equal_to(status_code))
    if expected_error:
        error_data = get_error_data(expected_error, object_id)
        utils.check_that(data, has_entries_casted(error_data))

    else:
        utils.check_that(sorted(data.keys()), equal_to(EXPECTED_RESPONSE_KEYS))
        utils.check_that(len(data), is_not(0))

    return data


def create_client_person(person_type=CommonDefaults.PERSON_TYPE):
    client_id = b_steps.ClientSteps.create()
    person_id = b_steps.PersonSteps.create(client_id, person_type)
    return client_id, person_id


def create_contract(client_id=None, person_id=None, person_type=CommonDefaults.PERSON_TYPE, firm_id=CommonDefaults.FIRM,
                    service=CommonDefaults.SERVICE, contract_type=ContractDefaults.CONTRACT_TYPE,
                    contract_dt=ContractDefaults.CONTRACT_DT,
                    payment_type=ContractDefaults.CONTRACT_PAYMENT_TYPE,
                    currency=ContractDefaults.CONTRACT_CURRENCY):
    if not client_id or not person_id:
        client_id, person_id = create_client_person(person_type)
    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': contract_dt,
        'IS_SIGNED': contract_dt,
        'FIRM': firm_id,
        'SERVICES': [service],
        'PAYMENT_TYPE': payment_type,
        'CURRENCY': currency,
    }
    return client_id, person_id, b_steps.ContractSteps.create_contract_new(contract_type, contract_params)[0]


def create_edo_collateral(contract_dt=ContractDefaults.CONTRACT_DT, collateral_type=ContractDefaults.COLLATERAL_TYPE):
    client_id, person_id, contract_id = create_contract()
    collateral_params = {
        'CONTRACT2_ID': contract_id,
        'DT': contract_dt,
        'FINISH_DT': contract_dt,
        'IS_SIGNED': contract_dt,
    }
    b_steps.ContractSteps.create_collateral(collateral_type, collateral_params)
    return client_id, person_id, contract_id


def get_firm_inn_kpp(firm_id=CommonDefaults.FIRM):
    firm_inn_kpp = db.balance().execute(
        """
        select max(rec.inn) as inn, max(rec.kpp) as kpp, f.id
        from mv_yandex_recs rec
          full join t_firm_export fe on fe.oebs_org_id=rec.org_id
          inner join t_firm f on f.id = fe.firm_id
        where f.id = :firm_id group by f.id
        """,
        {
            'firm_id': firm_id,
        },
    )[0]

    return firm_inn_kpp['inn'], firm_inn_kpp['kpp']


def create_edo_person(firm_id=CommonDefaults.FIRM, dt=CommonDefaults.DT):
    client_id, person_id = create_client_person()
    b_steps.PersonSteps.accept_edo(person_id, firm_id, dt)
    inn, kpp = get_firm_inn_kpp()
    return client_id, person_id, inn, kpp


def create_invoice(is_contract=None, is_credit=0, product_id=CommonDefaults.PRODUCT, qty=CommonDefaults.QTY,
                   service_id=CommonDefaults.SERVICE, paysys_id=CommonDefaults.PAYSYS):
    client_id, person_id = create_client_person()
    campaigns_list = [
        {'client_id': client_id, 'service_id': service_id, 'product_id': product_id, 'qty': qty}]
    contract_id = create_contract(client_id, person_id) if is_contract else None
    invoice_id, _, _, orders_list = b_steps.InvoiceSteps.create_force_invoice(client_id, person_id, campaigns_list,
                                                                              paysys_id=paysys_id, credit=is_credit,
                                                                              contract_id=contract_id)
    return client_id, person_id, invoice_id, orders_list


def do_shipment(service_id=CommonDefaults.SERVICE, qty=CommonDefaults.SHIPMENT_QTY, dt=CommonDefaults.DT):
    client_id, person_id, invoice_id, orders_list = create_invoice()
    b_steps.InvoiceSteps.pay(invoice_id)
    b_steps.CampaignsSteps.do_campaigns(service_id, orders_list[0]['ServiceOrderID'], {'Bucks': qty}, 0,
                                        campaigns_dt=dt)
    return client_id, invoice_id


def create_act(dt=CommonDefaults.DT):
    client_id, invoice_id = do_shipment()
    act_id = b_steps.ActsSteps.generate(client_id, 1, dt)[0]
    return client_id, invoice_id, act_id
