# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import balance.balance_db as db
import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.constants import Firms, ContractPaymentType, ContractCommissionType
from btestlib.data.defaults import Order
from temp.igogor.balance_objects import Contexts
import pagediff_steps

DIRECT_NON_RESIDENT = Contexts.DIRECT_FISH_YT_RUB_CONTEXT
MARKET_RESIDENT = Contexts.MARKET_RUB_CONTEXT

FIRM_ID = Firms.YANDEX_1.id
CONTRACT_PAYMENT_TYPE = ContractPaymentType.POSTPAY
CONTRACT_COMMISSION_TYPE = ContractCommissionType.COMMISS
DT = datetime.datetime.now()
START_DT = utils.Date.nullify_time_of_date(DT).isoformat()
ORDER_QTY = 100


def create_client_person(context, is_agency=0):
    client_id = steps.ClientSteps.create({'IS_AGENCY': is_agency})
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    return client_id, person_id


def create_contract(context):
    agency_id, person_id = create_client_person(context, is_agency=1)
    contract_id, contract_ext_id = steps.ContractSteps.create_contract_new(CONTRACT_COMMISSION_TYPE,
                                                                           {'CLIENT_ID': agency_id,
                                                                            'PERSON_ID': person_id,
                                                                            'IS_FAXED': START_DT, 'DT': START_DT,
                                                                            'FIRM': FIRM_ID,
                                                                            'SERVICES': [context.service.id],
                                                                            'DEAL_PASSPORT': START_DT,
                                                                            'PAYMENT_TYPE': CONTRACT_PAYMENT_TYPE})
    return agency_id, person_id, contract_id, contract_ext_id


def create_order_list(context, orders_number=2, subclient_number=1, client_id=None, agency_id=None):
    orders_list = []
    clients_list = []
    for _ in xrange(subclient_number):
        order_owner = client_id if client_id else steps.ClientSteps.create({'IS_AGENCY': 0, 'AGENCY_ID': agency_id})
        clients_list.append(order_owner)
        for _ in xrange(orders_number):
            service_order_id = steps.OrderSteps.next_id(context.service.id)
            steps.OrderSteps.create(order_owner, service_order_id, context.product.id, context.service.id,
                                    {'TEXT': 'Py_Test order',
                                     'AgencyID': agency_id if agency_id else None,
                                     'ManagerUID': None})
            orders_list += Order.default_orders_list(service_order_id, service_id=context.service.id, qty=ORDER_QTY)
    return orders_list, clients_list


def create_request_invoice(context, client_id=None, person_id=None, orders_list=None, contract_id=None, credit=0):
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': DT})
    invoice_id, invoice_eid, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                           paysys_id=context.paysys.id,
                                                           credit=credit, contract_id=contract_id,
                                                           overdraft=0,
                                                           endbuyer_id=None)
    return invoice_id, invoice_eid


def set_docs_detailed_in_client(client_id):
    db.balance().execute('''update (select * from t_client where id  =:client_id) set is_docs_detailed =1''',
                         {'client_id': client_id})


def set_docs_detailed_in_extprops(subclient_id, agency_id):
    db.balance().execute(
        '''insert into t_extprops (id, object_id, classname, attrname, value_clob, update_dt, passport_id)
           values(s_extprops.nextval, :client_id, 'Client', 'agencies_printable_doc_types',
           :agency_id, sysdate, null)''',
        {'client_id': subclient_id, 'agency_id': '{"' + str(agency_id) + '": [true, false]}'})


# Проверяем ПФ счетов для самоходных клиенов с t_client.is_docs_detailed=1
@pytest.mark.parametrize("unique_test_name, context",
                         [
                             ('simple_market_res', MARKET_RESIDENT),
                             ('simple_direct_nonres', DIRECT_NON_RESIDENT)]
                         )
def test_simple_client(unique_test_name, context):
    client_id, person_id = create_client_person(context)
    orders_list, _ = create_order_list(context, client_id=client_id)
    set_docs_detailed_in_client(client_id)
    invoice_id, invoice_eid = create_request_invoice(context, client_id, person_id, orders_list)
    pagediff_steps.check_publish_page(unique_test_name, invoice_id, invoice_eid)


# Проверяем ПФ счетов для агентств и субклиентов в разрезе агентства
@pytest.mark.parametrize("unique_test_name, is_docs_detailed, table",
                         [
                             ('t_client.agency-false&client-true', {'client': 1, 'agency': 0}, 't_client'),
                             ('t_client.agency-true&client-false', {'client': 0, 'agency': 1}, 't_client'),
                             ('t_extprops.agency-false&client-true', {'client': 1, 'agency': 0}, 't_extprops')
                         ])
def test_agency(unique_test_name, is_docs_detailed, table, context=DIRECT_NON_RESIDENT):
    agency_id, person_id, contract_id, contract_eid = create_contract(context)
    orders_list, clients_list = create_order_list(context, agency_id=agency_id, orders_number=2)
    for object, value in is_docs_detailed.iteritems():
        if value == 1:
            if table == 't_extprops':
                set_docs_detailed_in_extprops(clients_list[is_docs_detailed.keys().index(object)],
                                              agency_id)
            else:
                set_docs_detailed_in_client(
                    agency_id if object == 'agency' else clients_list[is_docs_detailed.keys().index(object)])
    invoice_id, invoice_eid = create_request_invoice(context, client_id=agency_id, person_id=person_id,
                                                     orders_list=orders_list, contract_id=contract_id)
    pagediff_steps.check_publish_page(unique_test_name, invoice_id, invoice_eid, contract_eid)
